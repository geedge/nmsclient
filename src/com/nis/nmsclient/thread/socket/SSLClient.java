package com.nis.nmsclient.thread.socket;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import net.sf.json.JSONArray;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.nis.nmsclient.common.Contants;
import com.nis.nmsclient.model.ReturnFilePO;
import com.nis.nmsclient.thread.task.TaskReqHandle;
import com.nis.nmsclient.thread.task.TaskResultOper;
import com.nis.nmsclient.util.FileUtil;
import com.nis.nmsclient.util.Utils;


/**
 * 安全通讯的客户端
 **/

public class SSLClient extends CommonSocket implements Callable<Object>{
	Logger logger = Logger.getLogger(SSLClient.class);
	private String name;
	private String reqCmd;
	private Object obj;
	private String serverHost;
	
	public SSLClient(String name, String reqCmd, Object obj) {
		this.name = name;
		this.reqCmd = reqCmd;
		this.obj = obj;
		this.serverHost = Contants.SOCKET_SERVER_HOST;
	}
	
	public SSLClient(String name, String reqCmd, Object obj, String serverHost) {
		this.name = name;
		this.reqCmd = reqCmd;
		this.obj = obj;
		this.serverHost = serverHost;
	}

	/**
	 * 初始化客户端Socket
	 **/
	public void init() throws Exception {
		SSLContext ctx = SSLCertOper.getSSLContext();
		SSLSocketFactory ssf = ctx.getSocketFactory();

		socket = (SSLSocket) ssf.createSocket(serverHost,
				Contants.SOCKET_SERVER_PORT);
		logger.debug("create socket success.");
		
		//2014-1-23 hyx 如果建立socket成功，但是startHandshake握手失败，且未设置超时时间时，则会一直阻塞
		socket.setSoTimeout(1000 * 60 * Contants.SOCKET_TIMEOUT_MINUTES);
		
		((SSLSocket) socket).startHandshake();
		logger.debug("handshake success.");
	}

	@Override
	public Object call() throws Exception {
//		Thread.currentThread().setName(name + ">>通信 " + serverHost);
		Thread.currentThread().setName(name + ">>Communication " + serverHost);
		Object object = null;
		try {
			init();
			if(socket!=null){
				out = socket.getOutputStream();
				in = socket.getInputStream();
				//设置超时时间
				socket.setSoTimeout(1000 * 60 * Contants.SOCKET_TIMEOUT_MINUTES);
				object = toDo();
			}
		}catch (Exception e) {
//			object = Contants.COMMON_MSG_FAIL + Contants.COMMON_MSG_SEPRATOR + "异常";
			object = Contants.COMMON_MSG_FAIL + Contants.COMMON_MSG_SEPRATOR + "anomaly";
			logger.error("Communication anomaly：" + Utils.printExceptionStack(e));
		} finally {
			logger.debug("关闭通信");
			close();
		}
		
		return object;
	}
	
	protected Object toDo() throws Exception {
		logger.debug("发送通信请求：" + reqCmd);
		//-- 无效操作处理
		if(StringUtils.isEmpty(reqCmd)){
			return null;
		}
		boolean flag = false;
		String msg = null;
		String result = null;
		//-- 命令判断
		// 与Server通信
		if(reqCmd.equals(REQ_HAND_SHAKE)){
			flag = this.sendMessageByChar(reqCmd);
			logger.debug("握手状态：" + (result = this.receiveMessageByChar()));
		}
		// 通知Server准备升级
		if(reqCmd.equals(REQ_SERVER_UPGRADE)){
			flag = this.sendMessageByChar(reqCmd);
			result = this.receiveMessageByChar();
		}
		// 获取本机标志UUID
		if(reqCmd.equals(REQ_LOCAL_UUID)){
			flag = this.sendMessageByChar(reqCmd);
			msg = this.receiveMessageByChar();
			flag = this.sendMessageByChar(CommonSocket.SUCCESS);
			logger.info("本机标志UUID：" + msg);
		}
		//注释 by jinsj 2012-0531 修改为DC主动获取NC时间
		// 获取服务器系统时间
		if(reqCmd.equals(REQ_SERVER_SYSTEMDATE)){
			flag = this.sendMessageByChar(reqCmd);
			msg = this.receiveMessageByChar();
			logger.debug("服务器系统时间：" + msg);
			flag = this.sendMessageByChar(CommonSocket.SUCCESS);
		}
		// 发送本机的变更信息（操作系统类型和IP）
		if(reqCmd.equals(REQ_LOCAL_CHANGE)){
			// 发送请求
			flag = this.sendMessageByChar(reqCmd);
			result = this.receiveMessageByChar();
			// 发送信息: UUID$@$操作系统类型$@$LocalIP
			flag = this.sendMessageByChar((String)obj);
			// 接收变更结果: 0/1 $@$ 信息
			msg = this.receiveMessageByChar();
			flag = this.sendMessageByChar(CommonSocket.SUCCESS);
			return msg;
		}
		// 初始化配置
		if(reqCmd.equals(REQ_INIT_CONFIG)){
			flag = this.sendMessageByChar(reqCmd);
			msg = this.receiveMessageByChar();//接收配置信息
			flag = this.sendMessageByChar(CommonSocket.SUCCESS);
		}
		// 初始化任务
		if(reqCmd.equals(REQ_INIT_TASK)){
			// 发送请求
			flag = this.sendMessageByChar(reqCmd);
			result = this.receiveMessageByChar();
			// 发送本机唯一标识
			flag = this.sendMessageByChar(Contants.AGENT_HOST_UUID + "");
			msg = this.receiveMessageByChar();
			flag = this.sendMessageByChar(CommonSocket.SUCCESS);
			if (msg != null && !"".equals(msg)) {
				JSONArray jsonArr = JSONArray.fromObject(msg);
				//这里处理的任务，原则上不应含有”1 文件推送“类型的任务
				for (int i = 0; i < jsonArr.size(); i++) {
					new TaskReqHandle().taskHandle(jsonArr.get(i).toString());
				}
			}
			logger.debug("初始化任务完成--" + msg);
			return null;
		}
		// 发送主动告警信息
		if(reqCmd.equals(REQ_ALARM)){
			if(obj!=null){
				// 主动告警请求
				flag = this.sendMessageByChar(reqCmd);
				result = this.receiveMessageByChar();
				// 发送主动告警信息内容
				flag = this.sendMessageByChar((String)obj);
				logger.debug("主动告警信息：" + (String)obj);
				result = this.receiveMessageByChar();
			}else{
				logger.debug("主动告警信息为空");
			}
		}
		// 发送任务结果
		if(reqCmd.equals(REQ_TASK_RESULT)){
			if(obj!=null){
				//发送任务结果请求
				flag = this.sendMessageByChar(reqCmd);
				result = this.receiveMessageByChar();
				//发送任务结果内容
				flag = this.sendMessageByChar((String)obj);
				result = this.receiveMessageByChar();
			}else{
				logger.warn("Task result information is empty");
			}
		}
		// 批量上传监测数据【数据收集方式改为DC主动后，此通信废弃】
		if(reqCmd.equals(REQ_UPLOAD_DATAS)){
			if(obj!=null && obj instanceof Object[]) {
				Object[] objArr = (Object[])obj;
				if (objArr != null && objArr.length > 1 && objArr[0] != null
						&& objArr[1] != null && objArr[1] instanceof List) {
					//发送上传数据请求
					flag = this.sendMessageByChar(reqCmd);
					result = this.receiveMessageByChar();
					//上传数据
					this.sendFileByBath((String) objArr[0], (List<File>) objArr[1]);
					result = this.receiveMessageByChar();
				}else{
					logger.warn("Uploading the contents of the monitored data object is incorrect");
				}
			}else{
				logger.warn("Uploading monitoring data objects is empty");
			}
		}
		// 任务执行的回传文件：单个文件发送，断点续传【数据收集方式改为DC主动后，此类废弃】
		if(reqCmd.equals(REQ_TASK_RETURNFILE)){
			if(obj!=null && obj instanceof ReturnFilePO){
				ReturnFilePO rfPo = (ReturnFilePO)obj;
				//发送回传文件请求
				flag = this.sendMessageByChar(reqCmd);
				result = this.receiveMessageByChar();
				//发送回传文件任务信息
				this.sendMessageByChar(TaskResultOper.getTaskResultMsg(rfPo
						.getTaskId(), rfPo.getTaskType(), null, null, null, rfPo
						.getStartTime(), rfPo.getEndTime(), rfPo.getIsLoop()));
				result = this.receiveMessageByChar();
				//发送回传文件文件名称
				this.sendMessageByChar(rfPo.getReturnFileName());
				result = this.receiveMessageByChar();
				//发送回传文件
				flag = this.bpSendFile(Contants.localTaskReturnPath + File.separator + rfPo.getReturnFileName());
				result = this.receiveMessageByChar();
			}else{
				logger.warn("The return file object is empty");
			}
		}
		
		// 发送压缩文件，断点续传
		if(reqCmd.equals(REQ_BP_UPLOAD_FIFE)){
			if(obj!=null && obj instanceof String[]) {
				String[] strArr = (String[])obj;
				if (strArr != null && strArr.length > 1){
					//打包上传文件请求
					flag = this.sendMessageByChar(reqCmd + ":" + strArr[0]);
					result = this.receiveMessageByChar();
					//发送打包文件名
					File file = new File(strArr[1]);
					flag = this.sendMessageByChar(file.getName());
					result = this.receiveMessageByChar();
					//上传打包文件
					flag = this.bpSendFile(strArr[1]);
					result = this.receiveMessageByChar();
					//上传成功后删除或移动文件
					if(flag && SUCCESS.equalsIgnoreCase(result) && file.exists()){
						String dataType = strArr[0];
						if(BP_TYPE_DETECT_DATA.equalsIgnoreCase(dataType)){
							FileUtil.moveFile(file, Contants.localDataDonePath, true);
						}else if(BP_TYPE_TASK_RESULT.equalsIgnoreCase(dataType)){
							FileUtil.moveFile(file, Contants.localTaskDonePath, true);
						}else if(BP_TYPE_TASK_RETURN.equalsIgnoreCase(dataType)){
							FileUtil.moveFile(file, Contants.localTaskDonePath, true);
						}
					}
				}
			}
		}
		// 向DC发送NC端异常信息
		if(reqCmd.equals(REQ_ERROR_INFO)){
			if(obj!=null){
				flag = this.sendMessageByChar(reqCmd);
				result = this.receiveMessageByChar();
				//发送异常内容
				flag = this.sendMessageByChar((String)obj);
				result = this.receiveMessageByChar();
			}else{
				logger.warn("Abnormal information is empty");
			}
		}
		
		if (flag && (SUCCESS.equalsIgnoreCase(result) || msg!=null)) {
			msg = Contants.COMMON_MSG_SUCCESS + Contants.COMMON_MSG_SEPRATOR + (msg!=null ? msg : "成功");
		} else {
//			msg = Contants.COMMON_MSG_FAIL + Contants.COMMON_MSG_SEPRATOR + "失败";
			msg = Contants.COMMON_MSG_FAIL + Contants.COMMON_MSG_SEPRATOR + "failed";
		}
		logger.debug("SSLClient toDo()---" + msg);
		
		logger.debug("发送通信请求结束：" + reqCmd);
		// -- 命令判断
		return msg;
	}
}