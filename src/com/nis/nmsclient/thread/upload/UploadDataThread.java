package com.nis.nmsclient.thread.upload;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import com.nis.nmsclient.common.Common;
import com.nis.nmsclient.common.Contants;
import com.nis.nmsclient.thread.socket.CommonSocket;
import com.nis.nmsclient.thread.socket.SSLClient;
import com.nis.nmsclient.util.DateUtil;
import com.nis.nmsclient.util.FileUtil;
import com.nis.nmsclient.util.Utils;
import com.nis.nmsclient.util.ZipUtil;

/**
 * 用于定时扫描并上传监测数据文件【数据收集方式改为DC主动后，此类废弃】
 * 	  
 **/
public class UploadDataThread implements Runnable {
	Logger logger = Logger.getLogger(UploadDataThread.class);
	private String name;

	public UploadDataThread(String name) {
		this.name = name;
	}

	public void run() {
		long startTime = System.currentTimeMillis();
		Thread.currentThread().setName(name);
		logger.debug("上传数据开始 ~~~~~~~");
		try {
			//针对数据文件过多时打包上传未完成的文件继续上传
			File parDir = new File(Contants.localDataCollection);
			if(!parDir.exists()){
				return;
			}
			File[] fileArr = FileUtil.getFilesEndWith(parDir, ".zip");
			if (fileArr != null && fileArr.length > 0) {
				for (File file : fileArr) {
					if (!file.getName().startsWith(CommonSocket.BP_TYPE_DETECT_DATA)) {
						continue;
					}
					Future<?> future = Common.service.submit(new SSLClient(name,
							CommonSocket.REQ_BP_UPLOAD_FIFE, new String[] {
							CommonSocket.BP_TYPE_DETECT_DATA,
									file.getAbsolutePath() }));
					future.get();
				}
			}
			//----------------------------------------
			
			File dataDir = new File(Contants.localDataFilePath);
			if (!dataDir.exists()) {
				logger.warn("Data directory“" + dataDir.getAbsolutePath() + "”Non-existent！！！");
				return;
			}
			
			long total = 0;
			List<File> allFiles = new ArrayList<File>();
			File[] dataDirs = FileUtil.getDirectoryArray(dataDir);
			for(File dir : dataDirs){
				File[] files = FileUtil.getFilesEndWith(dir, ".csv");
				if(files==null || files.length==0){
					continue;
				}
				files = FileUtil.sortASCByModify(files); // 修改日期升序排序
				//allFiles.addAll(Arrays.asList(files));
				total += files.length;
				for (File file : files) {
					if (file.length() > 0) {
						allFiles.add(file);
						continue;
					}
					//--- 处理空文件数据：移动空文件数据到指定日期目录
					String dirTime = DateUtil.getStingDate(
							DateUtil.YYYYMMDD,
							new Date(file.lastModified()));
					String newDir = Contants.localDataErrorPath
							+ File.separator
							+ file.getParentFile().getName()
							+ File.separator + dirTime;
					FileUtil.moveFile(file, newDir, true);
				}
			}
			logger.info("本次轮循数据文件总数：" + total + ", 正常数据：" + allFiles.size() + ", 空数据：" + (total-allFiles.size()));
			total = allFiles.size();//正常的要上传的数据个数
			
			//--- 将所有数据文件一起打包，发送
			if(total>Contants.COMMON_ZIP_MIN_SIZE){
				//与Server通信
				Future<?> serFuture = Common.service.submit(new SSLClient(
						Thread.currentThread().getName(),
						CommonSocket.REQ_HAND_SHAKE, null));
				if (!Contants.isSucessByResult((String) serFuture.get())) {
					logger.debug("UploadDataThread--ServerHandShake--fail return");
					return;
				}
				//压缩并删除原文件
				String compressFileStr = Contants.localDataCollection
						+ File.separator
						+ CommonSocket.addTimeTagForFileName(
								CommonSocket.BP_TYPE_DETECT_DATA, null, true) + ".zip";
				ZipUtil.zipWithDelFile(dataDirs, compressFileStr, true);
				//发送
				Future<?> future = Common.service.submit(new SSLClient(
						Thread.currentThread().getName(),
						CommonSocket.REQ_BP_UPLOAD_FIFE, new String[]{CommonSocket.BP_TYPE_DETECT_DATA, compressFileStr}));
				future.get();
				logger.info("-----本次轮循将所有数据打包上传");
			} else if (total > 0) {// -- 按正常所有监测数据批量上传
				Future<?> future = Common.service.submit(new SSLClient(name,
						CommonSocket.REQ_UPLOAD_DATAS, new Object[] {
								dataDir.getParent(), allFiles }));
				String msg = (String) future.get();
				if (Contants.isSucessByResult(msg)) {
					/**
					 * 移动上传成功的文件到指定日期目录 
					 *    完整文件到目录：.../done/type_procIden/yyyyMMdd
					 *    0大小文件到目录: .../error/type_procIden/yyyyMMdd
					 */
					for (File file : allFiles) {
						String dirTime = DateUtil.getStingDate(
								DateUtil.YYYYMMDD,
								new Date(file.lastModified()));
						String newDir = "";
						if (file.length() > 0) {
							newDir = Contants.localDataDonePath
									+ File.separator
									+ file.getParentFile().getName()
									+ File.separator + dirTime;
						} else {
							newDir = Contants.localDataErrorPath
									+ File.separator
									+ file.getParentFile().getName()
									+ File.separator + dirTime;
						}
						// -- 文件移动
						FileUtil.moveFile(file, newDir, true);
					}
				}
				logger.info("-----本次轮循上传数据总数：" + total + "，用时："
								+ (System.currentTimeMillis() - startTime) + "ms");
			}else{
				logger.info("-----本次轮循未上传数据");
			}

		} catch (Exception e) {
			logger.error("Upload data exception：" + Utils.printExceptionStack(e));
		}
		logger.debug("上传数据结束 ~~~~~~~");
	}

}