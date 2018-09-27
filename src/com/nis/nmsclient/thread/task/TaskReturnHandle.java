package com.nis.nmsclient.thread.task;

import java.io.File;
import java.io.IOException;
import java.util.List;

import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.nis.nmsclient.common.Common;
import com.nis.nmsclient.common.Contants;
import com.nis.nmsclient.model.ReturnFilePO;
import com.nis.nmsclient.thread.socket.CommonSocket;
import com.nis.nmsclient.util.CompressFileMgr;
import com.nis.nmsclient.util.FileWrUtil;
import com.nis.nmsclient.util.Utils;

public class TaskReturnHandle {
	Logger logger = Logger.getLogger(TaskReturnHandle.class);
	
	/*public void sendAllTaskReturnFile(){
		try {
			//针对回传文件过多时打包上传未完成的文件继续上传
			File taskDir = new File(Contants.localTaskPath);
			if (!taskDir.exists()) {
				return;
			}
			File[] zipArr = FileUtil.getFilesEndWith(taskDir, ".zip");
			if (zipArr != null && zipArr.length > 0) {
				for (File file : zipArr) {
					if (!file.getName().startsWith(CommonSocket.BP_TYPE_TASK_RETURN)) {
						continue;
					}
					Future<?> future = Common.service.submit(new SSLClient(Thread.currentThread()
							.getName(), CommonSocket.REQ_BP_UPLOAD_FIFE,
							new String[] { CommonSocket.BP_TYPE_TASK_RETURN,
									file.getAbsolutePath() }));
					future.get();
				}
			}
			//----------------------------------
			
			File returnDir = new File(Contants.localTaskReturnPath);
			if(!returnDir.exists()){
				return;
			}
			File[] fileArr = FileUtil.getFilesEndWith(returnDir, Contants.TASK_RETURN_FILE_SUFFIX);
			if(fileArr == null || fileArr.length == 0){
				return;
			}
			//--- 将所有任务的回传文件及回传信息保存文件一起打包，发送
			if(fileArr.length > Contants.COMMON_MAX_RETURN_CNT){
				//与Server通信
				Future<?> serFuture = Common.service.submit(new SSLClient(
						Thread.currentThread().getName(),
						CommonSocket.REQ_HAND_SHAKE, null));
				if (!Contants.isSucessByResult((String) serFuture.get())) {
					return;
				}
				//压缩并删除原文件
				String compressFileStr = Contants.localTaskPath
						+ File.separator
						+ CommonSocket.addTimeTagForFileName(CommonSocket.BP_TYPE_TASK_RETURN,
								null)
						+ ".zip";
				ZipUtil.zipWithDelFile(returnDir.listFiles(), compressFileStr, false);
				//发送
				Future<?> future = Common.service.submit(new SSLClient(Thread.currentThread()
						.getName(), CommonSocket.REQ_BP_UPLOAD_FIFE,
						new String[] { CommonSocket.BP_TYPE_TASK_RETURN,
								compressFileStr }));
				future.get();
			}else{//-- 按正常的一个任务一个任务的回传
				fileArr = FileUtil.sortASCByModify(fileArr); //修改日期升序排序
				for(File file : fileArr){
					if(!file.exists() || !file.isFile()){
						continue;
					}
					String[] resultArr = FileWrUtil.cfgFileReader(file);
					if (resultArr != null && resultArr.length > 0) {
						JSONObject jsonObject = JSONObject.fromObject(resultArr[0]);
						ReturnFilePO rfPo = (ReturnFilePO) JSONObject.toBean(jsonObject, ReturnFilePO.class);
						sendTaskReturnFile(rfPo);
					}else{
						//file.delete_bak();
						//使用删除文件公共方法
						FileUtil.delDir(file);
						logger.debug("TaskReturnHadle.sendAllTaskReturnFile()()任务回传临时文件（记录任务结果信息）删除："+file.getAbsolutePath());
						
					}
				}
			}
		} catch (Exception e) {
			logger.error(Utils.printExceptionStack(e));
		}
	}*/
	
	public static void startTaskReturnFileThread(final ReturnFilePO rfPo, final List<String> filePaths) {
		Common.service.execute(new Runnable() {
			@Override
			public void run() {
				Thread.currentThread().setName("Backpass File Processing ID:" + rfPo.getTaskId());
				TaskReturnHandle tReturnHandle = new TaskReturnHandle();
				ReturnFilePO rfPo2 = tReturnHandle.taskReturnFileHandle(rfPo, filePaths);
				//tReturnHandle.sendTaskReturnFile(rfPo2);
			}
		});
	}
	
	/**
	 * 回传文件的统一处理：将回传文件移动到统一回传位置，处理后一个任务只有一个回传文件，修改回传文件名，将任务回传信息写入文件
	 */
	public ReturnFilePO taskReturnFileHandle(ReturnFilePO rfPo, List<String> filePaths){
		if(rfPo==null || filePaths==null || filePaths.size()<=0){
			logger.warn("No return file, no return");
			return null;
		}
		File tempDir = null;
		try {
			File file = new File(getTaskReturnFile(rfPo.getTaskType(),
					rfPo.getTaskId(), rfPo.getIsLoop()));
			if (file.exists() && file.length()>0) {// 如果存在，说明之前已经整理过回传文件了，本次不做操作
				return null;
			}else if(!file.getParentFile().exists()){// 创建回传文件目录
				file.getParentFile().mkdirs();
			}
			
			StringBuffer sb = new StringBuffer();
			tempDir = new File(Contants.localTempPath + File.separator
					+ "return_" + rfPo.getTaskId());
			if(!tempDir.exists()){
				tempDir.mkdirs();
			}
			/**
			 * 处理回传文件：1、过滤不存在或文件路径为空的文件 2、对文件重命名移动到临时目录
			 */ 
			for(String returnPath: filePaths){
				if (StringUtils.isEmpty(returnPath)) {
//					sb.append("回传“" + returnPath + "”失败，回传文件路径值为空;");
//					sb.append("Backpass“" + returnPath + "” failed, the return file path value is empty;");
					sb.append("i18n_client.TaskReturnHandle.backpass_n81i“" + returnPath + "” i18n_client.TaskReturnHandle.backFail1_n81i;");
					continue;
				}
				File returnFile = new File(returnPath);
				if (!returnFile.exists()) {
//					sb.append("Backpass“" + returnPath + "” failed, return file does not exist;");
					sb.append("i18n_client.TaskReturnHandle.backpass_n81i“" + returnPath + "” i18n_client.TaskReturnHandle.backFail2_n81i;");
					continue;
				}
				//回传文件取别名:原文件名_T任务ID_时间戳
				String aliasName = CommonSocket.addTimeTagForFileName(
							returnFile.getName(), "T" + rfPo.getTaskId(), returnFile.isFile());
				//---- 移动回传文件到临时目录，并重命名为:原文件名_T任务ID_时间戳
				File returnTmpFile = new File(tempDir.getCanonicalPath()
						+ File.separator
						+ aliasName);
				if (returnFile.isDirectory()) {
						FileUtils.copyDirectory(returnFile, returnTmpFile);
				} else {
					FileUtils.copyFile(returnFile, returnTmpFile);
				}
			}
			
			/**
			 * 将回传的所有文件压缩放到统一回传位置
			 */
			String returnFileName = null;
			//---- 若回传文件的个数等于1，则把临时目录下的一个文件：按文件夹的话压缩到统一回传位置，文件的话直接移动到统一回传位置
			File[] files = tempDir.listFiles();
			if(files.length==1){
				returnFileName = files[0].getName();
				File destFile = new File(Contants.localTaskReturnPath + File.separator + files[0].getName());
				if (files[0].isDirectory()) {
					String compressFileStr = destFile.getAbsolutePath() + CompressFileMgr.getCompressSuffixByOs(false);
					returnFileName = new File(compressFileStr).getName();
					new CompressFileMgr().compressFile(files[0]
							.getAbsolutePath(), compressFileStr, null, false);
				} else {
					FileUtils.copyFile(files[0], destFile, false);// 复制并修改文件日期
				}
			}else if(files.length > 1){//---- 若回传文件的个数大于1，则把临时目录整个压缩到统一回传位置
				String compressFileStr = Contants.localTaskReturnPath + File.separator + tempDir.getName() + CompressFileMgr.getCompressSuffixByOs(false);
				returnFileName = new File(compressFileStr).getName();
				new CompressFileMgr().compressFile(tempDir
						.getAbsolutePath(), compressFileStr, null, false);	
			}
			
			rfPo.setReturnFileName(returnFileName);
			rfPo.setResDesc(sb.toString());
			
			/**
			 * 将信息写入文件
			 */
			if (!file.exists()) {
				String[] values = new String[] { JSONObject.fromObject(
						rfPo).toString() };
				FileWrUtil.cfgFilePrinter(file, Contants.charset,
						values);
			}
			
			return rfPo;
		} catch (Exception e) {
			logger.error("Handling backpass file exceptions：" + Utils.printExceptionStack(e));
			return null;
		} finally {
			if(tempDir!=null && tempDir.exists()){
				try {
					logger.debug("删除临时目录--" + tempDir.getAbsolutePath());
					FileUtils.deleteDirectory(tempDir);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	/**
	 * 回传文件：先将信息写入临时文件，再判断与Server端通信是否成功
	 *    1、通信成功：回传文件，发送最终结果；删除临时文件
	 *    2、通信失败：保留临时文件，直接返回
	 * @param rfPo 回传文件实体类
	 */
	/*public void sendTaskReturnFile(ReturnFilePO rfPo) {
		if(rfPo==null){
			logger.debug("回传文件实体对象为空, 不用回传");
			return;
		}
		try {
			File file = new File(getTaskReturnFile(rfPo.getTaskType(),
					rfPo.getTaskId()));
			
			//--回传文件名和回传描述信息均为空时，则无回传文件
			if(StringUtil.isEmpty(rfPo.getReturnFileName()) && StringUtil.isEmpty(rfPo.getResDesc())){
				logger.warn("无回传文件, 不用回传");
				if(file.exists()){
					//file.delete_bak();
					//使用删除文件公共方法
					FileUtil.delDir(file);
					logger.debug("TaskReturnHandle.sendTaskReturnFile()成功发送任务回传文件后删除："+file.getAbsolutePath());					
				}
				return;
			}
			//--回传文件名为空，但回传描述信息不为空，则进行步骤4发送任务结果、删除文件
			
			*//**
			 * 步骤1、将信息写入文件
			 *//*
			if (!file.exists()) {
				String[] values = new String[] { JSONObject.fromObject(
						rfPo).toString() };
				FileWrUtil.cfgFilePrinter(file, Contants.charset,
						values);
			}
			*//**
			 * 步骤2、与Server通信
			 *//*
			Future<?> serFuture = Common.service.submit(new SSLClient(
					Thread.currentThread().getName(),
					CommonSocket.REQ_HAND_SHAKE, null));
			if (!Contants.isSucessByResult((String) serFuture.get())) {
				return;
			}
			*//**
			 * 步骤3、回传文件
			 *//*
			StringBuffer sb = new StringBuffer();
			if(rfPo.getResDesc()!=null){//取已有的结果描述信息
				sb.append(rfPo.getResDesc());
			}
			//准备回传文件，回传文件名不为空即有回传的文件时，再回传
			boolean success = false;
			if(rfPo.getReturnFileName()!=null && rfPo.getReturnFileName().trim().length()>0){
				for(int i=0; i<Contants.max_times; i++){//回传失败，尝试几次
					Future<?> future = Common.service.submit(new SSLClient(
							Thread.currentThread().getName(),
							CommonSocket.REQ_TASK_RETURNFILE, rfPo));
					String msg = (String) future.get();
					success = Contants.isSucessByResult(msg);
					if(success){
						sb.append("回传成功;");
						break;
					}
							
					try {// 如果更新失败，让当前线程暂停几秒，再重试
						Thread.sleep(1000 * Contants.max_delay_seconds);
					} catch (InterruptedException e) {
						logger.error(Utils.printExceptionStack(e));
						continue;
					}
				}
			}else{
				success = true;
			}
			
			*//**
			 * 步骤4、判断文件是否回传完成
			 *//*
			if(success){
				*//**
				 * 步骤4-1、发送任务结果
				 *//*
				TaskResultOper.sendTaskResult(rfPo.getTaskId(), rfPo.getTaskType(),
						rfPo.getState(), sb.toString(), "", rfPo.getStartTime(),
						rfPo.getEndTime(), false, rfPo.getIsLoop());
				*//**
				 * 步骤4-2、删除保存回传文件信息的文件
				 *//*
				if(file.exists()){
					//file.delete_bak();
					//使用删除文件公共方法
					FileUtil.delDir(file);
					logger.debug("TaskReturnHandle.sendTaskReturnFile()删除保存回传文件信息的文件："+file.getAbsolutePath());
				}
				*//**
				 * 步骤4-3、正常回传完成，删除文件
				 *//*
				File curReturnFile = new File(Contants.localTaskReturnPath + File.separator + rfPo.getReturnFileName());
				if(curReturnFile.exists()){
					//curReturnFile.delete_bak();
					//使用删除文件公共方法
					FileUtil.delDir(curReturnFile);
					logger.debug("TaskReturnHandle.sendTaskReturnFile()正常回传完成，删除文件："+curReturnFile.getAbsolutePath());
				}
			}
		} catch (Exception e) {
			logger.error(Utils.printExceptionStack(e));
		}
	}*/
	
	/**
	 * 取任务回传临时记录任务信息文件的文件名: 若是周期任务，加时间后缀taskreturn_taskId_taskType_time.return；若未周期任务, taskreturn_taskId_taskType.return
	 */
	public static String getTaskReturnFile(long taskType, long taskId, long isLoop) {
		//isLoop 是否周期任务: 0 非周期， 1 周期
		if(isLoop==0){
			return Contants.localTaskReturnPath + File.separator + "tasktype"
					+ taskType + "_" + taskId
					+ Contants.TASK_RETURN_FILE_SUFFIX;
		}else{
			return Contants.localTaskReturnPath + File.separator + "tasktype"
					+ taskType + "_" + taskId + "_"
					+ System.currentTimeMillis()
					+ Contants.TASK_RETURN_FILE_SUFFIX;
		}
		
	}
}
