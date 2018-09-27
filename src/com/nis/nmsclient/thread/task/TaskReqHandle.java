package com.nis.nmsclient.thread.task;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.nis.nmsclient.common.Common;
import com.nis.nmsclient.common.Contants;
import com.nis.nmsclient.model.CommandPO;
import com.nis.nmsclient.model.ParamFilePush;
import com.nis.nmsclient.model.ParamUpgrade;
import com.nis.nmsclient.model.Task4;
import com.nis.nmsclient.model.Task6;
import com.nis.nmsclient.thread.socket.CommonSocket;
import com.nis.nmsclient.util.FileUtil;
import com.nis.nmsclient.util.ProcessUtil;
import com.nis.nmsclient.util.Utils;

public class TaskReqHandle {
	Logger logger = Logger.getLogger(TaskReqHandle.class);
	private Thread singleThread;
	
	/**
	 * 任务请求处理步骤1：分析任务请求参数，分不同任务类型处理
	 */
	public void taskHandle(String str){
		Date execTime = new Date();
		boolean flag = true;
		int taskType = 0;
		long taskId = 0;
		String threadName = null;
		CommandPO command = null;
		try {  
			JSONObject jsonObj = JSONObject.fromObject(str);
			if(str.contains("typeInfo")){
				taskType = jsonObj.getInt("typeInfo");
			}
			if(str.contains("taskInfo")){
				JSONObject jsonObj2 = jsonObj.getJSONObject("taskInfo");
				Object obj = null;
				/**
				 * 任务类型：2 非流文本数据获取，3 流文本数据获取，4 命令执行，5 shell注册, 6升级
				 */
				switch (taskType) {
					case 4:
						obj = JSONObject.toBean(jsonObj2,Task4.class);
						Task4 task4 = (Task4) obj;
						taskId = task4.getTaskId();
						/**
						 * 命令类型：1 Agent原生支持命令，2可执行命令(2 脚本，3 shell命令)
						 */
						if(task4.getCommandType() == 1){
//							threadName = "原生命令 id:" + task4.getTaskId() + ">>" + task4.getCommandName();
							threadName = "Native command ID:" + task4.getTaskId() + ">>" + task4.getCommandName();
						}else if(task4.getCommandType() == 2){
//							threadName = "可执行命令 id:" + task4.getTaskId();
							threadName = "Executable command ID:" + task4.getTaskId();
						}
						command = new CommandPO();
						command.setExecId(task4.getTaskId());
						command.setExecType(task4.getTaskType());
						command.setCommandName(task4.getCommandName());
						command.setCommandParam(task4.getCommandParam());
						command.setExecState(task4.getState());
						command.setExecVersion(null);
						command.setIsLoop(task4.getIsLoop());
						
						handleTaskThread(task4.getTaskId(), task4.getStartTime(), task4.getEndTime(), task4
								.getIsLoop(), task4.getLoopDelay(), command, threadName, task4.getMissionState());
						
						break;
					case 6:
						obj = JSONObject.toBean(jsonObj2,Task6.class);
						Task6 task6 = (Task6) obj;
						taskId = task6.getTaskId();
//						threadName = "升级 id:" + task6.getTaskId() + ">>" + task6.getCommandName();
						threadName = "Upgrade ID:" + task6.getTaskId() + ">>" + task6.getCommandName();
						command = new CommandPO();
						command.setExecId(task6.getTaskId());
						command.setExecType(task6.getTaskType());
						command.setCommandName(task6.getCommandName());
						command.setCommandParam(task6.getCommandParam());
						command.setExecState(task6.getState());
						command.setExecVersion(task6.getVersion());
						command.setSrcPath(getUpgradeTaskPushPath(task6.getTaskId()));
						
						handleTaskThread(task6.getTaskId(), task6.getUpgradeTime(), null, 0, 0, command, threadName, 0);
						break;
					default:
						flag = false;
						break;
				}
			}else{
				flag = false;
			}
		} catch (Exception e) {
			logger.error(Utils.printExceptionStack(e));
			flag = false;
		}
		
		if(!flag){
			TaskResultOper.sendTaskResult(taskId, taskType,
//					AgentCommand.RESULT_FAIL, "发送内容格式不正确", "", execTime, new Date(), false, -1l);
//					AgentCommand.RESULT_FAIL, "Incorrect content format", "", execTime, new Date(), false, -1l);
					AgentCommand.RESULT_FAIL, "i18n_client.TaskReqHandle.sendInfoFormatErr_n81i", "", execTime, new Date(), false, -1l);
		}
	}
	
	/**
	 * 文件推送处理
	 */
	public String filePush(CommonSocket socket, String taskParam, long taskId, boolean isUpgrade){
		String msg = null;
		StringBuffer sb = new StringBuffer();
		File tempDir = null;
		try {
			tempDir = new File(Contants.localTempPath + File.separator
					+ "filepush_" + taskId);
			if (!tempDir.exists()) {
				tempDir.mkdirs();
			}
			// ------步骤1：接收Md5校验的推送文件到临时目录
			int flag = socket.bpReceiveFileByBathMd5(tempDir.getAbsolutePath());
			
			if (flag == 0){// ------步骤2：接收成功，与参数比对
				if(taskParam==null || "".equals(taskParam)){
					msg = Contants.COMMON_MSG_FAIL + Contants.COMMON_MSG_SEPRATOR + "i18n_client.TaskReqHandle.pushFileParamIsNull_n81i";
//					logger.info(msg);//i18nlog
					return msg;
				}
				// ------步骤2-1：解析参数	
				String[] params = taskParam.trim().split(AgentCommand.PARAM_SEPRATOR);
				if (params != null && params.length >= 1) {
					for (int i = 0; i < params.length; i++) {
						//2012-4-28 任务参数中对路径的格式化将在界面上进行，原因此处会对转义字符的\也转换为/，故replace("\\", "/")去掉
						params[i] = params[i].trim().replaceAll("[\n\t\r]","");//.replace("\\", "/");//[\\s*\n\t\r]
						logger.debug("filePush-->param: " + params[i]);
						ParamFilePush fParam = null;
						if(isUpgrade){
							ParamUpgrade cfu = (ParamUpgrade) JSONObject.toBean(JSONObject
									.fromObject(params[i]), ParamUpgrade.class);
							fParam = new ParamFilePush();
							fParam.setFileName(cfu.getFileName());
							fParam.setUsername(cfu.getUsername());
							fParam.setGroupName(cfu.getGroupName());
							fParam.setParam1(cfu.getParam1());
						}else{
							fParam = (ParamFilePush) JSONObject.toBean(
									JSONObject.fromObject(params[i]),
									ParamFilePush.class);
						}
						if(fParam.getDestPath()==null || fParam.getDestPath().trim().length()<=0){
							fParam.setDestPath(getUpgradeTaskPushPath(taskId));//设置默认推送目的地
							logger.debug("filePush-->destPath: " + fParam.getDestPath());
						}
						if(fParam.getFileName()==null || fParam.getFileName().trim().length()<=0){
//							msg = "推送文件名参数为空";
//							msg = "File push parameters are empty";
							msg = "i18n_client.TaskReqHandle.pushFileNameParamIsNull_n81i";
//							logger.debug(msg);//i18nlog
							break;
						}
						/*if(!ProcessUtil.checkUserPass(fParam.getUsername(), fParam.getParam1())){
							msg = "[" + fParam.getFileName() + "]推送文件的用户名或密码不正确;";
							logger.debug(msg);
							break;
						}*/
						// 判断用户名是否正确
						if(!ProcessUtil.checkUserOrGroupExist(fParam.getUsername(), fParam.getGroupName())){
//							msg = "[" + fParam.getFileName() + "]推送文件的属主或属群不正确;";
//							msg = "[" + fParam.getFileName() + "]The owner or group of the push file is incorrect;";
							msg = "[" + fParam.getFileName() + "]i18n_client.TaskReqHandle.userGroupErr_n81i;";
//							logger.debug(msg);//i18nlog
							break;
						}
						// ------步骤2-2：文件存在并与Md5值比较文件是否完整
						File pushFile = new File(tempDir.getAbsolutePath()
								+ File.separator + fParam.getFileName());
						if (!pushFile.exists()){
//							msg = "推送临时文件不存在，请检查推送文件名称与参数文件名("
//									+ fParam.getFileName() + ")是否一致";
//							msg = "The push temporary file does not exist. Please check whether the push file name is consistent with the parameter file name("
//									+ fParam.getFileName() + ")";
							msg = "i18n_client.TaskReqHandle.pushFileNotExists_n81i("
									+ fParam.getFileName() + ")";
//							logger.warn(msg + "--" + pushFile.getAbsolutePath());//i18nlog
							break;
						}
						// ------步骤2-3：判断推送目录是否存在，不存在创建
						File destFile = new File(fParam.getDestPath()
								+ File.separator + fParam.getFileName());
						if(!destFile.getParentFile().exists()){
							destFile.getParentFile().mkdirs();
						}
						// ------步骤2-4：判断是否直接覆盖
						if (fParam.getIsCover() != null
								&& "Y".equalsIgnoreCase(fParam.getIsCover())) {// 覆盖，则直接Copy并赋权限与所有者
							if (destFile.exists()) {
								//destFile.delete_bak();
								//使用删除文件公共方法
								FileUtil.delDir(destFile);
								logger.debug("filePush delete file--" + destFile.getAbsolutePath());
								//FileUtil.checkParentDirExist(destFile);
							}
							msg = copyAndSetPermission(pushFile
									.getCanonicalPath(), destFile
									.getCanonicalPath(), fParam.getUsername(),
									fParam.getGroupName(), fParam
											.getPermisson());
						} else if (!destFile.exists()) {// 不覆盖，则判断文件不存在的话，再Copy并赋权限与所有者
							msg = copyAndSetPermission(pushFile
									.getCanonicalPath(), destFile
									.getCanonicalPath(), fParam.getUsername(),
									fParam.getGroupName(), fParam
											.getPermisson());
						}
						if(msg==null || msg.length()<=0){
//							sb.append("[" + fParam.getFileName() + "]成功推送到[" + fParam.getDestPath() + "];");
//							sb.append("[" + fParam.getFileName() + "]successfully pushed to[" + fParam.getDestPath() + "];");
							sb.append("[" + fParam.getFileName() + "]i18n_client.TaskReqHandle.successPush_n81i[" + fParam.getDestPath() + "];");
							logger.debug("推送文件" + (i+1) + "成功---" + pushFile.getCanonicalPath());
						}else{
//							msg = msg+"[" + fParam.getFileName() + "]推送失败;";//文件推送失败的具体原因
							msg = msg+"[" + fParam.getFileName() + "]i18n_client.TaskReqHandle.pushFail_n81i;";//文件推送失败的具体原因
							logger.debug("推送文件" + (i+1) + "失败---" + pushFile.getCanonicalPath());
							break;
						}
					}//for end
				} else {
//					msg = "文件推送参数不正确";
//					msg = "File push parameter is incorrect";
					msg = "i18n_client.TaskReqHandle.pushParamErr_n81i";
//					logger.warn(msg + "<" + taskParam + ">");//i18nlog
				}
				//所有文件推送成功，删除临时接收文件目录
				if(msg==null || msg.length()<=0){
					if(tempDir!=null && tempDir.exists()){
						try {
							logger.debug("删除临时目录--" + tempDir.getAbsolutePath());
							FileUtils.deleteDirectory(tempDir);
							FileUtil.checkParentDirExist(tempDir);
						} catch (IOException e) {
						}
					}
				}
			}else {
				socket.close();
			}
			
			if(msg == null){
				msg = Contants.COMMON_MSG_SUCCESS + Contants.COMMON_MSG_SEPRATOR + sb.toString(); 
			}else {
				sb.append(msg);
				msg = Contants.COMMON_MSG_FAIL + Contants.COMMON_MSG_SEPRATOR + sb.toString(); 
			}
			
		} catch (Exception e) {
			logger.error("Receive push file exception：" + Utils.printExceptionStack(e));
//			msg =  Contants.COMMON_MSG_FAIL + Contants.COMMON_MSG_SEPRATOR + "接收推送文件异常," + e.getMessage();
//			msg =  Contants.COMMON_MSG_FAIL + Contants.COMMON_MSG_SEPRATOR + "Received push file exception," + e.getMessage();
			msg =  Contants.COMMON_MSG_FAIL + Contants.COMMON_MSG_SEPRATOR + "i18n_client.TaskReqHandle.reciveFileErr_n81i," + e.getMessage();
			return msg;
		}finally{
			if(tempDir!=null && tempDir.exists() && tempDir.listFiles().length==0){
				try {
					FileUtils.deleteDirectory(tempDir);
					logger.debug("finally删除临时目录--" + tempDir.getAbsolutePath());
					FileUtil.checkParentDirExist(tempDir);
				} catch (IOException e) {
				}
			}
		}
		
		return msg;
	}
	
	/**
	 * 文件推送部分的拷备工作，由临时文件目录拷备到推送的最终目的地，并赋相应的权限组
	 */
	private String copyAndSetPermission(String source, String destFile, String user, String group, String permission) throws Exception{
		String result = null;
		if (source != null && destFile != null) {
			// 根据操作系统确定获取进程ID的方式
			String os = System.getProperty("os.name");
			if (os.startsWith("Windows")) {
				FileUtils.copyFile(new File(source), new File(
						destFile));//目标路径不存在自动创建
			} else if (os.startsWith("Linux")) {
				StringBuffer sb = new StringBuffer();
				//source destFile都不能含有空格
				source = source.replace(" ", "\\ ");
				destFile = destFile.replace(" ", "\\ ");
				sb.append("\\cp -f " + source + " " + destFile + ";");//2015-11-6 hyx:  cp - f修改成\\cp -f   (有时候如果不加\\会提示是否，就会有问题)
				if (permission != null && !"".equals(permission.trim())) {
					sb.append("chmod " + permission + " "
							+ destFile + ";");
				}
				if (user != null && !"".equals(user.trim())) {
					sb.append("chown " + user + " " + destFile + ";");
				}
				if (group != null && !"".equals(group.trim())) {
					sb.append("chgrp " + group + " " + destFile);
				}
				result = ProcessUtil.execLinuxCmd(sb.toString());
			} else {
				throw new IOException("unknown operating system: " + os);
			}
		}else{
//			result = "源文件或目标文件为空";
//			result = "The source file or target file is empty";
			result = "i18n_client.TaskReqHandle.sourceOrTargetIsNull_n81i";
		}
		
		return result;
	}
	
	
	/**
	 * 任务请求处理步骤2：将分析包装好的任务，统一判断处理并添加到线程中执行
	 */
	public void handleTaskThread(Long taskId, Long startTime, Long endTime,
			long isLoop, long loopDelay, final CommandPO command,
			final String threadName, long missionState) {
		if(missionState == AgentCommand.MISSION_CANCEL_START){//如果任务状态为，撤消任务
			logger.warn("The task is in the revocation, and the ID is not processed:" + taskId);
			return;
		}
		if(Common.getTaskFuture(taskId)!=null){//当前任务已存在执行，则不执行该当前任务
			logger.warn("The task already exists to execute the ID:" + taskId);
			return;
		}
		// 设置任务结束时间，且当前时间已超过任务结束时间
		if (endTime != null && endTime.longValue() > 0
				&& endTime.longValue() <= System.currentTimeMillis()) {
			logger.warn("The task has expired ID:" + taskId);
			return;
		}
		long delay = 0;
		if (startTime != null) {
			delay = startTime - System.currentTimeMillis();
		}
		ScheduledFuture<?> taskFuture = null;
		LoopTaskThread loopTaskThread = null;
		if (isLoop == 0) {// 非周期任务
			taskFuture = Common.scheduled.schedule(new Runnable() {
				public void run() {
					Thread.currentThread().setName(threadName);
					new AgentCommand(command).exec();
				}
			}, delay, TimeUnit.MILLISECONDS);
		} else {
			Future<?> singleFuture = null;
			if(delay <= 0){//开始时间之后接到任务，先执行一次，第二次按周期点执行
				long now = System.currentTimeMillis();
				long cnt = (now - startTime)/(loopDelay * 60 * 1000);
				if((now - startTime)%(loopDelay * 60 * 1000)!=0){
					delay = startTime + loopDelay *60 *1000 * (cnt + 1) - System.currentTimeMillis();
					///仅执行一次的，并在周期执行代码中第一次执行判断单次执行完成与否，未完成结束掉
					singleFuture = Common.scheduled.schedule(new Runnable() {
						public void run() {
							singleThread = Thread.currentThread();
//							Thread.currentThread().setName(threadName + " 周期单次");
							Thread.currentThread().setName(threadName + " Periodic Single Time");
							new AgentCommand(command).exec();
						}
					}, 0, TimeUnit.MILLISECONDS);
				}
			}
			loopTaskThread = new LoopTaskThread(threadName, command, loopDelay, singleFuture, singleThread);
			taskFuture = Common.scheduled.scheduleAtFixedRate(loopTaskThread, delay, loopDelay * 60 * 1000, TimeUnit.MILLISECONDS);
			// 周期任务，若设置结束时间，则添加取消线程
			if (endTime != null && endTime.longValue() > 0) {
				long endDelay = endTime.longValue() - System.currentTimeMillis();
				if (endDelay > 0) {
					Common.cancleTaskFuture(taskId, endDelay);
				}
			}// 取消线程结束
			
		}
		// 将正在执行的任务添加到全局变量，目的是避免重复执行任务
		Common.putTaskFuture(taskId, taskFuture, loopTaskThread);
	}
	
	public static String getUpgradeTaskPushPath(long taskId){
		return Contants.localUploadsPath + File.separator + taskId;
	}
}	
	