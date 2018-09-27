package com.nis.systeminfo.thread;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.nis.nmsclient.NmsClient;
import com.nis.nmsclient.common.Common;
import com.nis.nmsclient.common.Contants;
import com.nis.nmsclient.common.SysConfig;
import com.nis.nmsclient.model.AlarmInfo;
import com.nis.nmsclient.model.SetInfo;
import com.nis.nmsclient.thread.alarm.AlarmUtil;
import com.nis.nmsclient.util.DateUtil;
import com.nis.nmsclient.util.FileWrUtil;

/**
 * 临时文件的合并过程替换为NewPluginResultMerge
 */
@Deprecated
public class PluginResultMerge {

	private static final Logger logger = Logger.getLogger(PluginResultMerge.class);
	
	private static final File srcRootDir = new File(Contants.localTempDataIncomingPath);
	private static final File tarRootDir = new File(Contants.localDataFilePath);
	
	private static HashMap<Long, Boolean> historyAlarmStateMap = new HashMap<Long, Boolean>();
	private static HashMap<Long, Integer> historyAlarmTimesMap = new HashMap<Long, Integer>();
	
	public void merge() {
		try {
			logger.debug("合并临时结果文件开始 ~~~~~~~");

			Collection<SetInfo> setInfos = Common.getPluginDetecSetInfos();
			System.out.println(setInfos.size());
			for (SetInfo setInfo : setInfos) {
				String subDir = setInfo.getCheckTypeName() + "_" + setInfo.getProcessIden();
				File dir = new File(srcRootDir, subDir);
				File tarDir = new File(tarRootDir, subDir);

				if (!dir.exists() || dir.listFiles().length == 0) {
					continue;
				}

				// 监测信息报警相关信息
				System.out.println(Common.detecAlarmInfoMap);
				List<AlarmInfo> alarmInfoList = Common.getDetecAlarmInfo(setInfo.getId());
				
				// 相邻的连个结果文件超过两个监测周期时，该时间段内出现未生成数据结果的周期
				Long period = setInfo.getCheckGap() * (60 * 1000); // millisecond
				Long detecTimeout = period * 2;

				File[] results = dir.listFiles();
				Arrays.sort(results, new Comparator<File>() { // 同一类结果文件按按时间先后排序
					public int compare(File f1, File f2) {
						return f1.getName().compareTo(f2.getName());
					}
				});

				for (File srcFile : results) {
					// 从文件名获取监测时间毫秒数（YYYYMMDDhhmmss.tmp）
					Calendar cal = Calendar.getInstance();
					cal.set(Calendar.YEAR, Integer.parseInt(srcFile.getName().substring(0, 4)));
					cal.set(Calendar.MONTH, Integer.parseInt(srcFile.getName().substring(4, 6)) - 1);
					cal.set(Calendar.DATE, Integer.parseInt(srcFile.getName().substring(6, 8)));
					cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(srcFile.getName().substring(8, 10)));
					cal.set(Calendar.MINUTE, Integer.parseInt(srcFile.getName().substring(10, 12)));
					cal.set(Calendar.SECOND, Integer.parseInt(srcFile.getName().substring(12, 14)));

					Long detecTime = cal.getTimeInMillis();
					Date writeDate = new Date(detecTime); // 临时文件写入时间

					while (setInfo.getLastMergeDetecTime() + detecTimeout < detecTime) {
						// 存在未生成数据结果的监测周期（每个周期记录一次）
						historyAlarmStateMap.put(setInfo.getId(), true); // 数据状态不正常
						
						// 未生成监测数据（主动报警AlarmThread连续四次未生成监测数据报警）
						AlarmUtil.sendAlarmMsg(setInfo.getId(), setInfo.getCheckTypeName(), setInfo.getProcessIden(),
//								new Date(detecTime), new Date(detecTime), 1, Contants.DETECTION_STATUS_FAILURE, "监测数据未生成");
								new Date(detecTime), new Date(detecTime), 1, Contants.DETECTION_STATUS_FAILURE, "i18n_client.NewPluginResultMerge.detecateData_n81i");
						
						// TODO 存在主动告警发送延时问题，可能会有多次告警连续发送
						setInfo.setLastMergeDetecTime(setInfo.getLastMergeDetecTime() + period);
					}

					//读取临时结果文件
					List<String[]> lines = FileWrUtil.csvFileParser(srcFile, Contants.charset);
					
					/*
					 *  任务执行失败状态判断（临时文件存在时，告警并删除临时文件）：
					 *  	1、公共信息不完整；2、无具体数据信息或具体数据信息为空
					 */
					boolean execFail = false;
					if(lines.size() < 1 || lines.get(0).length < 3) { // 公共信息不完整
						logger.info("预设监测 setId: " + setInfo.getId() + " > " + srcFile.getName() + "，公共信息不完整，监测执行错误");
						execFail = true;
					}
					if(lines.size() < 3) { // 无具体数据信息
						logger.info("预设监测 setId: " + setInfo.getId() + " > " + srcFile.getName() + "，无具体数据信息，监测执行错误");
						execFail = true;
					}
					if(lines.size() >= 3) { // 具体数据信息为空字段
						String detailStr = Arrays.toString(lines.get(1));
						String detailReg = "^\\[details, (\\d+)\\]$";
						if(detailStr.matches(detailReg)) {
							int endNum = Integer.parseInt(detailStr.replaceAll(detailReg, "$1")); // details格式校验
							if (endNum < 1) {
								logger.info("预设监测 setId: " + setInfo.getId() + " > " + srcFile.getName() + "，具体数据信息格式错误，监测执行错误");
								execFail = true;
							}
							for (int r = 0; r < endNum; r++) {
								String[] sysData = lines.get(r + 2);
								if (Arrays.toString(sysData).matches("^\\[[,|\\s]*\\]$")) { // 空字段
									logger.info("预设监测 setId: " + setInfo.getId() + " > " + srcFile.getName() + "，具体数据信息为空字段，监测执行错误");
									execFail = true;
								}
							}
						} else {
							logger.info("预设监测 setId: " + setInfo.getId() + " > " + srcFile.getName() + "，具体数据信息格式错误，监测执行错误");
							 execFail = true;
						}
					}
					if(execFail) {
						historyAlarmStateMap.put(setInfo.getId(), true); // 数据状态不正常
						srcFile.delete(); // 删除错误临时文件，并告警
						AlarmUtil.sendAlarmMsg(setInfo.getId(), setInfo.getCheckTypeName(), setInfo.getProcessIden(),
//								new Date(detecTime), new Date(detecTime), 1, Contants.DETECTION_STATUS_FAILURE, "监测数据未生成");
								new Date(detecTime), new Date(detecTime), 1, Contants.DETECTION_STATUS_FAILURE, "i18n_client.NewPluginResultMerge.detecateData_n81i");
						continue;
					}
					

					String checkDelayTime = lines.get(0)[0];// 检测时延（秒）
					String checkTimes = lines.get(0)[1]; // 尝试次数
					String detectInfo = lines.get(0)[2]; // 状态信息（描述信息）
					StringBuffer functionSb = new StringBuffer(); // 性能数据
					
					// 执行状态，告警级别
					int totalStatus = Contants.DETECTION_STATUS_ABNORMAL; //监测信息不正常
					int totalAlarmLevel = 99;
					String totalShowNum ="";//告警序列号，取告警级别高的showNum add by jinsj for 紧急告警
					if(lines.size() > 1) {
						String[] detail = lines.get(1);
						if("details".equalsIgnoreCase(detail[0]) && detail[1].trim().matches("\\d+")) {
							 int endNum = StringUtils.isBlank(detail[1])?0:Integer.parseInt(detail[1].trim());

							 if(endNum > 0) { // 详细信息条数大于0，且所有数据不超过告警值时，监测信息正常
								 totalStatus = Contants.DETECTION_STATUS_NORMAL; // 监测信息正常
								 
								for (int r = 0; r < endNum; r++) {
									String[] sysData = lines.get(r + 2);
									String[] warninfos = getAlarmState(alarmInfoList, sysData);
									String[] combine = new String[sysData.length + 3]; // 组合详细信息
									System.arraycopy(warninfos, 0, combine, 0, 3);
									System.arraycopy(sysData, 0, combine, 3, sysData.length);
									lines.set(r + 2, combine);
									
									totalAlarmLevel = Integer.parseInt(warninfos[4]);
									totalShowNum = warninfos[5];
									
									if(StringUtils.isNotBlank(warninfos[0])) {
										totalStatus = Contants.DETECTION_STATUS_ABNORMAL; // 监测信息不正常
										functionSb.append(warninfos[3] + "</br>"); // 组织性能数据
									} else {
										if(StringUtils.isNotBlank(warninfos[3])) {
											functionSb.append(warninfos[3] + "</br>"); // 组织性能数据
										}
									}
								}
							 }
						}
					}
					
					// 主动告警
					String totalAlarmInfo = null;
					if(totalStatus == Contants.DETECTION_STATUS_NORMAL){
						historyAlarmStateMap.put(setInfo.getId(), false);
						historyAlarmTimesMap.put(setInfo.getId(), 0);
						
						// 本次数据正常，看原本的状态：若是告警状态，则发送恢复通知；若是正常状态，则不变
						if(getHistoryAlarmState(setInfo.getId())){
							totalAlarmInfo = "【i18n_client.NewPluginResultMerge.totalAlarmInfo1_n81i】</br>" + functionSb.toString();
						}

					}else if(totalStatus == Contants.DETECTION_STATUS_ABNORMAL){//状态异常
						historyAlarmStateMap.put(setInfo.getId(), true);
						historyAlarmTimesMap.put(setInfo.getId(), getHistoryAlarmTimes(setInfo.getId()) + 1);
						
						// 本次数据不正常，看原本的状态：若是正常状态，则发送告警，此主动告警也只有一次，除非监测恢复正常，之后又异常
						if(getHistoryAlarmTimes(setInfo.getId()) == 1) {//如果是进程监测							
							totalAlarmInfo = "【i18n_client.NewPluginResultMerge.totalAlarmInfo2_n81i】</br>" + functionSb.toString();
						}
						if(getHistoryAlarmTimes(setInfo.getId()) == Contants.overAlarmValTimes) {//若原本的状态是告警状态，则判断是否连续几次达到告警值，次数会一直累积，知道监测恢复正常，所以如果一直异常，则也只告警一次连续4次异常							
							totalAlarmInfo = "【i18n_client.NewPluginResultMerge.totalAlarmInfo3_n81i】</br>" + functionSb.toString();
						}
					}
					if(totalAlarmInfo!=null){
						//2014-5-12 add：根据配置的参数，决定是否启用监测数据超过设定值时的主动告警
						Boolean activeAlarmStart = Contants.ACTIIVE_ALARM_START;
						if(totalStatus == Contants.DETECTION_STATUS_NORMAL || //数据恢复主动告警
								(activeAlarmStart && totalStatus == Contants.DETECTION_STATUS_ABNORMAL)) { //数据异常主动告警
							AlarmUtil.sendAlarmMsg(setInfo.getId(), setInfo.getCheckTypeName(), setInfo.getProcessIden(),
									new Date(), new Date(), totalAlarmLevel, totalStatus, totalAlarmInfo,totalShowNum);
						}
					}

					// 组织监测数据组织
					List<String[]> dataList = new LinkedList<String[]>();
					// 总数据
					String[] totalData = new String[12];
					int index = 0;
					totalData[index++] = Contants.AGENT_HOST_UUID + ""; // UUID
					totalData[index++] = setInfo.getId() + ""; // 监测设置ID
					totalData[index++] = setInfo.getCheckTypeName(); // 监测类别
					totalData[index++] = setInfo.getProcessIden(); // 进程名称
					totalData[index++] = setInfo.getPlanCheckTime() + ""; // 监测服务启动时间
					totalData[index++] = checkDelayTime; // 检测时延（秒）
					totalData[index++] = writeDate.getTime() + ""; // 本次检测时间
					totalData[index++] = checkTimes; // 尝试次数
					totalData[index++] = getNextCheckTime(writeDate, setInfo);// 下次计划监测时间
					totalData[index++] = totalStatus + "";// 执行状态是否成功是否正常
					totalData[index++] = detectInfo; // 状态信息（描述信息）
					totalData[index++] = StringUtils.isBlank(functionSb.toString()) ? detectInfo : functionSb.toString();// 性能数据

					dataList.add(totalData);
					// 详细信息
					dataList.addAll(lines.subList(1, lines.size()));

					// 写入文件
					String fileName = DateUtil.getStingDate(DateUtil.YYYYMMDDHH24MMSS, writeDate) + ".csv";
					File tarFile1 = new File(tarDir, fileName + ".tp");
					FileWrUtil.csvFilePrinter(tarFile1, Contants.charset, dataList);
					File tarFile2 = new File(tarDir, fileName);
					if (tarFile2.exists())
						tarFile2.delete();
					tarFile1.renameTo(tarFile2);
					srcFile.delete();

					logger.info("生成监测结果文件：" + tarFile2.getParentFile().getName() + "/" + tarFile2.getName());

					// 忽略NC停止时间内的监测周期
					// setInfo.setLastMergeDetecTime(detecTime);
					setInfo.setLastMergeDetecTime(Math.max(detecTime, setInfo.getLastMergeDetecTime()));
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			logger.debug("合并临时结果文件结束 ~~~~~~~");
		}
	}

	// 下一次检测时间
	private String getNextCheckTime(Date now, SetInfo setInfo) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(now);
		cal.add(Calendar.MINUTE, setInfo.getCheckGap().intValue());
		return cal.getTimeInMillis() + "";
	}

	public static void main(String[] args) {
		NmsClient client = new NmsClient();
		client.initDetecConfig();
		for (SetInfo setInfo : client.setInfos) {
			Common.putPluginDetecSetInfo(setInfo.getId(), setInfo);
		}
		new PluginResultMerge().merge();
	}
	
	/**
	 * 获取当前行告警信息
	 * @param sysData
	 * @return
	 */
	private String[] getAlarmState(List<AlarmInfo> alarmInfos, String[] sysData) {
		// strs数组数据依次为：告警序列号、告警级别、告警值、性能数据、本次最高告警级别
		String[] strs = new String[] { "", "", "", "", "", "" };
		if (alarmInfos == null) {
			return strs;
		}
		int maxAlarmLevel = 99;
		String maxShowNum = "";
		for (AlarmInfo alarm : alarmInfos) {
			if (sysData.length < alarm.getShowNum()) {// 得到的数据个数和告警列数比较
				continue;
			}
			String data = sysData[alarm.getShowNum() - 1];
			boolean alarmFlag = false;

			/**
			 * 2014-5-15 add： 1.指定了特定的标识： (1).当前标识非空：不在指定标识内，则不做告警判断；在，则做告警判断
			 * (2).当前标识为空：空不在指定标识内，不做告警判断 2.未指定特定的标识：所有标识都进行告警判断
			 */
			String marker = alarm.getMarker();
			// Integer markerShowNum = 1;//先默认取第一个
			Integer markerShowNum = alarm.getMarkerFiledShowNum();
			logger.info("告警设置：checkType|" + alarm.getCheckType() + "  setInfoId|" + alarm.getSetInfoId()
					+ "  markerShowNum|" + alarm.getMarkerFiledShowNum() + "   marker|" + alarm.getMarker());
			if (markerShowNum != null && markerShowNum > 0 // 若未指定标识字段，则从DC传递到NC的json字符串中markerShowNum的值为0
					&& StringUtils.isNotBlank(marker)) {
				String markerCurVal = sysData[markerShowNum - 1];// 当前条详细监测数据的标识符
				String sperator = SysConfig.getStringVal("alarm.set.marker.separator", "|");
				if (StringUtils.isNotBlank(markerCurVal)) {
					if (!(sperator + marker.trim() + sperator).toLowerCase().contains(
									(sperator + markerCurVal.trim() + sperator).toLowerCase())) {// 当前标识不在指定的标识里
						continue;
					}
				} else {
					continue;
				}
			}

			if ("equals".equalsIgnoreCase(alarm.getPoliceSysmbols())) {// 相同
				if (data.equals(alarm.getPoliceValue())) {
					alarmFlag = true;
				}
			} else if ("include".equalsIgnoreCase(alarm.getPoliceSysmbols())) {// 包含告警值内容
				if (data.contains(alarm.getPoliceValue())) {
					alarmFlag = true;
				}
			} else if ("exclude".equalsIgnoreCase(alarm.getPoliceSysmbols())) {// 不包含告警值内容
				if (!data.contains(alarm.getPoliceValue())) {
					alarmFlag = true;
				}
			} else {
				double result = Double.parseDouble(data)
						- Double.parseDouble(alarm.getPoliceValue());
				if ((">".equals(alarm.getPoliceSysmbols()) && result > 0)
						|| ("<".equals(alarm.getPoliceSysmbols()) && result < 0)
						|| ("=".equals(alarm.getPoliceSysmbols()) && result == 0)
						|| (">=".equals(alarm.getPoliceSysmbols()) && result >= 0)
						|| ("<=".equals(alarm.getPoliceSysmbols()) && result <= 0)) {
					alarmFlag = true;
				}
			}
			
			String sysmbol = getAlarmSymbol(alarm.getPoliceSysmbols(), alarmFlag);
			if (alarmFlag) {
				strs[0] += alarm.getShowNum() + "|";// 告警序列号
				strs[1] += alarm.getPoliceLevel() + "|"; // 告警级别
				strs[2] += alarm.getPoliceValue() + "|"; // 告警值
				// 性能信息
//				strs[3] += alarm.getFiledCommonts() + ":"
//						+ sysData[alarm.getShowNum() - 1] + "("
//						+ alarm.getPoliceUnit() + ") " + sysmbol + "告警值"
//						+ alarm.getPoliceValue() + "(" + alarm.getPoliceUnit()
//						+ ") " + "不正常；";
				strs[3] += alarm.getFiledCommonts() + ":"
						+ sysData[alarm.getShowNum() - 1] + "("
						+ alarm.getPoliceUnit() + ") " + sysmbol + "i18n_client.NewPluginResultMerge.warningValue_n81i"
						+ alarm.getPoliceValue() + "(" + alarm.getPoliceUnit()
						+ ") " + "i18n_client.NewPluginResultMerge.abnormal_n81i；";
			} else {
				// 性能信息
				strs[3] += alarm.getFiledCommonts() + ":"
						+ sysData[alarm.getShowNum() - 1] + "("
						+ alarm.getPoliceUnit() + ") " + sysmbol + "i18n_client.NewPluginResultMerge.warningValue_n81i"
						+ alarm.getPoliceValue() + "(" + alarm.getPoliceUnit()
						+ ") " + "i18n_client.NewPluginResultMerge.normal_n81i；";
			}

			// 2011-09-29 添加了连续几次达到告警值后主动告警，恢复正常后发送恢复信息
			// 2013-03-26 添加了告警状态控制是否立刻主动告警，如果已经告警则后期不发送告警信息。
			if (maxAlarmLevel > alarm.getPoliceLevel()) {// 保留本次最高告警级别，值越小级别越高
				maxAlarmLevel = alarm.getPoliceLevel();
				maxShowNum = alarm.getShowNum() + "";
			}

		}// for end
		for (int i = 0; i < strs.length - 1; i++) {
			if (strs[i].length() > 0) {
				strs[i] = strs[i].substring(0, strs[i].length() - 1);
			}
		}
		strs[strs.length - 2] = maxAlarmLevel + "";// 本次告警最高级别
		strs[strs.length - 1] = maxShowNum;// 本次告警最高级别对应的序列号showNum
		return strs;
	}

	private String getAlarmSymbol(String oldSymbol, boolean alarmFlag) {
		String symbol = "";
		if (alarmFlag) {
//			if (">".equals(oldSymbol)) {
//				symbol = "大于";
//			} else if (">=".equals(oldSymbol)) {
//				symbol = "超过";
//			} else if ("<".equals(oldSymbol)) {
//				symbol = "小于";
//			} else if ("<=".equals(oldSymbol)) {
//				symbol = "未超过";
//			} else if ("=".equals(oldSymbol)) {
//				symbol = "等于";
//			} else {
//				symbol = oldSymbol;
//			}
			if (">".equals(oldSymbol)) {
				symbol = "i18n_client.NewPluginResultMerge.gt_n81i";
			} else if (">=".equals(oldSymbol)) {
				symbol = "i18n_client.NewPluginResultMerge.out_n81i";
			} else if ("<".equals(oldSymbol)) {
				symbol = "i18n_client.NewPluginResultMerge.lt_n81i";
			} else if ("<=".equals(oldSymbol)) {
				symbol = "i18n_client.NewPluginResultMerge.in_n81i";
			} else if ("=".equals(oldSymbol)) {
				symbol = "i18n_client.NewPluginResultMerge.eq_n81i";
			} else {
				symbol = oldSymbol;
			}
		} else {
			if (">".equals(oldSymbol)) {
				symbol = "i18n_client.NewPluginResultMerge.in_n81i";
			} else if (">=".equals(oldSymbol)) {
				symbol = "i18n_client.NewPluginResultMerge.lt_n81i";
			} else if ("<".equals(oldSymbol)) {
				symbol = "i18n_client.NewPluginResultMerge.out_n81i";
			} else if ("<=".equals(oldSymbol)) {
				symbol = "i18n_client.NewPluginResultMerge.gt_n81i";
			} else if ("=".equals(oldSymbol)) {
				symbol = "i18n_client.GetInfoRun.notEquels_n81i";
			} else if ("equals".equalsIgnoreCase(oldSymbol)) {
				symbol = "not equals";
			} else if ("include".equalsIgnoreCase(oldSymbol)) {
				symbol = "exclude";
			} else if ("exclude".equalsIgnoreCase(oldSymbol)) {
				symbol = "include";
			}
		}
		return symbol;
	}
	
	private boolean getHistoryAlarmState(Long setInfoId) {
		Boolean alarmState = historyAlarmStateMap.get(setInfoId);
		return (alarmState != null && alarmState == true);
	}
	
	private int getHistoryAlarmTimes(Long setInfoId) {
		Integer alarmTimes = historyAlarmTimesMap.get(setInfoId);
		return (alarmTimes != null ? alarmTimes : 0);
	}
}
