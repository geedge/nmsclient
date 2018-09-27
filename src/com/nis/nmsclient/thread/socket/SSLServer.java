package com.nis.nmsclient.thread.socket;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import com.nis.nmsclient.common.Common;
import com.nis.nmsclient.common.Contants;
import com.nis.nmsclient.common.SysConfig;
import com.nis.nmsclient.config.DetecConfReqHandle;
import com.nis.nmsclient.model.ReturnFilePO;
import com.nis.nmsclient.model.Task1;
import com.nis.nmsclient.model.Task4;
import com.nis.nmsclient.model.Task6;
import com.nis.nmsclient.thread.alarm.AlarmUtil;
import com.nis.nmsclient.thread.alarm.ErrorCode;
import com.nis.nmsclient.thread.task.AgentCommand;
import com.nis.nmsclient.thread.task.TaskReqHandle;
import com.nis.nmsclient.util.DateUtil;
import com.nis.nmsclient.util.FileUtil;
import com.nis.nmsclient.util.FileWrUtil;
import com.nis.nmsclient.util.Utils;

/**
 * 用于安全通讯的服务Socket，采用java中的SSLServerSocket
 * 	  接收服务端发送过来的对该客户端的各配置参数信息，并对相应程序进行设置
 **/
public class SSLServer implements Runnable {
	static Logger logger = Logger.getLogger(SSLServer.class);
	SSLServerSocket ss = null;

	private String startTime;// 服务启动时间
	
	public SSLServer() throws IOException{
		init();
	}

	/**
	 * 初始化服务Socket
	 **/
	public void init() throws IOException {
		try {
			startTime = System.currentTimeMillis() + "";

			//初始化上下文
			SSLContext ctx = SSLCertOper.getSSLContext();
			ss = (SSLServerSocket) ctx.getServerSocketFactory()
					.createServerSocket(Contants.SOCKET_AGENT_PORT);
			ss.setNeedClientAuth(true);// 客户端要认证	
		} catch (Exception e) {
			logger.error(Utils.printExceptionStack(e));
//			throw new IOException("NmsClient监听端口[" + Contants.SOCKET_AGENT_PORT + "]创建失败");
			throw new IOException("NmsClient monitor port[" + Contants.SOCKET_AGENT_PORT + "]Create failure");
		}
	}

	/**
	 * 重载方法：run 处理客户端的请求
	 **/
	public void run() {
		logger.info("通讯线程启动 成功");
		Socket socket = null;
		while(true){
			try {
				socket = ss.accept();
				if(!Common.NC_UPGRADE_FLAG){//当NC_UPGRADE_FLAG为false时，允许建立通讯，否则放弃通讯，用于NC升级功能
					logger.debug("来自："+socket.getInetAddress().getHostAddress());
					Common.service.submit(new ServerThread(socket));
				}else{	//关闭 放弃的通讯
					logger.info("NC升级 抛弃通讯："+socket.getInetAddress().getHostAddress());
					socket.close();
				}
			} catch (Exception e) {
				logger.error("Failure to establish communication " + ss.getInetAddress().getHostAddress()
						+ ": " + Utils.printExceptionStack(e));
			}
		}

	}
	
	class ServerThread  extends CommonSocket implements Runnable {
		
		public ServerThread(Socket s) throws Exception {
			super(s);
		}
		
		public void run(){
			String ip = null;
			try {
				ip = socket.getInetAddress().getHostAddress();
				
				//设置超时时间
				socket.setSoTimeout(1000 * 60 * Contants.SOCKET_TIMEOUT_MINUTES);
				
//				Thread.currentThread().setName("通讯线程 》" + socket.getInetAddress().getHostAddress());
				Thread.currentThread().setName("Communication Thread 》" + socket.getInetAddress().getHostAddress());
				
				String msg = this.receiveMessageByChar();
				logger.info("接收请求 " + msg);
				
				if(REQ_HAND_SHAKE.equalsIgnoreCase(msg)){// 握手操作
					//返回接收到的信息和NMSClient启动时间
					this.sendMessageByChar(SUCCESS+":"+msg+"|"+startTime);
				}else if(SERVER_COLLECT_DATA.equalsIgnoreCase(msg)){// DC主动向NC收集数据
					/** ----当前通信DC_IP与配置DC_IP不同，更新IP---- **/
					if(!Contants.SOCKET_SERVER_HOST.equals(ip)){
						logger.info("变更通信DC_IP: " + Contants.SOCKET_SERVER_HOST + " --> " + ip);
						Contants.SOCKET_SERVER_HOST = ip;
						/** ----SeqId未取到，更新配置文件---- **/
						if(Contants.AGENT_HOST_UUID == null){
							SysConfig.updateConfigFile(Contants.SOCKET_SERVER_HOST_KEY, Contants.SOCKET_SERVER_HOST);
						}
					}
					new ServerCollectData(this).sendData();
				}else if(SERVER_GET_TASKRESULT.equalsIgnoreCase(msg)){// DC再次向NC获取未入库的任务结果
					this.sendMessageByChar(SUCCESS);
					String taskInfo = null;
					List<File> fileList = new LinkedList<File>();
					while(!END.equalsIgnoreCase(taskInfo = this.receiveMessageByChar())){
						// taskInfo组织顺序：TaskId、TaskType、isLoop、startTime、endTime
						String[] infos = taskInfo.split(Contants.COMMON_MSG_SEPRATOR_SPLIT);
						if (infos.length < 4 || (!"0".equals(infos[2]) && infos.length < 5)) {// 参数个数不够4，或者周期任务参数个数不够5，则跳过本次处理
							logger.warn("DC gets the task result again, the task attribute is incomplete, skip this processing.");
							this.sendMessageByChar(AgentCommand.RESULT_FAIL 
//									+ Contants.COMMON_MSG_SEPRATOR + "任务参数不全，找不到任务结果");
									+ Contants.COMMON_MSG_SEPRATOR + "i18n_client.SSLServer.sendMsg_n81i");
							continue;
						}
						String eTime = null;
						if(infos.length > 4){// 非周期任务，参数个数为4
							eTime = infos[4];
						}
						// 检查指定任务的的结果或回传文件是否存在：若存在，移动到incoming目录；若不存在，回复失败
						boolean isExistResult = checkTaskResultExist(infos[0], infos[1], infos[2], infos[3], eTime, fileList);
						if(isExistResult){
							this.sendMessageByChar(AgentCommand.RESULT_OK 
									+ Contants.COMMON_MSG_SEPRATOR + "");
						} else {
							this.sendMessageByChar(AgentCommand.RESULT_FAIL 
//									+ Contants.COMMON_MSG_SEPRATOR + "任务结果不存在");
									+ Contants.COMMON_MSG_SEPRATOR + "i18n_client.SSLServer.noResult_n81i");
						}
					}
					this.sendMessageByChar(SUCCESS);
					// 移动存在的任务结果和回传文件
					moveTaskResultOrReturn(fileList);
				}else if(REQ_SERVER_SYSTEMDATE.equalsIgnoreCase(msg)){//add by jinsj 2012-05-31 DC主动获取NC时间
					this.sendMessageByChar(new Date().getTime()+"");
					this.receiveMessageByChar();
				}else if(Contants.AGENT_HOST_UUID == null ){
					// 除了以上三个通信，其他通信都得判断SeqID是否获取到，若未取到，则要放弃通讯
					logger.info("NC尚未取到SeqID 抛弃通讯："+socket.getInetAddress().getHostAddress());
					close();
					return;
				}
				
				if(REQ_CERT.equalsIgnoreCase(msg)){
					this.sendMessageByChar(SUCCESS);
					//接收证书
					this.receiveFileByByte(Contants.keyPath + File.separator + "server.cer");
					this.sendMessageByChar(SUCCESS);
					//导入认证证书到库
					SSLCertOper.importCertToStore("serverks"
							+ DateUtil.getCurrentDate(DateUtil.YYYYMMDD),
							Contants.SSL_TRUST_KEY_STORE,
							Contants.keyPath + File.separator + "server.cer", Contants.SSL_KEY_STORE_PASS);
					File file = new File(Contants.keyPath + File.separator + "server.cer");
					if(file.exists()){
						FileUtil.delDir(file);
					}
					
					/*SSLClient sc = new SSLClient();
					SSLCertOper.CreateAndSendCert(sc);
					sc.close();*/
				}else if(SERVER_UPDATE_CONFIG.equalsIgnoreCase(msg)){// 更新监测配置
					this.sendMessageByChar(SUCCESS);
					String str = this.receiveMessageByChar();
					this.sendMessageByChar(SUCCESS);
					logger.debug("updateConfig-->" + str);
					new DetecConfReqHandle().handlerConfigByUpdate(str);
				} else if(SEND_PLUGIN_SCRIPT_FILE.equalsIgnoreCase(msg)) { // 下发脚本
					File pluginDir = new File(Contants.localPluginScriptPath);
					this.sendMessageByChar(SUCCESS);
					String fileNames = this.receiveMessageByChar();
					Collection<?> files = FileUtils.listFiles(pluginDir, 
							new PrefixFileFilter(fileNames.split(",")), FalseFileFilter.FALSE);
					for (Object file : files) {
						((File)file).delete();
					}
					this.sendMessageByChar(SUCCESS);					
					this.bpReceiveFileByBath(pluginDir.getCanonicalPath());
					this.sendMessageByChar(SUCCESS);
					
				} else if(SERVER_FILE_PUSH.equalsIgnoreCase(msg)){//任务操作：文件推送
					this.sendMessageByChar(SUCCESS);
					String str = this.receiveMessageByChar();
					logger.debug("task-->" + str);
					this.sendMessageByChar(SUCCESS);
					JSONObject jsonObj = JSONObject.fromObject(str);
					String resultMsg = null;
					if(str.contains("taskInfo")){
						JSONObject jsonObj2 = jsonObj.getJSONObject("taskInfo");
						Object obj = JSONObject.toBean(jsonObj2,Task1.class);
						Task1 fileInfo = (Task1) obj;
						// 接收文件
						resultMsg = new TaskReqHandle().filePush(this, fileInfo.getTaskParam(), fileInfo.getTaskId(), false);
					}
					if (resultMsg !=null && Contants.isSucessByResult(resultMsg)) {
						this.sendMessageByChar(AgentCommand.RESULT_OK
								+ Contants.COMMON_MSG_SEPRATOR
//								+ "成功,详细信息如下：" + Contants.getDescByResult(resultMsg));
								+ "i18n_client.SSLServer.success_n81i：" + Contants.getDescByResult(resultMsg));
					} else {
						this.sendMessageByChar(AgentCommand.RESULT_FAIL
								+ Contants.COMMON_MSG_SEPRATOR
//								+ "失败,详细信息如下:" + Contants.getDescByResult(resultMsg));
								+ "i18n_client.SSLServer.fail_n81i:" + Contants.getDescByResult(resultMsg));
					}
					this.receiveMessageByChar(); 
				}else if(SERVER_UPGRADE.equalsIgnoreCase(msg)){//任务操作：升级
					this.sendMessageByChar(SUCCESS);
					String str = this.receiveMessageByChar();
					logger.debug("task-->" + str);
					this.sendMessageByChar(SUCCESS);
					JSONObject jsonObj = JSONObject.fromObject(str);
					String resultMsg = null;
					TaskReqHandle handle = new TaskReqHandle();
					if(str.contains("taskInfo")){
						JSONObject jsonObj2 = jsonObj.getJSONObject("taskInfo");
						Object obj = JSONObject.toBean(jsonObj2,Task6.class);
						Task6 task = (Task6) obj;
						// 判断是否重新执行任务，并作提前处理
						reExecTask(task.getTaskId(), task.getOldTaskId());
						// 接收升级文件
						resultMsg = handle.filePush(this, task.getCommandParam(),
								task.getTaskId(), true);
					}
					if (resultMsg !=null && Contants.isSucessByResult(resultMsg)) {
						this.sendMessageByChar(AgentCommand.RESULT_SEND_OK
								+ Contants.COMMON_MSG_SEPRATOR
//								+ "下发成功,详细信息如下：" + Contants.getDescByResult(resultMsg));
								+ "i18n_client.SSLServer.lssueSuccess_n81i：" + Contants.getDescByResult(resultMsg));
					} else {
						this.sendMessageByChar(AgentCommand.RESULT_FAIL
								+ Contants.COMMON_MSG_SEPRATOR
//								+ "失败,详细信息如下: " + Contants.getDescByResult(resultMsg));
								+ "i18n_client.SSLServer.fail_n81i: " + Contants.getDescByResult(resultMsg));
					}
					String receiveMsg = this.receiveMessageByChar();
					if(resultMsg !=null && Contants.isSucessByResult(resultMsg) &&receiveMsg.equals(SUCCESS)){//处理升级 
						handle.taskHandle(str);
					}
				}else if(SERVER_TASK.equalsIgnoreCase(msg)){//任务操作：命令执行和升级逆向任务
					this.sendMessageByChar(SUCCESS);
					String str = this.receiveMessageByChar();
					logger.debug("task-->" + str);
					this.sendMessageByChar(AgentCommand.RESULT_SEND_OK
//							+ Contants.COMMON_MSG_SEPRATOR + "下发成功");
							+ Contants.COMMON_MSG_SEPRATOR + "i18n_client.SSLServer.lssueSuccess1_n81i");
					this.receiveMessageByChar();
					
					//2015-6-23 针对reboot命令（之前存在会多次重启的问题，现修改为，接收到命令执行任务时，如果该任务的结果已经存在（incoming或者done里有），则不再执行）
					try	{
						int taskType = 0;
						JSONObject jsonObj = JSONObject.fromObject(str);
						if(str.contains("typeInfo")){
							taskType = jsonObj.getInt("typeInfo");
						}
						if(str.contains("taskInfo") && taskType==4){//taskType：命令执行任务（4）
							JSONObject jsonObj2 = jsonObj.getJSONObject("taskInfo");
							Task4 task4 = (Task4)JSONObject.toBean(jsonObj2,Task4.class);
							String taskId = task4.getTaskId()==null?"0":(task4.getTaskId()+"");
							String isLoop = task4.getIsLoop()+"";
							String startTime = task4.getStartTime()==null?"":task4.getStartTime()+"";
							String endTime = task4.getEndTime()==null?"":task4.getEndTime()+"";
							if(task4.getCommandType() == 2){//命令执行(4)->可执行命令(2)
								logger.info("可执行命令 taskId:" + task4.getTaskId());
								List<File> fileList = new ArrayList<File>();
								boolean isExist = checkTaskResultExistFromDoneAndIncoming(taskId+"", taskType+"", isLoop, startTime, endTime, fileList);//非周期任务：0
								if(isExist) {
									logger.info("任务已执行，不再重复执行：taskId："+taskId+" taskType："+taskType);
									return;
								}
							}
						}
					} catch (Exception e) {
						logger.error("For the next task, determine whether there is a result, if the result is no longer performing the exception", e);
					}
					
					new TaskReqHandle().taskHandle(str);
				}else if(SERVER_TASK_CANCEL.equalsIgnoreCase(msg)){//任务撤消操作
					this.sendMessageByChar(SUCCESS);
					String str = this.receiveMessageByChar();
					logger.debug("taskcancle-->" + str);
					if(str!=null && !"".equals(str)){
						Common.cancleTaskFuture(Long.parseLong(str), 0);
					}
					this.sendMessageByChar(AgentCommand.MISSION_CANCEL_FINISH
//							+ Contants.COMMON_MSG_SEPRATOR + "任务已撤消完成");
							+ Contants.COMMON_MSG_SEPRATOR + "i18n_client.SSLServer.missionRevokeSuccess_n81i");
					this.receiveMessageByChar();
				}else if(ACTIVE_ALARM_START_ALERT.equalsIgnoreCase(msg)){// NC端是否报主动告警 变更
					this.sendMessageByChar(SUCCESS);
					String str = this.receiveMessageByChar();
					this.sendMessageByChar(SUCCESS);
					logger.debug("isStartActiveAlarm-->" + str);
					JSONObject jsonObj = JSONObject.fromObject(str);
					Boolean isStartActiveAlarm = (Boolean)jsonObj.get("showAutoAlarm");
					String webHandleTime = (String)jsonObj.get("webHandleTime");
					//更新Contants.ACTIIVE_ALARM_START
					if(isStartActiveAlarm!=null) {
						
						Contants.ACTIIVE_ALARM_START = isStartActiveAlarm;
						logger.info("NC是否主动告警："+Contants.ACTIIVE_ALARM_START+" web端操作时间："+webHandleTime);
					}
				}
				
				logger.debug("接收请求 " + msg + " 完成");
			} catch (Exception e) {
				logger.error("Receiving information anomaly：" + Utils.printExceptionStack(e));
				if(ip==null){
					ip = Utils.getLocalIp();
				}
//				AlarmUtil.sendNMSErrorMsg(ErrorCode.SocketError, ip , "NC通讯线程异常：" + e.getMessage());
				AlarmUtil.sendNMSErrorMsg(ErrorCode.SocketError, ip , "NC communication thread exception：" + e.getMessage());
				
				return;
			} finally {
				logger.debug("关闭通信");
				close();
			}
		}
		
		/**
		 * 重新执行任务，针对升级任务的推送文件的提前处理，将原任务的文件拷贝到新任务的临时目录
		 * @param taskId
		 * @param oldTaskId
		 * @throws Exception
		 */
		private void reExecTask(Long taskId, Long oldTaskId) throws Exception {
			// 如果原任务ID为空，说明不是重新执行任务，不执行任何操作
			if (oldTaskId == null || "".equals(oldTaskId.toString())
					|| "0".equals(oldTaskId.toString())) {
				return;
			}
			File tempDir = new File(Contants.localTempDataIncomingPath + File.separator
					+ "filepush_" + taskId);
			if (!tempDir.exists()) {
				tempDir.mkdirs();
			}
			// 如果是升级任务，推送文件的保存路径getUpgradeTaskPushPath(taskId)
			File oldFileDir = new File(TaskReqHandle.getUpgradeTaskPushPath(oldTaskId));
			try {
				if(oldFileDir.exists()){
					FileUtils.copyDirectory(oldFileDir, tempDir);
				}
			} catch (IOException e) {
				logger.error(e);
			}
		}
		
		/**
		 * DC再次获取任务结果 -- 检查指定任务的的结果或回传文件是否存在
		 * @param isLoop 是否循环任务: 0 非周期， 1 周期
		 * @param startTime 若非周期任务，升级时间或创建时间；若周期任务，某一周期的起始时间
		 * @param endTime 若非周期任务，为空；若周期任务，某一周期的结束时间
		 * @param fileList 用于存在找到的结果文件和回传文件
		 * @return
		 */
		private boolean checkTaskResultExist(String taskId, String taskType, String isLoop, String sTime, String eTime, List<File> fileList) throws Exception{
			long startTime=(sTime==null || "".equals(sTime) || "null".equals(sTime)) ? 0l : Long.parseLong(sTime);
			long endTime=(eTime==null || "".equals(eTime) || "null".equals(eTime)) ? 0l : Long.parseLong(eTime);
			logger.debug("checkTaskResultExist startTime=" + DateUtil.getStingDate(DateUtil.YYYY_MM_DD_HH24_MM_SS, new Date(startTime))+" -- endTime=" + DateUtil.getStingDate(DateUtil.YYYY_MM_DD_HH24_MM_SS, new Date(endTime)));
			
			String dateName = DateUtil.getStingDate(DateUtil.YYYYMMDD, new Date(startTime));
			String prefix = "tasktype" + taskType + "_" + taskId;
			
			boolean isExistResult = false;
			// 依次取nc_task/done下的result和return目录
			File[] fileDirs = FileUtil.getDirectoryArray(new File(Contants.localTaskDonePath));
			if(fileDirs==null){
				logger.info("fileDirs为空");
			}else{
				try{
					for(File dir : fileDirs){
						// -- 找到指定的日期目录dateName及之后的日期目录
						File[] dateFiles = FileUtil.sortASCByFileName(FileUtil.getDirsAfterDateName(dir, dateName));
						// -- 在找到的日期目录下检查文件是否存在
						for(File dateFile : dateFiles){
							File[] files = null;
							if("0".equals(isLoop)){// 0 非周期
								files = FileUtil.getFilesStartWith(dateFile, prefix);
							}else{//--- 周期任务取某一时间段内的结果与回传文件
								files = FileUtil.getFilesStartWithByMillis(dateFile, prefix, startTime, endTime);
							}
							
							if(files.length>0){// 若在任一目录下找到，则不用再找其他日期目录，跳出第二个For循环
								fileList.addAll(Arrays.asList(files));
								isExistResult = true;
								break;
							}
						}
					}
				}catch(Exception e){
					logger.error(e);
				}
			}
			if(!isExistResult){
				logger.info("再次获取任务结果 > TaskId: " + taskId + ", TaskType: "
						+ taskType + ", IsLoop: " + isLoop + " > 任务结果不存在");
			}
			
			return isExistResult;
		}
		
		/**
		 * 
		 * 检查done和incoming里是否有任务结果信息（如果有，则不进行再次执行，避免重复执行任务，如重复reboot）
		 * @author dell Jun 23, 2015
		 * @version 1.0 
		 * @param taskId
		 * @param taskType
		 * @param isLoop
		 * @param sTime
		 * @param eTime
		 * @param fileList
		 * @return
		 * @throws Exception
		 */
		private boolean checkTaskResultExistFromDoneAndIncoming(String taskId, String taskType, String isLoop, String sTime, String eTime, List<File> fileList) throws Exception{
			long startTime=(sTime==null || "".equals(sTime) || "null".equals(sTime)) ? 0l : Long.parseLong(sTime);
			long endTime=(eTime==null || "".equals(eTime) || "null".equals(eTime)) ? 0l : Long.parseLong(eTime);
			logger.debug("checkTaskResultExist startTime=" + DateUtil.getStingDate(DateUtil.YYYY_MM_DD_HH24_MM_SS, new Date(startTime))+" -- endTime=" + DateUtil.getStingDate(DateUtil.YYYY_MM_DD_HH24_MM_SS, new Date(endTime)));
			
			String dateName = DateUtil.getStingDate(DateUtil.YYYYMMDD, new Date(startTime));
			String prefix = "tasktype" + taskType + "_" + taskId;
			
			boolean isExistResult = false;
			// 依次取nc_task/done和incoming下的result和return目录
			File[] fileDoneDirs = FileUtil.getDirectoryArray(new File(Contants.localTaskDonePath));
			File[] fileIncomingDirs = FileUtil.getDirectoryArray(new File(Contants.localTaskResultPath));
			File[] fileDirs = null;
			if(fileIncomingDirs!=null && fileDoneDirs!=null) {				
				fileDirs = (File[])ArrayUtils.addAll(fileDoneDirs, fileIncomingDirs);
			}
			if(fileDirs==null){
				logger.info("fileDirs为空");
			}else{
				try{
					for(File dir : fileDirs){
						// -- 找到指定的日期目录dateName及之后的日期目录
						File[] dateFiles = FileUtil.sortASCByFileName(FileUtil.getDirsAfterDateName(dir, dateName));
						// -- 在找到的日期目录下检查文件是否存在
						for(File dateFile : dateFiles){
							File[] files = null;
							if("0".equals(isLoop)){// 0 非周期
								files = FileUtil.getFilesStartWith(dateFile, prefix);
							}else{//--- 周期任务取某一时间段内的结果与回传文件
								files = FileUtil.getFilesStartWithByMillis(dateFile, prefix, startTime, endTime);
							}
							
							if(files.length>0){// 若在任一目录下找到，则不用再找其他日期目录，跳出第二个For循环
								fileList.addAll(Arrays.asList(files));
								isExistResult = true;
								break;
							}
						}
					}
				}catch(Exception e){
					logger.error(e);
				}
			}
			if(!isExistResult){
				logger.info("判断新下发的任务结果是否已经存在 > TaskId: " + taskId + ", TaskType: "
						+ taskType + ", IsLoop: " + isLoop + " > 任务结果不存在");
			}else {
				logger.info("判断新下发的任务结果是否已经存在 > TaskId: " + taskId + ", TaskType: "
						+ taskType + ", IsLoop: " + isLoop + " > 任务结果已存在");
			}
			
			return isExistResult;
		}
		
		/**
		 * DC再次获取任务结果 -- 移动找到的结果文件和回传文件到incoming目录
		 */
		private void moveTaskResultOrReturn(List<File> fileList){
			if(fileList==null || fileList.size()==0){
				return;
			}
			try {
				for(File file : fileList){
					// ---------- 任务回传文件处理
					if(file.getName().endsWith(Contants.TASK_RETURN_FILE_SUFFIX)){
						if(!file.exists() || !file.isFile()){
							continue;
						}
						// 移动实际回传的文件
						String[] resultArr = FileWrUtil.cfgFileReader(file);
						if (resultArr != null && resultArr.length > 0) {
							JSONObject jsonObject = JSONObject.fromObject(resultArr[0]);
							ReturnFilePO rfPo = (ReturnFilePO) JSONObject.toBean(jsonObject, ReturnFilePO.class);
							if(rfPo.getReturnFileName()!=null && !"".equals(rfPo.getReturnFileName())){
								File returnFile = new File(file.getParent() + File.separator + rfPo.getReturnFileName());
								FileUtil.moveFile(returnFile, Contants.localTaskReturnPath, true);
							}
						}
						// 移动记录任务回传的临时文件
						FileUtil.moveFile(file, Contants.localTaskReturnPath, true);
					}else {
					// ---------- 任务结果处理	
						FileUtil.moveFile(file, Contants.localTaskResultPath, true);
					}
				}
			} catch (Exception e) {
				logger.error("Get the task result again > mobile file exception again", e);
			}
		}
	}
	
}