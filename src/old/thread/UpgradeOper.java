package old.thread;

import org.apache.log4j.Logger;

public class UpgradeOper {
	static Logger logger = Logger.getLogger(UpgradeOper.class);
	
	public static boolean checkAgentVersionAndUpdate(){
		boolean flag = true;
		/*String curVer = VersionCfg.getValue();
		SSLClient sc = null;
		try {
			logger.info("检查最新版本……");

			sc = new SSLClient();
			sc.sendMessageByChar("char:agentVersion");
			String str = sc.receiveMessageByChar();//system type
			sc.sendMessageByChar(System.getProperty("os.name"));
			str = sc.receiveMessageByChar();//version num
			
			long newVersion = Long.parseLong(str);
			if (newVersion > Long.parseLong(curVer)) {
				logger.info("正在下载最新版本" + str);
				sc.sendMessageByChar("download");
				str = sc.receiveMessageByChar();// 文件相关属性
				sc.sendMessageByChar(CommonSocket.SUCCESS);
				JSONObject jsonObj = JSONObject.fromObject(str);
				
				Object obj = JSONObject.toBean(jsonObj, Task6.class);
				Task6 task = (Task6) obj;
				// 接收升级文件
				String error = new TaskReqHandle().filePush(sc, task.getCommandParam(),
						task.getTaskId(), true);
				flag = (error!=null && error.length()>0) ? true : false;
				if (flag) {
					sc.sendMessageByChar(AgentCommand.RESULT_SEND_OK
							+ TaskResultOper.TASK_RESULT_MSG_SEPRATOR + "下发成功");
					CommandPO command = new CommandPO();
					command.setExecId(task.getTaskId());
					command.setExecType(task.getTaskType());
					command.setCommandName(task.getCommandName());
					command.setCommandParam(task.getCommandParam());
					command.setExecState(task.getState());
					command.setExecVersion(task.getVersion());
					command.setSrcPath(TaskReqHandle.getUpgradeTaskPushPath(task.getTaskId()));
					flag = new AgentCommand(command).execAndSendResult(
							AgentCommand.UpgradeSteps.upgrade_agent.toString(),
							task.getCommandParam());// 相应的升级操作及各步骤结果状态的发送
				} else {
					sc.sendMessageByChar(AgentCommand.RESULT_FAIL
							+ TaskResultOper.TASK_RESULT_MSG_SEPRATOR
							+ "下发失败,详细信息如下: " + error);
				}
				sc.receiveMessageByChar();
			} else {
				sc.sendMessageByChar(CommonSocket.SUCCESS);
				logger.info("当前”" + curVer + "“是最新版本, 不用更新");
			}
		} catch (Exception e) {
			logger.error("检查更新最新版本失败" );
			logger.error(Utils.printExceptionStack(e));
			flag = false;
		} finally {
			if (sc != null)
				sc.close();
		}*/
		
		return flag;
	}
	
}
