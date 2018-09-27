package com.nis.nmsclient;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.nis.nmsclient.common.Common;
import com.nis.nmsclient.common.Contants;
import com.nis.nmsclient.common.SysConfig;
import com.nis.nmsclient.config.DetecConfReqHandle;
import com.nis.nmsclient.model.AlarmInfo;
import com.nis.nmsclient.model.SetInfo;
import com.nis.nmsclient.thread.WritePidThread;
import com.nis.nmsclient.thread.alarm.AlarmThread;
import com.nis.nmsclient.thread.alarm.AlarmUtil;
import com.nis.nmsclient.thread.alarm.ErrorCode;
import com.nis.nmsclient.thread.socket.CommonSocket;
import com.nis.nmsclient.thread.socket.SSLClient;
import com.nis.nmsclient.thread.socket.SSLServer;
import com.nis.nmsclient.thread.task.TaskResultOper;
import com.nis.nmsclient.thread.timer.DelLocalFileThread;
import com.nis.nmsclient.thread.upload.DataSendThread;
import com.nis.nmsclient.util.FileUtil;
import com.nis.nmsclient.util.ProcessUtil;
import com.nis.nmsclient.util.Utils;

/**
 * NMSClient 程序启动主方法类
 */
public class NmsClient{
	static Logger logger = Logger.getLogger(NmsClient.class);
	// 监测设置信息，初始化完成后清空
	public List<SetInfo> setInfos = new LinkedList<SetInfo>();
	// 监测设置的报警字段设置信息，初始化完成后清空
	public Map<Long, List<AlarmInfo>> alarmInfos = new HashMap<Long, List<AlarmInfo>>();
	private int testGap = 60; //单位：秒
	
	static{
//		Thread.currentThread().setName("NMSClient主程序");
		Thread.currentThread().setName("NMSClient Main Program");
	}
	
	/**
	 * NMSClient 程序启动入口
	 */
	public static void main(String[] args) {
		logger.info("------- NMSClient 启动开始------------");
		
		//NMSClient进程停止保存缓存操作
		doShutDownWork();
		
		/**
		 * 启动通信程序, 如果端口已存在即SSLServer创建失败，退出程序
		 */
		//通讯端口放在第一步，为了确保初始化配置不成功或个数为0时 新建任务下发、DC收集数据、握手等操作 能正常执行
		//注意：要在通讯程序中判断SeqId是否为空，为空 则只接收握手通信、收集数据通信，其他的通信都抛弃
		SSLServer sslServer = null;
		try {
			sslServer = new SSLServer();
		} catch (IOException e) {
			TaskResultOper.handerAgentUpgradeResult(false);
			logger.error("NMSClient Program termination：" + e.getMessage());
			AlarmUtil.sendNMSErrorMsg(ErrorCode.ProtListenerError, Utils.getLocalIp(), "i18n_client.NmsClient.ncCommunicatePortErr_n81i");
			System.exit(0);
		}
//		Thread server = new Thread(sslServer, "通讯线程");
		Thread server = new Thread(sslServer, "Communication Thread");
		server.start();
		
		/**
		 * 启动守护进程
		 */
		try {
			String os = System.getProperty("os.name");
			String cmd = "";
			String procSearchKey = null;
			if (os.startsWith("Windows")) {
				String homePath = new File(Contants.SYSTEM_PATH).getParent();//NC布署路径
				cmd = homePath + File.separator + "script" + File.separator + "nmsclient_shouhu.bat";
				procSearchKey = "nmsclient_shouhu.bat";
			} else if (os.startsWith("Linux")) {
				cmd = Contants.SYSTEM_PATH + File.separator + "nmsclient_shouhu.sh";
				procSearchKey = "nmsclient_shouhu.sh";
			} 
			Object[] objArr = ProcessUtil.checkPidAndGetPid(null, procSearchKey);
			int isExistFlag = Integer.parseInt(objArr[0].toString());
			if(isExistFlag == 0){// 守护进程不存在，启动
				logger.info("正在启动守护进程...");
				ProcessUtil.runExec(cmd, new String[]{Contants.localTaskResultPath}, null, null, true);
				objArr = ProcessUtil.checkPidAndGetPid(null, procSearchKey);
				isExistFlag = Integer.parseInt(objArr[0].toString());
				if(isExistFlag != 0){
					logger.info("守护进程 启动成功");
				}else{
					logger.info("守护进程 启动失败");
					AlarmUtil.sendNMSErrorMsg(ErrorCode.DeamonNotExist, Utils.getLocalIp(), "i18n_client.NmsClient.ncDeamonStartFail_n81i");
				}
			}else {
				logger.info("守护进程 已存在，无需再启动");
			}
			
		} catch (Exception e) {
			logger.error("Start the daemon exception", e);
			AlarmUtil.sendNMSErrorMsg(ErrorCode.DeamonNotExist, Utils.getLocalIp(), "i18n_client.NmsClient.ncDeamonStartException_n81i，" + e.getMessage());
		}
		
		/**
		 * 相关业务操作入口
		 */
		new NmsClient().run();
		
	}
	
	public void run() {
		//执行写PID线程
		Common.service.submit(new WritePidThread());
		
		// 2013-3-8 由于初始化监测配置个数为0时不断重新获取，致使任务初始化无法执行，先将任务与监测配置分开执行
		// 为了将初始化监测配置放于任务之后，所以在开始执行之前，先与DC握手通讯，以保证通讯正常
		while (true) {
			try {
				//与Server通信
				Future<?> serFuture = Common.service.submit(new SSLClient(
						Thread.currentThread().getName(),
						CommonSocket.REQ_HAND_SHAKE, null));
				if (Contants.isSucessByResult((String) serFuture.get())) {
					break;
				}
			} catch (Exception e) {
				logger.error("Handshake communication abnormality：" + Utils.printExceptionStack(e));
			}

			try {
				Thread.sleep(1000 * testGap);// 如果握手失败，让当前线程暂停N秒，再重试
			} catch (InterruptedException e) {
				logger.error(Utils.printExceptionStack(e));
				continue;
			}
		}
		
		// 获取本机唯一标识
		initUUID();
		// 检查本机操作系统和IP是否变更
		checkLocalOperSystemAndIp();
		
		/**************************** 任务部分处理操作 ***************************/
		// 处理Agent自身升级时的执行结果文件
		TaskResultOper.handerAgentUpgradeResult(true);
		// 发送所有之前上发失败的任务结果
		TaskResultOper.initSendAllTaskResult();
		
		// 初始化执行中的任务
		if (Contants.DEBUG_INIT_TASK_FLAG == 0) {
//			Common.scheduled.schedule(new SSLClient("初始化任务",
			Common.scheduled.schedule(new SSLClient("Initialization Task",
					CommonSocket.REQ_INIT_TASK, null),
					Contants.COMMON_TASK_INIT_DELAY_MINUTES, TimeUnit.MINUTES);
		}
		// 定时上传发送失败的任务结果
		/*if (Contants.DEBUG_TASKRESULT_FLAG == 0) {
			Common.scheduled.scheduleWithFixedDelay(new Runnable() {
				public void run() {
					Thread.currentThread().setName("上传任务结果");
					TaskResultOper.initSendAllTaskResult();
				}
			}, 0, Contants.COMMON_TASK_RESULT_SEND_MINUTES, TimeUnit.MINUTES);
		}*/
		// 定时上传发送失败的回传文件
		/*if (Contants.DEBUG_TASKRETURN_FLAG == 0) {
			Common.scheduled.scheduleWithFixedDelay(new Runnable() {
				public void run() {
					Thread.currentThread().setName("回传文件");
					new TaskReturnHandle().sendAllTaskReturnFile();
				}
			}, 0, Contants.COMMON_TASK_RESULT_SEND_MINUTES, TimeUnit.MINUTES);
		}*/
		// 定时清理内存中已完成的任务
		Common.scheduled.scheduleWithFixedDelay(new Runnable() {
			public void run() {
//				Thread.currentThread().setName("清理已完成任务");
				Thread.currentThread().setName("Clean Up The Completed Task");
				// ==  1、针对结果文件过多时打包上传未完成的文件
				File taskDir = new File(Contants.localTaskPath);
				if (taskDir.exists()) {
					// ----取所有未上传完成的Zip文件
					File[] zipArr = FileUtil.getFilesEndWith(taskDir, ".zip");
					// 若存在未上传完成的ZIP结果文件，则不清理内存中的任务
					if (zipArr.length > 0) {
						return;
					}
				}
				
				// ==  2、检查当前结果文件数量
				File resultDir = new File(TaskResultOper.getTaskResultPath());
				if(resultDir.exists()){
					File[] fileArr = FileUtil.getFilesEndWith(resultDir, Contants.TASK_RESULT_FILE_SUFFIX);
					// 若存在未上传的结果文件，则不清理内存中的任务
					if(fileArr.length > 0){
						return;
					}
				}
				
				// -- 清理已完成的任务，待考虑 以后定时清理方案
				Common.removeCancelAndDoneTaskFuture();
			}
		}, Contants.COMMON_TASK_CLEAR_HOURS, Contants.COMMON_TASK_CLEAR_HOURS, TimeUnit.HOURS);
		
		/**************************** 定时清理本地文件操作 ***************************/
		// 定时删除本地生成的文件
		if (Contants.DEBUG_DELFILE_FLAG == 0) {
			//2012-4-28 将所有删除文件的线程合并为一个，取设置的所有清理文件间隔中的最小值作为检查间隔
			//2012-12-17 第一次执行删除文件时间，不是启动立即删除，设置延迟到启动之后的第一个凌晨0点
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_MONTH, 1);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			long delay = cal.getTimeInMillis()-System.currentTimeMillis();
//			Common.scheduled.scheduleAtFixedRate(new DelLocalFileThread("删除文件"), delay,
			Common.scheduled.scheduleAtFixedRate(new DelLocalFileThread("Delete Files"), delay,
					getMinCheckPeriod() * 60 * 60 * 1000, TimeUnit.MILLISECONDS);
		}
		
		/**************************** 监测配置信息处理操作 ***************************/
		// 初始化监测配置信息
		initDetecConfig();
		// 启动三方监测程序
		if (Contants.DEBUG_PLUGIN_FLAG == 0) {
			//-------------初始化三方监测 start
			for (SetInfo setInfo : setInfos) {
				// 缓存三方监测配置信息，用于合并临时结果文件
				Common.putPluginDetecSetInfo(setInfo.getId(), setInfo);
				
				if (!Common.COMMON_SYS_SETINFO.equals(setInfo.getIsSchedule())) { // 第三方且由Agent启动
					Common.startPluginDetec(setInfo);
				}
			}
			//-------------初始化三方监测 end
		}
		//启动预设监测程序
		if (Contants.DEBUG_SYSDETECT_FLAG == 0) {
			//-------------初始化预设监测 start
			for (SetInfo setInfo : setInfos) {
				if (Common.COMMON_SYS_SETINFO.equals(setInfo.getIsSchedule())) {// 判断如果是系统预设类型
					Common.addOrUpdateSysDetec(setInfo, alarmInfos
							.get(setInfo.getId()));
				}
			}
			//-------------初始化预设监测 end
		}
		
		//启用监测主动上报
		if(Contants.DATA_SEND_THREAD_FLAG == 0){
			Common.scheduled.scheduleWithFixedDelay(new DataSendThread(Contants.DATA_SEND_THREAD_HOST, Contants.DATA_SEND_THREAD_PORT), (int)(Math.random()*Contants.DATA_SEND_THREAD_INTERVAL), Contants.DATA_SEND_THREAD_INTERVAL, TimeUnit.SECONDS);
			logger.info("监测主动上报已成功添加到线程池");
		}
		
		// 启动上传数据程序
		/*if (Contants.DEBUG_UPLOADDATA_FLAG == 0) {
			Common.scheduled.scheduleWithFixedDelay(new UploadDataThread("上传数据"), 1,
					Contants.COMMON_UPLOAD_DATA_MINUTES, TimeUnit.MINUTES);
		}*/
		// 启动主动报警程序
		if (Contants.DEBUG_ALARM_FLAG == 0) {
//			Common.scheduled.scheduleAtFixedRate(new AlarmThread("主动预警"), 1,
			Common.scheduled.scheduleAtFixedRate(new AlarmThread("Active Early Warning"), 1,
					Contants.COMMON_UPLOAD_DATA_MINUTES, TimeUnit.MINUTES);
		}
		
		//清空变量
		setInfos.clear();
		setInfos = null;
		alarmInfos.clear();
		alarmInfos = null;
	}
	
	/**
	  * 第一次布署NMSAgent时，初始化本机唯一标志
	  */
	 public void initUUID() {
	  if (Contants.AGENT_HOST_UUID == null) {// 第一次布署Agent
	   while (true) {
	    String uuid = null;
	    try {
	     Future<?> future = Common.service.submit(new SSLClient(
//	       "获取本机标识", CommonSocket.REQ_LOCAL_UUID, null));
	       "Obtain The Local Identity", CommonSocket.REQ_LOCAL_UUID, null));
	     String msg = (String) future.get();
	     if (Contants.isSucessByResult(msg)) {
	    	 // dc发送的数据格式为 uuid:::localIp
		      String[] result = Contants.getDescByResult(msg).split(
		        ":::");
		      uuid = result[0];
		      String localIp = result[1];
		      logger.info("本机标识ID：" + uuid);
		      if (!(uuid == null || "".equals(uuid)
		        || "null".equals(uuid) || localIp == null
		        || "".equals(localIp) || "null".equals(localIp))) {
		    	  	
		    	  SysConfig.setUUIDValue(uuid);
			      //根据ip地址获取端口名称
			      String name = Utils.getNetInterfaceNameByIp(localIp);
			      //将端口名称写入配置文件
			      SysConfig.setInterfaceNameValue(name);
			      break;
		      }
	     }
	    } catch (Exception e) {
	     logger.error("Get the unique identity of the native or IP port name exception：" + Utils.printExceptionStack(e));
	    }
	    try {
	     logger.debug((1000 * testGap ) +"s 后重试");
	     Thread.sleep(1000 * testGap);// 如果获取失败，让当前线程暂停N秒，再重试
	    } catch (InterruptedException e) {
	     logger.error(Utils.printExceptionStack(e));
	    }
	   }
	  }
	 }
	/**
	  * 每次启动时检查本机操作系统类型和IP是否有变更，若有，则将最新信息写入文件并发送信息到Server，
	  * 由Server端根据SeqID更新所有相关节点信息
	  */
	 public void checkLocalOperSystemAndIp() {
	  // ---- 取本机相关信息
	  String operateSysType = getOperateSysType();
	  String localIp = Utils.getIpAddressByEthName(Contants.AGENT_INTERFACE_NAME_KEY);

	  logger.info("本机通讯IP：" + localIp);
	  if (localIp != null && !"".equals(localIp) && !"null".equals(localIp)) {
	   // ----- 与原有信息比较
	   if (Contants.AGENT_OPERATE_SYSTEM == null
	     || Contants.AGENT_LOCAL_IP == null
	     || !operateSysType.equals(Contants.AGENT_OPERATE_SYSTEM)
	     || !localIp.equals(Contants.AGENT_LOCAL_IP)) {
	    // 若有变更，则将本机系统和IP写入文件，再发送到Server
	    SysConfig.setUUIDValue(operateSysType, localIp);
	    // 发送UUID、SystemType、LocalIp到Server
	    String sendMsg = Contants.AGENT_HOST_UUID
	      + Contants.COMMON_MSG_SEPRATOR
	      + Contants.AGENT_OPERATE_SYSTEM;
	    try {
	     Future<?> future1 = Common.service.submit(new SSLClient(
//	       "信息变更", CommonSocket.REQ_LOCAL_CHANGE, sendMsg));
	       "Information Change", CommonSocket.REQ_LOCAL_CHANGE, sendMsg));
	     String resultMsg = (String) future1.get();
	     String descMsg = Contants.getDescByResult(resultMsg);
	     if (!Contants.isSucessByResult(resultMsg)) {
	      if (descMsg == null || "".equals(descMsg)
	        || "null".equals(descMsg)) {
//	       descMsg = "信息变更出现问题，可能存在重复IP，请手动检查";
	       descMsg = "Information changes may occur. Duplicate IP may exist. Please check manually.";
	      }
	      logger.error("Failure of information change：" + descMsg);
	     } else if (descMsg != null && !"".equals(descMsg)) {
	      logger.info("信息变更：" + descMsg);
	     }
	    } catch (Exception e) {
	     logger.error("Information change：" + Utils.printExceptionStack(e));
	    }
	   }
	  }
	 }
	 

		/**
		 * 获取本机操作系统类型
		 * 
		 * @return 1代表linux ；2代表windows； “” 为其它
		 */
		public static String getOperateSysType() {
			String operateSysType = null;
			String os = System.getProperty("os.name");
			if (os.startsWith("Windows")) {
				operateSysType = "2";
			} else if (os.startsWith("Linux")) {
				operateSysType = "1";
			} else {
				operateSysType = "";
			}
			return operateSysType;
		}

	
//	/**
//	 * 第一次布署NMSAgent时，初始化本机唯一标志
//	 */
//	public void initUUID(){
//		if(Contants.AGENT_HOST_UUID == null){//第一次布署Agent
//			while (true) {
//				String uuid = null;
//				try {
//					Future<?> future = Common.service.submit(new SSLClient(
//							"获取本机标识", CommonSocket.REQ_LOCAL_UUID, null));
//					String msg = (String) future.get();
//					if (Contants.isSucessByResult(msg)) {
//						uuid = Contants.getDescByResult(msg);
//						logger.info("本机标识ID：" + uuid);
//						if(uuid != null && !"".equals(uuid) && !"null".equals(uuid)){
//							SysConfig.setUUIDValue(uuid);
//							break;
//						}
//					}
//				} catch (Exception e) {
//					logger.error("获取本机唯一标识异常：" + Utils.printExceptionStack(e));
//				}
//
//				try {
//					Thread.sleep(1000 * testGap);// 如果获取失败，让当前线程暂停N秒，再重试
//				} catch (InterruptedException e) {
//					logger.error(Utils.printExceptionStack(e));
//				}
//			}
//		}
//	}
//	
//	/**
//	 * 每次启动时检查本机操作系统类型和IP是否有变更，若有，则将最新信息写入文件并发送信息到Server，由Server端根据SeqID更新所有相关节点信息
//	 */
//	public void checkLocalOperSystemAndIp(){
//		//---- 取本机相关信息
//		String operateSysType = null;
//		String os = System.getProperty("os.name");
//		if (os.startsWith("Windows")) {
//		     operateSysType = "2";
//		}else if (os.startsWith("Linux")){
//			operateSysType = "1";
//		}else{
//			operateSysType = "";
//		}
//		String localIp = Utils.getLocalIp();
//		//----- 与原有信息比较
//		if (Contants.AGENT_OPERATE_SYSTEM == null
//				|| Contants.AGENT_LOCAL_IP == null
//				|| !operateSysType.equals(Contants.AGENT_OPERATE_SYSTEM)
//				|| !localIp.equals(Contants.AGENT_LOCAL_IP)) {
//			//若有变更，则将本机系统和IP写入文件，再发送到Server
//			SysConfig.setUUIDValue(operateSysType, localIp);
//			//发送UUID、SystemType、LocalIp到Server
//			String sendMsg = Contants.AGENT_HOST_UUID
//					+ Contants.COMMON_MSG_SEPRATOR
//					+ Contants.AGENT_OPERATE_SYSTEM;
//			try {
//				Future<?> future = Common.service.submit(new SSLClient("信息变更",
//						CommonSocket.REQ_LOCAL_CHANGE, sendMsg));
//				String resultMsg = (String) future.get();
//				String descMsg = Contants.getDescByResult(resultMsg);
//				if (!Contants.isSucessByResult(resultMsg)) {
//					if(descMsg == null || "".equals(descMsg) || "null".equals(descMsg)){
//						descMsg = "信息变更出现问题，可能存在重复IP，请手动检查";
//					}
//					logger.error("信息变更失败：" + descMsg);
//				}else if(descMsg!=null && !"".equals(descMsg)){
//					logger.info("信息变更：" + descMsg);
//				}
//			} catch (Exception e) {
//				logger.error("信息变更：" + Utils.printExceptionStack(e));
//			}
//		}
//	}
	
	/**
	 * 请求获得初始化监测配置信息： 获取失败或获取配置个数为0，则不断循环获取
	 */
	public void initDetecConfig(){
		while (true) {
			try {
				Future<?> future = Common.service.submit(new SSLClient(
//						"初始化监测配置", CommonSocket.REQ_INIT_CONFIG, null));
						"Initialization Of Monitoring Configuration", CommonSocket.REQ_INIT_CONFIG, null));
				String msg = (String) future.get();
				if (Contants.isSucessByResult(msg)) {
					msg = Contants.getDescByResult(msg);
					new DetecConfReqHandle().handlerConfigByInit(msg, setInfos, alarmInfos);
					logger.info("初始化监测配置个数：" + setInfos.size());
					if(setInfos.size()>0){
						break;
					}
					
				}
			} catch (Exception e) {
				logger.error("Initialization of monitoring configuration exceptions：" + Utils.printExceptionStack(e));
			}
			
			try {
				Thread.sleep(1000 * testGap);//如果初始化失败，让当前线程暂停N秒，再重试
			} catch (InterruptedException e) {
				logger.error(Utils.printExceptionStack(e));
			}
		}
	}
	
	/**
	 * 取设置的所有清理文件间隔中的最小值， 单位：小时
	 */
	public int getMinCheckPeriod(){
		int period = Contants.COMMON_DEL_DATA_HOURS;
		period = period < Contants.COMMON_DEL_LOG_DAYS * 24 ? period
				: Contants.COMMON_DEL_LOG_DAYS * 24; 
		period = period < Contants.COMMON_DEL_TEMP_DAYS * 24 ? period
				: Contants.COMMON_DEL_TEMP_DAYS * 24;
		period = period < Contants.COMMON_DEL_UPGRADEFILE_DAYS * 24 ? period
				: Contants.COMMON_DEL_UPGRADEFILE_DAYS * 24;
		logger.debug("=========del file period=" + period);
		
		return period;
	}
	
	
	/**
	 * 进程停用时，触发该事件，将缓存数据存入硬盘
	 * 在NC侧 当前情况是当NMSClient服务停止时无需做操作，因为NC启动的时候NC会将执行失败的任务结果发送给DC（TaskResultOper.initSendAllTaskResult()），
	 * 				再收集DC的任务，不会有数据丢失。
	 * 
	 * 暂时无需做操作，先提供退出触发的机制，供后续使用
	 * 
	 * @author jinshujuan Jul 15, 2013
	 * @version 1.0
	 */
	public static void doShutDownWork() {
		logger.info("注册程序退出事件");
		Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
//						Thread.currentThread().setName("退出NMSClient，缓存数据清理线程");
						Thread.currentThread().setName("Exit NMSClient, Caching Data Cleaning Thread");
						logger.info("停止NMSClient，处理缓存数据。。。");
						//清理缓存数据Common类控制
						//logger.info("清空缓存");
						logger.info("停止NMSClient，处理缓存数据 完成");
					} catch (Exception ex) {
						logger.error("Stop NMSClient, cache data entry anomalies, cache data to disk.", ex);//1.全部入库，入库异常时存盘 或 2.全部存盘，下次启动时入库
						//缓存数据存入硬盘操作	
						//logger.info("保存缓存");
						logger.error("Stop NMSClient, cache data entry anomalies, cache data stored in hard disk to complete", ex);//1.全部入库，入库异常时存盘 或 2.全部存盘，下次启动时入库
					}
				}
		});
	} 
	
}
