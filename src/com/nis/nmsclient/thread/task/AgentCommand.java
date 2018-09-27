 package com.nis.nmsclient.thread.task;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.nis.nmsclient.common.Common;
import com.nis.nmsclient.common.Contants;
import com.nis.nmsclient.common.VersionCfg;
import com.nis.nmsclient.model.CommandPO;
import com.nis.nmsclient.model.ParamBackup;
import com.nis.nmsclient.model.ParamCmdExec;
import com.nis.nmsclient.model.ParamCoverUpdate;
import com.nis.nmsclient.model.ParamUpgrade;
import com.nis.nmsclient.model.ReturnFilePO;
import com.nis.nmsclient.thread.socket.CommonSocket;
import com.nis.nmsclient.thread.socket.SSLClient;
import com.nis.nmsclient.util.CompressFileMgr;
import com.nis.nmsclient.util.FileUtil;
import com.nis.nmsclient.util.FileWrUtil;
import com.nis.nmsclient.util.ProcessUtil;
import com.nis.nmsclient.util.Utils;

public class AgentCommand {
	static Logger logger = Logger.getLogger(AgentCommand.class);
	public static final int RESULT_OK = 0;
	public static final int RESULT_FAIL = 1;
	public static final int RESULT_SEND_OK = 40;
	public static final int RESULT_SEND_FAIL = 41;
	public static final int RESULT_KILL_OK = 50;
	public static final int RESULT_KILL_FAIL = 51;
	public static final int RESULT_BACKUP_OK = 60;
	public static final int RESULT_BACKUP_FAIL = 61;
	public static final int RESULT_UPDATE_OK = 70;
	public static final int RESULT_UPDATE_FAIL = 71;
	public static final int RESULT_START_OK = 80;
	public static final int RESULT_START_FAIL = 81;
	public static final int RESULT_RECOVER_OK = 90;
	public static final int RESULT_RECOVER_FAIL = 91;
	public static final int MISSION_CANCEL_CREATE = 5;//任务准备撤消
	public static final int MISSION_CANCEL_START = 6;//任务撤消中
	public static final int MISSION_CANCEL_FINISH = 7;//任务撤消完成
	
	private static final String RESULT_SEPRATOR = Contants.COMMON_MSG_SEPRATOR;
	private static final String RESULT_SEPRATOR_SPLIT = Contants.COMMON_MSG_SEPRATOR_SPLIT;
	
	public static enum UpgradeSteps {
		upgrade_agent, upgrade_other,
		upgrade_server,	upgrade_kill_process,
		upgrade_backup, upgrade_update,
		upgrade_start, upgrade_recover,
		upgrade_exec, single_exec;
	}
	public static final String PARAM_SEPRATOR = "@&##&@";
	
	// NC/DC升级锁，确保NC与DC不同时升级
	private static final Byte[] upgradeLock = new Byte[0];
	
	private CommandPO command;
	private Thread thread;
	
	public AgentCommand(CommandPO command) {
		super();
		this.command = command;
	}
	
	public boolean exec(){
		Date execTime = new Date();
//		String resultDesc = "执行失败";
//		String resultDesc = "Failure to execute";
		String resultDesc = "i18n_client.AgentCommand.execFail_n81i";
		long resultState = RESULT_FAIL;
		boolean isServer = false;
		try {
			String commandParam = command.getCommandParam();
			if(commandParam==null || "".equals(commandParam)){
//				resultDesc = "执行失败：命令参数为空";
//				resultDesc = "Execution failure: command parameters are empty";
				resultDesc = "i18n_client.AgentCommand.execFailDesc1_n81i";
				resultState = RESULT_FAIL;
//				logger.warn(resultDesc);//i18nlog
			} else {
				logger.debug("commandParam 1--" + commandParam);
				//2012-4-28 任务参数中对路径的格式化将在界面上进行，原因此处会对转义字符的\也转换为/，故replace("\\", "/")去掉
				commandParam = commandParam.trim().replaceAll("[\n\t\r]","");//.replace("\\", "/");//[\\s*\n\t\r]
				logger.debug("commandParam 2--" + commandParam);
				String[] params = commandParam.trim().split(PARAM_SEPRATOR);
				if (params != null && params.length >= 1) {
					for(int i=0; i<params.length; i++){
						params[i] = params[i].trim();
					}
					UpgradeSteps step = Enum.valueOf(UpgradeSteps.class,command.getCommandName());
					String resultStr = "";
					switch (step) {
					case upgrade_server:// 命令参数，以;分隔的一个CommandForUpgrade对象的Json字符串
						isServer = true;
						synchronized (upgradeLock) {//DC升级时锁，确保不与NC同时升级
							resultStr = upgradeOther(params, command
									.getSrcPath(), command.getExecState(), command
									.getExecId(), command.getExecVersion(),
									isServer);
						}
						break;
					case upgrade_other:// 命令参数，以；分隔的多个CommandForUpgrade对象的Json字符串
						resultStr = upgradeOther(params, command
								.getSrcPath(), command.getExecState(), command
								.getExecId(), command.getExecVersion(),
								isServer);
						break;
					case upgrade_agent:// 命令参数，以;分隔的一个CommandForUpgrade对象的Json字符串
						//-- NC升级开始 关闭通讯
						Common.NC_UPGRADE_FLAG = true;
						synchronized (upgradeLock) {//NC升级时锁，确保不与DC同时升级
							resultStr = upgradeAgent(params[0], command
									.getSrcPath(), command.getExecState(), command
									.getExecId(), command.getExecVersion());
						}
						//-- NC升级完成 打开通讯
						Common.NC_UPGRADE_FLAG = false;
						break;
					case upgrade_kill_process:// 命令参数, 以;分隔的多个存放进程ID的文件
						resultStr = killProcessCmd(params);
						break;
					case upgrade_backup:// 命令参数，以;分隔的多个Backup对象的Json字符串
						ParamBackup[] backups = new ParamBackup[params.length];
						for (int i = 0; i < params.length; i++) {
							logger.debug("backupCmd-->param: " + params[i]);
							ParamBackup backup = (ParamBackup) JSONObject
									.toBean(JSONObject.fromObject(params[i]),
											ParamBackup.class);
							backups[i] = backup;
						}
						resultStr = backupCmd(backups, null, command
								.getExecId());
						break;
					case upgrade_update:// 命令参数，以;分隔多个CoverUpdate对象的Json字符串
					case upgrade_recover:// 命令参数，以;分隔多个CoverUpdate对象的Json字符串
						ParamCoverUpdate[] updates = new ParamCoverUpdate[params.length];
						for (int i = 0; i < params.length; i++) {
							logger.debug("updateCmd-->param: " + params[i]);
							ParamCoverUpdate update = (ParamCoverUpdate) JSONObject
									.toBean(JSONObject.fromObject(params[i]),
											ParamCoverUpdate.class);
							updates[i] = update;
						}
						resultStr = updateCmd(updates);
						break;
					case upgrade_start:// 命令参数，以;分隔多个CmdExec对象的Json字符串
					case upgrade_exec:// 可执行命令
						List<ParamCmdExec> execList = new ArrayList<ParamCmdExec>();
						for (String param : params) {
							logger.debug("startCmd-->param: " + param);
							ParamCmdExec exec = (ParamCmdExec) JSONObject
									.toBean(JSONObject.fromObject(param),
											ParamCmdExec.class);
							execList.add(exec);
						}
						resultStr = startCmd(execList, execTime, false, command.getExecState());
						break;
					case single_exec:// 单次执行命令，命令参数，以;分隔多个CmdExec对象的Json字符串
						List<ParamCmdExec> cmdExecList = new ArrayList<ParamCmdExec>();
						for (String param : params) {
							logger.debug("singleExecCmd-->param: " + param);
							ParamCmdExec exec = (ParamCmdExec) JSONObject
									.toBean(JSONObject.fromObject(param),
											ParamCmdExec.class);
							cmdExecList.add(exec);
						}
						resultStr = singleExecCmd(cmdExecList, execTime, command.getExecState());
						break;

					default:
//						resultDesc = "执行失败，不存在该命令";
//						resultDesc = "The execution failed and the command did not exist";
						resultDesc = "i18n_client.AgentCommand.execFailDesc2_n81i";
						resultState = RESULT_FAIL;
//						logger.warn(resultDesc);//i18nlog
						break;
					}

					String[] res = resultStr.split(RESULT_SEPRATOR_SPLIT);
					if (res != null && res.length >= 2) {
						resultState = Long.parseLong(res[0]);
						resultDesc = res[1];
					}
				} else {
//					resultDesc = "执行失败：命令参数不正确<" + commandParam + ">";
//					resultDesc = "Execution failure: incorrect command parameters<" + commandParam + ">";
					resultDesc = "i18n_client.AgentCommand.execFailDesc3_n81i<" + commandParam + ">";
					resultState = RESULT_FAIL;
//					logger.warn(resultDesc);//i18nlog
				}

			}
		} catch (Exception e) {
//			resultDesc = "执行失败：出现异常，详细信息请查看日志";
//			resultDesc = "Execution failure: abnormal. Please check log for details";
			resultDesc = "i18n_client.AgentCommand.execFailDesc4_n81i";
			resultState = RESULT_FAIL;
//			logger.error(resultDesc + "\n" + Utils.printExceptionStack(e));//i18nlog
		}
		
		// 发送信息
		TaskResultOper.sendTaskResult(command.getExecId(), command.getExecType(),
				resultState, resultDesc, "", execTime, new Date(), isServer, command.getIsLoop());
		
		return (resultState % 2 == 1) ? false : true;
	}
	
	/**
	 * Agent原生支持命令，Agent自身升级
	 * @param params 升级任务的参数
	 * @param upgradePath 升级文件存放路径
	 * @param stepState 任务执行步骤标志
	 * @param curVersion 当前升级版本
	 * @param reVersion 恢复到的版本
	 * @return
	 */
	public String upgradeAgent(String param, String upgradePath, long stepState, Long curVersion, Long reVersion) {
		logger.info("NC升级开始……");
//		String resultDesc = "执行失败";
//		String resultDesc = "Failure to execute";
		String resultDesc = "i18n_client.AgentCommand.execFail_n81i";
		int resultState = RESULT_FAIL;
		File tempUpdateDir = null;
		try {
			Long curVer = Long.parseLong(VersionCfg
					.getValue(VersionCfg.NAGENT_VERSION));
			if (curVer >= curVersion) {
//				resultDesc = "执行失败:当前Agent版本为最新";
//				resultDesc = "Execution failure: the current Agent version is the latest";
				resultDesc = "i18n_client.AgentCommand.execFailDesc5_n81i";

				logger.info("NC升级结果描述信息 >> " + resultDesc);
				logger.info("NC升级结束……");
				return resultState + RESULT_SEPRATOR + resultDesc;
			}

			/**
			 * 1、解析命令参数，包装到对象
			 */
			logger.debug("upgradeAgent-->param: " + param);
			ParamUpgrade cfu = (ParamUpgrade) JSONObject.toBean(
					JSONObject.fromObject(param), ParamUpgrade.class);
			//Agent的布署路径
			String homePath = new File(Contants.SYSTEM_PATH).getParent();
			/*
			 * 设置启动文件和PID文件
			 */
			String pidFile = Contants.localAgentPidFile;
			String startFile = null;
			String os = System.getProperty("os.name");
			if (os.startsWith("Windows")) {
				startFile = homePath + File.separator + "script" + File.separator + "restart.bat";
				if(!new File(startFile).exists()){
					startFile = Contants.SYSTEM_PATH + File.separator + "restart.bat";
				}
			} else if (os.startsWith("Linux")) {
				startFile = Contants.SYSTEM_PATH + File.separator + "restart.sh";
			}
			cfu.setStartupFile(new File(startFile).getCanonicalPath());
			cfu.setPidFile(new File(pidFile).getCanonicalPath());
			/*
			 * 处理覆盖目录
			 */
			if(cfu.getCover()!=null && !"".equals(cfu.getCover().trim())){
				cfu.setCover(getFilePath(cfu.getCover(), homePath));
			}else{//如果没有参数覆盖目录，则按默认Agent的布署路径
				cfu.setCover(homePath);
			}
			String backupOppDir = new File(cfu.getCover()).getCanonicalPath();
			
			logger.debug("AgentUpgrade homePath----" + homePath);
			logger.debug("AgentUpgrade restart file----" + cfu.getStartupFile());
			logger.debug("AgentUpgrade pid file----" + cfu.getPidFile());
			logger.debug("AgentUpgrade cover----" + cfu.getCover());
			
			/**
			 *  步骤2：判断是逆向任务还是正常升级，恢复版本不为空则为逆向任务，反之正常升级
			 */
			boolean next = true;
			String srcDesc = null;
			String destDesc = null;
			File upgradeFile = null;
			File coverDirFile = null;
			if (command.getExecVersion() != null
					&& command.getExecVersion().longValue() > 0) {//恢复
				if(cfu.getRecoverys()==null || cfu.getRecoverys().length<=0){
//					resultDesc = "执行失败:恢复参数为空";
//					resultDesc = "Execution failure: recovery parameters are empty";
					resultDesc = "i18n_client.AgentCommand.execFailDesc6_n81i";
//					logger.info("NC升级结果描述信息 >> " + resultDesc);//i18nlog
					logger.info("NC升级结束……");
					return resultState + RESULT_SEPRATOR + resultDesc;
				}
				ParamCoverUpdate recover = cfu.getRecoverys()[0];
				cfu.setIsAbs(recover.getIsAbs());
				//文件名称在此没有实际意义，只是为了下面验证时不为空
				cfu.setFileName(getRecoverSource(recover, reVersion));
				//-------必须先处理恢复目录，再处理恢复源文件，恢复源文件会用到恢复目录的名称，否则恢复目录如果是（.）这样的相对路径就会出问题
				//恢复目录若是相对，则相对升级的覆盖目录，若是绝对，则不变
				recover.setCover(getFilePath(recover.getCover(), cfu.getCover()));
//				destDesc = "恢复目录";
//				destDesc = "Recovery Directory";
				destDesc = "i18n_client.AgentCommand.destDesc_n81i";
				coverDirFile = new File(recover.getCover());
//				srcDesc = "恢复源文件";
//				srcDesc = "Recovery Source File";
				srcDesc = "i18n_client.AgentCommand.srcDesc_n81i";
				upgradeFile = new File(getRecoverSource(recover, reVersion));
				
				logger.debug("AgentUpgrade recover cover----" + recover.getCover());
			} else {
				// 处理是否创建覆盖目录
				cfu.setIsCreateCover(handleYesOrNoField(cfu.getIsCreateCover()));
//				srcDesc = "升级文件";
//				srcDesc = "Upgrade File";
				srcDesc = "i18n_client.AgentCommand.srcDesc_n81i";
				upgradeFile = new File(getFilePath(cfu.getFileName(), upgradePath));
//				destDesc = "更新目录";
//				destDesc = "Update Table Of Contents";
				destDesc = "i18n_client.AgentCommand.destDesc_n81i";
				coverDirFile = new File(cfu.getCover());
			}
			if(coverDirFile.isFile()){
				coverDirFile = coverDirFile.getParentFile();
			}
			// 处理解压时按绝对路径，还是相对路径, 只对LInux起作用, 默认按相对
			boolean isAbs = false;
			if ("Y".equals(handleYesOrNoField(cfu.getIsAbs()))) {
				isAbs = true;
			}
			/**
			 * 判断参数指定的升级文件与覆盖目录是否存在
			 */
			if (cfu.getFileName() == null
					|| cfu.getFileName().trim().length() <= 0) {
//				resultDesc = "执行失败：" + srcDesc + "为空";
//				resultDesc = "Failure to execute：" + srcDesc + "is empty";
				resultDesc = "i18n_client.AgentCommand.execFail_n81i：" + srcDesc + "i18n_client.AgentCommand.execFailDesc7_n81i";
				resultState = RESULT_FAIL;
//				logger.debug(resultDesc);//i18nlog
				next = false;
			} else if (!upgradeFile.exists()) {
//				resultDesc = "执行失败：" + srcDesc + "不存在--" + upgradeFile.getAbsolutePath();
//				resultDesc = "Failure to execute：" + srcDesc + "non-existent--" + upgradeFile.getAbsolutePath();
				resultDesc = "i18n_client.AgentCommand.execFail_n81i：" + srcDesc + "i18n_client.AgentCommand.execFailDesc8_n81i--" + upgradeFile.getAbsolutePath();
				resultState = RESULT_FAIL;
//				logger.debug(resultDesc + "--" + upgradeFile.getAbsolutePath());//i18nlog
				next = false;
			}
			// 判断用户和密码是否正确
			/*if (!ProcessUtil.checkUserPass(cfu.getUsername(), cfu.getParam1())) {
				resultDesc = "执行失败：用户名或密码不正确;";
				resultState = RESULT_FAIL;
				logger.debug(resultDesc);
				next = false;
			}*/
			// 判断用户名是否正确
			if(!ProcessUtil.checkUserOrGroupExist(cfu.getUsername(), null)){
//				resultDesc = "执行失败：属主不正确;";
//				resultDesc = "Failure of execution: the owner is not correct;";
				resultDesc = "i18n_client.AgentCommand.execFailDesc9_n81i;";
				resultState = RESULT_FAIL;
//				logger.debug(resultDesc);//i18nlog
				next = false;
			}
			boolean isCompressFile = false;
			if (next) {
				/**
				 * 判断覆盖目录是否存在：
				 * 1、必须判断：Window系统、或者Linux系统且不是压缩文件、或者Linux系统且是压缩文件且是相对路径
				 * 2、不用判断：Linux系统下源文件是压缩文件且绝对解压，该属性无效
				 */
				isCompressFile = CompressFileMgr.isCompressFile(upgradeFile);
				if (os.startsWith("Windows")
						|| (!os.startsWith("Windows") && !isCompressFile)
						|| (!os.startsWith("Windows") && isCompressFile && !isAbs)) {
					if (!coverDirFile.exists()
							&& "N".equalsIgnoreCase(cfu.getIsCreateCover())) {
//						resultDesc = "执行失败：" + destDesc + "不存在";
//						resultDesc = "Failure to execute：" + destDesc + "non-existent";
						resultDesc = "i18n_client.AgentCommand.execFail_n81i：" + destDesc + "i18n_client.AgentCommand.execFailDesc8_n81i";
						resultState = RESULT_FAIL;
//						logger.debug(resultDesc);//i18nlog
						next = false;
					} else if (!coverDirFile.exists()
							&& "Y".equalsIgnoreCase(cfu.getIsCreateCover())) {// Y
																				// 创建目录
						coverDirFile.mkdirs();
					}
				} else {
					coverDirFile = new File("/");
				}
			}
			/**
			 * 步骤3、对参数指定的备份目录进行备份
			 */
			boolean flag = false;
			String resultStr = "";
			if (next && (stepState > RESULT_OK && stepState < RESULT_BACKUP_OK)) {// 备份
				Date execTime = new Date();
				resultStr = this.backupCmd(cfu.getBackups(), backupOppDir, curVersion);
				String[] res = resultStr.split(RESULT_SEPRATOR_SPLIT);
				if (res != null && res.length >= 2) {
					resultState = Integer.parseInt(res[0]);
					resultDesc = res[1];
				}
				if (resultState == RESULT_OK) {// 备份成功，发送信息
					flag = true;
					resultState = RESULT_BACKUP_OK;
					TaskResultOper.sendTaskResult(command.getExecId(), command
							.getExecType(), resultState, resultDesc, "",
							execTime, new Date(), false, command.getIsLoop());
				} else {
					flag = false;
				}

			}
			
			/**
			 * 步骤4、覆盖并重启Agent
			 */
			if (next && flag) {// 解压成功，覆盖并重启Agent
				Date execTime = new Date();
				
				/**
				 * 4-1 发送重启请求
				 */
				TaskResultOper.sendTaskResult(command.getExecId(), command
//						.getExecType(), resultState, "正在覆盖并重启Agent", "",
//						.getExecType(), resultState, "Overlaying and restarting Agent", "",
						.getExecType(), resultState, "i18n_client.AgentCommand.coverRestart_n81i", "",
						execTime, new Date(), false, command.getIsLoop());
				
				// 判断是否解压升级文件，并放入临时目录
				tempUpdateDir = new File(Contants.localTempPath + File.separator
						+ "agent_updatedir_" + command.getExecId());
				if (upgradeFile.isDirectory()) {
					FileUtils.copyDirectoryToDirectory(upgradeFile, tempUpdateDir);
				} else {
					if (isCompressFile) {
						flag = new CompressFileMgr().decompressFile(
								upgradeFile.getAbsolutePath(), tempUpdateDir
										.getAbsolutePath(), false);
					} else {
						FileUtils.copyFileToDirectory(upgradeFile,tempUpdateDir);
					}
				}
				
				/**
				 * 4-2 删除文件，并写临时结果文件
				 */
				//准备临时结果文件内容，初始状态为升级失败
				String[] msgs = new String[1];
				msgs[0] = TaskResultOper.getTaskResultMsg(command
						.getExecId(), command.getExecType(),
//						(long) RESULT_FAIL, "升级失败", "", execTime, new Date(), command.getIsLoop());
//						(long) RESULT_FAIL, "Failure to upgrade", "", execTime, new Date(), command.getIsLoop());
						(long) RESULT_FAIL, "i18n_client.AgentCommand.upgradeFail_n81i", "", execTime, new Date(), command.getIsLoop());
				File resultFile = new File(TaskResultOper
						.getAgentUpgradeResultTempFile(command.getExecType(),
								command.getExecId()));
				if(!resultFile.getParentFile().exists()){
					resultFile.getParentFile().mkdirs();
				}
				// 删除指定的文件
				String delMsg = this.delete(cfu.getDelete(), coverDirFile.getAbsolutePath());
				// 将删除日志换行加入结果文件
				if(!StringUtils.isEmpty(delMsg)){
					msgs = new String[]{msgs[0], delMsg};
				}
				// 正式写入临时结果文件
				FileWrUtil.cfgFilePrinter(resultFile, Charset.defaultCharset().name(), msgs);

				/**
				 * 4-3 执行脚本（脚本中将覆盖操作的详细信息追加到结果文件；覆盖成功后将任务标识ID写入version.txt）
				 */
				List<ParamCmdExec> startList = new ArrayList<ParamCmdExec>();
				ParamCmdExec exec = new ParamCmdExec();
				exec.setExecCmd(cfu.getStartupFile());
				exec.setExecParams(new String[] { tempUpdateDir.getCanonicalPath(),
						coverDirFile.getCanonicalPath(),
						resultFile.getCanonicalPath() });
				exec.setExecResult(cfu.getPidFile());
				exec.setUsername(cfu.getUsername());
				exec.setParam1(cfu.getParam1());
				exec.setForceExec("Y");
				exec.setResidentFlag("Y");
				exec.setReturnFlag("N");
				startList.add(exec);
				resultStr = this.startCmd(startList, execTime, true, resultState);
				String[] res = resultStr.split(RESULT_SEPRATOR_SPLIT);
				if (res != null && res.length >= 2) {
					resultState = Integer.parseInt(res[0]);
					resultDesc = res[1];
				}
				if (resultState == RESULT_FAIL && resultFile.exists()) {// 如果失败直接返回结果，删除临时结果文件
					//resultFile.delete_bak();
					//使用删除文件公共方法
					FileUtil.delDir(resultFile);
					logger.debug("upgradeAgent delete file -- " + resultFile.getAbsolutePath());
					//FileUtil.checkParentDirExist(resultFile);
				}
			} else {
				if (stepState == RESULT_BACKUP_OK) {
//					resultDesc = "重启失败，手工启动成功";
//					resultDesc = "Restart failure and start success by hand";
					resultDesc = "i18n_client.AgentCommand.restartFail_n81i";
					resultState = RESULT_FAIL;
					String upgradeResultFile = TaskResultOper
							.getAgentUpgradeResultTempFile(command
									.getExecType(), command.getExecId());
					File resultFile = new File(upgradeResultFile);
					if (resultFile.exists()) {// 如果失败直接返回结果，删除临时结果文件
						//resultFile.delete_bak();
						//使用删除文件公共方法
						FileUtil.delDir(resultFile);
						logger.debug("upgradeAgent delete file 2 -- " + resultFile.getAbsolutePath());
						//FileUtil.checkParentDirExist(resultFile);
					}
				}

			}
		} catch (Exception e) {
//			resultDesc = "执行失败：出现异常，" + e.getMessage() + "，详细信息请查看日志";
			resultDesc = "i18n_client.AgentCommand.execFailDesc10_n81i，" + e.getMessage() + "，i18n_client.AgentCommand.execFailDesc10.showDetail_n81i";
			resultState = RESULT_FAIL;
			logger.error("NC upgrade exception：" + Utils.printExceptionStack(e));
		} finally {
			// 不能删除临时目录原因：覆盖操作在脚本中进行，可能出现脚本未运行完，临时目录被删除，这样覆盖操作就不能完成
			/*if(tempUpdateDir!=null && tempUpdateDir.exists()){
				try {
					FileUtils.deleteDirectory(tempUpdateDir);
					logger.debug("upgradeAgentNew-----finally delele tempUpdateDir = " + tempUpdateDir.getCanonicalPath());
					FileUtil.checkParentDirExist(tempUpdateDir);
				} catch (IOException e) {
				}
			}*/
		}
//		logger.info("NC升级结果描述信息 >> " + resultDesc);//i18nlog
		logger.info("NC升级结束……");
		
		return resultState + RESULT_SEPRATOR + resultDesc;
	}	 
	
	/**
	 * Agent原生支持命令，第三方升级
	 * @param params 升级任务的参数
	 * @param upgradePath 升级文件存放目录
	 * @param stepState 任务执行步骤标志
	 * @param curVersion 当前升级版本
	 * @param reVersion 恢复到的版本，如果是逆向任务即恢复时用到
	 * @param isServer 是否NmsServer升级
	 * @return
	 */
	public String upgradeOther(String[] params, String upgradePath, long stepState, long curVersion, Long reVersion, boolean isServer) {
//		String pre = "三方升级";
		String pre = "Three Party Upgrade";
		if (isServer) {// 如果是NmsServer升级，判断版本
//			pre = "DC升级";
			pre = "DC Upgrade";
		}
		logger.info(pre + "开始……");
//		String resultDesc = "执行失败";
		String resultDesc = "Failure to execute";
		int resultState = RESULT_FAIL;
		try {
			if(params==null || params.length==0){
//				resultDesc = "执行失败:参数为空";
//				resultDesc = "Execution failure: parameters are empty";
				resultDesc = "i18n_client.AgentCommand.execFailDesc11_n81i";
//				logger.info(pre + "结果描述信息 >> " + resultDesc);//i18nlog
				logger.info(pre + "结束……");
				return resultState + RESULT_SEPRATOR + resultDesc;
			}
			if (isServer) {// 如果是NmsServer升级，判断版本
				Long curVer = Long.parseLong(VersionCfg
						.getValue(VersionCfg.NSERVER_VERSION));
				if (curVer >= curVersion) {
//					resultDesc = "执行失败:当前Server版本为最新";
//					resultDesc = "Execution failure: the current Server version is the latest";
					resultDesc = "i18n_client.AgentCommand.execFailDesc12_n81i";
//					logger.info(pre + "结果描述信息 >> " + resultDesc);//i18nlog
					logger.info(pre + "结束……");
					return resultState + RESULT_SEPRATOR + resultDesc;
				}
				
			}
			/**
			 * 1、解析命令参数，包装到对象, 判断是否为逆向任务：
			 *       不是，判断所有参数指定的升级文件与覆盖目录是否存在；
			 *       是，设置默认强制执行并跳过判断
			 */
			boolean next = true;
			List<ParamUpgrade> cfuList = new ArrayList<ParamUpgrade>();
			for (String param : params) {
				if (param.trim().length() <= 0) {
					continue;
				}
				logger.debug("upgradeOther-->param: " + param);
				ParamUpgrade cfu = (ParamUpgrade) JSONObject.toBean(JSONObject
						.fromObject(param), ParamUpgrade.class);
				logger.debug("upgradeOther-->pidFile: " + cfu.getPidFile());
				
				// 判断用户和密码是否正确
				/*if (!ProcessUtil.checkUserPass(cfu.getUsername(), cfu.getParam1())) {
					resultDesc = "执行失败：用户名或密码不正确;";
					resultState = RESULT_FAIL;
					logger.debug(resultDesc);
					next = false;
					break;
				}*/
				// 判断用户名是否正确
				if(!ProcessUtil.checkUserOrGroupExist(cfu.getUsername(), null)){
//					resultDesc = "执行失败：属主不正确;";
//					resultDesc = "Failure of execution: the owner is not correct;";
					resultDesc = "i18n_client.AgentCommand.execFailDesc13_n81i;";
					resultState = RESULT_FAIL;
//					logger.debug(resultDesc);//i18nlog
					next = false;
				}
				
				// 恢复版本不为空则为逆向任务
				if (reVersion != null && reVersion.longValue() > 0) {// 恢复
					cfuList.add(cfu);
					continue;
				}

				cfu.setIsCreateCover(handleYesOrNoField(cfu.getIsCreateCover()));
				// 处理解压时按绝对路径，还是相对路径, 只对LInux起作用, 默认按相对
				boolean isAbs = false;
				if ("Y".equals(handleYesOrNoField(cfu.getIsAbs()))) {
					isAbs = true;
				}
				/**
				 * 判断升级文件是否存在
				 */
				if (cfu.getFileName() == null
						|| cfu.getFileName().trim().length() <= 0) {
//					resultDesc = "执行失败：升级文件名为空";
//					resultDesc = "Execution failure: the upgrade file name is empty";
					resultDesc = "i18n_client.AgentCommand.execFailDesc14_n81i";
					resultState = RESULT_FAIL;
//					logger.debug(resultDesc);//i18nlog
					next = false;
					break;
				}
				File upgradeFile = new File(getFilePath(cfu.getFileName(),
						upgradePath));
				if (!upgradeFile.exists()) {
//					resultDesc = "执行失败：升级文件“" + cfu.getFileName() + "”不存在--"
//					resultDesc = "Execution failure: upgrade file“" + cfu.getFileName() + "”non-existent--"
					resultDesc = "i18n_client.AgentCommand.execFailDesc15_n81i“" + cfu.getFileName() + "”i18n_client.AgentCommand.execFailDesc8_n81i--"
							+ upgradeFile.getAbsolutePath();
					resultState = RESULT_FAIL;
//					logger.debug(resultDesc);//i18nlog
					next = false;
					break;
				}
				/**
				 * 判断覆盖目录是否存在：
				 * 1、必须判断：Window系统、或者Linux系统且不是压缩文件、或者Linux系统且是压缩文件且是相对路径
				 * 2、不用判断：Linux系统下源文件是压缩文件且绝对解压，该属性无效
				 */
				File coverDirFile = null;
				boolean isCompressFile = CompressFileMgr.isCompressFile(upgradeFile);
				String os = System.getProperty("os.name");
				if (os.startsWith("Windows")
						|| (!os.startsWith("Windows") && !isCompressFile)
						|| (!os.startsWith("Windows") && isCompressFile && !isAbs)) {
					if (cfu.getCover() == null
							|| cfu.getCover().trim().length() <= 0) {
//						resultDesc = "执行失败：覆盖目录为空";
//						resultDesc = "Execution failure: overlay the directory to be empty";
						resultDesc = "i18n_client.AgentCommand.execFailDesc16_n81i";
						resultState = RESULT_FAIL;
//						logger.debug(resultDesc);//i18nlog
						next = false;
						break;
					}
					coverDirFile = new File(cfu.getCover());
					if (!coverDirFile.exists()
							&& "N".equalsIgnoreCase(cfu.getIsCreateCover())) {
//						resultDesc = "执行失败：覆盖目录不存在--"
//						resultDesc = "Execution failure: the overlay directory does not exist--"
						resultDesc = "i18n_client.AgentCommand.execFailDesc17_n81i--"
								+ coverDirFile.getAbsolutePath();
						resultState = RESULT_FAIL;
//						logger.debug(resultDesc);//i18nlog
						next = false;
						break;
					}
					// Y 创建目录
					if (!coverDirFile.exists()) {
						coverDirFile.mkdirs();
					}
				} else {
					coverDirFile = new File("/");
				}

				cfuList.add(cfu);
			}
			/**
			 * 步骤2、停用所有进程，所有对象同步进行，有一个失败则失败
			 */
			String resultStr = "";
			boolean flag = false;
			if (next && (stepState > RESULT_OK  && stepState < RESULT_KILL_OK)) {
				String temp = "";
				if (isServer) {// 如果是NmsServer升级，发送升级请求
					for (int i = 0; i < Contants.max_times; i++) {// 失败，尝试几次
						Future<?> future = Common.service
								.submit(new SSLClient(Thread.currentThread().getName() + " >> DC升级请求",
										CommonSocket.REQ_SERVER_UPGRADE,
										null, Utils.getLocalIp()));
						String msg = (String) future.get();
						temp = Contants.getDescByResult(msg);
						if(Contants.isSucessByResult(msg)){
							break;
						}
						try {// 如果更新失败，让当前线程暂停几秒，再重试
							Thread.sleep(1000 * Contants.max_delay_seconds);
						} catch (InterruptedException e) {
							logger.error(Utils.printExceptionStack(e));
							continue;
						}
					}
//					temp = "DC升级请求" + ((temp!=null && temp.trim().length()>0) ? temp : "失败") + ";";
//					temp = "DC upgrade request" + ((temp!=null && temp.trim().length()>0) ? temp : "失败") + ";";
					temp = "i18n_client.AgentCommand.DCupdate_n81i" + ((temp!=null && temp.trim().length()>0) ? temp : "i18n_client.AgentCommand.fail_n81i") + ";";
				}
				
				Date execTime = new Date();
				String[] pidArr = new String[cfuList.size()];
				int i = 0;
				for(ParamUpgrade cfu:cfuList){
					pidArr[i++] = cfu.getPidFile();
				}
				resultStr = this.killProcessCmd(pidArr);
				String[] res = resultStr.split(RESULT_SEPRATOR_SPLIT);
				if (res != null && res.length >= 2) {
					resultState = Integer.parseInt(res[0]);
					resultDesc = temp + res[1];
				}
				if(resultState == RESULT_OK){
					flag = true;
					resultState = RESULT_KILL_OK;
					// 发送信息
					TaskResultOper.sendTaskResult(command.getExecId(), command.getExecType(),
								resultState, resultDesc, "", execTime, new Date(), isServer, command.getIsLoop());
				}else{
					flag = false;
				}
			}
			/**
			 * 步骤3、对所有参数指定的备份目录进行备份，有一个失败则失败
			 */
			if (next && (flag || stepState == RESULT_KILL_OK)) {// 停用进程成功，再进行备份
				Date execTime = new Date();
				StringBuffer sb = new StringBuffer();
				for(ParamUpgrade cfu:cfuList){
					resultStr = this.backupCmd(cfu.getBackups(), cfu.getCover(), curVersion);
					String[] res = resultStr.split(RESULT_SEPRATOR_SPLIT);
					if (res != null && res.length >= 2) {
						resultState = Integer.parseInt(res[0]);
						sb.append(res[1] + ";");
					}
					if(resultState == RESULT_FAIL){
						break;
					}
				}
				resultDesc = sb.toString();
				if(resultState == RESULT_OK){
					flag = true;
					resultState = RESULT_BACKUP_OK;
					// 发送信息
					TaskResultOper.sendTaskResult(command.getExecId(), command.getExecType(),
							resultState, resultDesc, "", execTime, new Date(), isServer, command.getIsLoop());
				}else{
					flag = false;
				}
			}
			/**
			 * 步骤4、对所有参数指定的覆盖目录进行更新并删除指定的文件，有一个失败则失败
			 */
			if (next && (flag || stepState == RESULT_BACKUP_OK)) {// 备份成功，再进行覆盖即更新
				Date execTime = new Date();
				ParamCoverUpdate[] updates = handleCoverUpdates(cfuList, reVersion, upgradePath);
				for (int i = 0; i < Contants.max_times; i++) {
					resultStr = this.updateCmd(updates);
					String[] res = resultStr.split(RESULT_SEPRATOR_SPLIT);
					if (res != null && res.length >= 2) {
						resultState = Integer.parseInt(res[0]);
						resultDesc = res[1];
					}
					if (resultState == RESULT_OK) {
						break;
					}
					try {// 如果更新失败，让当前线程暂停几秒，再重试
						Thread.sleep(1000 * Contants.max_delay_seconds);
					} catch (InterruptedException e) {
						logger.error(Utils.printExceptionStack(e));
						continue;
					}
				}
				if (resultState == RESULT_OK) {
					flag = true;
					resultState = RESULT_UPDATE_OK;
					// 发送信息
					TaskResultOper.sendTaskResult(command.getExecId(), command.getExecType(),
							resultState, resultDesc, "", execTime, new Date(), isServer, command.getIsLoop());
				}else{
					flag = false;
				}
				if(flag && isServer){
					Long curVer = Long.parseLong(VersionCfg
							.getValue(VersionCfg.NSERVER_VERSION));
					if (curVer < curVersion) {
						VersionCfg.setValue(VersionCfg.NSERVER_VERSION, curVersion + "");
						logger.info("DC升级到版本" + curVersion);
					}
				}
			}
			/**
			 * 步骤5、顺序启动所有进程，有一个失败则失败
			 */
			if (next && (flag || stepState == RESULT_UPDATE_OK)) {// 更新成功，再启动进程
				Date execTime = new Date();
				List<ParamCmdExec> startList = new ArrayList<ParamCmdExec>();
				for(ParamUpgrade cfu:cfuList){
					ParamCmdExec exec = new ParamCmdExec();
					exec.setExecCmd(cfu.getStartupFile());
					exec.setExecParams(cfu.getExecParams());
					exec.setExecResult(cfu.getPidFile());
					exec.setMaxWaitTime(cfu.getMaxWaitTime());
					exec.setUsername(cfu.getUsername());
					exec.setParam1(cfu.getParam1());
					exec.setForceExec("Y");
					exec.setResidentFlag("Y");
					exec.setReturnFlag("N");
					startList.add(exec);
				}
				resultStr = this.startCmd(startList, execTime, false, resultState);
				String[] res = resultStr.split(RESULT_SEPRATOR_SPLIT);
				if (res != null && res.length >= 2) {
					resultState = Integer.parseInt(res[0]);
					resultDesc = res[1];
				}
			}
		} catch (Exception e) {
//			resultDesc = "执行失败：出现异常，" + e.getMessage() + "，详细信息请查看日志";
//			resultDesc = "Execution failure: abnormality，" + e.getMessage() + "，please check the log for details";
			resultDesc = "i18n_client.AgentCommand.execFailDesc18_n81i，" + e.getMessage() + "，i18n_client.AgentCommand.execFailDesc10.showDetail_n81i";
			resultState = RESULT_FAIL;
			logger.error(pre + "anormly：" + Utils.printExceptionStack(e));
		}finally{
		}
//		logger.info(pre + "结果描述信息 >> " + resultDesc);//i18nlog
		logger.info(pre + "结束……");
		
		return resultState + RESULT_SEPRATOR + resultDesc;
	}
	
	/**
	 * Agent原生支持命令，备份
	 * @param backups 备份文件包装对象数组（需要备份的目录、备份到的目录、排除的目录）
	 * @param version 
	 * @return
	 */
	public String backupCmd(ParamBackup[] backups, String oppositeBackupDir, Long version){
		logger.info("备份开始……");
		String resultDesc = "";
		long resultState = RESULT_FAIL;
		try {
			if(backups==null || backups.length<=0){
//				resultDesc = "备份成功：未指定备份参数，不需要备份";
//				resultDesc = "Backup success: no backup parameters specified, no backup required";
				resultDesc = "i18n_client.AgentCommand.backupSuccess1_n81i";
				resultState = RESULT_OK;
//				logger.debug(resultDesc);//i18nlog
			}else{
				StringBuffer sb = new StringBuffer();
				CompressFileMgr cfmImpl = new CompressFileMgr();
				for(int i=0; i<backups.length; i++){
					ParamBackup backup = backups[i];
					// 处理需要备份的目录
					logger.debug("backupCmd---->oppositeBackupDir=" + oppositeBackupDir);
					if(oppositeBackupDir!=null && oppositeBackupDir.trim().length()>0){
						backup.setBackup(getFilePath(backup.getBackup(), oppositeBackupDir));
					}
					// 判断需要备份的文件是否存在
					String backupPath = backup.getBackup();
					if(backup.getBackup()==null || backup.getBackup().trim().length()<=0){
//						resultDesc = "备份失败，需要备份的文件属性为空";
//						resultDesc = "Backup failed, the file property that needs to be backed up is empty";
						resultDesc = "i18n_client.AgentCommand.backupFail1_n81i";
						sb.append(resultDesc + ";");
						resultState = RESULT_FAIL;
//						logger.debug(resultDesc);//i18nlog
						break;
					}
					File srcFile = new File(backup.getBackup().trim());
					if(!srcFile.exists()){
//						resultDesc = "备份“" +  backupPath + "”失败，需要备份的文件不存在--" + srcFile.getAbsolutePath();
//						resultDesc = "Backup“" +  backupPath + "” failed, the file that needs to be backed up does not exist--" + srcFile.getAbsolutePath();
						resultDesc = "i18n_client.AgentCommand.backup_n81i“" +  backupPath + "” i18n_client.AgentCommand.backupFail2_n81i--" + srcFile.getAbsolutePath();
						sb.append(resultDesc + ";");
						resultState = RESULT_FAIL;
//						logger.debug(resultDesc);//i18nlog
						break;
					}else if(srcFile.isDirectory() && srcFile.list().length<=0){
//						resultDesc = "备份“" +  backupPath + "”成功，需要备份的文件目录为空，不用进行备份";
//						resultDesc = "Backup“" +  backupPath + "”successful, the backup file directory is empty without backup";
						resultDesc = "i18n_client.AgentCommand.backup_n81i“" +  backupPath + "”i18n_client.AgentCommand.backupSuccess2_n81i";
						sb.append(resultDesc + ";");
						resultState = RESULT_OK;
//						logger.debug(resultDesc);//i18nlog
						continue;
					}
					// 处理压缩时按绝对路径，还是相对路径, 只对LInux起作用, 默认按相对
					boolean isAbs = false;
					if("Y".equals(handleYesOrNoField(backup.getIsAbs()))){
						isAbs = true;
					}
					// 处理备份到的目录
					if(backup.getBackupTo()==null || backup.getBackupTo().trim().length()<=0){
						backup.setBackupTo(Contants.localBackupPath);
					}else{
						backup.setBackupTo(Contants.localBackupPath
								+ File.separator + backup.getBackupTo().trim());
					}
					// 处理备份操作
					String compressName = backup.getBackupTo() + File.separator
							+ getBakFileName(srcFile.getName(), version, isAbs);
					File destDirFile = new File(compressName);
					if (!destDirFile.exists()) {
						if (!destDirFile.getParentFile().exists()) {
							destDirFile.getParentFile().mkdirs();
						}
						if (destDirFile.getParentFile().canWrite()) {
							String[] excepts = backup.getExcept();
							if(excepts!=null && excepts.length>0){
								for(int j=0; j<excepts.length; j++){
									if(excepts[j]!=null && excepts[j].trim().length()>0){
										excepts[j] = getFilePath(excepts[j], backup.getBackup());
									}
								}
							}
							boolean flag = cfmImpl.compressFile(srcFile.getAbsolutePath(), destDirFile.getAbsolutePath(), excepts, isAbs);
							
//							resultDesc = flag ? "备份“" + backupPath
//									+ "”成功，备份文件" + destDirFile.getAbsolutePath()
//									: "备份“" + backupPath + "”失败";
							resultDesc = flag ? "i18n_client.AgentCommand.backup_n81i“" + backupPath
									+ "”i18n_client.AgentCommand.backupSuccess3_n81i" + destDirFile.getAbsolutePath()
									: "i18n_client.AgentCommand.backup_n81i“" + backupPath + "”i18n_client.AgentCommand.backupFail3_n81i";
							sb.append(resultDesc + ";");
							resultState = flag ? RESULT_OK : RESULT_FAIL;
							logger.debug(resultDesc + ": "
//									+ srcFile.getAbsolutePath() + " 到 "
									+ srcFile.getAbsolutePath() + " to "
									+ destDirFile.getAbsolutePath());
							if (resultState == RESULT_FAIL) {
								break;
							}
						}else{
//							resultDesc = "备份“" +  backupPath + "”失败，备份到的目录”" + backup.getBackupTo() + "“只读";
//							resultDesc = "Backup“" +  backupPath + "”failed, backup to the directory”" + backup.getBackupTo() + "“read-only";
							resultDesc = "i18n_client.AgentCommand.backup_n81i“" +  backupPath + "”i18n_client.AgentCommand.backupFail4_n81i”" + backup.getBackupTo() + "“i18n_client.AgentCommand.readonly_n81i";
							sb.append(resultDesc + ";");
							resultState = RESULT_FAIL;
//							logger.debug(resultDesc + "--" + destDirFile.getParent());//i18nlog
							break;
						}
					}else{
//						resultDesc = "备份“" +  backupPath + "”成功，已存在该版本备份，不用备份--备份文件" + destDirFile.getAbsolutePath();
//						resultDesc = "Backup“" +  backupPath + "”Successful version already exists, without backup - backup files" + destDirFile.getAbsolutePath();
						resultDesc = "i18n_client.AgentCommand.backup_n81i“" +  backupPath + "”i18n_client.AgentCommand.backupSuccess4_n81i" + destDirFile.getAbsolutePath();
						sb.append(resultDesc + ";");
						resultState = RESULT_OK;
//						logger.debug(resultDesc + "--" + destDirFile.getAbsolutePath());//i18nlog
					}
				}//for end
//				resultDesc = resultState==RESULT_OK ? "备份成功，详情信息如下：" + sb.toString() : "备份失败，详情信息如下：" + sb.toString();
//				resultDesc = resultState==RESULT_OK ? "The backup is successful, and the details are as follows：" + sb.toString() : "Backup failed, the details are as follows：" + sb.toString();
				resultDesc = resultState==RESULT_OK ? "i18n_client.AgentCommand.backupSuccess5_n81i：" + sb.toString() : "i18n_client.AgentCommand.backupFail5_n81i：" + sb.toString();
			}
		} catch (Exception e) {
//			resultDesc = "备份失败：出现异常，" + e.getMessage() + "，详细信息请查看日志";
//			resultDesc = "Backup failure: Exception，" + e.getMessage() + "，please check the log for details";
			resultDesc = "i18n_client.AgentCommand.backupFail6_n81i，" + e.getMessage() + "，i18n_client.AgentCommand.execFailDesc10.showDetail_n81i";
			resultState = RESULT_FAIL;
			logger.error("备份异常：" + Utils.printExceptionStack(e));
		}
//		logger.info("备份结果描述信息 >> " + resultDesc);//i18nlog
		logger.info("备份结束……");
		
		return resultState + RESULT_SEPRATOR + resultDesc;
	}
	
	/**
	 * Agent原生支持命令，覆盖更新或恢复
	 * @param updates 覆盖更新包装对象数组（备份的文件、恢复的目录、删除的文件或目录）
	 * @return
	 */
	public String updateCmd(ParamCoverUpdate[] updates){
		logger.info("更新或恢复开始……");
		String resultDesc = "";
		long resultState = RESULT_FAIL;
		try {
			if(updates==null || updates.length<=0){
//				resultDesc = "覆盖成功：未指定参数，不需要覆盖";
//				resultDesc = "Overwrite success: unspecified parameters, no need to cover";
				resultDesc = "i18n_client.AgentCommand.coverSuccess1_n81i";
				resultState = RESULT_OK;
//				logger.debug(resultDesc);//i18nlog
			} else {
				StringBuffer sb = new StringBuffer();
				CompressFileMgr cfmImpl = new CompressFileMgr();
				for(int i=0; i<updates.length; i++){
					ParamCoverUpdate update = updates[i];

					
					
					// 处理是否创建覆盖目录
					update.setIsCreateCover(handleYesOrNoField(update.getIsCreateCover()));
					// 处理解压时按绝对路径，还是相对路径, 只对LInux起作用, 默认按相对
					boolean isAbs = false;
					if("Y".equals(handleYesOrNoField(update.getIsAbs()))){
						isAbs = true;
					}
					/**
					 * 判断源文件是否存在
					 */
					if(update.getSource()==null || update.getSource().trim().length()<=0){
//						resultDesc = "覆盖“" + update.getCover() + "”失败，源文件为空";
//						resultDesc = "Overwrite“" + update.getCover() + "”failure, source file is empty";
						resultDesc = "i18n_client.AgentCommand.coverFail1_n81i“" + update.getCover() + "”i18n_client.AgentCommand.coverFail1_n81i";
						sb.append(resultDesc + ";");
						resultState = RESULT_FAIL;
//						logger.debug(resultDesc);//i18nlog
						break;
					}
					File sourceFile = new File(update.getSource().trim());
					if (!sourceFile.exists()) {
//						resultDesc = "覆盖“" + update.getCover() + "”失败，源文件不存在--" + sourceFile.getAbsolutePath();
//						resultDesc = "Overwrite“" + update.getCover() + "”failure, source files do not exist--" + sourceFile.getAbsolutePath();
						resultDesc = "i18n_client.AgentCommand.coverFail1_n81i“" + update.getCover() + "”i18n_client.AgentCommand.coverFail2_n81i--" + sourceFile.getAbsolutePath();
						sb.append(resultDesc + ";");
						resultState = RESULT_FAIL;
//						logger.debug(resultDesc);//i18nlog
						break;
					} 
					/**
					 * 判断覆盖目录是否存在：
					 *    1、必须判断：Window系统、或者Linux系统且不是压缩文件、或者Linux系统且是压缩文件且是相对路径
					 *    2、不用判断：Linux系统下源文件是压缩文件且绝对解压，该属性无效
					 */
					File coverDir = null;
					boolean isCompressFile = CompressFileMgr.isCompressFile(sourceFile);
					String os = System.getProperty("os.name");
					if (os.startsWith("Windows")
							|| (!os.startsWith("Windows") && !isCompressFile)
							|| (!os.startsWith("Windows") && isCompressFile && !isAbs)) {
						if(update.getCover()==null || update.getCover().trim().length()<=0){
//							resultDesc = "覆盖“" + update.getCover() + "”失败，覆盖目录为空";
//							resultDesc = "Overwrite“" + update.getCover() + "”overwrite directory is empty";
							resultDesc = "i18n_client.AgentCommand.coverFail1_n81i“" + update.getCover() + "”i18n_client.AgentCommand.coverFail3_n81i";
							sb.append(resultDesc + ";");
							resultState = RESULT_FAIL;
//							logger.debug(resultDesc);//i18nlog
							break;
						}
						coverDir = new File(update.getCover().trim());
						if (!coverDir.exists() && "N".equals(update.getIsCreateCover())) {
//							resultDesc = "覆盖“" + update.getCover() + "”失败：覆盖目录不存在--" + coverDir.getAbsolutePath();
//							resultDesc = "Overwrite“" + update.getCover() + "”failed: Overwrite directory does not exist--" + coverDir.getAbsolutePath();
							resultDesc = "i18n_client.AgentCommand.coverFail1_n81i“" + update.getCover() + "”i18n_client.AgentCommand.coverFail4_n81i--" + coverDir.getAbsolutePath();
							sb.append(resultDesc + ";");
							resultState = RESULT_FAIL;
//							logger.debug(resultDesc);//i18nlog
							break;
						}
						// Y 创建目录
						if (!coverDir.exists()) {
							coverDir.mkdirs();
						}
					}else{
						coverDir = new File("/");
						if(update.getCover()==null || update.getCover().trim().length()<=0){
							update.setCover("/");
						}
					}
					
					//判断源文件或目录是否和覆盖目录相同
					if(sourceFile.getPath().equals(coverDir.getPath())){
//						resultDesc = "覆盖“" + update.getCover() + "”失败：覆盖目录“" + update.getCover() + "”和源目录相同！";
						resultDesc = "i18n_client.AgentCommand.coverFail1_n81i“" + update.getCover() + "”i18n_client.AgentCommand.coverFail5_n81i“" + update.getCover() + "”i18n_client.AgentCommand.coverFail5.sameDir_n81i";
						sb.append(resultDesc + ";");
						resultState = RESULT_FAIL;
//						logger.debug(resultDesc);//i18nlog
						break;
					}
					
					if (coverDir.canWrite()) {
						String delMsg = this.delete(update.getDelete(), coverDir.getAbsolutePath());
						if(!StringUtils.isEmpty(delMsg)){
							sb.append(delMsg + ";");
						}
						boolean flag = true;
						if (sourceFile.isDirectory()) {
							FileUtils.copyDirectoryToDirectory(sourceFile, coverDir);
						} else {
							if (isCompressFile) {
								flag = cfmImpl.decompressFile(sourceFile.getAbsolutePath(), coverDir.getAbsolutePath(), isAbs);
							} else {
								if (coverDir.isFile()) {
									coverDir = coverDir.getParentFile();
								}
								FileUtils.copyFileToDirectory(sourceFile,coverDir);
							}
						}
						if (flag) {
//							resultDesc = "覆盖“" + update.getCover() + "”成功, "
//									+ sourceFile.getAbsolutePath() + " 到 "
//									+ coverDir.getAbsolutePath();
//							resultDesc = "Overwrite“" + update.getCover() + "”success, "
//									+ sourceFile.getAbsolutePath() + " to "
//									+ coverDir.getAbsolutePath();
							resultDesc = "i18n_client.AgentCommand.coverFail1_n81i“" + update.getCover() + "”i18n_client.AgentCommand.coverSuccess2_n81i, "
									+ sourceFile.getAbsolutePath() + "i18n_client.AgentCommand.coverSuccess2.to_n81i "
									+ coverDir.getAbsolutePath();
						} else {
//							resultDesc = "覆盖“" + update.getCover() + "”失败";
//							resultDesc = "Overwrite“" + update.getCover() + "”failed";
							resultDesc = "i18n_client.AgentCommand.coverFail1_n81i“" + update.getCover() + "”i18n_client.AgentCommand.coverFail6_n81i";
						}
						sb.append(resultDesc + ";");
						resultState = flag ? RESULT_OK : RESULT_FAIL;
						logger.debug(resultDesc);
						if (resultState == RESULT_FAIL) {
							break;
						}
					} else {
//						resultDesc = "覆盖“" + update.getCover() + "”失败：要覆盖的目录只读--" + coverDir.getAbsolutePath();
//						resultDesc = "Overwrite“" + update.getCover() + "”failed: The directory to be overwritten is read-only--" + coverDir.getAbsolutePath();
						resultDesc = "i18n_client.AgentCommand.coverFail1_n81i“" + update.getCover() + "”i18n_client.AgentCommand.coverFail7_n81i--" + coverDir.getAbsolutePath();
						sb.append(resultDesc + ";");
						resultState = RESULT_FAIL;
//						logger.debug(resultDesc);//i18nlog
						break;
					}
				}// for end
//				resultDesc = resultState==RESULT_OK ? "覆盖成功，详情信息如下：" + sb.toString() : "覆盖失败，详情信息如下：" + sb.toString();
//				resultDesc = resultState==RESULT_OK ? "Overwrite success, details are as follows：" + sb.toString() : "Overwrite failed, details are as follows：" + sb.toString();
				resultDesc = resultState==RESULT_OK ? "i18n_client.AgentCommand.coverSuccess3_n81i：" + sb.toString() : "i18n_client.AgentCommand.coverFail8_n81i：" + sb.toString();
			}
		} catch (Exception e) {
//			resultDesc = "覆盖失败：出现异常，" + e.getMessage() + "，详细信息请查看日志";
//			resultDesc = "Overwrite failed: An exception occurred," + e.getMessage() + "，see the logs for details";
			resultDesc = "i18n_client.AgentCommand.coverFail9_n81i," + e.getMessage() + "，i18n_client.AgentCommand.execFailDesc10.showDetail_n81i";
			resultState = RESULT_FAIL;
			logger.error("覆盖命令异常：" + Utils.printExceptionStack(e));
		}
		
//		logger.info("更新或恢复结果描述信息 >> " + resultDesc);//i18nlog
		logger.info("更新或恢复结束……");
		
		return resultState + RESULT_SEPRATOR + resultDesc;
	}
	
	/**
	 * Agent原生支持命令，停用进程
	 * @param pidFileArr 存放进程ID的文件的数组集
	 * @return
	 */
	public String killProcessCmd(String[] pidFileArr) {
		logger.info("停用进程集开始……");
		String resultDesc = "";
		long resultState = RESULT_FAIL;
		
		try {
			if(pidFileArr==null || pidFileArr.length<=0){
//				resultDesc = "停用进程失败：未指定PID文件参数";
//				resultDesc = "Deactivation process failed: PID file parameter not specified";
				resultDesc = "i18n_client.AgentCommand.killProcFail1_n81i";
				resultState = RESULT_FAIL;
//				logger.debug(resultDesc);//i18nlog
			}else {
				StringBuffer sb = new StringBuffer();
				for(String pidFileStr : pidFileArr){
					logger.debug("upgradeKillProcess pidFile------->" + pidFileStr);
					File pidFile = new File(pidFileStr);
					if(pidFileStr==null || pidFileStr.trim().length()<=0){
						resultState = RESULT_OK;
//						resultDesc = "停用成功,进程PID文件为空";
//						resultDesc = "Disabled, process PID file is empty";
						resultDesc = "i18n_client.AgentCommand.killProcSuccess1_n81i";
						sb.append(resultDesc + "；");
//						logger.debug(resultDesc + "--" + pidFile.getAbsolutePath() );//i18nlog
					}else if(!pidFile.exists()){
						resultState = RESULT_OK;
//						resultDesc = "停用“" + pidFileStr + "”成功,进程PID文件找不到";
						resultDesc = "i18n_client.AgentCommand.deactivate_n81i“" + pidFileStr + "”i18n_client.AgentCommand.killProcSuccess2_n81i";
						sb.append(resultDesc + "；");
//						logger.debug(resultDesc + "--" + pidFile.getAbsolutePath() );//i18nlog
					} else {
						String[] pidArr = ProcessUtil.getProcessIdByFile(pidFile);
						boolean isExist = false;
						if (pidArr != null && pidArr.length > 0) {
							for (int i = 0; i < pidArr.length; i++) {// 目前考虑只有一个PID的情况
								isExist = ProcessUtil
										.isProcessExistByPid(pidArr[i]);
								if (isExist) {// 只要有一个PID存在，就认为是进程存在
									break;
								}
							}
						}
						if (!isExist) {
//							resultDesc = "停用“" + pidFileStr + "”成功：进程原本不存在";
//							resultDesc = "Deactivate“" + pidFileStr + "”Success: Process does not exist originally";
							resultDesc = "i18n_client.AgentCommand.deactivate_n81i“" + pidFileStr + "”i18n_client.AgentCommand.killProcSuccess3_n81i";
							sb.append(resultDesc + "；");
							resultState = RESULT_OK;
//							logger.debug(resultDesc);//i18nlog
						} else {
							for (int i = 0; i < pidArr.length; i++) {
								ProcessUtil.killProcess(pidArr[i]);// 杀进程
							}
							for (int i = 0; i < Contants.max_times; i++) {
								pidArr = ProcessUtil.getProcessIdByFile(new File(pidFileStr));
								isExist = false;
								if (pidArr != null && pidArr.length > 0) {
									for (int j = 0; j < pidArr.length; j++) {
										isExist = ProcessUtil
												.isProcessExistByPid(pidArr[j]);
										if (isExist) {// 只要有一个PID存在，就认为是停用进程失败
											break;
										}
									}
								}
								if (!isExist) {
									break;
								}
								try {// 如果停用进程失败，让当前线程暂停几秒，再重试
									Thread
											.sleep(1000 * Contants.max_delay_seconds);
								} catch (InterruptedException e) {
									logger.error(Utils.printExceptionStack(e));
									continue;
								}
							}
							resultState = !isExist ? RESULT_OK : RESULT_FAIL;
//							resultDesc = !isExist ? "停用“" + pidFileStr + "”成功"
//									: "停用“" + pidFileStr
//											+ "”失败, 进程存在，请查看是否有守护进程";
//							resultDesc = !isExist ? "Deactivation of“" + pidFileStr + "”succeeded"
//									: "Deactivate“" + pidFileStr
//									+ "”Failed, process exists, see if there is a daemon";
							resultDesc = !isExist ? "i18n_client.AgentCommand.deactivate_n81i“" + pidFileStr + "”i18n_client.AgentCommand.killProcSuccess4_n81i"
									: "i18n_client.AgentCommand.deactivate_n81i“" + pidFileStr
											+ "”i18n_client.AgentCommand.killProcFail2_n81i";
							sb.append(resultDesc + "；");
//							logger.debug(resultDesc);//i18nlog
							if (resultState == RESULT_FAIL) {
								break;
							}
						}
					}
					
				}//for end
//				resultDesc = resultState == RESULT_OK ? "停用进程成功，详情信息如下：" + sb.toString() : "停用进程失败，详情信息如下：" + sb.toString();
//				resultDesc = resultState == RESULT_OK ? "Deactivation process is successful. Details are as follows：" + sb.toString() : "Deactivation process failed, details are as follows：" + sb.toString();
				resultDesc = resultState == RESULT_OK ? "i18n_client.AgentCommand.killProcSuccess5_n81i：" + sb.toString() : "i18n_client.AgentCommand.killProcFail3_n81i：" + sb.toString();
			}
		} catch (Exception e) {
//			resultDesc = "停用进程失败：出现异常，" + e.getMessage() + "，详细信息请查看日志";
			resultDesc = "i18n_client.AgentCommand.killProcFail4_n81i，" + e.getMessage() + "，i18n_client.AgentCommand.execFailDesc10.showDetail_n81i";
			resultState = RESULT_FAIL;
			logger.error("Disable process set exception：" + Utils.printExceptionStack(e));
		}
//		logger.info("停用进程集结果描述信息 >> " + resultDesc);//i18nlog
		logger.info("停用进程集结束……");
		
		return resultState + RESULT_SEPRATOR + resultDesc;
	}
	
	/**
	 * Agent原生支持命令，启动进程
	 * @param execList 命令参数对象集
	 * @param isRestart 当前运行的文件或命令，是否是重新启动，若是则不用判断进程是否存在，直接执行
	 * @return
	 */
	public String startCmd(List<ParamCmdExec> execList, Date startTime, boolean isRestart, long stepState){
		logger.info("启动开始……");
		String resultDesc = "";
		StringBuffer sb = new StringBuffer();
		long resultState = RESULT_FAIL;
		try {
			if(execList==null || execList.size()<=0){
//				resultDesc = "执行成功：未指定内容，不需要执行";
//				resultDesc = "Execution success: unspecified content, no need to execute";
				resultDesc = "i18n_client.AgentCommand.startCmdSuccess1_n81i";
				resultState = RESULT_OK;
				logger.debug(resultDesc);
			} else {
				List<String> returnFileList = new ArrayList<String>();
				
				for(ParamCmdExec exec : execList){
					if (stepState != RESULT_START_OK
							&& stepState != RESULT_START_FAIL) {// 若还未执行过，则继续执行，否则，只进行回传文件
						/**
						 * 步骤1、判断执行文件是否存在且可执行
						 */
						if(exec.getExecCmd()==null || exec.getExecCmd().trim().length()<=0){
//							sb.append("执行“" + exec.getExecCmd() + "”失败，执行命令为空;");
//							sb.append("Execute“" + exec.getExecCmd() + "”Failure, the result file is empty;");
							sb.append("i18n_client.AgentCommand.exec_n81i“" + exec.getExecCmd() + "”i18n_client.AgentCommand.startCmdFail1_n81i;");
							resultState = RESULT_FAIL;
							break;
						}
						if(exec.getExecResult()==null || exec.getExecResult().trim().length()<=0){
//							sb.append("执行“" + exec.getExecCmd() + "”失败，结果文件为空;");
//							sb.append("Execute“" + exec.getExecCmd() + "”Failure, the result file is empty;");
							sb.append("i18n_client.AgentCommand.exec_n81i“" + exec.getExecCmd() + "”i18n_client.AgentCommand.startCmdFail2_n81i;");
							resultState = RESULT_FAIL;
							break;
						}
						// 2013-4-26 由于DC改为Windows服务后升级要支持命令，若命令复杂的话，无法正确判断，做不作此步判断，待以后确定执行方案后考虑此步要不要
						/*boolean existFlag= ProcessUtil.isFileOrCmdExist(exec.getExecCmd());
						if(!existFlag){
							sb.append("执行“" + exec.getExecCmd() + "”失败，执行文件不存在或不是可执行命令;");
							resultState = RESULT_FAIL;
							break;
						}*/
						// 判断用户和密码是否正确
						/*boolean flag = ProcessUtil.checkUserPass(exec.getUsername(), exec.getParam1());
						if (!flag) {
							sb.append("执行“" + exec.getExecCmd() + "”失败，用户名或密码不正确;");
							resultState = RESULT_FAIL;
							break;
						}*/
						// 判断用户名是否正确
						if(!ProcessUtil.checkUserOrGroupExist(exec.getUsername(), null)){
//							sb.append("执行“" + exec.getExecCmd() + "”失败，用户名不正确;");
//							sb.append("Execute“" + exec.getExecCmd() + "”Failure, incorrect username");
							sb.append("i18n_client.AgentCommand.exec_n81i“" + exec.getExecCmd() + "”i18n_client.AgentCommand.startCmdFail3_n81i");
							resultState = RESULT_FAIL;
							break;
						}
						//判断用户是否有执行命令的权限
						//------------待完成su - -c "[ -x jzz_test.py ];echo $?" nmstest;
						/**
						 * 步骤2、判断是否常驻内存
						 *    (1) 常驻内存，判断进程是否存在，是否强制执行；
						 *    (2) 非常驻内存，直接执行
						 */
						boolean residentFlag = false;
						if ("Y".equals(handleYesOrNoField(exec.getResidentFlag()))) {
							residentFlag = true;
						}
						// 如果不是重启操作，则判断进程是否存在，反之，不判断进程是否存在，直接进行下面的执行部分
						if (residentFlag && !isRestart) {//----常驻内存，针对PID判断操作，execResult存放PID的文件
							//----步骤2-1-0、判断进程是否存在: 先判断进程PID文件是否存在--不存在认为进程不存在，存在判断进程PID是否存在
							File pidFile = new File(exec.getExecResult());
							boolean isExist = false;
							String[] pidArr = null;
							if(pidFile.exists()){
								pidArr = ProcessUtil.getProcessIdByFile(pidFile);
								if (pidArr != null) {
									for(int i=0; i<pidArr.length; i++){
										isExist = ProcessUtil.isProcessExistByPid(pidArr[i]);
										if(isExist){//如果有一个PID存在，就认为进程存在
											break;
										}
									}
								}
							}
							// ----步骤2-1-1、进程存在：则判断是否是强制执行; 进程不存在：则直接执行
							if (isExist) {
								// -------进程存在：强制执行，则杀进程继续执行
								if ("Y".equals(handleYesOrNoField(exec.getForceExec()))) {
									for (int i = 0; i < pidArr.length; i++) {
										ProcessUtil.killProcess(pidArr[i]);// 杀进程
									}
								}else{// --------进程存在：非强制执行，则不执行(continue, 跳过下面执行的部分)
									resultState = RESULT_OK;
//									sb.append("执行“" + exec.getExecCmd() + "”成功，进程原本存在，不用执行;");
//									sb.append("Execute“" + exec.getExecCmd() + "”Execution success: unspecified content, no need to execute;");
									sb.append("i18n_client.AgentCommand.exec_n81i“" + exec.getExecCmd() + "”i18n_client.AgentCommand.startCmdSuccess2_n81i;");
									continue;
								}
							}
						}
						/**
						 * 步骤3、执行文件或命令部分
						 *    A、常驻内存，结果文件存放PID，查看是否成功；
						 *    B、非常驻内存，结果文件存放结果标识，解析结果文件判断是否成功
						 */ 
						String resultMsg = ProcessUtil.runExec(exec.getExecCmd(), exec.getExecParams(), exec.getUsername(), exec.getParam1());
						// -------------步骤3-0、暂停最终结果获取最大等待时间（单位秒）
						long delay = 3;
						if (exec.getMaxWaitTime() != null
								&& !"".equals(exec.getMaxWaitTime().trim())) {
							delay = Long.parseLong(exec.getMaxWaitTime().trim());
						}
						if(delay>0){
							synchronized (this) {
								logger.debug("wait: " + delay + " seconds");
								this.wait(delay * 1000);
							}
						}
						// -------------步骤3-1、判断进程启动成功与否
						if (residentFlag) {// ---常驻内存，针对PID判断操作
							File pidFile = new File(exec.getExecResult());
							boolean isExist = false;
							if (pidFile.exists()) {
								String[] pidArr = ProcessUtil.getProcessIdByFile(pidFile);
								isExist = false;
								String notExitPids = null;
								if (pidArr != null) {
									for (int i = 0; i < pidArr.length; i++) {
										isExist = ProcessUtil.isProcessExistByPid(pidArr[i]);
										if (!isExist) {// 如果有一个PID不存在，就认为启动不成功
											notExitPids = pidArr[i];
											break;
										}
									}
								}
								resultState = isExist ? RESULT_OK : RESULT_FAIL;
								if(isExist){
//									sb.append("执行“" + exec.getExecCmd() + "”成功;");
//									sb.append("Execute“" + exec.getExecCmd() + "”success;");
									sb.append("i18n_client.AgentCommand.exec_n81i“" + exec.getExecCmd() + "”i18n_client.AgentCommand.success_n81i;");
								}else{
//									sb.append("执行“" + exec.getExecCmd() + "”失败, " + (notExitPids==null ? "PID为空;" : ("PID“"+notExitPids+"”不存在;")));
//									sb.append("Execute" + exec.getExecCmd() + "”failed, " + (notExitPids==null ? "PID is empty;" : ("PID is empty“"+notExitPids+"”do not exist;")));
									sb.append("i18n_client.AgentCommand.exec_n81i" + exec.getExecCmd() + "”failed, " + (notExitPids==null ? "i18n_client.AgentCommand.startCmdFail4_n81i;" : ("PID “"+notExitPids+"”i18n_client.AgentCommand.execFailDesc8_n81i;")));
								}
							} else {
								resultState = RESULT_FAIL;
//								sb.append("Failure to execute“" + exec.getExecCmd() + "”process PID file“" + pidFile.getAbsolutePath() + "”do not exist;");
								sb.append("i18n_client.AgentCommand.exec_n81i“" + exec.getExecCmd() + "”i18n_client.AgentCommand.startCmdFail6_n81i“" + pidFile.getAbsolutePath() + "”i18n_client.AgentCommand.execFailDesc8_n81i;");
							}
						} else {// ---非常驻内存，解析结果文件：结果标识（0 成功 1失败）|结果描述
							File resultFile = new File(exec.getExecResult());
							if (resultFile.exists()) {
								String[] result = FileWrUtil.cfgFileReader(resultFile);
								if (result != null && result.length > 0) {
									String[] res = result[0].split("\\|");
									if (res != null && res.length >= 1) {
										resultState = Long.parseLong(res[0]);
										logger.debug("startCmd resultState-->" + resultState);
//										String tt = (resultState == RESULT_OK ? "成功" : "失败");
										String tt = (resultState == RESULT_OK ? "Success" : "Failed");
										if(res.length >= 2){
//											sb.append("Execute“" + exec.getExecCmd() + "”" + tt + "，" + res[1] + ";");
											sb.append("i18n_client.AgentCommand.exec_n81i“" + exec.getExecCmd() + "”" + tt + "，" + res[1] + ";");
										}else{
//											sb.append("Execute“" + exec.getExecCmd() + "”" + tt + ";");
											sb.append("i18n_client.AgentCommand.exec_n81i“" + exec.getExecCmd() + "”" + tt + ";");
										}
									}
								} else {
									resultState = RESULT_FAIL;
//									sb.append("Failure to execute“" + exec.getExecCmd() + "”result file is not written in result file;");
									sb.append("i18n_client.AgentCommand.exec_n81i“" + exec.getExecCmd() + "”i18n_client.AgentCommand.startCmdFail7_n81i;");
								}
							} else {
								resultState = RESULT_FAIL;
//								sb.append("Failure to execute“" + exec.getExecCmd() + "”result file“" + resultFile.getAbsolutePath() + "”does not exist;");
								sb.append("i18n_client.AgentCommand.exec_n81i“" + exec.getExecCmd() + "”i18n_client.AgentCommand.startCmdFail8_n81i“" + resultFile.getAbsolutePath() + "”i18n_client.AgentCommand.execFailDesc8_n81i;");
							}
						}// -------------步骤3-1结束
						if(resultMsg!=null && resultMsg.length()>0){
							sb.append(resultMsg + ";");
						}
					}else{
						resultState = (stepState == RESULT_START_OK ? RESULT_OK : RESULT_FAIL);
					}
					
					/**
					 * 步骤4 判断是否回传文件
					 */
					if(exec.getReturnFlag()!=null && "Y".equalsIgnoreCase(exec.getReturnFlag().trim())){
						returnFileList.add(exec.getReturnPath());
					}
					
					if(resultState == RESULT_FAIL){//如果有一个失败，就停止执行
						break;
					}
				}// for end
				
				/**
				 * 步骤5 如果有回传文件，则回传文件并修改结果为中间结果；如果无回传文件，不回传且结果为最终结果
				 */
				if(returnFileList.size()>0){
					ReturnFilePO rfPO = new ReturnFilePO();
					rfPO.setTaskId(command.getExecId());
					rfPO.setTaskType(command.getExecType());
					rfPO.setUuid(Contants.AGENT_HOST_UUID);
					rfPO.setState(resultState);
					//rfPO.setFilePaths(returnFileList);
					rfPO.setStartTime(startTime);
					rfPO.setEndTime(new Date());
					rfPO.setIsLoop(command.getIsLoop());
					TaskReturnHandle.startTaskReturnFileThread(rfPO, returnFileList);
					resultState = (resultState == RESULT_OK) ? RESULT_START_OK : RESULT_START_FAIL;
				}
				
				if(resultState == RESULT_OK || resultState == RESULT_START_OK){
//					resultDesc = "执行成功，详细信息如下：" + sb.toString();
//					resultDesc = "Execution succeeded, details are as follows：" + sb.toString();
					resultDesc = "i18n_client.AgentCommand.startCmdSuccess4_n81i：" + sb.toString();
				}else{
//					resultDesc = "执行失败，详细信息如下：" + sb.toString();
//					resultDesc = "Execution failed with the following details：" + sb.toString();
					resultDesc = "i18n_client.AgentCommand.startCmdFail9_n81i：" + sb.toString();
				}
//				logger.debug(resultDesc);//i18nlog
			}
				
		} catch (Exception e) {
//			resultDesc = "执行失败：出现异常，" + e.getMessage() + "，详细信息请查看日志";
//			resultDesc = "Execution failed: An exception occurred，" + e.getMessage() + "，see the logs for details";
			resultDesc = "i18n_client.AgentCommand.startCmdFail10_n81i，" + e.getMessage() + "，i18n_client.AgentCommand.execFailDesc10.showDetail_n81i";
			resultState = RESULT_FAIL;
			logger.error("Start abnormity: " + Utils.printExceptionStack(e));
		}
//		logger.info("启动结果描述信息 >> " + resultDesc);//i18nlog
		logger.info("启动结束……");
		
		return resultState + RESULT_SEPRATOR + resultDesc;
	}
	
	/**
	 * 可执行命令，单次执行命令
	 * @param execList 命令参数对象集
	 * @return
	 */
	public String singleExecCmd(List<ParamCmdExec> execList, Date startTime, long stepState){
		logger.info("单次命令执行开始……");
		String resultDesc = "";
		StringBuffer sb = new StringBuffer();
		long resultState = RESULT_FAIL;
		try {
			if(execList==null || execList.size()<=0){
//				resultDesc = "执行成功：未指定内容，不需要执行";
				resultDesc = "Execution succeeded: no content specified, no need to execute";
				resultState = RESULT_OK;
				logger.debug(resultDesc);
			} else {
				List<String> returnFileList = new ArrayList<String>();
				for(ParamCmdExec exec : execList){
					if (stepState != RESULT_START_OK
							&& stepState != RESULT_START_FAIL) {// 若还未执行过，则继续执行，否则，只进行回传文件
						/**
						 * 步骤1、必要的空判断
						 */
						if(exec.getExecCmd()==null || exec.getExecCmd().trim().length()<=0){
//							sb.append("执行“" + exec.getExecCmd() + "”失败，执行命令为空;");
							sb.append("The execution of“" + exec.getExecCmd() + "”failed and the execution command was empty;");
							resultState = RESULT_FAIL;
							break;
						}
						// 判断用户和密码是否正确
						/*boolean flag = ProcessUtil.checkUserPass(exec.getUsername(), exec.getParam1());
						if (!flag) {
							sb.append("执行“" + exec.getExecCmd() + "”失败，用户名或密码不正确;");
							resultState = RESULT_FAIL;
							break;
						}*/
						// 判断用户名是否正确
						if(!ProcessUtil.checkUserOrGroupExist(exec.getUsername(), null)){
//							sb.append("执行“" + exec.getExecCmd() + "”失败，用户名不正确;");
//							sb.append("Failure to execute“" + exec.getExecCmd() + "” with incorrect user name;");
							sb.append("i18n_client.AgentCommand.exec_n81i“" + exec.getExecCmd() + "” i18n_client.AgentCommand.singleCmdFail2_n81i;");
							resultState = RESULT_FAIL;
							break;
						}
						//判断用户是否有执行命令的权限
						//------------待完成su - -c "[ -x jzz_test.py ];echo $?" nmstest;
						/**
						 * 步骤2、执行命令部分
						 */
						final String execCmd = exec.getExecCmd();
						final String username = exec.getUsername();
						final String pass = exec.getParam1();
						final String threadName = Thread.currentThread().getName();
						Future<String> future = Common.service.submit(new Callable<String>(){
							@Override
							public String call() throws Exception {
								thread = Thread.currentThread();
								Thread.currentThread().setName(threadName);
								String result = ProcessUtil.runExecGetResult(execCmd, username, pass);
								return result;
							}
						});
						// ----获取最大等待时间（单位秒）
						long delay = 3;
						if (exec.getMaxWaitTime() != null
								&& !"".equals(exec.getMaxWaitTime().trim())) {
							delay = Long.parseLong(exec.getMaxWaitTime().trim());
							logger.debug("singleExecCmd Timeout time value: " + delay + " seconds");
						}
						// ----超过一定时间，终止执行线程，返回结果超时
						try {
							String result = future.get(delay, TimeUnit.SECONDS);
							logger.debug("singleExecCmd result-->" + result);
							// ---解析结果：结果标识（0 成功 1失败）|结果描述
							if (result != null && !result.isEmpty()) {
								String[] res = result.split("\\|");
								if (res != null && res.length >= 1) {
									resultState = Long.parseLong(res[0]);
									logger.debug("singleExecCmd resultState-->" + resultState);
//									String tt = (resultState == RESULT_OK ? "成功" : "失败");
//									String tt = (resultState == RESULT_OK ? "Success" : "Failed");
									String tt = (resultState == RESULT_OK ? "i18n_client.AgentCommand.success_n81i" : "i18n_client.AgentCommand.fail_n81i");
									if(res.length >= 2){
//										sb.append("Execute“" + exec.getExecCmd() + "”" + tt + "，" + res[1] + ";");
										sb.append("i18n_client.AgentCommand.exec_n81i“" + exec.getExecCmd() + "”" + tt + "，" + res[1] + ";");
									}else{
//										sb.append("Execute“" + exec.getExecCmd() + "”" + tt + ";");
										sb.append("i18n_client.AgentCommand.exec_n81i“" + exec.getExecCmd() + "”" + tt + ";");
									}
								}
							} else {
								resultState = RESULT_FAIL;
//								sb.append("Execute“" + exec.getExecCmd() + "”failed");
								sb.append("i18n_client.AgentCommand.exec_n81i“" + exec.getExecCmd() + "”i18n_client.AgentCommand.singleCmdFail3_n81i");
							}
						} catch (TimeoutException e) {
							if(thread!=null && thread.isAlive()){
								thread.stop();
								logger.debug(execCmd + "---singleExecCmd Timeout stop thread--" + thread.isAlive());
							}
							future.cancel(true);
							resultState = RESULT_FAIL;
//							sb.append("执行“" + exec.getExecCmd() + "”失败， 超时;");
//							sb.append("Failed to execute“" + exec.getExecCmd() + "”timeout;");
							sb.append("i18n_client.AgentCommand.exec_n81i“" + exec.getExecCmd() + "”i18n_client.AgentCommand.singleCmdFail4_n81i;");
							logger.debug("---------singleExecCmd timeout----------");
						}catch (InterruptedException e) {
							if(thread!=null && thread.isAlive()){
								thread.stop();
								logger.debug(execCmd + "---singleExecCmd Interrupted stop thread--" + thread.isAlive());
							}
							future.cancel(true);
							resultState = RESULT_FAIL;
//							sb.append("Failure to execute“" + exec.getExecCmd() + "”thread was interrupted;");
							sb.append("i18n_client.AgentCommand.exec_n81i“" + exec.getExecCmd() + "”i18n_client.AgentCommand.singleCmdFail5_n81i;");
							logger.debug("---------singleExecCmd Interrupted----------");
						}
					}else{
						resultState = (stepState == RESULT_START_OK ? RESULT_OK : RESULT_FAIL);
					}
						
					/**
					 * 步骤3 判断是否回传文件
					 */
					if(exec.getReturnFlag()!=null && "Y".equalsIgnoreCase(exec.getReturnFlag().trim())){
						returnFileList.add(exec.getReturnPath());
					}
					
					if(resultState == RESULT_FAIL){//如果有一个失败，就停止执行
						break;
					}
				}// for end
				
				/**
				 * 步骤4 如果有回传文件，则回传文件并修改结果为中间结果；如果无回传文件，不回传且结果为最终结果
				 */
				if(returnFileList.size()>0){
					ReturnFilePO rfPO = new ReturnFilePO();
					rfPO.setTaskId(command.getExecId());
					rfPO.setTaskType(command.getExecType());
					rfPO.setUuid(Contants.AGENT_HOST_UUID);
					rfPO.setState(resultState);
					//rfPO.setFilePaths(returnFileList);
					rfPO.setStartTime(startTime);
					rfPO.setEndTime(new Date());
					rfPO.setIsLoop(command.getIsLoop());
					TaskReturnHandle.startTaskReturnFileThread(rfPO, returnFileList);
					resultState = (resultState == RESULT_OK) ? RESULT_START_OK : RESULT_START_FAIL;
				}
				
				if(resultState == RESULT_OK || resultState == RESULT_START_OK){
//					resultDesc = "执行成功，详细信息如下：" + sb.toString();
//					resultDesc = "The execution was successful. The details are as follows：" + sb.toString();
					resultDesc = "i18n_client.AgentCommand.singleCmdSuccess2_n81i：" + sb.toString();
				}else{
//					resultDesc = "执行失败，详细信息如下：" + sb.toString();
//					resultDesc = "Execution failed with the following details：" + sb.toString();
					resultDesc = "i18n_client.AgentCommand.singleCmdFail6_n81i：" + sb.toString();
				}
			}
				
		} catch (Exception e) {
//			resultDesc = "执行失败：出现异常，" + e.getMessage() + "，详细信息请查看日志";
//			resultDesc = "Execution failed: An exception occurred，" + e.getMessage() + "， See the logs for details";
			resultDesc = "i18n_client.AgentCommand.singleCmdFail7_n81i，" + e.getMessage() + "，i18n_client.AgentCommand.execFailDesc10.showDetail_n81i";
			resultState = RESULT_FAIL;
			logger.error("Single command execution exception: " + Utils.printExceptionStack(e));
		}
//		logger.info("单次命令执行结果描述信息 >> " + resultDesc);//i18nlog
		logger.info("单次命令执行结束……");
		
		return resultState + RESULT_SEPRATOR + resultDesc;
	}
	
	/**
	 * 取得备份文件的名称：原文件名_version_bak_压缩文件的相应后缀
	 * @param oldFileName
	 * @param version
	 * @param isAbs
	 * @return
	 */
	private String getBakFileName(String oldFileName, long version, boolean isAbs){
		String fileName = "";
		if(oldFileName!=null){
			fileName += oldFileName + "_";
		}
		fileName += version + "_bak" + CompressFileMgr.getCompressSuffixByOs(isAbs);
		
		return fileName;
	}
	
	/**
	 * 处理相对和绝对文件路径：如果文件本身是绝对路径不做处理；如果文件是相对路径则相对parent下
	 * @param filepath 相对或绝对路径的文件
	 * @param parent 相对路径文件的父目录
	 * @return
	 * @throws Exception
	 */
	private String getFilePath(String filepath, String parent) throws Exception{
		if(filepath!=null && filepath.trim().length()>0){
			File file = new File(filepath);
			if(file.isAbsolute()){
				return file.getCanonicalPath();
			}else if(parent!=null && parent.trim().length()>0){
				File parentFile = new File(parent);
				if(!parentFile.isDirectory()){
					parent = parentFile.getParent();
				}
				return new File(parent + File.separator + filepath).getCanonicalPath();
			}else {
				return file.getCanonicalPath();
			}
		}
		return null;
	}
	
	/**
	 * 删除文件，先对删除的文件做处理：如果是绝对路径直接删除；如果是相对路径则相应处理
	 * @param dels 将要删除的文件集
	 * @param oppositeDelDir 如果删除文件是相对路径，则为相对路径文件的父目录
	 * @return 删除文件的描述信息
	 * @throws Exception
	 */
	private String delete(String[] dels, String oppositeDelDir) throws Exception{
		StringBuffer sb = new StringBuffer();
		if(dels!=null && dels.length>0){
			for(String del : dels){
				if(del==null || del.trim().length()<=0){
					continue;
				}
				del = getFilePath(del, oppositeDelDir);
				if(del==null){
					continue;
				}
				File file = new File(del);
				if(file.exists()){
					FileUtil.delDir(file);
//					sb.append("删除”" + file.getCanonicalPath() + "“成功;");
//					sb.append("Delete”" + file.getCanonicalPath() + "“success;");
					sb.append("i18n_client.AgentCommand.deleteSuccess_n81i”" + file.getCanonicalPath() + "“i18n_client.AgentCommand.success_n81i;");
				}else{
//					sb.append("删除”" + file.getCanonicalPath() + "“,文件不存在;");
//					sb.append("Delete”" + file.getCanonicalPath() + "“,the file does not exist;");
					sb.append("i18n_client.AgentCommand.deleteFail_n81i”" + file.getCanonicalPath() + "“,i18n_client.AgentCommand.deleteFail.noFile_n81i;");
				}
			}
		}
		if(sb.length()>0){
			sb.deleteCharAt(sb.length()-1);
		}
		return sb.toString();
	}
	
    /**
     * 对是否字段(Y\N)的默认值处理
     * @param field
     * @return
     */
	private String handleYesOrNoField(String field){
		if(field==null || field.trim().length()<=0){
			return "N";
		}else if("Y".equalsIgnoreCase(field.trim())){
			return "Y";
		}else{
			return "N";
		}
	}
	
	/**
	 * 升级中的覆盖更新参数处理：
	 * *逆向任务，按恢复到的版本和本地备份目录来处理覆盖源文件；恢复目录，如果是相对则相对升级的覆盖目录，如果是绝对则不变
	 * *升级任务，按升级文件的路径处理源文件
	 * @param cfuList 升级参数列表
	 * @param reVersion 恢复到的版本
	 * @param upgradePath 升级文件的路径
	 * @return 覆盖更新数组集
	 * @throws Exception
	 */
	private ParamCoverUpdate[] handleCoverUpdates(List<ParamUpgrade> cfuList,
			Long reVersion, String upgradePath) throws Exception {
		List<ParamCoverUpdate> pcuList = new ArrayList<ParamCoverUpdate>();
		for (ParamUpgrade cfu : cfuList) {
			if (reVersion != null && reVersion.longValue() > 0) {//逆向任务，恢复
				for (ParamCoverUpdate update : cfu.getRecoverys()) {
					//必须先处理恢复目录
					update.setCover(getFilePath(update.getCover(), cfu.getCover()));
					//再处理恢复源文件
					update.setSource(getRecoverSource(update, reVersion));
					pcuList.add(update);
				}
			} else {//升级任务
				ParamCoverUpdate update = new ParamCoverUpdate();
				update = new ParamCoverUpdate();
				update.setSource(getFilePath(cfu.getFileName(), upgradePath));
				update.setCover(cfu.getCover());
				update.setIsCreateCover(cfu.getIsCreateCover());
				update.setIsAbs(cfu.getIsAbs());
				update.setDelete(cfu.getDelete());

				pcuList.add(update);
			}
		}
		ParamCoverUpdate[] updates = new ParamCoverUpdate[cfuList.size()];
		return pcuList.toArray(updates);
	}
	
	/**
	 * 升级中的恢复参数的源处理：source应为 localBackupPath + source + bakFileName(coverFileName_version_bak_压缩后缀)
	 * @param update 覆盖更新对象
	 * @param reVersion 恢复到的版本
	 * @return 恢复参数的源文件
	 * @throws Exception
	 */
	private String getRecoverSource(ParamCoverUpdate update, Long reVersion)
			throws Exception {
		// 逆向任务，source是原任务的备份目标目录(相对的)，cover是原任务的备份过的文件
		// source还可以是操作人员自己备份的文件(完整的绝对路径且必须存在),如果source文件不存在，则按上面默认的方法重新拼写
		String source = update.getSource();
		logger.debug("getRecoverSource source----" + update.getSource());
		logger.debug("getRecoverSource cover----" + update.getCover());
		if (update.getSource() != null
				&& !new File(update.getSource()).exists()) {
			String oldFileName = "";
			if (update.getCover() != null) {
				oldFileName = new File(update.getCover()).getCanonicalFile().getName();
			}
			boolean isAbs = false;
			if ("Y".equals(handleYesOrNoField(update.getIsAbs()))) {
				isAbs = true;
			}
			source = Contants.localBackupPath + File.separator
					+ update.getSource() + File.separator
					+ getBakFileName(oldFileName, reVersion, isAbs);
		}
		return source;
	}
	
	private void backup(File srcFile, File destDir, String[] excepts) throws Exception{
		if (!srcFile.exists()) {
			return;
		}
		if (!srcFile.isDirectory()) {
			boolean flag = true;
			if (excepts != null && excepts.length > 0) {
				for (String except : excepts) {
					if (srcFile.getCanonicalPath().equalsIgnoreCase(except)) {
						flag = false;
						break;
					}
				}
			}
			if (flag) {
				FileUtils.copyFileToDirectory(srcFile, destDir);
			}
		} else if (srcFile.isDirectory()) {
			destDir = new File(destDir.getAbsolutePath() + File.separator
					+ srcFile.getName());
			for (File f : srcFile.listFiles()) {
				boolean flag = true;
				if (excepts != null && excepts.length > 0) {
					for (String except : excepts) {
						if (f.getCanonicalPath().equalsIgnoreCase(except)) {
							flag = false;
							break;
						}
					}
				}
				if (flag) {
					if (!f.isDirectory()) {
						FileUtils.copyFileToDirectory(f, destDir);
					} else if (f.isDirectory()) {
						backup(f, new File(destDir.getAbsolutePath()), excepts);
					}
				}
			}
		}
	}
	
}
