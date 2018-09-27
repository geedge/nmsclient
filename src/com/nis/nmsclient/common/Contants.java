package com.nis.nmsclient.common;

import java.io.File;

import org.apache.commons.lang.StringUtils;

import com.nis.nmsclient.util.FileUtil;


public class Contants {
	
	public static final String SYSTEM_PATH;		//NMSClient应用启动主目录
	
	/** ========= SSLSocket相关配置参数，客户与服务两端交互数据信息 ========**/
	public static final String SOCKET_SERVER_HOST_KEY= "server_host";
	public static String SOCKET_SERVER_HOST;//服务器IP
	public static final Integer SOCKET_SERVER_PORT;//服务器端口
	public static final Integer SOCKET_AGENT_PORT;//客户端端口
	public static final Integer SOCKET_TIMEOUT_MINUTES;// Socket通信超时时间
	public static final String SSL_KEY_STORE;//key证书库文件
	public static final String SSL_TRUST_KEY_STORE;//认证证书库文件
	public static final String SSL_JSSE_TYPE = "TLS";//类型TLS、SSL
	public static final String SSL_KEYSTORE_TYPE = "JCEKS";//KeyStore的类型有：jceks、jks
	public static final String SSL_KEY_PRIVATE_PASS = "123456";
	public static final String SSL_KEY_STORE_PASS = "client";
	
	public static final String keyPath;
	
	/**=======================本地文件相关参数========================*/
	public static final String localDetecConfSuffix;// 监测设置信息文件名后缀
	public static final String localDetecConfPath;// 监测设置信息存放路径
	public static final String localDataCollection;// 监测数据存放集
	public static final String localDataFilePath;//------------监测数据存放路径
	public static final String localDataDonePath;//------------成功处理数据存放路径
	public static final String localDataErrorPath;//------------不完整数据0大小文件存放路径
	public static final String localBackupPath;// 备份文件存放目录
	public static final String localUploadsPath;// 推送文件存入目录
	public static final String localTaskPath;// 任务相关信息存放目录
	public static final String localTaskDonePath;// ------------成功处理任务存放目录
	public static final String localTaskErrorPath;// -----------不完整0大小回传文件存放目录
	public static final String localTaskResultPath;// ------------任务结果存放目录
	public static final String localTaskReturnPath;// ------------任务回传文件存放目录
	public static final String localLogsPath;// 日志存放路径
	public static final String localTempPath;// 临时文件存放目录
	public static final String localTempDataIncomingPath; // 第三方监测临时文件存放目录
	public static final String localAgentPidFile;// Agent自身进程PID存放文件
	public static final String localPluginScriptPath;// 第三方监测脚本存放目录
	public static final String LOCAL_SCRIPT_PATH;
	
	/**=======================系统预计监测类型========================*/
	public static final String SYS_CHECK_TYPE_CPU;
	public static final String SYS_CHECK_TYPE_MEMORY;
	public static final String SYS_CHECK_TYPE_DISK;
	public static final String SYS_CHECK_TYPE_NET;
	public static final String SYS_CHECK_TYPE_SYSDATE;
	public static final String SYS_CHECK_TYPE_PROCESS;
	public static final String SYS_CHECK_TYPE_PROCESS_NMSAGENT;
	public static final String SYS_CHECK_TYPE_SYSTEMINFO;
	
	/** =====================Common时间时隔==================== **/
	// ------------ 清除本地文件
	public static final Integer COMMON_DEL_LOG_DAYS;// 删除日志文件间隔时间
	public static final Integer COMMON_DEL_DATA_HOURS;// 删除数据文件间隔时间
	public static final Integer COMMON_DEL_TASK_HOURS;// 删除任务相关文件间隔时间
	public static final Integer COMMON_DEL_UPGRADEFILE_DAYS;// 删除升级文件间隔时间
	public static final Integer COMMON_DEL_TEMP_DAYS;// 删除临时文件间隔时间
	public static String[] COMMON_DEL_PATH_INCLUDE;// 指定Agent可删除文件的范围
	public static String[] COMMON_DEL_PATH_EXCLUDE;// 指定Agent可删除文件范围内不可删除部分
	// ------------ 监测、任务、预警
	public static final Integer COMMON_ALARM_MINUTES = 5;// 主动告警轮循间隔时间
	public static final Integer COMMON_TASK_RESULT_SEND_MINUTES = 5;// 重发之前发送失败的任务执行结果间隔时间
	public static final Integer COMMON_TASK_INIT_DELAY_MINUTES = 2;// 启动时初始化任务请求延迟时间
	public static final Integer COMMON_UPLOAD_DATA_MINUTES;// 上传数据轮循间隔时间
	public static final Integer COMMON_TASK_CLEAR_HOURS;// 定时清理内存中已完成任务的间隔时间
	// ------------线程池
	public static final Integer COMMON_THREAD_SOCKET_SIZE ;// socket通信线程最大个数
	public static final Integer COMMON_THREAD_SCHEDULE_SIZE;// 定时任务线程最大个数
	// ------------打包上传
	public static final Integer COMMON_ZIP_MIN_SIZE;// 文件数越过一定值时压缩用
	public static final Integer COMMON_ZIP_MAX_SIZE;// 文件数越过一定值时压缩文件最多包含文件个数
	public static final Integer COMMON_MAX_RETURN_CNT;// 回传文件数越过一定值时压缩用
	// -----------任务结果、主动告警等信息中各字段的分隔符
	public static final String COMMON_MSG_SEPRATOR = "$@$";
	public static final String COMMON_MSG_SEPRATOR_SPLIT = "\\$@\\$";
	public static final int COMMON_MSG_SUCCESS = 0;
	public static final int COMMON_MSG_FAIL = 1;
	// -----------设置文件编码方式
	public static final String charset;
	
	/** ========================告警状态常量========================== **/
	//用于报警: -1监测执行失败，0监测信息不正常，1监测信息正常，-2异常主动告警，2主动告警恢复正常）
	public static final int DETECTION_STATUS_FAILURE = -1;//监测执行失败
	public static final int DETECTION_STATUS_ABNORMAL = 0;//监测信息不正常
	public static final int DETECTION_STATUS_NORMAL = 1;//监测信息正常
	//public static final int DETECTION_ALARM_ABNORMAL = -2;//告警检查：监测线程异常（未取到数据，相应的文件找不到，或者连续N次都超过设置的告警值）
	//public static final int DETECTION_ALARM_NORMAL = 2;//告警检查：监测线程恢复正常
	
	/** ========================任务部分文件后缀============================ **/
	public static final String TASK_RESULT_FILE_SUFFIX = ".result";
	public static final String TASK_RESULT_AGENTTMPFILE_SUFFIX = ".upgrade";
	public static final String TASK_RETURN_FILE_SUFFIX = ".return";
	
	/** ========================Debug============================ **/
	public static final Integer DEBUG_INIT_TASK_FLAG;
	public static final Integer DEBUG_PLUGIN_FLAG;
	public static final Integer DEBUG_SYSDETECT_FLAG;
	public static final Integer DEBUG_UPLOADDATA_FLAG;
	public static final Integer DEBUG_ALARM_FLAG;
	public static final Integer DEBUG_DELFILE_FLAG;
	public static final Integer DEBUG_TASKRESULT_FLAG;
	public static final Integer DEBUG_TASKRETURN_FLAG;
	
	//=================
	public static final int max_times = 5;// 失败后重试次数
	public static final long max_delay_seconds = 30;// 重试间隔，秒
	public static final int noDataTimes = 4;// 未取到数据的次数，用于主动告警
	public static final int overAlarmValTimes = 4;// 连续超过预警值的次数，用于主动告警
	//--------------Agent唯一标志UUID
	public static Long AGENT_HOST_UUID = null;
	public static String AGENT_OPERATE_SYSTEM = null;
	public static String AGENT_LOCAL_IP = null;
	
	public static final String DETEC_STATE_INFO_FORMATE_POINT = "$@$";//用于监测数据的状态信息web界面显示的格式化的连接符
	
	public static Boolean ACTIIVE_ALARM_START = false;//默认不启动主动告警
	public static String AGENT_INTERFACE_NAME_KEY = null;//网络端口名称
	/**
	 * 监测数据主动上报
	 */
	//监测数据主动上报
	public static final int DATA_SEND_THREAD_FLAG;
	//主动数据上报 IP
	public static final String DATA_SEND_THREAD_HOST;
	//主动数据上报 PORT
	public static final int DATA_SEND_THREAD_PORT;
	//主动数据上报间隔 INTERVAL，单位 10 S
	public static final int DATA_SEND_THREAD_INTERVAL;
	
	static{
		SYSTEM_PATH = SysConfig.getSystemDir();
		
		// -------------------SSLSocket
		SOCKET_SERVER_HOST = SysConfig.getStringVal(SOCKET_SERVER_HOST_KEY);
		SOCKET_SERVER_PORT = SysConfig.getIntegerVal("server_port");
		SOCKET_AGENT_PORT = SysConfig.getIntegerVal("agent_port");
		SOCKET_TIMEOUT_MINUTES = SysConfig.getIntegerVal("socket.timeout.minutes", "30");
		SSL_KEY_STORE = formatPath(SysConfig.getStringVal("local.ssl.keys"));
		SSL_TRUST_KEY_STORE = formatPath(SysConfig.getStringVal("local.ssl.trust"));
		
		keyPath = formatPath(SysConfig.getStringVal("local.ssl.path"));
		
		/*=======================文件相关参数========================*/
		// 可删范围
		String path = SysConfig.getStringVal("common.del.path.include");
		if(path!=null && !"".equals(path)){
			COMMON_DEL_PATH_INCLUDE = path.split(",");
			for(int i=0; i<COMMON_DEL_PATH_INCLUDE.length; i++){
				COMMON_DEL_PATH_INCLUDE[i] = FileUtil.handlerPath(COMMON_DEL_PATH_INCLUDE[i]);
			}
		}
		// 禁删范围
		path = SysConfig.getStringVal("common.del.path.exclude");
		if(path!=null && !"".equals(path)){
			COMMON_DEL_PATH_EXCLUDE = path.split(",");
			for(int i=0; i<COMMON_DEL_PATH_EXCLUDE.length; i++){
				COMMON_DEL_PATH_EXCLUDE[i] = FileUtil.handlerPath(COMMON_DEL_PATH_EXCLUDE[i]);
			}
		}
		// 文件总路径
		String localFilePath = SysConfig.getStringVal("local.data.path");
		// ---------------Local Path
		LOCAL_SCRIPT_PATH = formatPath(SysConfig.getStringVal("local.script.path"));
		localDetecConfSuffix = SysConfig.getStringVal("local.config.file.suffix");
		localAgentPidFile = formatPath(SysConfig.getStringVal("local.agent.pidfile"));
		localDetecConfPath = localFilePath + File.separator + "nc_config";
		localPluginScriptPath = localFilePath + File.separator + "nc_detecScript";
		localDataCollection = localFilePath + File.separator + "nc_data";
		localDataFilePath = localDataCollection + File.separator + "incoming";
		localDataDonePath = localDataCollection + File.separator + "done";
		localDataErrorPath = localDataCollection + File.separator + "error";
		localBackupPath = localFilePath + File.separator + "nc_backup";
		localUploadsPath = localFilePath + File.separator + "nc_uploads";
		localTaskPath = localFilePath + File.separator + "nc_task";
		localTaskDonePath = localTaskPath + File.separator + "done";// 成功处理任务存放目录
		localTaskErrorPath = localTaskPath + File.separator + "error";// 成功处理任务存放目录
		localTaskResultPath = localTaskPath + File.separator + "incoming" + File.separator + "result";//任务结果存放目录
		localTaskReturnPath = localTaskPath + File.separator + "incoming" + File.separator + "return";//任务回传文件存放目录
		localLogsPath = formatPath(SysConfig.getLogPath());
		localTempPath = localFilePath + File.separator + "nc_temp";
		localTempDataIncomingPath = localDataCollection + File.separator + "temp";
		
		// ---------------字符编码
		charset = SysConfig.getStringVal("charset");
		
		// ---------------System Check Type
		SYS_CHECK_TYPE_CPU = SysConfig.getStringVal("sys.check.type.cpu", "cpu");
		SYS_CHECK_TYPE_MEMORY = SysConfig.getStringVal("sys.check.type.memory", "memory");
		SYS_CHECK_TYPE_DISK = SysConfig.getStringVal("sys.check.type.disk", "disk");
		SYS_CHECK_TYPE_NET = SysConfig.getStringVal("sys.check.type.net", "net");
		SYS_CHECK_TYPE_SYSDATE = SysConfig.getStringVal("sys.check.type.systemdate", "systemdate");
		SYS_CHECK_TYPE_PROCESS = SysConfig.getStringVal("sys.check.type.process", "process");
		SYS_CHECK_TYPE_PROCESS_NMSAGENT = SysConfig.getStringVal("sys.check.type.process.nmsagent", "nmsclient");
		SYS_CHECK_TYPE_SYSTEMINFO =  SysConfig.getStringVal("sys.check.type.systeminfo", "systeminfo");
		
		// --------------Common Clear Gaps
		COMMON_DEL_LOG_DAYS = SysConfig.getIntegerVal("common.del.log.days", "7");
		COMMON_DEL_DATA_HOURS = SysConfig.getIntegerVal("common.del.data.hours", "24");
		// 2013-5-20 添加功能  删除任务相关信息最小时间，不得少于一天
		if(COMMON_DEL_DATA_HOURS > 24){
			COMMON_DEL_TASK_HOURS = COMMON_DEL_DATA_HOURS;
		}else{
			COMMON_DEL_TASK_HOURS = 24;
		}
		COMMON_DEL_UPGRADEFILE_DAYS = SysConfig.getIntegerVal("common.del.upgradefile.days", "30");
		COMMON_DEL_TEMP_DAYS = SysConfig.getIntegerVal("common.del.temp.days", "2");
		COMMON_UPLOAD_DATA_MINUTES = SysConfig.getIntegerVal("common.upload.data.minutes", "5");
		COMMON_TASK_CLEAR_HOURS = SysConfig.getIntegerVal("common.task.clear.hours", "2");
		
		// -------------- ThreadPool
		COMMON_THREAD_SOCKET_SIZE = SysConfig.getIntegerVal("common.thread.socket.size", "10");
		COMMON_THREAD_SCHEDULE_SIZE = SysConfig.getIntegerVal("common.thread.schedule.size", "15");
		// -------------- Compress
		COMMON_ZIP_MIN_SIZE = SysConfig.getIntegerVal("common.zip.min.size", "1000");
		COMMON_ZIP_MAX_SIZE = SysConfig.getIntegerVal("common.zip.max.size", "2000");
		COMMON_MAX_RETURN_CNT = SysConfig.getIntegerVal("common.max.return.size", "10");
		// ----------------Debug
		DEBUG_INIT_TASK_FLAG = SysConfig.getIntegerVal("debug.init.task.flag", "0");
		DEBUG_PLUGIN_FLAG = SysConfig.getIntegerVal("debug.plugin.flag", "0");
		DEBUG_SYSDETECT_FLAG = SysConfig.getIntegerVal("debug.sysdetect.flag", "0");
		DEBUG_UPLOADDATA_FLAG = SysConfig.getIntegerVal("debug.uploaddata.flag", "0");
		DEBUG_ALARM_FLAG = SysConfig.getIntegerVal("debug.alarm.flag", "0");
		DEBUG_DELFILE_FLAG = SysConfig.getIntegerVal("debug.delfile.flag", "0");
		DEBUG_TASKRESULT_FLAG = SysConfig.getIntegerVal("debug.taskresult.flag", "0");
		DEBUG_TASKRETURN_FLAG = SysConfig.getIntegerVal("debug.taskreturn.flag", "0");
		
//		ACTIIVE_ALARM_START = SysConfig.getStringVal("active.alarm.start", "true");//是否启用主动告警，默认不启动主动告警---用于nc配置文件，现修改为从web控制
		
		
		//监测数据主动上报
		DATA_SEND_THREAD_FLAG = SysConfig.getIntegerVal("data.send.thread.flag", "0");
		//主动数据上报 IP
		DATA_SEND_THREAD_HOST = SysConfig.getStringVal("data.send.thread.host", SOCKET_SERVER_HOST);
		//主动数据上报 PORT
		DATA_SEND_THREAD_PORT = SysConfig.getIntegerVal("data.send.thread.port", "9527");
		//主动数据上报间隔 INTERVAL，单位 10 S
		DATA_SEND_THREAD_INTERVAL = SysConfig.getIntegerVal("data.send.thread.interval", "10");
		
		
		// 初始化创建文件夹
		if(!new File(localDetecConfPath).exists()){
			new File(localDetecConfPath).mkdirs();
		}
		if(!new File(localPluginScriptPath).exists()) {
			new File(localPluginScriptPath).mkdirs();
		}
		if(!new File(localDataCollection).exists()){
			new File(localDataCollection).mkdirs();
		}
		if(!new File(localBackupPath).exists()){
			new File(localBackupPath).mkdirs();
		}
		if(!new File(localUploadsPath).exists()){
			new File(localUploadsPath).mkdirs();
		}
		if(!new File(localTaskPath).exists()){
			new File(localTaskPath).mkdirs();
		}
		if(!new File(localTempPath).exists()){
			new File(localTempPath).mkdirs();
		}
		if(!new File(localTempDataIncomingPath).exists()){
			new File(localTempDataIncomingPath).mkdirs();
		}
	}
	
	private static String formatPath(String path){
		String returnPath = path;
		if(path!=null && !"".equals(path) && !new File(path).isAbsolute()){// 路径不为空且是相对路径
			returnPath = SYSTEM_PATH + File.separator + path;
		}
		return returnPath;
	}
	
	public static boolean isSucessByResult(String msg){
		boolean flag = false;
		if (!StringUtils.isEmpty(msg)) {
			String[] result = msg.split(Contants.COMMON_MSG_SEPRATOR_SPLIT);
			if (result != null && result.length > 0) {
				if (Integer.parseInt(result[0])==Contants.COMMON_MSG_SUCCESS) {
					flag = true;
				}
			}
		}
		return flag;
	}
	
	public static String getDescByResult(String msg){
		String desc = null;
		if (!StringUtils.isEmpty(msg)) {
			String[] result = msg.split(Contants.COMMON_MSG_SEPRATOR_SPLIT);
			if (result != null && result.length > 1) {
				desc = result[1];
			}
		}
		return desc;
	}
}
