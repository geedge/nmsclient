package com.nis.nmsclient.thread.socket;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.nis.nmsclient.common.Contants;
import com.nis.nmsclient.model.ReturnFilePO;
import com.nis.nmsclient.thread.task.TaskResultOper;
import com.nis.nmsclient.util.DateUtil;
import com.nis.nmsclient.util.FileUtil;
import com.nis.nmsclient.util.FileWrUtil;
import com.nis.nmsclient.util.StringUtil;
import com.nis.nmsclient.util.Utils;
import com.nis.nmsclient.util.ZipUtil;

/**
 * 用于定时扫描并上传监测数据文件
 * 	  
 **/
public class ServerCollectData {
	Logger logger = Logger.getLogger(ServerCollectData.class);
	private CommonSocket socket;

	public ServerCollectData(CommonSocket socket) {
		this.socket = socket;
	}

	public void sendData() {
		logger.debug("传送数据开始 ~~~~~~~");
		try {
			// 发送监测数据
			handleDetectData();
			// 发送任务结果
			handleTaskResult();
			// 发送任务回传文件
			handleTaskReturnFile();
			// 发送任务结果--针对有回传文件时写的任务结果
			handleTaskResult();
			// 结束通讯
			socket.sendMessageByChar(CommonSocket.END);

		} catch (Exception e) {
			logger.error("Transmits data anomalies：" + Utils.printExceptionStack(e));
		}
		logger.debug("传送数据结束 ~~~~~~~");
	}

	private void handleDetectData() throws Exception {
		logger.debug("传送监测数据开始 ~~~~~~~");
		long startTime = System.currentTimeMillis();
		File parDir = new File(Contants.localDataCollection);
		if(!parDir.exists()){
			return;
		}
		// == 1、针对数据文件过多时打包上传未完成的文件继续上传
		// ------------取所有未上传完成的Zip文件
		List<File> fileList = new LinkedList<File>();
		File[] fileArr = FileUtil.getFilesEndWith(parDir, ".zip");
		if (fileArr != null && fileArr.length > 0) {
			for (File file : fileArr) {
				if (!file.getName().startsWith(CommonSocket.BP_TYPE_DETECT_DATA)) {
					continue;
				}
				fileList.add(file);
			}
		}
		// ------------传送Zip文件
		if (fileList.size() > 0) {
			sendZipFile(fileList, CommonSocket.DATA_TYPE_ZIP_DETECT);
		}

		// == 2、检查当前数据文件数量，批量发送文件或打包上传
		File dataDir = new File(Contants.localDataFilePath);
		if (!dataDir.exists()) {
			logger.warn("Data directory“" + dataDir.getAbsolutePath() + "”Non-existent！！！");
		} else {
			long total = 0;
			List<File> allFiles = new ArrayList<File>();
			File[] dataDirs = FileUtil.getDirectoryArray(dataDir);
			// ---- 数据处理
			total = handleNullDataFile(allFiles, dataDirs);
			logger.info("本次收集监测数据文件总数：" + total + ", 正常数据：" + allFiles.size() + ", 空数据：" + (total - allFiles.size()));
			total = allFiles.size();// 正常的要上传的数据个数

			// --- 将所有数据文件一起打包，发送
			if (total > Contants.COMMON_ZIP_MIN_SIZE) {
				long zipCnt = total/Contants.COMMON_ZIP_MAX_SIZE;
				if (zipCnt > 0) {//2013-5-6 未上传的数据太多时，将监测数据压缩为多个文件
					for(int i=0; i<total/Contants.COMMON_ZIP_MAX_SIZE; i++){
						// 组织每次压缩的文件数组
						File[] dataFiles = new File[Contants.COMMON_ZIP_MAX_SIZE];
						int start = i * Contants.COMMON_ZIP_MAX_SIZE;
						int end = (i + 1) * Contants.COMMON_ZIP_MAX_SIZE;
						for (int j = start, k = 0; j < end; j++, k++) {
							dataFiles[k] = allFiles.get(j);
						}
						// 压缩并发送监测数据
						compressAndSendDetecData(dataFiles);
					}
				}else{
					// 压缩并发送监测数据
					compressAndSendDetecData(dataDirs);
				}
				logger.info("本次收集将所有监测数据打包传送，监测数据总数：" + total + "，用时："
						+ (System.currentTimeMillis() - startTime) + "ms");
			} else if (total > 0) {
				// -- 按正常所有监测数据批量上传
				sendCSVData(dataDir, allFiles);
				logger.info("本次收集传送监测数据总数：" + total + "，用时："
						+ (System.currentTimeMillis() - startTime) + "ms");
			} else {
				logger.info("本次收集未传送监测数据");
			}
		}
		logger.debug("传送监测数据结束 ~~~~~~~");
	}
	
	private void handleTaskResult() throws Exception {
		logger.debug("传送任务结果开始 ~~~~~~~");
		long startTime = System.currentTimeMillis();
		// ==  1、针对结果文件过多时打包上传未完成的文件继续上传
		File taskDir = new File(Contants.localTaskPath);
		if (!taskDir.exists()) {
			return;
		}
		// ------------取所有未上传完成的Zip文件
		List<File> fileList = new LinkedList<File>();
		File[] zipArr = FileUtil.getFilesEndWith(taskDir, ".zip");
		if (zipArr != null && zipArr.length > 0) {
			for (File file : zipArr) {
				if (!file.getName().startsWith(CommonSocket.BP_TYPE_TASK_RESULT)) {
					continue;
				}
				fileList.add(file);
			}
		}
		// ------------传送Zip文件
		if(fileList.size()>0){
			sendZipFile(fileList, CommonSocket.DATA_TYPE_ZIP_TASKRESULT);
		}
		
		// ==  2、检查当前结果文件数量，批量发送文件或打包上传
		File resultDir = new File(TaskResultOper.getTaskResultPath());
		if(!resultDir.exists()){
			return;
		}
		File[] fileArr = FileUtil.getFilesEndWith(resultDir, Contants.TASK_RESULT_FILE_SUFFIX);
		// -- 将所有任务结果文件一起打包，发送
		if(fileArr.length > Contants.COMMON_ZIP_MIN_SIZE){
			int zipCnt = fileArr.length/Contants.COMMON_ZIP_MAX_SIZE;
			if(zipCnt>0){//2013-5-6 未上传的结果文件太多时，将结果文件压缩为多个文件
				for(int i=0; i<fileArr.length/Contants.COMMON_ZIP_MAX_SIZE; i++){
					// 组织每次压缩的文件数组
					File[] resultFiles = new File[Contants.COMMON_ZIP_MAX_SIZE];
					int start = i * Contants.COMMON_ZIP_MAX_SIZE;
					int end = (i + 1) * Contants.COMMON_ZIP_MAX_SIZE;
					for(int j=start,k=0; j<end; j++,k++){
						resultFiles[k] = fileArr[j];
					}
					// 压缩并发送任务结果
					compressAndSendTaskResult(resultFiles);
				}
			}else{
				// 压缩并发送任务结果
				compressAndSendTaskResult(fileArr);
			}
			
			logger.info("本次收集将所有任务结果打包传送，任务结果总数：" + fileArr.length + "，用时：" + (System.currentTimeMillis() - startTime) + "ms");
		}else if(fileArr.length > 0){
			// -- 按正常的多个结果批量发送
			sendTaskResult(fileArr);
			logger.info("本次收集传送任务结果总数：" + fileArr.length + "，用时："
					+ (System.currentTimeMillis() - startTime) + "ms");
		} else {
			logger.info("本次收集未传送任务结果");
		}
		logger.debug("传送任务结果结束 ~~~~~~~");
	}
	
	private void handleTaskReturnFile() throws Exception {
		logger.debug("传送回传文件开始 ~~~~~~~");
		long startTime = System.currentTimeMillis();
		// ==  1、针对回传文件过多时打包上传未完成的文件继续上传
		File taskDir = new File(Contants.localTaskPath);
		if (!taskDir.exists()) {
			return;
		}
		// ------------取所有未上传完成的Zip文件
		List<File> fileList = new LinkedList<File>();
		File[] zipArr = FileUtil.getFilesEndWith(taskDir, ".zip");
		if (zipArr != null && zipArr.length > 0) {
			for (File file : zipArr) {
				if (!file.getName().startsWith(CommonSocket.BP_TYPE_TASK_RETURN)) {
					continue;
				}
				fileList.add(file);
			}
		}
		// ------------传送Zip文件
		if(fileList.size()>0){
			sendZipFile(fileList, CommonSocket.DATA_TYPE_ZIP_TASKRETURN);
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
		//--- 将所有任务的回传文件及回传信息保存文件一起打包，发送
		if(fileArr.length > Contants.COMMON_MAX_RETURN_CNT){
			//压缩并删除原文件
			String compressFileStr = Contants.localTaskPath
					+ File.separator
					+ CommonSocket.addTimeTagForFileName(CommonSocket.BP_TYPE_TASK_RETURN,
							null, true)
					+ ".zip";
			 // 2013-03-22 由于DC再次获取未保存任务结果这个功能的实现，现修改将任务结果和回传文件压缩时不删除文件，而是将其移动到相应的日期目录
			ZipUtil.zipWithMoveFile(returnDir.listFiles(), compressFileStr, false);
			//发送
			sendZipFile(new File(compressFileStr), CommonSocket.DATA_TYPE_ZIP_TASKRETURN);
			logger.info("本次收集将所有任务回传文件打包传送，回传文件总数：" + fileArr.length + "，用时：" + (System.currentTimeMillis() - startTime) + "ms");
		}else if(fileArr.length > 0){
			//-- 按正常的一个任务一个任务的回传
			sendTaskReturn(fileArr);
			logger.info("本次收集传送任务回传总数：" + fileArr.length + "，用时："
					+ (System.currentTimeMillis() - startTime) + "ms");
		} else {
			logger.info("本次收集未传送任务回传文件");
		}
		logger.debug("传送回传文件结束 ~~~~~~~");
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
	
	private void compressAndSendDetecData(File[] dataFiles) throws Exception{
		// 压缩并移动原文件
		String compressFileStr = Contants.localDataCollection
				+ File.separator
				+ CommonSocket.addTimeTagForFileName(
						CommonSocket.BP_TYPE_DETECT_DATA, null, true)
				+ ".zip";
		// 2013-3-29 由于压缩上传数据后，主动告警线程部分对数据检查存在问题，现将压缩后删除数据改为移动数据到日期目录 
		ZipUtil.zipWithMoveFile(dataFiles, compressFileStr, true);
		// 发送
		sendZipFile(new File(compressFileStr), CommonSocket.DATA_TYPE_ZIP_DETECT);
	}
	
	private void compressAndSendTaskResult(File[] resultFiles) throws Exception {
		//压缩并删除原文件
		String compressFileStr = Contants.localTaskPath
				+ File.separator
				+ CommonSocket.addTimeTagForFileName(CommonSocket.BP_TYPE_TASK_RESULT, null, true)
				+ ".zip";
		// 2013-03-22 由于DC再次获取未保存任务结果这个功能的实现，现修改将任务结果和回传文件压缩时不删除文件，而是将其移动到相应的日期目录
		ZipUtil.zipWithMoveFile(resultFiles, compressFileStr, true);
		//发送
		sendZipFile(new File(compressFileStr), CommonSocket.DATA_TYPE_ZIP_TASKRESULT);
	}
	
	private void compressAndSendTaskReturn(File[] returnFiles) throws Exception {
		//压缩并删除原文件
		String compressFileStr = Contants.localTaskPath
				+ File.separator
				+ CommonSocket.addTimeTagForFileName(CommonSocket.BP_TYPE_TASK_RETURN,
						null, true)
				+ ".zip";
		 // 2013-03-22 由于DC再次获取未保存任务结果这个功能的实现，现修改将任务结果和回传文件压缩时不删除文件，而是将其移动到相应的日期目录
		ZipUtil.zipWithMoveFile(returnFiles, compressFileStr, false);
		//发送
		sendZipFile(new File(compressFileStr), CommonSocket.DATA_TYPE_ZIP_TASKRETURN);
	}
	
	/**
	 * 发送打包文件：整个命令通信包装方法
	 * @param file
	 * @throws Exception
	 */
	private void sendZipFile(File file, String dataType) throws Exception {
		//打包上传文件请求
		socket.sendMessageByChar(dataType);
		socket.receiveMessageByChar();
		socket.sendMessageByChar(file.getName());//发送打包文件名
		socket.receiveMessageByChar();
		//上传打包文件
		socket.bpSendFile(file.getAbsolutePath());
		String result = socket.receiveMessageByChar();
		//上传成功后移动文件
		if(CommonSocket.SUCCESS.equalsIgnoreCase(result)){
			if(CommonSocket.DATA_TYPE_ZIP_DETECT.equalsIgnoreCase(dataType)){
				FileUtil.moveFile(file, Contants.localDataDonePath, true);
			}else if(CommonSocket.DATA_TYPE_ZIP_TASKRESULT.equalsIgnoreCase(dataType)){
				FileUtil.moveFile(file, Contants.localTaskDonePath, true);
			}else if(CommonSocket.DATA_TYPE_ZIP_TASKRETURN.equalsIgnoreCase(dataType)){
				FileUtil.moveFile(file, Contants.localTaskDonePath, true);
			}
		}
	}
	
	private void sendZipFile(List<File> fileList, String dataType) throws Exception {
		for(File file : fileList){
			sendZipFile(file, dataType);
		}
	}
	
	// 批量发送ZIP的
	/*private void sendZipFile(List<File> fileList, String parDir) throws Exception {
		//打包上传文件请求
		socket.sendMessageByChar(CommonSocket.DETECT_DATA_TYPE_ZIP);
		socket.receiveMessageByChar();
		//上传打包文件
		socket.bpSendFileByBath(fileList, parDir);
		String result = socket.receiveMessageByChar();
		//上传成功后移动文件
		if(CommonSocket.SUCCESS.equalsIgnoreCase(result)){
			for(File file : fileList){
				FileUtil.moveFile(file, Contants.localDataDonePath, true);
			}
		}
	}*/
	
	/**
	 * 批量发送CSV数据文件
	 * @param dataDir
	 * @param allFiles
	 * @throws Exception
	 */
	private void sendCSVData(File dataDir, List<File> allFiles) throws Exception {
		//发送上传数据请求
		socket.sendMessageByChar(CommonSocket.DATA_TYPE_CSV_DETECT);
		socket.receiveMessageByChar();
		//上传数据
		socket.sendFileByBath(dataDir.getParent(), allFiles);
		String result = socket.receiveMessageByChar();
		if (CommonSocket.SUCCESS.equalsIgnoreCase(result)) {
			/**
			 * 移动上传成功的数据文件到指定日期目录 
			 */
			File[] files = new File[allFiles.size()];
			moveDetecDataToDateDir(allFiles.toArray(files));
		}
	}
	 
	/**
	 * 批量发送任务结果
	 * @param fileArr
	 * @throws Exception
	 */
	private void sendTaskResult(File[] fileArr) throws Exception {
		//2013-4-16 修改升序排列方式：按修改时间 改为 按文件名，任务结果文件名都有时间后缀(ms)，文件修改时间只到s取不到ms
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
		logger.debug("sendTaskResult-->" + Arrays.toString(results.toArray()));
		//发送任务结果请求
		socket.sendMessageByChar(CommonSocket.DATA_TYPE_OBJ_TASKRESULT);
		socket.receiveMessageByChar();
		//发送任务结果内容
		socket.sendObject(results);
		String result = socket.receiveMessageByChar();
		if (CommonSocket.SUCCESS.equalsIgnoreCase(result)) {
			// 移动上传成功的任务结果到指定日期目录
			moveTaskResultToDateDir(fileArr);
		}
	}
	/**
	 * 单个发送任务回传文件
	 * @param fileArr
	 * @throws Exception
	 */
	private void sendTaskReturn(File[] fileArr) throws Exception {
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
				logger.warn("No return file, no return");
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
			boolean success = false;
			if(rfPo.getReturnFileName()!=null && rfPo.getReturnFileName().trim().length()>0){
				//发送回传文件请求
				socket.sendMessageByChar(CommonSocket.DATA_TYPE_FILE_TASKETURN);
				socket.receiveMessageByChar();
				//发送回传文件任务信息
				socket.sendMessageByChar(TaskResultOper.getTaskResultMsg(rfPo
						.getTaskId(), rfPo.getTaskType(), null, null, null, rfPo
						.getStartTime(), rfPo.getEndTime(), rfPo.getIsLoop()));
				socket.receiveMessageByChar();
				//发送回传文件文件名称
				socket.sendMessageByChar(rfPo.getReturnFileName());
				socket.receiveMessageByChar();
				//发送回传文件
				socket.bpSendFile(Contants.localTaskReturnPath + File.separator + rfPo.getReturnFileName());
				String result = socket.receiveMessageByChar();
				success = true;
//				sb.append("回传成功");
				sb.append("i18n_client.ServerCollectData.transFile_n81i");
			}else{
				success = true;
			}
			
			/**
			 * 步骤2、判断文件是否回传完成
			 */
			if(success){
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
	
}