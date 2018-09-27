package com.nis.nmsclient.thread.alarm;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.nis.nmsclient.common.Common;
import com.nis.nmsclient.common.Contants;
import com.nis.nmsclient.config.DetecConfOper;
import com.nis.nmsclient.util.DateUtil;
import com.nis.nmsclient.util.FileUtil;
import com.nis.nmsclient.util.ProcessUtil;
import com.nis.nmsclient.util.Utils;

/**
 * 主动报警线程：针对当前所有有效监测设置进行主动报警
 *    定时监测某设置是否正常生成数据，N次未取到数据后向Server报警，恢复正常后向Server发送提示信息
 * @date Mar 22, 2012
 * @author zhenzhen
 * @version
 */
public class AlarmThread implements Runnable{
	Logger logger = Logger.getLogger(AlarmThread.class);
	private String name;
	
	private Map<Long, AlarmStateMsg> alarmSmMap = new HashMap<Long, AlarmStateMsg>();
	
	public AlarmThread(String name) {
		super();
		this.name = name;
	}
	
	public void run() {
		Thread.currentThread().setName(name);
		
		logger.debug("主动预警检查开始 ~~~~~~~");
		try{
			Map<Long, AlarmPO> alarmPOs = new HashMap<Long, AlarmPO>(Common
					.getAlarmPOs());
			// 清除垃圾数据
			if (alarmSmMap.size() > alarmPOs.size()) {
				Iterator<Long> it = alarmSmMap.keySet().iterator();
				while (it.hasNext()) {
					Long key = it.next();
					if(!alarmPOs.containsKey(key)){
						//alarmSmMap.remove(key);
						it.remove();//update by jinsj for 2013-07-08 java.util.ConcurrentModificationException
					}
				}

			}
			Set<Map.Entry<Long, AlarmPO>> entrys = alarmPOs.entrySet();
			int alarmCnt = 0;
			StringBuffer alarmDescInfo = new StringBuffer();
			for (Map.Entry<Long, AlarmPO> entry : entrys) {
				// 如果监测进程未启动，则不进行告警检查
				if (entry.getValue().getControlStartTime() != null
						&& entry.getValue().getControlStartTime() > System
								.currentTimeMillis()) {
					continue;
				}
				// 对各监测设置告警状态信息进行赋初值
				Long key = entry.getValue().getId();
				if (alarmSmMap.get(key) == null) {
					alarmSmMap.put(key, new AlarmStateMsg(new Date(), 0,
							new StringBuffer(), false));
				}else if(alarmSmMap.get(key).getAlarmMsg().length() == 0){
					alarmSmMap.get(key).setStart(new Date());
				}

				boolean flag = alarm(entry.getValue());
				if(flag){
					alarmCnt += 1;
					alarmDescInfo.append("\tsetId: " + key + " >> " + entry.getValue().getType() + "_" + entry.getValue().getProcIden());
					alarmDescInfo.append("\n");
				}
			}
			logger.info("本次预警轮循检查监测设置总数：" + alarmPOs.size() + "，正常："
					+ (alarmPOs.size() - alarmCnt) + "，不正常：" + alarmCnt);
			if(alarmCnt>0){
				logger.info("本次预警检查不正常的监测如下：\n" + alarmDescInfo.toString());
			}
		}catch (Exception e) {
			logger.error("Active early warning thread exception：" + Utils.printExceptionStack(e));
		}
		logger.debug("主动预警检查结束 ~~~~~~~");
	}
	
	/**
	 * 针对一个监测设置的主动预警具体逻辑实现
	 * @param alarm
	 * @return true 告警监测不正常 false 监测正常
	 * @throws Exception
	 */
	public boolean alarm(AlarmPO alarm) throws Exception{
		Long key = alarm.getId();
		String sonDirName = DetecConfOper.getFileName(alarm.getType(), alarm.getProcIden(), null);
		StringBuffer sbMsg = new StringBuffer();
		/**
		 * 1、检查是否存在数据: alarmLevel 0 -- 数据存在，非0 -- 数据不存在 
		 */
		int alarmLevel = checkDataIsExist(sonDirName, alarm.getCheckGap(), sbMsg);
		
		/**
		 * 2、判断进程是否存在：是进程且数据不存在才判断，不是进程如系统类型CPU，则不进行这些操作
		 */
		if(alarmLevel!=0 && alarm.isIsproc()){
			if("2".equals(alarm.getIsControlStart())) { // NC周期启动(脚本间断性执行且执行时间较短，通过判断Java定时任务确认进程)
				if(Common.containPluginDetecFuture(key)){ // 进程存在
					String temp = sbMsg.toString();
					sbMsg.delete(0, sbMsg.length());
//					sbMsg.append("进程存在，但" + temp);
					sbMsg.append("i18n_client.AlarmThread.processExists_n81i" + temp);
					alarmLevel = 2;
				} else { // 进程不存在
					sbMsg.delete(0, sbMsg.length());
//					sbMsg.append("定时任务“" + alarm.getType() + "”不存在");
					sbMsg.append("i18n_client.AlarmThread.loopMission1_n81i“" + alarm.getType() + "”i18n_client.AlarmThread.loopMission2_n81i");
					alarmLevel = 1;
				}
			} else {
				Object[] objArr = ProcessUtil.checkPidAndGetPid(alarm.getPidFileStr(), alarm.getProcessSearchKeyCode());
				int isExistFlag = Integer.parseInt(objArr[0].toString());
				String pidInfo = objArr[1].toString();
				
				if(isExistFlag==0){// 进程不存在(包括PID文件不存在，PID值不存在，搜索关键字也找不到进程)
					sbMsg.delete(0, sbMsg.length());
					sbMsg.append(pidInfo);
					alarmLevel = 1;
				} else {// 进程存在(一个进程或多个进程)
					String temp = sbMsg.toString();
					sbMsg.delete(0, sbMsg.length());
//					sbMsg.append("进程存在，但" + temp);
					sbMsg.append("i18n_client.AlarmThread.processExists_n81i" + temp);
					alarmLevel = 2;
				}
			}
		}
		
		/**
		 * 3、统计并发送报警信息
		 */
		AlarmStateMsg asMsg= alarmSmMap.get(key);
		int times = asMsg.getTimes();
		boolean flag = true;
		if (sbMsg.length()>0 && sbMsg.toString().equals(asMsg.getAlarmMsg().toString())) {
			times ++ ;
			
			if (times == Contants.noDataTimes) {// Contants.noDataTime次未取到数据，报警
				AlarmUtil.sendAlarmMsg(alarm, asMsg.getStart(), new Date(),
						alarmLevel, Contants.DETECTION_STATUS_ABNORMAL, asMsg
								.getAlarmMsg().toString()
//								+ "，此状态持续了" + times + "次");
								+ "，i18n_client.AlarmThread.state_n81i" + times + "i18n_client.AlarmThread.times_n81i");
				asMsg.setAlarmState(true);
			}
		} else if (sbMsg.length() > 0
				&& !sbMsg.toString().equals(asMsg.getAlarmMsg().toString())) {
			asMsg.getAlarmMsg().delete(0, asMsg.getAlarmMsg().length());
			asMsg.getAlarmMsg().append(sbMsg.toString());
			times = 1;
		} else {
			asMsg.getAlarmMsg().delete(0, asMsg.getAlarmMsg().length());
			times = 0;
			if(asMsg.isAlarmState()){
				/*AlarmUtil.sendAlarmMsg(alarm, asMsg.getStart(), new Date(),
						99, Contants.DETECTION_STATUS_NORMAL, "恢复正常");*/
				asMsg.setAlarmState(false);
			}
			flag = false;
		}
		asMsg.setTimes(times);
		alarmSmMap.put(key, asMsg);
		
		return flag;
	}
	
	/**
	 * 检查是否存在数据
	 * @param sonDirName
	 * @param checkGap
	 * @return
	 */
	private int checkDataIsExist(String sonDirName, long checkGap, StringBuffer sbMsg){
		int alarmLevel = 0;
		// ------- 1、先查看当前正在写入的数据文件夹
		File curDir = new File(Contants.localDataFilePath + File.separator + sonDirName);
		if(!curDir.exists()){
//			sbMsg.append("数据文件目录“" + curDir.getAbsolutePath() + "”不存在");
			sbMsg.append("i18n_client.AlarmThread.dataFileNotExists1_n81i“" + curDir.getAbsolutePath() + "”i18n_client.AlarmThread.dataFileNotExists2_n81i");
			alarmLevel = 1;
			return alarmLevel;
		}
		long timeMillis = System.currentTimeMillis() - Contants.noDataTimes * checkGap * 60 * 1000; 
		File[] rightFiles = FileUtil.getFilesAfterMillis(curDir, timeMillis);
		if(rightFiles!=null && rightFiles.length>0){// 找到数据，正常
			alarmLevel = 0;
			return alarmLevel;
		}
		
		// -------- 2、如果当前正在写入的数据文件夹没有找到符合条件的数据，则查找上传成功后备份到的相应目录
		// 2013-3-28 修改内容：原本只检查当天日期目录是否存在数据，针对监测时间间隔超过一天的存在问题，所以当间隔时间大于等于一天时，改为检查当天日期之前的目录
		String dateDirName = DateUtil.getCurrentDate(DateUtil.YYYYMMDD);
		File doneDir = new File(Contants.localDataDonePath + File.separator + sonDirName);
		if(doneDir.exists()){
			if(checkGap >= 1440){// --- 检查当天日期及之前的目录
				// 找到指定的日期目录及之前的日期目录，将其降序排列，是为了最先查找当天日期
				File[] dateDirs = FileUtil.sortDescByFileName(FileUtil.getDirsBeforeDateName(doneDir, dateDirName));
				// 在找到的日期目录下检查文件是否存在
				for(File dateDir : dateDirs){
					rightFiles = FileUtil.getFilesAfterMillis(dateDir, timeMillis);
					if(rightFiles!=null && rightFiles.length>0){// 在任一目录找到则不再继续查找其他目录
						break;
					}
				}
			}else {// --- 只检查当天日期
				File dateDir = new File(doneDir.getAbsolutePath() + File.separator + dateDirName);
				if(dateDir.exists()){
					rightFiles = FileUtil.getFilesAfterMillis(dateDir, timeMillis);
				}
			}
		}
		if(rightFiles!=null && rightFiles.length>0){// 找到数据，正常
			alarmLevel = 0;
			return alarmLevel;
		}
		
		// -------- 3、查找不完整数据即0大小文件备份到的相应目录
		File[] errorFiles = null;
		File errorDir = new File(Contants.localDataErrorPath + File.separator + sonDirName);
		if(errorDir.exists()){
			if(checkGap >= 1440){// --- 检查当天日期及之前的目录
				// 找到指定的日期目录及之前的日期目录，将其降序排列，是为了最先查找当天日期
				File[] dateDirs = FileUtil.sortDescByFileName(FileUtil.getDirsBeforeDateName(errorDir, dateDirName));
				// 在找到的日期目录下检查文件是否存在
				for(File dateDir : dateDirs){
					errorFiles = FileUtil.getFilesAfterMillis(dateDir, timeMillis);
					if(errorFiles!=null && errorFiles.length>0){// 在任一目录找到则不再继续查找其他目录
						break;
					}
				}
			}else {// --- 只检查当天日期
				File dateDir = new File(doneDir.getAbsolutePath() + File.separator + dateDirName);
				if(dateDir.exists()){
					errorFiles = FileUtil.getFilesAfterMillis(errorDir, timeMillis);
				}
			}
			
		}
		if(errorFiles!=null && errorFiles.length>0){
//			sbMsg.append("监测数据文件大小为0");
			sbMsg.append("i18n_client.AlarmThread.dataSize_n81i");
			alarmLevel = 1;
		}else{
//			sbMsg.append("未取到监测数据");
			sbMsg.append("i18n_client.AlarmThread.noDetecateData_n81i");
			alarmLevel = 1;
		}
		
		return alarmLevel;
	}
	
	/**
	 * 告警检查过程中针对每个监测设置的过程变量状态的封装类
	 * @date Mar 22, 2012
	 * @author zhenzhen
	 * @version
	 */
	class AlarmStateMsg {
		private Date start;//告警检查不正常情况的开始时间
		private Integer times;//连续告警次数
		private StringBuffer alarmMsg;//告警信息
		private boolean alarmState;//是否向Server发送了主动告警请求
		
		public AlarmStateMsg(Date start, Integer times, StringBuffer alarmMsg,
				boolean alarmState) {
			super();
			this.start = start;
			this.times = times;
			this.alarmMsg = alarmMsg;
			this.alarmState = alarmState;
		}
		
		public Date getStart() {
			return start;
		}
		public void setStart(Date start) {
			this.start = start;
		}
		public Integer getTimes() {
			return times;
		}
		public void setTimes(Integer times) {
			this.times = times;
		}
		public StringBuffer getAlarmMsg() {
			return alarmMsg;
		}
		public void setAlarmMsg(StringBuffer alarmMsg) {
			this.alarmMsg = alarmMsg;
		}
		public boolean isAlarmState() {
			return alarmState;
		}
		public void setAlarmState(boolean alarmState) {
			this.alarmState = alarmState;
		}
	}
	
}
