package com.nis.nmsclient.common;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.log4j.Logger;

import com.nis.nmsclient.config.DetecConfOper;
import com.nis.nmsclient.model.AlarmInfo;
import com.nis.nmsclient.model.SetInfo;
import com.nis.nmsclient.thread.alarm.AlarmPO;
import com.nis.nmsclient.thread.alarm.AlarmUtil;
import com.nis.nmsclient.thread.plugin.StartPluginRun;
import com.nis.nmsclient.thread.task.LoopTaskThread;
import com.nis.nmsclient.util.ProcessUtil;
import com.nis.systeminfo.thread.GetInfoRun;

public class Common {
	static Logger logger = Logger.getLogger(Common.class);

	public static final String COMMON_SYS_SETINFO = "0";
	public static boolean NC_UPGRADE_FLAG = false;

	public static final ExecutorService service = Executors
			.newFixedThreadPool(Contants.COMMON_THREAD_SOCKET_SIZE); // 通讯线程池
	public static final ScheduledExecutorService scheduled = Executors
			.newScheduledThreadPool(Contants.COMMON_THREAD_SCHEDULE_SIZE); // 定时执行线程池

	// 任务变更或取消控制集 object[1]=ScheduledFuture<?>,object[2]=LoopTaskThread
	private static Map<Long, Object[]> taskFutureMap = Collections.synchronizedMap(new HashMap<Long, Object[]>());
	// 主动告警信息: 主动报警线程使用
	private static Map<Long, AlarmPO> alarmPOs = Collections.synchronizedMap(new HashMap<Long, AlarmPO>());
	// 预置监测控制集
	private static Map<Long, ScheduledFuture<?>> sysDetecFutrue = Collections.synchronizedMap(new HashMap<Long, ScheduledFuture<?>>());
	// Agent定时启动的三方监测控制集
	private static Map<Long, ScheduledFuture<?>> pluginDetecFutrue = Collections.synchronizedMap(new HashMap<Long, ScheduledFuture<?>>());
	// 三方监测设置集
	private static Map<Long, SetInfo> pluginDetecSetInfoMap = new HashMap<Long, SetInfo>();
	// 监测信息报警相关信息（alarmInfo.setInfoId, alarmInfo）
	public static Map<Long, List<AlarmInfo>> detecAlarmInfoMap = new HashMap<Long, List<AlarmInfo>>();

	/**
	 * 缓存三方监测设置
	 * 
	 * @param key
	 * @param setInfo
	 * @param lastMergeFileDetecTime
	 */
	public static void putPluginDetecSetInfo(Long key, SetInfo setInfo) {
		long planTime = (setInfo.getControlStartTime() != null) ? setInfo.getControlStartTime() : 0;
		
		// 初始化已合并的最后一个临时结果文件的监测时间，用于判断是否出现未生成监测数据的周期
		// 监测设置下发时，记录该监测的计划启动时间
		// NC重启时，记录当前时间（周期启动监测由NC控制，若NC重启后存在未合并的临时结果，忽略该时间段内未生成监测数据的周期）
		if(setInfo.getLastMergeDetecTime() == null) {
			setInfo.setLastMergeDetecTime(Math.max(System.currentTimeMillis(), planTime));
		}
		if(setInfo.getPlanCheckTime() == null || setInfo.getPlanCheckTime().longValue() == 0) {
			// GetRunInfo.startTime
			setInfo.setPlanCheckTime(System.currentTimeMillis());
		}
		
		pluginDetecSetInfoMap.put(key, setInfo);
	}

	/**
	 * 获取三方监测设置集
	 * 
	 * @param key
	 * @return
	 */
	public static Collection<SetInfo> getPluginDetecSetInfos() {
		return pluginDetecSetInfoMap.values();
	}
	
	public static void putAllDetecAlarmInfo(Map<Long, List<AlarmInfo>> alarmMap) {
		detecAlarmInfoMap.putAll(alarmMap);
	}
	
	/**
	 * 监测信息报警相关信息
	 * @param setInfoId
	 * @return
	 */
	public static List<AlarmInfo> getDetecAlarmInfo(Long setInfoId) {
		return detecAlarmInfoMap.get(setInfoId);
	}

	/**
	 * 获取任务
	 */
	public static ScheduledFuture<?> getTaskFuture(Long key) {
		synchronized (taskFutureMap) {
			Object[] objects = taskFutureMap.get(key);
			if (objects != null && objects.length > 0 && objects[0] != null) {
				return (ScheduledFuture<?>) objects[0];
			} else {
				return null;
			}
		}
	}

	/**
	 * 添加任务
	 */
	public static void putTaskFuture(Long key, ScheduledFuture<?> value, LoopTaskThread loopTask) {
		synchronized (taskFutureMap) {
			taskFutureMap.put(key, new Object[] { value, loopTask });
			logger.info("添加任务 id:" + key);
		}
	}

	/**
	 * 注销任务
	 */
	public static void cancleTaskFuture(final Long key, long delayMs) {
		scheduled.schedule(new Runnable() {
			public void run() {
				synchronized (taskFutureMap) {
//					Thread.currentThread().setName("注销任务 id:" + key);
					Thread.currentThread().setName("Write Off Task ID:" + key);
					Object[] objects = taskFutureMap.get(key);
					if (objects!=null && objects.length>0 && objects[0]!=null) {
						ScheduledFuture<?> future = (ScheduledFuture<?>) objects[0];
						logger.info("任务状态: "
										+ ((future.isDone() || future
												.isCancelled()) ? "已停止" : "运行中"));
						if (objects.length > 1 && objects[1] != null) {
							LoopTaskThread loopTask = (LoopTaskThread) objects[1];
							loopTask.cancle();
						}
						future.cancel(true);
						taskFutureMap.remove(key);
						logger.info("注销成功");
					} else {
						logger.info("任务不存在");
					}

				}
			}
		}, delayMs, TimeUnit.MILLISECONDS);
	}

	/**
	 * 从全局变量移除执行完成或者取消的任务（每次在上传发送失败的结果时检查并移除）
	 */
	public static void removeCancelAndDoneTaskFuture() {
		synchronized (taskFutureMap) {
			Iterator<Long> iterator = taskFutureMap.keySet().iterator();
			while (iterator.hasNext()) {
				Long key = iterator.next();
				Object[] objects = taskFutureMap.get(key);
				if (objects != null && objects.length > 0) {
					ScheduledFuture<?> future = (ScheduledFuture<?>) objects[0];
					if (future.isCancelled() || future.isDone()) {
						iterator.remove();
						logger.info("任务控制集 移除 id：" + key + " 状态: "
								+ ((future.isDone() || future
										.isCancelled()) ? "已停止" : "运行中"));
					}
				} else {
					iterator.remove();
					logger.info("任务控制集 移除 id：" + key);
				}
			}
		}
	}

	/**
	 * 获取存放预警信息集
	 */
	public static Map<Long, AlarmPO> getAlarmPOs() {
		synchronized (alarmPOs) {
			return alarmPOs;
		}
	}

	/**
	 * 取消某一监测类型的主动预警
	 */
	public static void removeAlarmPO(Long key) {
		synchronized (alarmPOs) {
			if (alarmPOs.containsKey(key)) {
				AlarmPO alarmPO = alarmPOs.get(key);
				alarmPOs.remove(key);
				logger.info("主动预警集 移除 setId:" + key + " >> "
						+ alarmPO.getType() + "_" + alarmPO.getProcIden());
			}
		}
	}

	/**
	 * 添加或更新对某一监测类型的主动预警
	 */
	public static void addOrUpdateAlarmPO(AlarmPO alarmPO) {
		synchronized (alarmPOs) {
			Long key = alarmPO.getId();
			String infoMsg = "添加";

			if (alarmPOs.containsKey(key)) {
				infoMsg = "更新";
			}

			alarmPOs.put(key, alarmPO);

            logger.info("主动预警集 " + infoMsg + " setId:" + key + " >> " + alarmPO.getType() + "_" + alarmPO.getProcIden());
		}
	}

	/**
	 * 取得预设监测总数
	 */
	public static int getSysDetecCount() {
		synchronized (sysDetecFutrue) {
			return sysDetecFutrue.size();
		}
	}

	/**
	 * 停用预设监测
	 */
	public static void stopSysDetec(SetInfo setInfo) {
		synchronized (sysDetecFutrue) {
			Long key = setInfo.getId();
//			String threadName = "预设监测_"
			String threadName = "Presupposition Monitoring_"
					+ DetecConfOper.getFileName(setInfo.getCheckTypeName(),
							setInfo.getProcessIden(), null);

			ScheduledFuture<?> future = sysDetecFutrue.get(key);
			if (future != null) {
				future.cancel(true);
				sysDetecFutrue.remove(key);
				logger.info("预设监测线程 停用 setId:" + setInfo.getId() + " >> "
						+ threadName);
			}
		}
	}

	/**
	 * 添加或更新系统预设监测
	 * @param setInfo
	 * @param alarmInfos
	 */
	public static void addOrUpdateSysDetec(SetInfo setInfo, List<AlarmInfo> alarmInfos) {
		synchronized (sysDetecFutrue) {
			Long key = setInfo.getId();
			String infoMsg = "添加";

			ScheduledFuture<?> future = sysDetecFutrue.get(key);
			if (future != null) {
				future.cancel(true);
				sysDetecFutrue.remove(key);

				infoMsg = "更新";
			}

			long delay = 0;
			Date startTime = new Date();
			if (setInfo.getPlanCheckTime() != null) {
				try {
					long gap = setInfo.getPlanCheckTime() - System.currentTimeMillis();
					if (gap > 0) {
						delay = gap;
						startTime = new Date(setInfo.getPlanCheckTime());
					}
				} catch (Exception e) {
					logger.error("Please check whether the next test time is set correctly！", e);
				}
			}

//			String threadName = "预设监测_"
			String threadName = "Presupposition Monitoring_"
					+ DetecConfOper.getFileName(setInfo.getCheckTypeName(),
							setInfo.getProcessIden(), null);

			future = Common.scheduled.scheduleAtFixedRate(new GetInfoRun(
					threadName, setInfo, startTime, alarmInfos), delay, setInfo
					.getCheckGap(), TimeUnit.MINUTES);
			sysDetecFutrue.put(key, future);

            logger.info("预设监测线程 " + infoMsg + " setId:" + setInfo.getId() + " >> " + threadName);
		}
	}

	/**
	 * 启动三方监测
	 */
	public static void startPluginDetec(SetInfo setInfo) {
//		String threadName = "三方监测_"
		String threadName = "Three Party Monitoring_"
				+ DetecConfOper.getFileName(setInfo.getCheckTypeName(),
						setInfo.getProcessIden(), null);

		Common.scheduled.schedule(new StartPluginRun(setInfo, threadName), 0,
				TimeUnit.MILLISECONDS);

		logger.info("三方监测 添加 setId:" + setInfo.getId() + " >> " + threadName);
	}

	/**
	 * 添加定时启动的三方监测
	 */
	public static void putPluginDetecFuture(Long key, ScheduledFuture<?> future) {
		synchronized (pluginDetecFutrue) {
			pluginDetecFutrue.put(key, future);
		}
	}

	/**
	 * 停止定时启动的三方监测任务
	 */
	public static void stopPluginDetecFuture(Long key, String threadName) {
		synchronized (pluginDetecFutrue) {
			ScheduledFuture<?> future = pluginDetecFutrue.get(key);
			if (future != null) {
				future.cancel(true);
				sysDetecFutrue.remove(key);
				logger.info("三方监测 移除 setId:" + key + " >> " + threadName);
			}
		}
	}
	
	/**
	 * 检查三方监测是否存在（NC周期启动、NC单次启动）
	 */
	public static boolean containPluginDetecFuture(Long key) {
		ScheduledFuture<?> future = pluginDetecFutrue.get(key);
		return (future != null);
	}

	/**
	 * 停用三方监测
	 */
	public static void stopPluginDetec(SetInfo setInfo) {
		// NC周期启动监测需要获取三方监测的关键字
		if("2".equals(setInfo.getIsControlStart())) {
			generateCommandAndKeyword(setInfo);
		}

		Long key = setInfo.getId();
//		String threadName = "三方监测_"
		String threadName = "Three Party Monitoring_"
				+ DetecConfOper.getFileName(setInfo.getCheckTypeName(),
						setInfo.getProcessIden(), null);
		synchronized (pluginDetecFutrue) {
			ScheduledFuture<?> future = pluginDetecFutrue.get(key);
			if (future != null) {
				future.cancel(true);
				sysDetecFutrue.remove(key);
				logger.info("三方监测 移除 setId:" + setInfo.getId() + " >> " + threadName);
			}
		}
		try {
			// 检查PID
			Object[] objArr = ProcessUtil.checkPidAndGetPid(setInfo.getProcessFile(), setInfo.getProcessSearchKeyCode());
			int isExistFlag = Integer.parseInt(objArr[0].toString());
			String pidInfo = objArr[1].toString();

			if (isExistFlag == 0) {// 不存在
				logger.info("停用" + threadName + "：进程原本不存在，不用杀进程");
			} else if (isExistFlag == 1) {// 存在且只有一个进程，杀PID
				ProcessUtil.killProcess(pidInfo);
				logger.info("停用" + threadName + "：杀进程 PID:" + pidInfo);
			} else if (isExistFlag == 2) {// 找到多个进程，告警
				logger.info("停用" + threadName + "：" + pidInfo);
//				String alarmMsg = "停用三方监测进程：" + pidInfo;
				String alarmMsg = "Discontinuation Of The Three Party Monitoring Process：" + pidInfo;
				AlarmUtil.sendAlarmMsg(setInfo.getId(), setInfo
						.getCheckTypeName(), setInfo.getProcessIden(),
						new Date(), new Date(), 1,
						Contants.DETECTION_STATUS_FAILURE, alarmMsg);
			}
		} catch (Exception e) {
			logger.error("Discontinuation of three party monitoring anomalies", e);
		}
	}

	/**
	 * 设置三方监测中由Web管理的监测脚本的启动参数（针对NC启动的周期监测）<br/>
	 * 生成三方监测的执行命令及查询关键字
	 * 
	 * @param setInfo
	 * @return 脚本启动命令
	 */
	public static String generateCommandAndKeyword(SetInfo setInfo) {
		String command = null;
		try {
			if ("2".equals(setInfo.getIsControlStart())) { // NC周期启动
				File scriptDir = new File(Contants.localPluginScriptPath);
				final String keyword = "_" + setInfo.getProcessIden() + ".";
				Collection<?> files = FileUtils.listFiles(scriptDir,
						FileFilterUtils.asFileFilter(new FilenameFilter() {
							@Override
							public boolean accept(File dir, String name) {
								if(name.endsWith(".tp")) {
									return false; // 排除临时文件
								}
								return name.contains(keyword);
							}
						}), FalseFileFilter.FALSE);
				if (!files.isEmpty()) {
					File scriptFile = (File) files.iterator().next();
					String os = System.getProperty("os.name");
					if (os.startsWith("Windows")) {
						command = scriptFile.getCanonicalPath();
					} else if (os.startsWith("Linux")) {
						command = "./ " + scriptFile.getCanonicalFile();
					}
					setInfo.setProcessPath(command); // 设置执行命令
					setInfo.setProcessSearchKeyCode(scriptFile.getName()); // 搜索关键字
					// 更新缓存中的监测设置
					Common.putPluginDetecSetInfo(setInfo.getId(), setInfo);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return command;
	}
	
	public static int byteArrayToInt(byte[] b) {  
	    return   b[3] & 0xFF |  
	            (b[2] & 0xFF) << 8 |  
	            (b[1] & 0xFF) << 16 |  
	            (b[0] & 0xFF) << 24;  
	}  
	  
	public static byte[] intToByteArray(int a) {  
	    return new byte[] {  
	        (byte) ((a >> 24) & 0xFF),  
	        (byte) ((a >> 16) & 0xFF),     
	        (byte) ((a >> 8) & 0xFF),     
	        (byte) (a & 0xFF)  
	    };  
	}
}
