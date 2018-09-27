package com.nis.nmsclient.model;

public class CommandPO {
	/**
	 * ID
	 */
	private Long execId;
	/**
	 * 类型：4 命令执行，6 升级
	 */
	private long execType;
	/**
	 * 升级文件存放目录
	 */
	private String srcPath;
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
	private long execState;
	/**
	 * 恢复版本
	 */
	private Long execVersion;
	/**
	 * 是否周期任务
	 */
	private long isLoop;
	
	public Long getExecId() {
		return execId;
	}
	public void setExecId(Long execId) {
		this.execId = execId;
	}
	public long getExecType() {
		return execType;
	}
	public void setExecType(long execType) {
		this.execType = execType;
	}
	public String getSrcPath() {
		return srcPath;
	}
	public void setSrcPath(String srcPath) {
		this.srcPath = srcPath;
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
	public long getExecState() {
		return execState;
	}
	public void setExecState(long execState) {
		this.execState = execState;
	}
	public Long getExecVersion() {
		return execVersion;
	}
	public void setExecVersion(Long execVersion) {
		this.execVersion = execVersion;
	}
	public long getIsLoop() {
		return isLoop;
	}
	public void setIsLoop(long isLoop) {
		this.isLoop = isLoop;
	}
	
}
