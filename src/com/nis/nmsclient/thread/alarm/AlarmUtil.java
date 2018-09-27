package com.nis.nmsclient.thread.alarm;

import java.util.Date;
import java.util.concurrent.Future;
import org.apache.log4j.Logger;
import com.nis.nmsclient.common.Common;
import com.nis.nmsclient.common.Contants;
import com.nis.nmsclient.config.DetecConfOper;
import com.nis.nmsclient.model.SetInfo;
import com.nis.nmsclient.thread.socket.CommonSocket;
import com.nis.nmsclient.thread.socket.SSLClient;
import com.nis.nmsclient.util.Utils;

public class AlarmUtil {
	static Logger logger = Logger.getLogger(AlarmUtil.class);
	
	public static void sendAlarmMsg(AlarmPO alarm, Date start,
			Date end, int alarmLevel, int state, String alarmMsg,String... showNum) {
		sendAlarmMsg(alarm.getId(), alarm.getType(), alarm
				.getProcIden(), start, end, alarmLevel, state, alarmMsg,showNum);
	}
	
	public static void sendAlarmMsg(Long alarmId, String checkType,
			String procIden, Date start, Date end, int alarmLevel, int state,
			String alarmMsg,String... showNum) {
		String seprator = Contants.COMMON_MSG_SEPRATOR;
		String showAlarmNum="";
		if (showNum!=null && showNum.length>0)
		{
			showAlarmNum = showNum[0];
		}
		String msg = alarmId + seprator + Contants.AGENT_HOST_UUID + seprator
				+ checkType + seprator + procIden + seprator + start.getTime()
				+ seprator + end.getTime() + seprator + alarmLevel + seprator
				+ state + seprator + alarmMsg+ seprator + showAlarmNum;
		
		Common.service.submit(new SSLClient(Thread.currentThread()
				.getName(), CommonSocket.REQ_ALARM, msg));
		
		logger.warn("Active alarm level" + alarmLevel + " >> setId: " + alarmId
				+ "，" + checkType + "_" + procIden + "，state: " + state + " > "
				+ alarmMsg+ "，showNum: " +showAlarmNum);
	}
	
	public static AlarmPO getAlarmPO(SetInfo setInfo){
		AlarmPO alarmPO = new AlarmPO(setInfo.getId(), setInfo
				.getCheckTypeName(), setInfo.getProcessIden(), setInfo
				.getCheckGap(), DetecConfOper.isProcess(setInfo), DetecConfOper
				.getProcess(setInfo), setInfo.getProcessFile(), setInfo
				.getControlStartTime(), setInfo.getProcessSearchKeyCode(),
				setInfo.getIsControlStart());

		return alarmPO;
	}
	
	// 向DC发送NC端的异常信息
	public static void sendNMSErrorMsg(ErrorCode errorCode, String errorIp, String errorDesc) {
		String seprator = Contants.COMMON_MSG_SEPRATOR;
		try {
			//异常信息顺序：CODE、发送IP、发生错误IP、发生错误时间、处理状态(1)、错误描述
			String msg = errorCode.toString() + seprator + Utils.getLocalIp() + seprator + errorIp
					+ seprator + System.currentTimeMillis() + seprator + "1"
					+ seprator + errorDesc;
			
			Future<?> future = Common.service.submit(new SSLClient(Thread.currentThread()
					.getName(), CommonSocket.REQ_ERROR_INFO, msg));
			
			if(errorCode.equals(ErrorCode.ProtListenerError)){
				future.get();
			}
			
			logger.error("Abnormal information " + errorCode + " >> errorIp: " + errorIp + " > "
					+ errorDesc);
		} catch (Exception e) {
			logger.error(e);
		}
		
	}
}
