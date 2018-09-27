package com.nis.nmsclient.thread.task;

import java.io.File;
import java.util.Date;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import com.nis.nmsclient.common.Common;
import com.nis.nmsclient.common.Contants;
import com.nis.nmsclient.common.VersionCfg;
import com.nis.nmsclient.config.DetecConfOper;
import com.nis.nmsclient.thread.socket.CommonSocket;
import com.nis.nmsclient.thread.socket.SSLClient;
import com.nis.nmsclient.thread.socket.ServerCollectData;
import com.nis.nmsclient.util.FileUtil;
import com.nis.nmsclient.util.FileWrUtil;
import com.nis.nmsclient.util.Utils;
import com.nis.nmsclient.util.ZipUtil;

public class TaskResultOper {
	static Logger logger = Logger.getLogger(DetecConfOper.class);
	
	/**
	 * Agent在启动时，发送所有之前发送失败的任务结果
	 */
	public static void initSendAllTaskResult(){
		try {
			//针对结果文件过多时打包上传未完成的文件继续上传
			File taskDir = new File(Contants.localTaskPath);
			if (!taskDir.exists()) {
				return;
			}
			File[] zipArr = FileUtil.getFilesEndWith(taskDir, ".zip");
			if (zipArr != null && zipArr.length > 0) {
				for (File file : zipArr) {
					if (!file.getName().startsWith(CommonSocket.BP_TYPE_TASK_RESULT)) {
						continue;
					}
					Future<?> future = Common.service.submit(new SSLClient(Thread
							.currentThread().getName(),
							CommonSocket.REQ_BP_UPLOAD_FIFE, new String[] {
						CommonSocket.BP_TYPE_TASK_RESULT, file.getAbsolutePath() }));
					future.get();
				}
			}
			//-------------------------------------
			
			File resultDir = new File(getTaskResultPath());
			if(!resultDir.exists()){
				return;
			}
			File[] fileArr = FileUtil.getFilesEndWith(resultDir, Contants.TASK_RESULT_FILE_SUFFIX);
			if(fileArr == null || fileArr.length == 0){
				/*
				 * 当任务正常执行完成或者取消后则从全局变量移除
				 */
				Common.removeCancelAndDoneTaskFuture();
				return;
			}
			//--- 将所有任务结果文件一起打包，发送
			if(fileArr.length > Contants.COMMON_ZIP_MIN_SIZE){
				//与Server通信
				Future<?> serFuture = Common.service.submit(new SSLClient(
						Thread.currentThread().getName(),
						CommonSocket.REQ_HAND_SHAKE, null));
				if (!Contants.isSucessByResult((String) serFuture.get())) {
					return;
				}
				
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
						String compressFileStr = Contants.localTaskPath
								+ File.separator
								+ CommonSocket.addTimeTagForFileName(CommonSocket.BP_TYPE_TASK_RESULT, null, true)
								+ ".zip";
						// 2013-03-22 由于DC再次获取未保存任务结果这个功能的实现，现修改将任务结果和回传文件压缩时不删除文件，而是将其移动到相应的日期目录
						ZipUtil.zipWithMoveFile(resultDir.listFiles(), compressFileStr, true);
						//发送
						Future<?> future = Common.service.submit(new SSLClient(Thread.currentThread()
								.getName(), CommonSocket.REQ_BP_UPLOAD_FIFE,
								new String[] { CommonSocket.BP_TYPE_TASK_RESULT, compressFileStr }));
						future.get();
					}
				}else{
					// 压缩并发送任务结果
					String compressFileStr = Contants.localTaskPath
							+ File.separator
							+ CommonSocket.addTimeTagForFileName(CommonSocket.BP_TYPE_TASK_RESULT, null, true)
							+ ".zip";
					// 2013-03-22 由于DC再次获取未保存任务结果这个功能的实现，现修改将任务结果和回传文件压缩时不删除文件，而是将其移动到相应的日期目录
					ZipUtil.zipWithMoveFile(resultDir.listFiles(), compressFileStr, true);
					//发送
					Future<?> future = Common.service.submit(new SSLClient(Thread.currentThread()
							.getName(), CommonSocket.REQ_BP_UPLOAD_FIFE,
							new String[] { CommonSocket.BP_TYPE_TASK_RESULT, compressFileStr }));
					future.get();
				}
			}else{//-- 按正常的一个结果一个结果的发送
				fileArr = FileUtil.sortASCByFileName(fileArr); //按文件名升序排列，任务结果文件名都有时间后缀(ms)
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
						Future<?> future = Common.service.submit(new SSLClient(
//								"上传任务结果",
								"Upload Task Results",
								CommonSocket.REQ_TASK_RESULT, sb.toString()));
						String msg = (String) future.get();
						if (Contants.isSucessByResult(msg)) {
							// 移动上传成功的任务结果到指定日期目录
							ServerCollectData.moveTaskResultToDateDir(file);
						}
					}else{
						// 移动上传成功的任务结果到指定日期目录
						ServerCollectData.moveTaskResultToDateDir(file);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Upload task result exception：" + Utils.printExceptionStack(e));
		}
	}

	/**
	 * 处理Agent自动升级的最终结果文件，即升级后是否启动成功 
	 *     Agent升级的临时结果文件，初始写入状态为升级失败：
	 *     1、当启动失败后将其临时文件后缀改为任务结果文件的后缀; 
	 *     2、当启动成功后先修改文件第一行的后两项内容为执行状态与描述为成功，再改其后缀。
	 * 
	 * @param isSuccess
	 */
	public static void handerAgentUpgradeResult(boolean isSuccess){
		try {
			File dir = new File(getTaskResultPath());
			if(!dir.exists()){
				return;
			}
			File[] fileArr = FileUtil.getFilesEndWith(dir, Contants.TASK_RESULT_AGENTTMPFILE_SUFFIX);
			fileArr = FileUtil.sortASCByFileName(fileArr); //按文件名升序排列，任务结果文件名都有时间后缀(ms)
			if (fileArr != null && fileArr.length > 0) {
				for(int i=0; i<fileArr.length; i++){
					if (isSuccess && i == fileArr.length-1) {// 如果启动成功处理其结果内容
						try {// 本层try是为了防止其中一个结果文件不正常，影响其他结果文件不能正常修改
							String[] msgs = FileWrUtil.cfgFileReader(fileArr[i]);
							if (msgs != null && msgs.length > 0) {
								int descIndex = msgs[0]
										.lastIndexOf(Contants.COMMON_MSG_SEPRATOR);
								int stateIndex = msgs[0].substring(0, descIndex)
										.lastIndexOf(Contants.COMMON_MSG_SEPRATOR);
								 msgs[0] = msgs[0].substring(0, stateIndex)
										+ Contants.COMMON_MSG_SEPRATOR + AgentCommand.RESULT_OK
//										+ Contants.COMMON_MSG_SEPRATOR + "重启成功";
										+ Contants.COMMON_MSG_SEPRATOR + "i18n_client.TaskResultOper.restart_n81i";

								 FileWrUtil.cfgFilePrinter(fileArr[i], Contants.charset, msgs);
								
								int taskIdIndex = msgs[0].indexOf(Contants.COMMON_MSG_SEPRATOR);
								//处理写入当前更新的版本信息
								String taskId = msgs[0].substring(0, taskIdIndex);
								Long curVer = Long.parseLong(VersionCfg
										.getValue(VersionCfg.NAGENT_VERSION));
								if (curVer < Long.parseLong(taskId.trim())) {
									VersionCfg.setValue(VersionCfg.NAGENT_VERSION, taskId);
									logger.info("NC更新为版本" + taskId);
								}
							}
						} catch (Exception e) {
							logger.error("Handling the exception of the NC upgrade result file：" + Utils.printExceptionStack(e));
						}
					}
					String fileStr = fileArr[i].getAbsolutePath();
					int index = fileStr.lastIndexOf(Contants.TASK_RESULT_AGENTTMPFILE_SUFFIX);
					fileStr = fileStr.substring(0, index);
					fileArr[i].renameTo(new File(fileStr + Contants.TASK_RESULT_FILE_SUFFIX));
				}
			}
		} catch (Exception e) {
			logger.error("Handling the exception of the NC upgrade result file：" + Utils.printExceptionStack(e));
		}
	}
	
	/**
	 * 发送任务执行结果：若发送失败写入文件
	 */
	/*private static void sendTaskResult(long taskId, long taskType, String msg) {
		try{
			Future<?> future = Common.service.submit(new SSLClient(
					Thread.currentThread().getName(),
					CommonSocket.REQ_TASK_RESULT, msg));

			String result = (String) future.get();

			if (!Contants.isSucessByResult(result)) {//失败
				File file = new File(getTaskResultFile(taskType, taskId));
				if(!file.getParentFile().exists()){
					file.getParentFile().mkdirs();
				}
				FileWrUtil.cfgFilePrinter(file, Contants.charset, new String[]{msg});
			}
		}catch (Exception e) {
			logger.error("发送任务结果异常：" + Utils.printExceptionStack(e));
		}
	}*/
	
	/**
	 * 发送任务执行结果：判断是否是Server升级，若是直接写入文件，若不是则发送结果
	 * @param taskId 任务ID
	 * @param taskType 任务类型
	 * @param resultState 执行结果状态
	 * @param resultDesc 执行结果描述
	 * @param resultConfig 
	 * @param startTime 开始执行时间
	 * @param endTime   执行结束时间
	 * @param isServer 是否是Server升级
	 * @param isLoop 是否是循环任务
	 */
	/*public static void sendTaskResult(long taskId, long taskType,
			long resultState, String resultDesc, String resultConfig,
			Date startTime, Date endTime, boolean isServer, long isLoop) {
		try {
			String msg = getTaskResultMsg(taskId, taskType, resultState,
					resultDesc, resultConfig, startTime, endTime, isLoop);
			if(isServer){//如果是Server升级，直接写入文件
				File file = new File(getTaskResultFile(taskType, taskId));
				if(!file.getParentFile().exists()){
					file.getParentFile().mkdirs();
				}
				FileWrUtil.cfgFilePrinter(file, Contants.charset, new String[]{msg});
			}else{//反之，发送任务结果
				sendTaskResult(taskId, taskType, msg);
			}
		} catch (Exception e) {
			logger.error("发送任务结果异常：" + Utils.printExceptionStack(e));
		}
	}*/
	
	/**
	 * 发送任务结果： 将结果保存到文件，等待DC主动来收集
	 * @date Jan 15, 2013
	 * @author zhenzhen
	 * @version
	 */
	public static void sendTaskResult(long taskId, long taskType,
			long resultState, String resultDesc, String resultConfig,
			Date startTime, Date endTime, boolean isServer, long isLoop) {
		try {
			// 2013-4-9 为了防止多步操作时结果文件名重复，所以暂停50ms为了使时间后缀不一样
			if (resultState > AgentCommand.RESULT_SEND_OK) {
				try {Thread.sleep(50);} catch (Exception ignored) {	}
			}
			
			String msg = getTaskResultMsg(taskId, taskType, resultState,
					resultDesc, resultConfig, startTime, endTime, isLoop);
			File file = new File(getTaskResultFile(taskType, taskId));
			if(!file.getParentFile().exists()){
				file.getParentFile().mkdirs();
			}
			FileWrUtil.cfgFilePrinter(file, Contants.charset, new String[]{msg});
		} catch (Exception e) {
			logger.error("Send task result exception：" + Utils.printExceptionStack(e));
		}
	}
	
	/**
	 * 按指定的分隔符和顺序拼写任务执行结果信息
	 */
	public static String getTaskResultMsg(long taskId, long taskType,
			Long resultState, String resultDesc, String resultConfig,
			Date startTime, Date endTime, long isLoop) {
		StringBuffer sb = new StringBuffer();
		sb.append(taskId);
		sb.append(Contants.COMMON_MSG_SEPRATOR);
		sb.append(taskType);
		sb.append(Contants.COMMON_MSG_SEPRATOR);
		sb.append(Contants.AGENT_HOST_UUID);
		sb.append(Contants.COMMON_MSG_SEPRATOR);
		sb.append(isLoop);
		sb.append(Contants.COMMON_MSG_SEPRATOR);
		sb.append(startTime.getTime());
		sb.append(Contants.COMMON_MSG_SEPRATOR);
		sb.append(endTime.getTime());
		sb.append(Contants.COMMON_MSG_SEPRATOR);
		sb.append(resultConfig);
		sb.append(Contants.COMMON_MSG_SEPRATOR);
		sb.append(resultState);
		sb.append(Contants.COMMON_MSG_SEPRATOR);
		sb.append(resultDesc);
		
		return sb.toString();
	}
	
	public static String getTaskResultFile(long taskType, long taskId){
		return getTaskResultFileNoSuffix(taskType, taskId) + "_" + System.currentTimeMillis()
				+ Contants.TASK_RESULT_FILE_SUFFIX;
	}
	
	public static String getAgentUpgradeResultTempFile(long taskType, long taskId){
		return getTaskResultFileNoSuffix(taskType, taskId)
				+ Contants.TASK_RESULT_AGENTTMPFILE_SUFFIX;
	}
	
	private static String getTaskResultFileNoSuffix(long taskType, long taskId){
		return  getTaskResultPath() + File.separator + "tasktype" + taskType + "_" + taskId;
	}
	
	public static String getTaskResultPath(){
		return Contants.localTaskResultPath;
	}
}
