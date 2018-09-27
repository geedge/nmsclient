package com.nis.nmsclient.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.nis.nmsclient.common.Common;
import com.nis.nmsclient.common.Contants;
import com.nis.nmsclient.model.AlarmInfo;
import com.nis.nmsclient.model.SetInfo;
import com.nis.nmsclient.thread.alarm.AlarmUtil;
import com.nis.nmsclient.util.FileUtil;
import com.nis.nmsclient.util.FileWrUtil;
import com.nis.nmsclient.util.ProcessUtil;
import com.nis.nmsclient.util.Utils;

public class DetecConfReqHandle {
	Logger logger = Logger.getLogger(DetecConfReqHandle.class);
	
	/**
	 * 初始化监测配置
	 *   处理接收到监测配置信息：解析成自定义的对象
	 * @param str JSONObject串
	 */
	public void handlerConfigByInit(String str, List<SetInfo> setInfos, Map<Long, List<AlarmInfo>> alarmInfos) {
		// -- 参数处理
		if (str == null || str.trim().length() <= 0) {
			return;
		}
		if(setInfos==null || alarmInfos == null){
			return;
		}
		setInfos.clear();
		alarmInfos.clear();
		
		// -- 初始化配置环境处理
		File file = new File(Contants.localDetecConfPath);
		if (!file.exists()) {// 判断存放目录是否存在，不存创建
			file.mkdirs();
		}
		File[] cfgFiles = file.listFiles();
		for (int i = 0; i < cfgFiles.length; i++) {//清空所有配置文件
			// 使用删除文件公共方法
			FileUtil.delDir(cfgFiles[i]);
			logger.debug("handlerConfigByInit()--delete file=" + cfgFiles[i].getAbsolutePath());
		}
		
		// -- 监测配置解析
		JSONObject jsonObj = JSONObject.fromObject(str);
		
		List<SetInfo> setList = null;
		if (str.toLowerCase().contains("setinfo")) {
			JSONArray jsonArr = jsonObj.getJSONArray("setInfo");
			setList = handerSetInfo(jsonArr);
		}
		if (setList == null || setList.size() == 0) {
			return;
		}
		Map<Long, List<AlarmInfo>> alarmMap = null;
		if (str.toLowerCase().contains("alarminfo")) {
			JSONArray jsonArr = jsonObj.getJSONArray("alarmInfo");
			alarmMap = handerAlarmInfo(jsonArr, setList);
		}
		// 20170413 zbc 缓存AlarmInfo信息（cfg文件中无报警单位字段）
		if(alarmMap != null) {
			Common.putAllDetecAlarmInfo(alarmMap);
		}

		for (SetInfo setInfo : setList) {
			if ("1".equals(setInfo.getCheckState())
					&& "1".equals(setInfo.getCheckWay())) {// 有效,被动方式获取
				setInfos.add(setInfo);
				Common.addOrUpdateAlarmPO(AlarmUtil.getAlarmPO(setInfo));
			}
		}
		if (alarmMap != null && alarmMap.size()>0) {
			alarmInfos.putAll(alarmMap);
		}
		
		if (str.toLowerCase().contains("showautoalarm")) {
			Object obj = jsonObj.get("showAutoAlarm");
			if (obj != null) {
				Contants.ACTIIVE_ALARM_START = (Boolean) obj;
			}
		}
	}
	
	/**
	 * 更新监测配置：先解析成自定义的对象，再根据对象信息添加、更新、停用监测
	 *    注：三方监测停用时Agent根据指定的PID杀掉进程 
	 * @param str JSONObject串
	 */
	public void handlerConfigByUpdate(String str) {
		if (str == null || str.trim().length() <= 0) {
			return;
		}
		
		File file = new File(Contants.localDetecConfPath);
		if (!file.exists()) {// 判断存放目录是否存在，不存创建
			file.mkdirs();
		}
		
		JSONObject jsonObj = JSONObject.fromObject(str);
		/*long flag = -1;
		if (str.toLowerCase().contains("number")) {
			flag = jsonObj.getLong("number");
		}*/
		List<SetInfo> setList = null;
		if (str.toLowerCase().contains("setinfo")) {
			JSONArray jsonArr = jsonObj.getJSONArray("setInfo");
			setList = handerSetInfo(jsonArr);
		}
		if (setList == null || setList.size() == 0) {
			return;
		}
		Map<Long, List<AlarmInfo>> alarmMap = null;
		if (str.toLowerCase().contains("alarminfo")) {
			JSONArray jsonArr = jsonObj.getJSONArray("alarmInfo");
			alarmMap = handerAlarmInfo(jsonArr, setList);
		}

		for (SetInfo setInfo : setList) {
			if ("0".equals(setInfo.getCheckState())
					|| "0".equals(setInfo.getCheckWay())) {// 将有效置为无效，或者将被动设为主动，停止线程
				Common.removeAlarmPO(setInfo.getId());
				if ("0".equals(setInfo.getIsSchedule())) {
					Common.stopSysDetec(setInfo);
				} else {
					Common.stopPluginDetec(setInfo);
				}
			} else {
				Common.addOrUpdateAlarmPO(AlarmUtil.getAlarmPO(setInfo));
				if ("0".equals(setInfo.getIsSchedule())) {// 如果是系统预设监测类型，则修改相应的监控线程
					List<AlarmInfo> alarmList = null;
					if (alarmMap != null) {
						alarmList = alarmMap.get(setInfo.getId());
					}
					Common.addOrUpdateSysDetec(setInfo, alarmList);
				} else { // 第三方监测
					// 缓存三方监测配置信息，用于合并临时结果文件
					Common.putPluginDetecSetInfo(setInfo.getId(), setInfo);
					
					if(!Common.COMMON_SYS_SETINFO.equals(setInfo.getIsSchedule())) { // 由Agent启动的第三方插件
						Common.startPluginDetec(setInfo);
					}
				}
			}
		}// for end
	}
	
	/**
	 * 解析监测设置基本信息
	 * 	1、相应类型的监测数据目录不存在，创建，若是Linux并赋读写权限
	 *  2、监测配置文件存在，先删除再重写，停用监测不再重写
	 */
	private List<SetInfo> handerSetInfo(JSONArray jsonArr) {
		// 文件路径
		List<SetInfo> setList = new ArrayList<SetInfo>();
		for (int i = 0; i < jsonArr.size(); i++) {
			Object obj = JSONObject.toBean(JSONObject
					.fromObject(jsonArr.get(i)), SetInfo.class);
			if (obj == null) {
				continue;
			}
			SetInfo setInfo = (SetInfo) obj;
			// 创建建相应类型的数据文件夹
			File dataDir = new File(Contants.localDataFilePath
					+ File.separator
					+ DetecConfOper.getFileName(setInfo.getCheckTypeName(),
							setInfo.getProcessIden(), null));
			if (!dataDir.exists()) {
				dataDir.mkdirs();
				logger.debug("handerSetInfo()--create dataDir=" +dataDir.getAbsolutePath());
			}
			// 文件名
			String fileName = DetecConfOper.getFileName(
					setInfo.getCheckTypeName(), setInfo.getProcessIden(),
					Contants.localDetecConfSuffix);

			// 判断文件是否存在，如果存在删除
			File cfgfile = new File(Contants.localDetecConfPath + File.separator
					+ fileName);
			if (cfgfile.exists()){
				//cfgfile.delete_bak();
				//使用删除文件公共方法
				FileUtil.delDir(cfgfile);
				logger.debug("handerSetInfo()--delete cfgfile=" + cfgfile.getAbsolutePath());
				//FileUtil.checkParentDirExist(cfgfile);
			}
			
			if ("1".equals(setInfo.getCheckState())
					&& "1".equals(setInfo.getCheckWay())) {//有效且被动方式获取的写入配置文件
				String dataDirStr = null;
				try {// 为了去除路径中的"." 和 ".."
					dataDirStr = dataDir.getCanonicalPath();
				} catch (IOException e) {
					dataDirStr = dataDir.getAbsolutePath();
				}
				try {
					FileWrUtil.cfgFilePrinter(cfgfile, Contants.charset,
							setInfo.getValArr(Contants.AGENT_HOST_UUID, dataDirStr));
					logger.debug("handerSetInfo()--write cfgfile=" + cfgfile.getAbsolutePath());
				} catch (Exception e) {
					logger.error("Write the document“" + cfgfile.getAbsolutePath() + "”error: " + Utils.printExceptionStack(e));
					continue;
				}
			}
			setList.add(setInfo);

		}// for end
		//修改数据目录的权限
		ProcessUtil.permit(777, new File(Contants.localDataFilePath));
		
		return setList;
	}
	
	/**
	 * 解析监测设置的告警信息
	 *   1、将同一监测的所有告警信息整理放到一个列表
	 *   2、根据监测设置类型和设置名称找配置文件，找到了将告警信息追加到配置文件
	 *   3、只要有监测设置，不管有没有设置告警信息，都要将相应字段追加到配置文件，只是无告警信息时，配置字段为空
	 */
	private Map<Long, List<AlarmInfo>> handerAlarmInfo(JSONArray jsonArr, List<SetInfo> setList) {
		if(setList==null || setList.size()==0){
			logger.info("无监测设置信息，无需处理报警设置");
			return null;
		}
		File dir = new File(Contants.localDetecConfPath);
		if (!dir.exists()) {// 判断存放目录是否存在, 不存在返回
			logger.error("Please check whether the alarm settings information is consistent with the monitoring settings information！");
			return null;
		}
		Map<Long, List<AlarmInfo>> alarmMap = new HashMap<Long, List<AlarmInfo>>();
		if(jsonArr!=null){
			for (int i = 0; i < jsonArr.size(); i++) {
				Object obj = JSONObject.toBean(JSONObject
						.fromObject(jsonArr.get(i)), AlarmInfo.class);
				if (obj != null) {
					AlarmInfo alarmInfo = (AlarmInfo) obj;
					if (alarmMap.containsKey(alarmInfo.getSetInfoId())) {// 如果报警信息中已经存在相应类型的报警则追加
						List<AlarmInfo> aList = alarmMap.get(alarmInfo.getSetInfoId());
						aList.add(alarmInfo);
						alarmMap.put(alarmInfo.getSetInfoId(), aList);
					} else {
						List<AlarmInfo> aList = new ArrayList<AlarmInfo>();
						aList.add(alarmInfo);
						alarmMap.put(alarmInfo.getSetInfoId(), aList);
					}
				}
			}
		}
		Map<Long, List<AlarmInfo>> alarmMapNew = new HashMap<Long, List<AlarmInfo>>();
		// 依次查看监测设置信息，不管有没有设置告警信息，都要写配置police=，若无告警信息，配置字段值为空
		for(SetInfo setInfo : setList){
			Long key = setInfo.getId();
			List<AlarmInfo> alarmList = alarmMap.get(key);
			// 配置信息保存对应的文件名
			String fileName = DetecConfOper.getFileName(
					setInfo.getCheckTypeName(), setInfo.getProcessIden(),
					Contants.localDetecConfSuffix);
			File f = DetecConfOper.getConfigFile(dir, fileName);
			{// 追加报警设置到相应的监测配置文件
			if (f != null && f.exists()) 
				try {
					FileWrUtil.cfgFileAppender(f,
							Contants.charset, new String[]{getAlarmArr(alarmList)});
					if(alarmList!=null){// 只有成功保存了文件并且设置为告警信息的，才返回给上一方法
						alarmMapNew.put(key, alarmList);
					}
				} catch (Exception e) {
					logger.error("Write the document“" + f.getAbsolutePath() + "”error: " + Utils.printExceptionStack(e));
				}
			}
		}
		return alarmMapNew;
	}
	
	private static String getAlarmArr(List<AlarmInfo> alarmList) {
		StringBuffer sb = new StringBuffer();
		sb.append("police=");
		if (alarmList != null && alarmList.size() > 0) {
			for (AlarmInfo alarmInfo : alarmList) {
				sb.append(alarmInfo.toStringVal() + ",");
			}
			sb.delete(sb.toString().length() - 1, sb.toString().length());
		}
		return sb.toString();
	}
}
