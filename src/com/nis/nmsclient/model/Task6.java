package com.nis.nmsclient.model;


/**
 * 任务实体类：6 升级
 * 
 */
public class Task6 {
	/**
	 * 任务ID
	 */
	private Long taskId;
	/**
	 * 任务类型：1 文件推送，4 命令执行，5 shell注册，6 升级
	 */
	private long taskType;
	/**
	 * 命令类型：4 Agent原生支持命令
	 */
	private long commandType;
	/**
	 * 命令名称
	 */
	private String commandName;
	/**
	 * 命令参数
	 */
	private String commandParam;
	/**
	 * 执行状态：4下发任务（40 下发成功，41下发失败），5杀进程（50成功，51失败）、6备份、7更新（覆盖） 、8启动进程 
	 */
	private Long state;
	/**
	 * 如果是恢复(逆向任务),则为恢复到的版本（恢复到的任务ID）。判断它是否为空，来确定是否逆向任务
	 */
	private Long version;
	/**
	 * 升级时间
	 */
	private Long upgradeTime;
	/**
	 * 全局权限用户名（未来）
	 */
	private String username;
	/**
	 * 全局权限密码(未来 约定加密方式)
	 */
	private String userpwd;
	/**
	 * 重新执行，原任务ID
	 */
	private Long oldTaskId;
	
	public Long getTaskId() {
		return taskId;
	}
	public void setTaskId(Long taskId) {
		this.taskId = taskId;
	}
	public long getTaskType() {
		return taskType;
	}
	public void setTaskType(long taskType) {
		this.taskType = taskType;
	}
	public long getCommandType() {
		return commandType;
	}
	public void setCommandType(long commandType) {
		this.commandType = commandType;
	}
	public String getCommandName() {
		return commandName;
	}
	public void setCommandName(String commandName) {
		this.commandName = commandName;
	}
	public String getCommandParam() {
		return commandParam;
	}
	public void setCommandParam(String commandParam) {
		this.commandParam = commandParam;
	}
	public Long getState() {
		return state;
	}
	public void setState(Long state) {
		this.state = state;
	}
	public Long getVersion() {
		return version;
	}
	public void setVersion(Long version) {
		this.version = version;
	}
	public Long getUpgradeTime() {
		return upgradeTime;
	}
	public void setUpgradeTime(Long upgradeTime) {
		this.upgradeTime = upgradeTime;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getUserpwd() {
		return userpwd;
	}
	public void setUserpwd(String userpwd) {
		this.userpwd = userpwd;
	}
	public Long getOldTaskId() {
		return oldTaskId;
	}
	public void setOldTaskId(Long oldTaskId) {
		this.oldTaskId = oldTaskId;
	}
	
}
