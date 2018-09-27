package com.nis.nmsclient.thread.plugin;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.nis.nmsclient.common.Common;
import com.nis.nmsclient.common.Contants;
import com.nis.nmsclient.config.DetecConfOper;
import com.nis.nmsclient.model.SetInfo;
import com.nis.nmsclient.thread.alarm.AlarmUtil;
import com.nis.nmsclient.util.ProcessUtil;
import com.nis.systeminfo.thread.NewPluginResultMerge;

public class StartPluginRun implements Runnable {
	static Logger logger = Logger.getLogger(StartPluginRun.class);
	private String name;
	private SetInfo setInfo;

	public StartPluginRun(SetInfo setInfo, String name) {
		this.name = name;
		this.setInfo = setInfo;
	}

	public void run() {
		Thread.currentThread().setName(name);
		
		logger.info("启动第三方插件程序开始！");
		if ("1".equals(setInfo.getCheckState())
				&& "1".equals(setInfo.getCheckWay())
				&& "1,2".contains(setInfo.getIsControlStart())) {// 有效,被动方式获取,Agent端启动
			Date startTime = null;
			if (setInfo.getControlStartTime() != null && setInfo.getControlStartTime().longValue() != 0) {
				startTime = new Date(setInfo.getControlStartTime());
			} else {
				startTime = new Date();
			}
			
			/*
			 *  停止未启动的定时任务/周期任务
			 */
			Common.stopPluginDetecFuture(setInfo.getId(), Thread.currentThread().getName());

			/*
			 * 首先看进程设置的启动时间是否过时：如果过时，则查看进程是否存在，不存在启动；如果未过时，则定时启动
			 */
			String cmd = DetecConfOper.getProcess(setInfo);
			try {
				/**
				 * 进程存在与否的判断标准：
				 *    1、先检查PID文件，取出PID，验证PID指定进程是否存在
				 *       PID文件不存在，或者PID文件中内容为空，或者取出的PID进程不存在，都认为不存在，进行下一步检查
				 *    2、不存在再使用搜索关键字查找进程
				 *       若未找出进程，不存在，若找出一个进程，正常存在，若找出多个进程，报警
				 */

				Object[] objArr = ProcessUtil.checkPidAndGetPid(setInfo.getProcessFile(), setInfo.getProcessSearchKeyCode());
				int isExistFlag = Integer.parseInt(objArr[0].toString());
				String pidInfo = objArr[1].toString();

				if (isExistFlag == 0 || isExistFlag == 1) { // 进程不存在/存在一个进程
					if (isExistFlag == 1) {// 存在一个进程
						logger.info("三方监测进程”" + cmd + "“已存在，重启启动");
						ProcessUtil.killProcess(pidInfo);
					}

					long delay = startTime.getTime() - System.currentTimeMillis();
					delay = Math.max(delay, 0);

					ScheduledFuture<?> future = null;
					if ("1".equals(setInfo.getIsControlStart())) {
						future = Common.scheduled.schedule(new ExecProcess(), delay, TimeUnit.MILLISECONDS);
					} else if ("2".equals(setInfo.getIsControlStart())) {
						long period = setInfo.getCheckGap() * 60 * 1000;
						future = Common.scheduled.scheduleAtFixedRate(new ExecProcess2(), delay, period, TimeUnit.MILLISECONDS);
					}

					Common.putPluginDetecFuture(setInfo.getId(), future);
					logger.info("三方监测程序已加入定时器定时启动");

				} else { // 找到多个进程，告警
//					String alarmMsg = "启动三方监测：" + pidInfo;
					String alarmMsg = "i18n_client.StartPluginRun.startupDetecate_n81i：" + pidInfo;
					AlarmUtil.sendAlarmMsg(setInfo.getId(), setInfo
							.getCheckTypeName(), setInfo.getProcessIden(),
							new Date(), new Date(), 1,
							Contants.DETECTION_STATUS_FAILURE, alarmMsg);
				}

			} catch (Exception e) {
				logger.error("Start the three party monitoring exception. Please check whether the process execution file and the PID file are set correctly：" + cmd, e);
			}
		}
		logger.info("启动第三方插件程序结束！");
	}// run end

	class ExecProcess implements Runnable {
		public void run() {
			Thread.currentThread().setName(name);
			String cmd = DetecConfOper.getProcess(setInfo);
			try {
				// 启动
				ProcessUtil.runExec(cmd, null, null, null);

				// 检查PID
				Object[] objArr = ProcessUtil.checkPidAndGetPid(setInfo.getProcessFile(), setInfo.getProcessSearchKeyCode());
				int isExistFlag = Integer.parseInt(objArr[0].toString());
				String pidInfo = objArr[1].toString();

				if (isExistFlag == 1) {// 存在, 一个进程
					logger.info("三方监测程序“" + cmd + "“启动成功");
				} else { // 进程不存在 或 找到多个进程，告警
					String alarmMsg = null;
					if (isExistFlag == 0) {// 进程不存在
//						alarmMsg = "三方监测程序启动失败，请检查进程启动文件“" + cmd + "”是否设置正确";
						alarmMsg = "i18n_client.StartPluginRun.startupDetecateErr1_n81i “" + cmd + "” i18n_client.StartPluginRun.startupDetecateErr1.isTrue_n81i";
					} else {// 找到多个进程
//						alarmMsg = "启动三方监测：" + pidInfo;
						alarmMsg = "i18n_client.StartPluginRun.startupDetecate_n81i：" + pidInfo;
					}
					AlarmUtil.sendAlarmMsg(setInfo.getId(), setInfo
							.getCheckTypeName(), setInfo.getProcessIden(),
							new Date(), new Date(), 1,
							Contants.DETECTION_STATUS_FAILURE, alarmMsg);
				}

			} catch (Exception e) {
				logger.error("Start the three party monitoring exception. Please check whether the process execution file and the PID file are set correctly：" + cmd, e);
			}
		}
	}

	class ExecProcess2 implements Runnable {
		public void run() {
			
			Thread.currentThread().setName(name);
			// 三方监测脚本执行命令不存在，获取脚本命令及关键词
			// 因脚本下发与配置下发不同步，需要在执行任务中多次获取执行命令
			String cmd = Common.generateCommandAndKeyword(setInfo);
			try {
				boolean executeStatus = true;
	
				// 脚本执行前，清理临时数据目录
				new NewPluginResultMerge().clearTmpFile(setInfo);
			
				if (StringUtils.isBlank(cmd)) {
					// 三方监测脚本执行命令不存在，当前时间NC上无监测脚本
					logger.info("三方监测程序启动失败，监测脚本下发未完成！");
//					String alarmMsg = "启动三方监测异常,监测脚本下发未完成！";
					String alarmMsg = "i18n_client.StartPluginRun.startupDetecateErr2_n81i";
					AlarmUtil.sendAlarmMsg(setInfo.getId(),
							setInfo.getCheckTypeName(),
							setInfo.getProcessIden(), new Date(), new Date(),
							1, Contants.DETECTION_STATUS_FAILURE, alarmMsg);
					executeStatus = false;

				} else {
					// 启动脚本
					String msg = ProcessUtil.runExec(cmd, null, null, null);

					if (StringUtils.isBlank(msg)) { // 执行成功
						logger.info("三方监测程序“" + cmd + "”执行成功");

					} else { // 执行失败，返回错误信息
						logger.error("Three party monitoring procedure“" + cmd + "”erroneous execution：" + msg);
//						String alarmMsg = "启动三方监测异常,监测脚本执行错误！";
						String alarmMsg = "i18n_client.StartPluginRun.startupDetecateErr3_n81i";
						AlarmUtil.sendAlarmMsg(setInfo.getId(),
								setInfo.getCheckTypeName(),
								setInfo.getProcessIden(), new Date(),
								new Date(), 1,
								Contants.DETECTION_STATUS_FAILURE, alarmMsg);
						executeStatus = false;
					}
				}
				
				// 针对执行合并临时文件（ProcessUtil.runExec为同步执行，合并临时文件前已完成脚本执行过程）
				if(executeStatus) {
					new NewPluginResultMerge().merge(setInfo);
				}
				
			} catch (Exception e) {
				logger.error("Start the three party monitoring exception. Please check whether the process execution file and the PID file are set correctly：" + cmd, e);
			}
		}
	}

}
