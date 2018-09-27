package com.nis.nmsclient.thread.upload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.nis.nmsclient.common.Common;
import com.nis.nmsclient.common.Contants;
import com.nis.nmsclient.common.StopWatch;
import com.nis.nmsclient.model.ReturnFilePO;
import com.nis.nmsclient.thread.socket.CommonSocket;
import com.nis.nmsclient.thread.socket.SSLCertOper;
import com.nis.nmsclient.thread.task.TaskResultOper;
import com.nis.nmsclient.util.DateUtil;
import com.nis.nmsclient.util.FileUtil;
import com.nis.nmsclient.util.FileWrUtil;
import com.nis.nmsclient.util.StringUtil;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

import net.sf.json.JSONObject;

/**
 * 数据发送线程
 * 1、暂时只发送监测数据，csv或zip
 * 2、间隔 10 秒启动一次，同时具备心跳的功能
 * 3、实现断线重连
 * @author dell
 *
 */
public class DataSendThread implements Runnable{
	public static final Logger logger = Logger.getLogger(DataSendThread.class);
	private Socket socket;
	private OutputStream os;
	private InputStream is;
	private String host;
	private int port;
	
	public DataSendThread(String host,int port) {
		this.host = host;
		this.port = port;
	}
	
	
	public void init() throws Exception {
		SSLContext ctx = SSLCertOper.getSSLContext();
		SSLSocketFactory ssf = ctx.getSocketFactory();
		socket = (SSLSocket) ssf.createSocket(host,port);
		logger.debug("create socket success.");
		
		//2014-1-23 hyx 如果建立socket成功，但是startHandshake握手失败，且未设置超时时间时，则会一直阻塞
		socket.setSoTimeout(1000 * 60 * Contants.SOCKET_TIMEOUT_MINUTES);
		((SSLSocket) socket).startHandshake();
		logger.debug("handshake success.");
		os = socket.getOutputStream();
		is = socket.getInputStream();
	}
	
	
	@Override
	public void run() {
		Thread.currentThread().setName("DataSendThread");
		logger.debug("开始");
		StopWatch sw = new StopWatch();
		sw.start();
		try {
			//重连
			if(socket == null || socket.isClosed()|| !socket.isConnected()|| os == null || is ==null ){
				init();
			}
			//发送心跳
			sendHeartBeat();
			//发送监测数据
			sendDetectData();
			//发送任务回传文件
			sendTaskReturnFile();
			//发送任务结果
			sendTaskResult();
			
		} catch (SSLException | SocketException e){
			logger.error("SocketException|SSLException",e);
			try {
				init();
			} catch (Exception e1) { }
		} catch (Exception e) {
			logger.error("",e);
		}
		sw.end();
		logger.debug("耗时："+sw.toString(sw.total()));
		logger.debug("结束");
	}

	
	
	/**
	 * 发送监测数据
	 */
	private void sendDetectData(){
		StopWatch sw = new StopWatch();
		try {
			File dataDir = new File(Contants.localDataFilePath);
			if (!dataDir.exists()) {
				logger.warn("数据目录 : " + dataDir.getAbsolutePath() + " 不存在！！！");
			} else{
				long total = 0;
				List<File> allFiles = new ArrayList<File>();
				File[] dataDirs = FileUtil.getDirectoryArray(dataDir);
				// ---- 数据处理
				total = handleNullDataFile(allFiles, dataDirs);
				logger.info("本次收集监测数据文件总数：" + total + ", 正常数据：" + allFiles.size() + ", 空数据：" + (total - allFiles.size()));
				total = allFiles.size();// 正常的要上传的数据个数
				
				if(total > 0){
					for (int i = 0; i < total; i++) {
						File tem = allFiles.get(i);
						this.sendData(tem, BusinessType.DetectData);
						String res = this.readLine();
						if(CommonSocket.SUCCESS.equalsIgnoreCase(res)){//发送成功
							this.moveDetecDataToDateDir(tem);
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("",e);
		}
		sw.end();
		logger.debug("耗时："+sw.toString(sw.total()));
		logger.debug("结束");
	}
	
	
	/**
	 * 发送任务结果
	 */
	private void sendTaskResult() {
		StopWatch sw = new StopWatch();
		try {
			logger.debug("传送任务结果开始 ~~~~~~~");
			long startTime = System.currentTimeMillis();
			// ==  1、针对结果文件过多时打包上传未完成的文件继续上传
			File taskDir = new File(Contants.localTaskPath);
			if (!taskDir.exists()) {
				return;
			}
			
			// ==  2、检查当前结果文件数量，批量发送文件或打包上传
			File resultDir = new File(TaskResultOper.getTaskResultPath());
			if(!resultDir.exists()){
				return;
			}
			// ==  3 
			File[] fileArr = FileUtil.getFilesEndWith(resultDir, Contants.TASK_RESULT_FILE_SUFFIX);
			fileArr = FileUtil.sortASCByFileName(fileArr); 
			List<String> results = new LinkedList<String>();
			StringBuffer sb = new StringBuffer();
			for(File file : fileArr){
				sb.delete(0, sb.length());
				if(!file.exists() || !file.isFile()){
					continue;
				}
				String[] resultArr = FileWrUtil.cfgFileReader(file);
				if(resultArr!=null && resultArr.length>0){
					for(String res : resultArr){
						sb.append(res + ";");
					}
					sb.deleteCharAt(sb.length()-1);
					results.add(sb.toString());
				}
			}
			if(results.size() >0 ){
				logger.debug("sendTaskResult-->" + Arrays.toString(results.toArray()));
				//发送任务结果请求
				this.sendObject(results, BusinessType.ObjTaskResult);
				String res = this.readLine();
				if (CommonSocket.SUCCESS.equalsIgnoreCase(res)) {
					// 移动上传成功的任务结果到指定日期目录
					moveTaskResultToDateDir(fileArr);
				}
			}
			sw.end();
			logger.info("本次收集传送任务结果总数：" + fileArr.length + "，用时：" + sw.toString(sw.total()));
		} catch (Exception e) {
			logger.error("",e);
		}
		logger.debug("结束");
	}
	
	
	private void sendTaskReturnFile(){
		StopWatch sw = new StopWatch();
		logger.debug("传送回传文件开始 ~~~~~~~");
		try {
			// ==  1、针对回传文件过多时打包上传未完成的文件继续上传
			File taskDir = new File(Contants.localTaskPath);
			if (!taskDir.exists()) {
				return;
			}
			
			// ==  2、检查当前回传文件数量，单个发送文件或打包上传
			File returnDir = new File(Contants.localTaskReturnPath);
			if(!returnDir.exists()){
				return;
			}
			File[] fileArr = FileUtil.getFilesEndWith(returnDir, Contants.TASK_RETURN_FILE_SUFFIX);
			if(fileArr == null || fileArr.length == 0){
				return;
			}
			
			// ==  3、发送
			fileArr = FileUtil.sortASCByModify(fileArr); //修改日期升序排序
			for(File file : fileArr){
				if(!file.exists() || !file.isFile()){
					continue;
				}
				String[] resultArr = FileWrUtil.cfgFileReader(file);
				if (resultArr == null || resultArr.length <= 0) {
					continue;
				}
				JSONObject jsonObject = JSONObject.fromObject(resultArr[0]);
				ReturnFilePO rfPo = (ReturnFilePO) JSONObject.toBean(jsonObject, ReturnFilePO.class);
				
				//--回传文件名和回传描述信息均为空时，则无回传文件
				if(StringUtil.isEmpty(rfPo.getReturnFileName()) && StringUtil.isEmpty(rfPo.getResDesc())){
					logger.warn("无回传文件, 不用回传");
					FileUtil.delDir(file);
					continue;
				}
				//--回传文件名为空，但回传描述信息不为空，则进行步骤2发送任务结果、删除文件
				/**
				 * 步骤1、回传文件
				 */
				StringBuffer sb = new StringBuffer();
				if(rfPo.getResDesc()!=null){//取已有的结果描述信息
					sb.append(rfPo.getResDesc());
				}
				//准备回传文件，回传文件名不为空即有回传的文件时，再回传
				if(rfPo.getReturnFileName()!=null && rfPo.getReturnFileName().trim().length()>0){
					//发送回传文件请求
					String msg = TaskResultOper.getTaskResultMsg(rfPo
							.getTaskId(), rfPo.getTaskType(), null, null, null, rfPo
							.getStartTime(), rfPo.getEndTime(), rfPo.getIsLoop());
					File returnFile = new File(Contants.localTaskReturnPath + File.separator + rfPo.getReturnFileName());
					String res = this.sendReturnFile(msg, returnFile, BusinessType.FileTaskReturn);
					if(CommonSocket.SUCCESS.equals(res)){
						/**
						 * 步骤2-1、发送任务结果
						 */
						TaskResultOper.sendTaskResult(rfPo.getTaskId(), rfPo.getTaskType(),
								rfPo.getState(), sb.toString(), "", rfPo.getStartTime(),
								rfPo.getEndTime(), false, rfPo.getIsLoop());
						/**
						 * 步骤2-2、移动上传成功的 保存回传文件信息的文件 和 回传文件 到指定日期目录
						 */
						moveTaskReturnToDateDir(file, rfPo.getReturnFileName());
					}
				}
			}
			sw.end();
			logger.info("本次收集传送任务回传总数：" + fileArr.length + "，用时：" + sw.toString(sw.total()));
		} catch (Exception e) {
			logger.error("send Task Return File error",e);
		}
		logger.debug("结束");
	}
	
	
	
	/**
	 * 发送心跳
	 * @throws IOException
	 */
	private void sendHeartBeat() throws IOException{
		byte[] msg = (System.currentTimeMillis()+"").getBytes();
		int len = msg.length;
		int t = BusinessType.HeartBeat.getType();
		os.write(Common.intToByteArray(len));
		os.write(0);//消息类型
		os.write(t);//业务类型
		os.write(msg);
		os.flush();
		String res = readLine();
		logger.debug("HeartBeat :" + res);
	}
	
	/***
	 * 发送任务回传文件
	 * @param msg
	 * @param file
	 * @param type
	 * @throws IOException
	 */
	private String sendReturnFile(String msg,File file,BusinessType type) throws IOException{
		String name = file.getName();
		byte[] nameByte = name.getBytes();
		if(nameByte.length > Byte.MAX_VALUE){
			throw new IllegalArgumentException("file name too long,max length is 127");
		}
		byte[] msgBytes = msg.getBytes();
		int msgLen = msgBytes.length;
		int totalLen = (int)file.length() + msgBytes.length +4;
		int t = type.getType();
		os.write(Common.intToByteArray(totalLen));
		os.write(nameByte.length);//文件名的长度
		os.write(t);//业务类型
		os.write(nameByte);//文件名
		os.write(Common.intToByteArray(msgLen));//消息头长度
		os.write(msgBytes);//消息头
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			IOUtils.copy(fis, os);//文件内容
			os.flush();
		} finally {
			if(fis != null){
				fis.close();
			}
		}
		String res = readLine();
		logger.debug("sendReturnFile :" + res);
		return res;
	}
	
	private void sendObject(Object data,BusinessType type) throws IOException{
		ByteOutputStream bos = null;
		ObjectOutputStream oos = null;
		try {
			bos = new ByteOutputStream();
			oos = new ObjectOutputStream(bos);
			oos.writeObject(data);
			byte[] bytes = bos.getBytes();
			int len = bytes.length;
			int t = type.getType();
			os.write(Common.intToByteArray(len));
			os.write(0);
			os.write(t);
			os.write(bytes);
		} finally {
			if(bos != null){
				bos.close();
			}
			if(oos != null){
				oos.close();
			}
		}
		
	}
	
	/**
	 * 发送数据
	 * @param data
	 * @param type
	 * @throws IOException
	 */
	private void sendData(byte[] data,BusinessType type) throws IOException{
		int len = data.length;
		int t = type.getType();
		os.write(Common.intToByteArray(len));
		os.write(0);
		os.write(t);
		os.write(data);
	}
	
	/**
	 * 发送数据
	 * @param file
	 * @param bt
	 * @throws IOException 
	 */
	private void sendData(File file,BusinessType bt) throws IOException{
		int len = (int)file.length();
		int t = bt.getType();
		os.write(Common.intToByteArray(len));
		os.write(0);
		os.write(t);
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			IOUtils.copy(fis, os);
			os.flush();
		} finally {
			if(fis != null){
				fis.close();
			}
		}
	}
	
	
	/**
	 * 发送文件 包括 文件名
	 * @param file
	 * @param type
	 * @throws IOException
	 */
	private void sendFile(File file ,BusinessType type) throws IOException{
		String name = file.getName();
		byte[] nameByte = name.getBytes();
		if(nameByte.length > Byte.MAX_VALUE){
			throw new IllegalArgumentException("file name too long,max length is 127");
		}
		int len = (int)file.length();
		os.write(Common.intToByteArray(len));//文件长度
		os.write(nameByte.length);//文件名长度
		os.write((byte)type.getType());//业务类型
		os.write(name.getBytes());//文件名
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			IOUtils.copy(fis, os);
			os.flush();
		} finally {
			if(fis != null){
				fis.close();
			}
		}
		
		String res = readLine();
		logger.debug("HeartBeat :" + res);
	}
	
	private String readLine() throws IOException{
		BufferedReader br = new BufferedReader(new InputStreamReader(is, Contants.charset));
		String str = br.readLine();
		logger.debug("recieve : " + str);
		return str;
	}
	
	/**
	 * 移动上传成功的数据文件到指定日期目录 
	 *    完整文件到目录：.../done/type_procIden/yyyyMMdd
	 *    0大小文件到目录: .../error/type_procIden/yyyyMMdd
	 */
	public static void moveDetecDataToDateDir(File... allFiles){
		if(allFiles==null || allFiles.length==0){
			return;
		}
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
	
	/**
	 * 遍历所有准备上传的数据文件，将空数据文件移动到指定目录，并记录所有文件总数
	 * @param allFiles 所有非空文件集合
	 * @param dataDirs 所有数据目录
	 * @return 所有文件个数（包括空文件）
	 * @throws Exception
	 */
	private long handleNullDataFile(List<File> allFiles, File[] dataDirs) throws Exception {
		long total = 0l;
		for(File dir : dataDirs){
			File[] files = FileUtil.getFilesEndWith(dir, ".csv");
			if(files==null || files.length==0){
				continue;
			}
			files = FileUtil.sortASCByModify(files); // 修改日期升序排序
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
		return total;
	}
	

	/**
	 * 移动上传成功的任务结果到指定日期目录 
	 *    完整文件到目录：.../done/result/yyyyMMdd
	 *    0大小文件到目录: .../error/result/yyyyMMdd
	 */
	public static void moveTaskResultToDateDir(File... fileArr){
		if(fileArr==null || fileArr.length==0){
			return;
		}
		for (File file : fileArr) {
			String dirTime = DateUtil.getStingDate(
					DateUtil.YYYYMMDD,
					new Date(file.lastModified()));
			String newDir = "";
			if (file.length() > 0) {
				newDir = Contants.localTaskDonePath
						+ File.separator
						+ file.getParentFile().getName()
						+ File.separator + dirTime;
			} else {
				newDir = Contants.localTaskErrorPath
						+ File.separator
						+ file.getParentFile().getName()
						+ File.separator + dirTime;
			}
			// -- 文件移动
			FileUtil.moveFile(file, newDir, true);
		}
	}
	
	/**
	 * 移动上传成功的 保存回传文件信息的文件 和 回传文件 到指定日期目录
	 * 		完整文件到目录：.../done/return/yyyyMMdd
	 *      0大小文件到目录: .../error/return/yyyyMMdd
	 */
	public static void moveTaskReturnToDateDir(File file, String returnFileName){
		String dirTime = DateUtil.getStingDate(
				DateUtil.YYYYMMDD,
				new Date(file.lastModified()));
		String newDir = Contants.localTaskDonePath
					+ File.separator
					+ file.getParentFile().getName()
					+ File.separator + dirTime;
		// -- 文件移动
		FileUtil.moveFile(file, newDir, true);// 保留任务信息的临时文件.return
		if(returnFileName!=null && !"".equals(returnFileName)){// 实际回传的文件
			File curReturnFile = new File(Contants.localTaskReturnPath + File.separator + returnFileName);
			FileUtil.moveFile(curReturnFile, newDir, true);
		}
		
	}
	
	
	public static void main(String[] args) throws Exception {
		
		DataSendThread.logger.debug("--------------开始-----------------");
		
		int threadSize = 1;//默认开10个线程
		int interval = 10;//间隔 60 s
		if(args != null&&args.length >0){
			threadSize = Integer.valueOf(args[0]);
		}
		if(args != null&&args.length >1){
			interval = Integer.valueOf(args[1]);
		}
		
		ScheduledExecutorService stp = Executors.newScheduledThreadPool(100);
		for(int i = 0;i< threadSize;i++){
			DataSendThread dst = new DataSendThread("10.0.6.108", 9527);
			stp.scheduleWithFixedDelay(dst, 0, interval, TimeUnit.SECONDS);
		}
	}
	
}
