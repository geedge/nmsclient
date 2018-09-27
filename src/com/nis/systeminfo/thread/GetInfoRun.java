package com.nis.systeminfo.thread;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.nis.nmsclient.common.Contants;
import com.nis.nmsclient.common.SysConfig;
import com.nis.nmsclient.config.DetecConfOper;
import com.nis.nmsclient.model.AlarmInfo;
import com.nis.nmsclient.model.SetInfo;
import com.nis.nmsclient.thread.alarm.AlarmUtil;
import com.nis.nmsclient.util.DateUtil;
import com.nis.nmsclient.util.FileWrUtil;
import com.nis.nmsclient.util.Utils;
import com.nis.systeminfo.model.DetectInfo;
import com.nis.systeminfo.model.SystemInfo;

/**
 * 客户端读取本机信息，并写入到.csv文件的具体实现体
 * 
 */
public class GetInfoRun implements Runnable{
	Logger logger = Logger.getLogger(GetInfoRun.class);
	private String name;
	private SetInfo setInfo; // 当前线程读取的信息要存入的表，如CPU信息表、内存信息表等
	private Date startTime;// 服务启动时间
	private List<AlarmInfo> alarmInfos;
	
	private int diskUsageTime=0;
	private int cpuUsageTime=0;
	private String cpuUsageName="";
	private String diskUsageName="";
	
	private List<String> detailDatas ;
	
	//暂存各告警字段连续超过告警值的次数
	//public static Map<Long, Integer> alarmTimes = new HashMap<Long, Integer>();
	
	//暂存各告警信息告警状态
	//public static Map<Long, Boolean> alarmStates = new HashMap<Long, Boolean>();
	
	private int alarmTimes = 0;
	private boolean alarmState = false;
	
	private String firstPerformData=""; 
	
	public GetInfoRun(String name, SetInfo setInfo, Date startTime, List<AlarmInfo> alarmInfos) {
		super();
		this.name = name;
		this.setInfo = setInfo;
		this.startTime = startTime;
		this.alarmInfos = alarmInfos;
		//如果设置是process类型，且设置名称是nmsclient，即是自身则设置进程PID文件
		if (Contants.SYS_CHECK_TYPE_PROCESS.equalsIgnoreCase(setInfo.getCheckTypeName())
				&& Contants.SYS_CHECK_TYPE_PROCESS_NMSAGENT
						.equalsIgnoreCase(setInfo.getProcessIden())) {
			setInfo.setProcessFile(Contants.localAgentPidFile);
		}
	}
	
	public void run() {
		Thread.currentThread().setName(name);
		
		Date writeDate = new Date();
		int checkTimes = 0;//尝试次数
		//初始化值
		String filePath = Contants.localDataFilePath
				+ File.separator
				+ DetecConfOper.getFileName(setInfo.getCheckTypeName(), setInfo
						.getProcessIden(), null) + File.separator;
		String fileName = DateUtil.getStingDate(DateUtil.YYYYMMDDHH24MMSS, writeDate) + ".csv"; 
		// 针对进程，是NC启动还是手动启动
		boolean isAgentStart = ("0".equals(setInfo.getIsControlStart()) ? false : true);
		
		do {
			checkTimes++;
			try {
				/* 
				 * 2013-4-11 数据顺序调整如下:
				 * 总数据：本机IP,监测设置ID,监测类别,进程名称（监测类别设置名称）,监测服务启动时间,检测时延（秒）,本次检测时间,尝试次数,下次计划监测时间,
				 *       执行状态是否成功是否正常（用于报警:-1执行失败，0不正常，1正常）,状态信息（描述信息）,性能数据
				 * 详细信息：details,详细信息条数（要入到对应的监测类别信息表的数据条数）
				 *         告警列序号,告警级别,告警值,监测具体数据信息（多列）,,,,
				 */
				
				// detailDatas存放监测具体数据信息(可能多条，如多个网卡)，descInfo存放状态信息
				DetectInfo detectInfo = new SystemInfo().getDetectInfo(setInfo
						.getCheckTypeName(), setInfo.getProcessFile(), setInfo
						.getProcessPath(), setInfo.getProcessSearchKeyCode(), isAgentStart);
				
				String checkDelayTime = getCheckDelayTime(writeDate);// 检测时延
				
				// ------- 数据状态（告警）和详细监测数据组织
				int totalStatus = Contants.DETECTION_STATUS_NORMAL;
				int totalAlarmLevel = 99;
				String totalShowNum ="";//告警序列号，取告警级别高的showNum add by jinsj for 紧急告警
				StringBuffer functionSb = new StringBuffer();
				List<String[]> detailDataList = new LinkedList<String[]>();
				if (detectInfo == null || detectInfo.getDetailDatas() == null || detectInfo.getDetailDatas().size() == 0) {// 执行失败
					totalStatus = Contants.DETECTION_STATUS_FAILURE;
					// detectInfo不为空，即无具体的详细数据，有描述信息（目前是针对进程与系统时间）
					if (detectInfo != null){
						// 如果是进程监测, 找不到进程或找到多个进程时，告警；如果时间差监测，获取DC系统时间失败，告警
						String alarmMsg = detectInfo.getDescInfo();
						AlarmUtil.sendAlarmMsg(setInfo.getId(), setInfo.getCheckTypeName(), setInfo.getProcessIden(),
								new Date(), new Date(), 1, Contants.DETECTION_STATUS_FAILURE, alarmMsg);
					}
					logger.info("预设监测 setId: " + this.setInfo.getId() + " > "
							+ this.setInfo.getCheckTypeName() + "_"	+ setInfo.getProcessIden() + "，获取数据失败");
				}else if(!StringUtils.isBlank(detectInfo.getDiskMsg())){
					//出现硬盘不可写或磁盘满
					AlarmUtil.sendAlarmMsg(setInfo.getId(), setInfo.getCheckTypeName(), setInfo.getProcessIden(),
							new Date(), new Date(), 0, Contants.DETECTION_STATUS_ABNORMAL, detectInfo.getDiskMsg());
					return ;
				}else {// 即正常生成详细监测数据
					detailDatas = detectInfo.getDetailDatas();
					for(int i=0; i<detailDatas.size(); i++){
						String[] detailData = detailDatas.get(i).split(SystemInfo.SEPARATOR);
						int index = 0;
						String[] dataArr = new String[4 + detailData.length];
						// 与报警字段值比较是否状态正常
						String[] alarmArr = getAlarmState(detailData);
						if (alarmArr != null && "".equals(alarmArr[0])) {// 记录的状态正常
							dataArr[index++] = "";// 告警列序号
							dataArr[index++] = "";// 告警级别
							dataArr[index++] = "";// 告警值
						} else {// 记录的状态不正常
							totalStatus = Contants.DETECTION_STATUS_ABNORMAL;
							dataArr[index++] = alarmArr[0];// 告警列序号
							dataArr[index++] = alarmArr[1];// 告警级别
							dataArr[index++] = alarmArr[2];// 告警值
							
							if(!"".equals(alarmArr[4]) && totalAlarmLevel > Integer.parseInt(alarmArr[4])){// 将更高的告警级别赋给总告警级别
								totalAlarmLevel = Integer.parseInt(alarmArr[4]);
								totalShowNum = alarmArr[5];
							}
						}
						if(detailDatas.size()>1 && alarmArr[3].length()>0){
							functionSb.append("【" + detailData[0] + "】 " + alarmArr[3] + "</br>");// 组织性能数据
						}else{
							functionSb.append(alarmArr[3]);// 组织性能数据
						}
						for (int j = 0; j < detailData.length; j++) {// 监测具体数据信息
							dataArr[index + j] = detailData[j];
						}
						
						detailDataList.add(dataArr);
					}
				}
				if(StringUtils.isNotBlank(firstPerformData)){
					functionSb.insert(0,firstPerformData+"</br>");
				}
				// ------- 主动告警
				String totalAlarmInfo = null;
				if(totalStatus == Contants.DETECTION_STATUS_NORMAL){
					// 本次数据正常，看原本的状态：若是告警状态，则发送恢复通知；若是正常状态，则不变
					if(alarmState){
						totalAlarmInfo = "【i18n_client.GetInfoRun.totalAlarmInfo1_n81i】</br>" + functionSb.toString();
					}
					alarmState = false;
					alarmTimes = 0;
				}else if(totalStatus == Contants.DETECTION_STATUS_ABNORMAL){//状态异常
					alarmTimes ++;
					if(!alarmState){// 本次数据不正常，看原本的状态：若是正常状态，则发送告警，此主动告警也只有一次，除非监测恢复正常，之后又异常
						if(setInfo!=null && "process".equalsIgnoreCase(setInfo.getCheckTypeName())) {//如果是进程监测							
							totalAlarmInfo = functionSb.toString();
						}else {							
							totalAlarmInfo = "【i18n_client.GetInfoRun.totalAlarmInfo2_n81i】</br>" + functionSb.toString();
						}
					}else if(alarmTimes == Contants.overAlarmValTimes && alarmState){//若原本的状态是告警状态，则判断是否连续几次达到告警值，次数会一直累积，知道监测恢复正常，所以如果一直异常，则也只告警一次连续4次异常
						totalAlarmInfo = "【i18n_client.GetInfoRun.totalAlarmInfo3_n81i】</br>" + functionSb.toString();
					}
					alarmState = true;
				}
				if(totalAlarmInfo!=null){
					//2014-5-12 add：根据配置的参数，决定是否启用监测数据超过设定值时的主动告警
					Boolean activeAlarmStart = Contants.ACTIIVE_ALARM_START;
					if(totalStatus == Contants.DETECTION_STATUS_NORMAL ||//数据恢复主动告警
							(activeAlarmStart && totalStatus == Contants.DETECTION_STATUS_ABNORMAL)) {//数据异常主动告警
						AlarmUtil.sendAlarmMsg(setInfo.getId(), setInfo.getCheckTypeName(), setInfo.getProcessIden(),
								new Date(), new Date(), totalAlarmLevel, totalStatus, totalAlarmInfo,totalShowNum);
					}
				}
				// ------- 总监测数据组织
				int index = 0;
				String[] totalData = new String[12];
				totalData[index++] = getUUID();// UUID
				totalData[index++] = setInfo.getId() + "";// 监测设置ID
				totalData[index++] = setInfo.getCheckTypeName();// 监测类别
				totalData[index++] = setInfo.getProcessIden();// 进程名称
				totalData[index++] = getStartClientTime();// 监测服务启动时间
				totalData[index++] = checkDelayTime;// 检测时延（秒）
				totalData[index++] = writeDate.getTime() + "";// 本次检测时间
				totalData[index++] = checkTimes + "";// 尝试次数
				totalData[index++] = getNextCheckTime(writeDate);// 下次计划监测时间
				totalData[index++] = totalStatus + "";// 执行状态是否成功是否正常
				totalData[index++] = (detectInfo == null ? "" : detectInfo.getDescInfo());// 状态信息（描述信息）
				totalData[index++] = StringUtils.isBlank(functionSb.toString())?detectInfo.getDescInfo():functionSb.toString();// 性能数据
				
				List<String[]> dataList = new LinkedList<String[]>();
				// 总数据
				dataList.add(totalData);
				// 详细信息
				dataList.add(new String[]{"details", detailDataList.size() + ""});// details(解析标识),详细信息条数
				dataList.addAll(detailDataList);// 具体的详细数据
				// 特殊数据定制(目前是针对系统信息监测systeminfo)
				if (detectInfo!=null && detectInfo.getRelatedDatas() != null) {
					Set<Map.Entry<String, List<String[]>>> set = detectInfo.getRelatedDatas().entrySet();
					for(Map.Entry<String, List<String[]>> entry : set){
						dataList.add(new String[]{entry.getKey(), entry.getValue().size() + ""});// 解析标识, 行数（当前解析标识指定的类型有多少条数据）
						dataList.addAll(entry.getValue());// 多条数据
					}
				}
				
				for(String[] val : dataList){
//					logger.debug(Arrays.toString(val));//i18nlog
				}

				File file = new File(filePath);
				if (!file.exists()) {
					file.mkdirs();
				}
				file = new File(filePath + fileName + ".tp");
				FileWrUtil.csvFilePrinter(file, Contants.charset, dataList);
				
				File file2 = new File(filePath + fileName);
				file.renameTo(file2);
				break;
			} catch (Exception e) {
				logger.error("Client write file" + fileName + "error: " + Utils.printExceptionStack(e));
				continue;
			}finally{
				diskUsageTime=0;
				cpuUsageTime=0;
				cpuUsageName="";
				diskUsageName="";
			}
		} while (checkTimes < setInfo.getCheckMaxTimes());

	}
	
	/**
	 * 获得当前的报警设置信息，与相应字段值比较
	 * 2014-5-15 update: 标识字段，标识
	 */
	private String[] getAlarmState(String[] sysData){
		// strs数组数据依次为：告警序列号、告警级别、告警值、性能数据、本次最高告警级别
		String[] strs = new String[]{"", "", "", "", "",""};
		if(alarmInfos==null){
			return strs;
		}
		int maxAlarmLevel = 99;
		String maxShowNum = "" ;
		for (AlarmInfo alarm : alarmInfos) {
//			logger.info("*****marker:"+alarm.getMarker());
			if (sysData.length < alarm.getShowNum()) {// 得到的数据个数和告警列数比较
				continue;
			}
			String data = sysData[alarm.getShowNum() - 1];
			boolean alarmFlag = false;
			
			/**
			 * 2014-5-15 add：
			 * 1.指定了特定的标识：
			 *     (1).当前标识非空：不在指定标识内，则不做告警判断；在，则做告警判断
			 *     (2).当前标识为空：空不在指定标识内，不做告警判断
			 * 2.未指定特定的标识：所有标识都进行告警判断
			 */
			String marker = alarm.getMarker();
//			Integer markerShowNum = 1;//先默认取第一个		
			Integer markerShowNum = alarm.getMarkerFiledShowNum();
			logger.info("告警设置：checkType|"+alarm.getCheckType()+"  setInfoId|"+alarm.getSetInfoId()+"  markerShowNum|"+alarm.getMarkerFiledShowNum()+"   marker|"+alarm.getMarker());
			if(markerShowNum!=null && markerShowNum>0 //若未指定标识字段，则从DC传递到NC的json字符串中markerShowNum的值为0
					&& StringUtils.isNotBlank(marker)) {
				String markerCurVal = sysData[markerShowNum - 1];//当前条详细监测数据的标识符
				String sperator = SysConfig.getStringVal("alarm.set.marker.separator", "|");
				if(StringUtils.isNotBlank(markerCurVal)) {
					if(!(sperator+marker.trim()+sperator).toLowerCase().contains((sperator+markerCurVal.trim()+sperator).toLowerCase())) {//当前标识不在指定的标识里
						continue;
					}
				}else {
					continue;
				}
			}
			
			if ("equals".equalsIgnoreCase(alarm.getPoliceSysmbols())) {//相同
				if(data.equals(alarm.getPoliceValue())){
					alarmFlag = true;
				}
			} else if ("include".equalsIgnoreCase(alarm.getPoliceSysmbols())) {//包含告警值内容
				if(data.contains(alarm.getPoliceValue())){
					alarmFlag = true;
				}
			} else if ("exclude".equalsIgnoreCase(alarm.getPoliceSysmbols())) {//不包含告警值内容
				if(!data.contains(alarm.getPoliceValue())){
					alarmFlag = true;
				}
			} else {
				double result = Double.parseDouble(data) - Double.parseDouble(alarm.getPoliceValue());
				if ((">".equals(alarm.getPoliceSysmbols()) && result > 0)
						|| ("<".equals(alarm.getPoliceSysmbols()) && result < 0)
						|| ("=".equals(alarm.getPoliceSysmbols()) && result == 0)
						|| (">=".equals(alarm.getPoliceSysmbols()) && result >= 0)
						|| ("<=".equals(alarm.getPoliceSysmbols()) && result <=0) ) {
					alarmFlag = true;
				}
			}
			String sysmbol = getAlarmSymbol(alarm.getPoliceSysmbols(), alarmFlag);
			if(alarmFlag){
				strs[0] += alarm.getShowNum() + "|";// 告警序列号
				strs[1] += alarm.getPoliceLevel() + "|"; //告警级别
				strs[2] += alarm.getPoliceValue() + "|"; //告警值
				//性能信息
				if(setInfo!=null && "process".equalsIgnoreCase(setInfo.getCheckTypeName())) {//如果是进程监测
//					strs[3] +="进程不存在 ";//如果不加\"，则只显示进程不存，少一个字符，所以在最后加空格，这样就不会少显示一个字符了
					strs[3] +="i18n_client.GetInfoRun.processNotExists_n81i ";//如果不加\"，则只显示进程不存，少一个字符，所以在最后加空格，这样就不会少显示一个字符了
//					strs[3] +="进程不存在（进程PID文件："+setInfo.getProcessFile()+",进程搜索关键字："+setInfo.getProcessSearchKeyCode()+"）";
//				}else if(setInfo!=null && "disk".equalsIgnoreCase(setInfo.getCheckTypeName()) && "磁盘是否可写".equals(alarm.getFiledCommonts())){
				}else if(setInfo!=null && "disk".equalsIgnoreCase(setInfo.getCheckTypeName()) && "i18n_client.GetInfoRun.isDiskWriteAble_n81i".equals(alarm.getFiledCommonts())){
					strs[3] += alarm.getFiledCommonts()+":not writable;";
				}else {
//					if(setInfo!=null && "disk".equalsIgnoreCase(setInfo.getCheckTypeName()) && "磁盘使用率(%)".equals(alarm.getFiledCommonts())){
					if(setInfo!=null && "disk".equalsIgnoreCase(setInfo.getCheckTypeName()) && "i18n_client.GetInfoRun.diskUsed_n81i".equals(alarm.getFiledCommonts())){
						diskUsageTime ++;
						if("Linux".equals(System.getProperties().getProperty("os.name"))){
							diskUsageName+=sysData[0]+",";
//							firstPerformData = diskUsageTime+"个磁盘使用率超过"+alarm.getPoliceValue()+"%："+diskUsageName;
							firstPerformData = diskUsageTime+"i18n_client.GetInfoRun.warning_n81i"+alarm.getPoliceValue()+"%："+diskUsageName;
						}else{
							diskUsageName+=sysData[0].substring(0,sysData[0].length()-2)+",";
//							firstPerformData = diskUsageTime+"个磁盘使用率超过"+alarm.getPoliceValue()+"%："+diskUsageName.substring(0,diskUsageName.length()-1);
							firstPerformData = diskUsageTime+"i18n_client.GetInfoRun.warning_n81i"+alarm.getPoliceValue()+"%："+diskUsageName.substring(0,diskUsageName.length()-1);
						}
					}
//					if(setInfo!=null && "cpu".equalsIgnoreCase(setInfo.getCheckTypeName()) && "总的使用率(%)".equals(alarm.getFiledCommonts())){
					if(setInfo!=null && "cpu".equalsIgnoreCase(setInfo.getCheckTypeName()) && "i18n_client.GetInfoRun.ZongShiYongLv_n81i".equals(alarm.getFiledCommonts())){
						cpuUsageTime ++;//超过告警值得cpu数量
						cpuUsageName+=sysData[0]+",";
//						firstPerformData = cpuUsageTime+"个CPU的使用率占"+alarm.getPoliceValue()+"%："+cpuUsageName.substring(0,cpuUsageName.length()-1);
						firstPerformData = cpuUsageTime+"i18n_client.GetInfoRun.cpuShiyonglv_n81i"+alarm.getPoliceValue()+"%："+cpuUsageName.substring(0,cpuUsageName.length()-1);
					}
//					strs[3] += alarm.getFiledCommonts()+":"
//							  + sysData[alarm.getShowNum() - 1]
//					          + "("+alarm.getPoliceUnit()+")  " + sysmbol + "告警值"
//					          + alarm.getPoliceValue()
//					          + "("+alarm.getPoliceUnit()+") " + "  不正常；";
					strs[3] += alarm.getFiledCommonts()+":"
							  + sysData[alarm.getShowNum() - 1]
					          + "("+alarm.getPoliceUnit()+")  " + sysmbol + "i18n_client.GetInfoRun.warningValue_n81i"
					          + alarm.getPoliceValue()
					          + "("+alarm.getPoliceUnit()+") " + "  i18n_client.GetInfoRun.abnormal_n81i；";
				}
				
				//2011-09-29 添加了连续几次达到告警值后主动告警，恢复正常后发送恢复信息
				//2013-03-26 添加了告警状态控制是否立刻主动告警，如果已经告警则后期不发送告警信息。
				//alarmHandler(alarm, strs[3]);
				
				if (maxAlarmLevel  > alarm.getPoliceLevel()) {// 保留本次最高告警级别，值越小级别越高
					maxAlarmLevel = alarm.getPoliceLevel();
					maxShowNum = alarm.getShowNum()+"";
				}
				
			}else{
//				if(setInfo!=null && "disk".equalsIgnoreCase(setInfo.getCheckTypeName()) && "磁盘是否可写".equals(alarm.getFiledCommonts())){
//					strs[3] += alarm.getFiledCommonts()+":可写;";
				if(setInfo!=null && "disk".equalsIgnoreCase(setInfo.getCheckTypeName()) && "i18n_client.GetInfoRun.isDiskWriteAble_n81i".equals(alarm.getFiledCommonts())){
					strs[3] += alarm.getFiledCommonts()+":i18n_client.GetInfoRun.writeAble_n81i;";
				}else{
					// 性能信息
					strs[3] += alarm.getFiledCommonts()
							+ sysData[alarm.getShowNum() - 1]
							+ alarm.getPoliceUnit() + sysmbol + "i18n_client.GetInfoRun.warningValue_n81i"
							+ alarm.getPoliceValue()
							+ alarm.getPoliceUnit() + "  i18n_client.GetInfoRun.normal_n81i；";
				}
				//对发送告警信息后，恢复正常的信息(为保证告警成对)
				//2013-03-26 当告警恢复后发送恢复告警信息。
				//alarmRecovery(alarm, strs[3]);
				
			}

		}//for end
		for (int i = 0; i < strs.length-1; i++) {
			if (strs[i].length() > 0) {
				strs[i] = strs[i].substring(0, strs[i].length() - 1);
			}
		}
		strs[strs.length-2] = maxAlarmLevel + "";// 本次告警最高级别
		strs[strs.length-1] = maxShowNum ;// 本次告警最高级别对应的序列号showNum
		return strs;
	}
	
	private String getAlarmSymbol(String oldSymbol, boolean alarmFlag){
		String symbol = "";
		if(alarmFlag){
//			if(">".equals(oldSymbol)){
//				symbol = "大于";
//			}else if(">=".equals(oldSymbol)){
//				symbol = "超过";
//			}else if("<".equals(oldSymbol)){
//				symbol = "小于";
//			}else if("<=".equals(oldSymbol)){
//				symbol = "未超过";
//			}else if("=".equals(oldSymbol)){
//				symbol = "等于";
//			}else{
//				symbol = oldSymbol;
//			}
			if(">".equals(oldSymbol)){
				symbol = "i18n_client.GetInfoRun.gt_n81i";
			}else if(">=".equals(oldSymbol)){
				symbol = "i18n_client.GetInfoRun.out_n81i";
			}else if("<".equals(oldSymbol)){
				symbol = "i18n_client.GetInfoRun.lt_n81i";
			}else if("<=".equals(oldSymbol)){
				symbol = "i18n_client.GetInfoRun.in_n81i";
			}else if("=".equals(oldSymbol)){
				symbol = "i18n_client.GetInfoRun.eq_n81i";
			}else{
				symbol = oldSymbol;
			}
		}else{
//			if(">".equals(oldSymbol)){
//				symbol = "未超过";
//			}else if(">=".equals(oldSymbol)){
//				symbol = "小于";
//			}else if("<".equals(oldSymbol)){
//				symbol = "超过";
//			}else if("<=".equals(oldSymbol)){
//				symbol = "大于";
//			}else if("=".equals(oldSymbol)){
//				symbol = "不等于";
			if(">".equals(oldSymbol)){
				symbol = "i18n_client.GetInfoRun.in_n81i";
			}else if(">=".equals(oldSymbol)){
				symbol = "i18n_client.GetInfoRun.lt_n81i";
			}else if("<".equals(oldSymbol)){
				symbol = "i18n_client.GetInfoRun.out_n81i";
			}else if("<=".equals(oldSymbol)){
				symbol = "i18n_client.GetInfoRun.gt_n81i";
			}else if("=".equals(oldSymbol)){
				symbol = "i18n_client.GetInfoRun.notEquels_n81i";
			}else if("equals".equalsIgnoreCase(oldSymbol)){
				symbol = "not equals";
			}else if("include".equalsIgnoreCase(oldSymbol)){
				symbol = "exclude";
			}else if("exclude".equalsIgnoreCase(oldSymbol)){
				symbol = "include";
			}
		}
		return symbol;
	}
	
	/*// 判断是否主动告警
	private void alarmHandler(AlarmInfo alarm, String alarmInfo){
		// 判断告警状态是否变化，主动告警
		synchronized (alarmStates) {
			boolean isAlarm = false;
			if(alarmStates.containsKey(alarm.getId())){
				isAlarm = alarmStates.get(alarm.getId());
			}
			if(!isAlarm){
				alarmStates.put(alarm.getId(), true);
				// 报警
				AlarmUtil.sendAlarmMsg(setInfo.getId(), 
						setInfo.getCheckTypeName(), 
						setInfo.getProcessIden(), new Date(),
						new Date(), alarm.getPoliceLevel(),
						Contants.DETECTION_STATUS_ABNORMAL, alarmInfo);
				logger.debug("主动告警：" + alarmInfo);
				logger.debug("------id=" + alarm.getId() + "---Comments="+alarm.getFiledCommonts()+"---val=-1");
			}
		}
		// 判断连续达到告警值的次数，达到一定次数，主动告警
		synchronized (alarmTimes) {
			if(alarmTimes.containsKey(alarm.getId())){
				int alarmCnt = alarmTimes.get(alarm.getId());
				if(alarmCnt + 1 >= Contants.overAlarmValTimes){//连续几次达到告警值
					String alarmMsg = alarm.getFiledCommonts() + ", 连续" + (alarmCnt + 1) + "次达到告警值";
					AlarmUtil.sendAlarmMsg(setInfo.getId(), 
							setInfo.getCheckTypeName(), 
							setInfo.getProcessIden(), new Date(),
							new Date(), alarm.getPoliceLevel(),
							Contants.DETECTION_STATUS_ABNORMAL, alarmMsg);
					logger.debug("主动告警："+alarmMsg);
					alarmTimes.put(alarm.getId(), -1);
					
					logger.debug("------id=" + alarm.getId() + "---Comments="+alarm.getFiledCommonts()+"---val=-1");
				}else if(alarmCnt > -1){
					alarmTimes.put(alarm.getId(), alarmCnt + 1);
					logger.debug("------id=" + alarm.getId() + "---Comments="+alarm.getFiledCommonts()+"---val="+(alarmCnt + 1));
				}
			}else{
				alarmTimes.put(alarm.getId(), 1);
				logger.debug("---nokey---id=" + alarm.getId() + "---Comments="+alarm.getFiledCommonts()+"---val=1");
			}
		}
	}
	// 判断是否恢复正常
	private void alarmRecovery(AlarmInfo alarm, String alarmInfo){
		// 判断是否告警状态，恢复正常
		synchronized (alarmStates) {
			if(alarmStates.containsKey(alarm.getId())){
				boolean isAlarm = alarmStates.get(alarm.getId());
				if(isAlarm){
					alarmStates.put(alarm.getId(), false);
					// 报警状态：恢复正常
					AlarmUtil.sendAlarmMsg(setInfo.getId(), 
							setInfo.getCheckTypeName(), 
							setInfo.getProcessIden(), new Date(),
							new Date(), 99, Contants.DETECTION_STATUS_NORMAL,
							alarmInfo);
					logger.debug(alarmInfo + ", 恢复正常");
					logger.debug("------id=" + alarm.getId() + "---Comments="+alarm.getFiledCommonts()+"---val=0");
				}
			}
		}
		// 判断是否连续几次达到告警值后主动告警，恢复正常
		synchronized (alarmTimes) {
			if(alarmTimes.containsKey(alarm.getId())){
				if(alarmTimes.get(alarm.getId()) == -1){
					AlarmUtil.sendAlarmMsg(setInfo.getId(), 
							setInfo.getCheckTypeName(), 
							setInfo.getProcessIden(), new Date(),
							new Date(), 99, Contants.DETECTION_STATUS_NORMAL,
							alarm.getFiledCommonts() + ", 恢复正常");
					logger.debug(alarm.getFiledCommonts() + ", 恢复正常");
				}
				alarmTimes.put(alarm.getId(), 0);
				logger.debug("------id=" + alarm.getId() + "---Comments="+alarm.getFiledCommonts()+"---val=0");
			}
		}
	}*/
	
    private String getUUID(){
    	return Contants.AGENT_HOST_UUID + ""; //Utils.getLocalIp();
    }
    // 检测时延（秒）
    private String getCheckDelayTime(Date now){
    	long val = System.currentTimeMillis() - now.getTime();
    	return val/1000 + "";
    }
    // 下一次检测时间
    private String getNextCheckTime(Date now){
    	Calendar cal = Calendar.getInstance();
		cal.setTime(now);
		cal.add(Calendar.MINUTE, setInfo.getCheckGap().intValue());
    	return cal.getTimeInMillis() + "";
    }
    // 服务监控开始时间
    private String getStartClientTime(){
    	return this.startTime.getTime() + "";
    }
    // 开机时间
    private String getStartComputerTime(){
    	return SystemInfo.getStartComputerTime();
    }
    
	public SetInfo getSetInfo() {
		return setInfo;
	}

	public void setSetInfo(SetInfo setInfo) {
		this.setInfo = setInfo;
	}

	public List<AlarmInfo> getAlarmInfos() {
		return alarmInfos;
	}

	public void setAlarmInfos(List<AlarmInfo> alarmInfos) {
		this.alarmInfos = alarmInfos;
	}

	public String getFirstPerformData() {
		return firstPerformData;
	}

	public void setFirstPerformData(String firstPerformData) {
		this.firstPerformData = firstPerformData;
	}

}
