package com.nis.nmsclient.model;


/**
 * 任务实体类：4 命令执行
 * 
 */
public class Task4 {
	/**
	 * 任务ID
	 */
	private Long taskId;
	/**
	 * 任务类型：1 文件推送，2 非流文本数据获取，3 流文本数据获取，4 命令执行，5 shell注册
	 */
	private long taskType;
	/**
	 * 命令类型：1 Agent原生支持命令，2 脚本，3 shell命令
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
	 * 脚本路径(已无用)
	 */
	private String scriptPath;
	/**
	 * 执行状态：4下发任务（40 下发成功，41下发失败），5杀进程（50成功，51失败）、6备份、7更新（覆盖） 、8启动进程 
	 */
	private long state;
	/**
	 * 执行时间
	 */
	private Long startTime;
	/**
	 * 结束时间
	 */
	private Long endTime;
	/**
	 * 是否循环任务: 0 非周期， 1 周期，默认是0
	 */
	private long isLoop;
	/**
	 * 如果是循环任务，循环周期
	 */
	private long loopDelay;
	/**
	 * 任务状态：6撤销执行
	 */
	private Long missionState;
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

	public long getState() {
		return state;
	}

	public void setState(long state) {
		this.state = state;
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

	public String getScriptPath() {
		return scriptPath;
	}

	public void setScriptPath(String scriptPath) {
		this.scriptPath = scriptPath;
	}

	public Long getStartTime() {
		return startTime;
	}

	public void setStartTime(Long startTime) {
		this.startTime = startTime;
	}

	public Long getEndTime() {
		return endTime;
	}

	public void setEndTime(Long endTime) {
		this.endTime = endTime;
	}

	public long getIsLoop() {
		return isLoop;
	}

	public void setIsLoop(long isLoop) {
		this.isLoop = isLoop;
	}

	public long getLoopDelay() {
		return loopDelay;
	}

	public void setLoopDelay(long loopDelay) {
		this.loopDelay = loopDelay;
	}

	public Long getMissionState() {
		return missionState;
	}

	public void setMissionState(Long missionState) {
		this.missionState = missionState;
	}

	public Long getOldTaskId() {
		return oldTaskId;
	}

	public void setOldTaskId(Long oldTaskId) {
		this.oldTaskId = oldTaskId;
	}
	
}
