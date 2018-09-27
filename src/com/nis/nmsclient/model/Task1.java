package com.nis.nmsclient.model;

/**
 * 任务实体类：1 文件推送
 * 
 */
public class Task1 {
	/**
	 * 任务ID
	 */
	private Long taskId;
	/**
	 * 任务类型：1 文件推送，2 非流文本数据获取，3 流文本数据获取，4 命令执行，5 shell注册
	 */
	private long taskType;
	/**
	 * 推送文件参数设置
	 */
	private String taskParam;
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
	public String getTaskParam() {
		return taskParam;
	}
	public void setTaskParam(String taskParam) {
		this.taskParam = taskParam;
	}
	public Long getOldTaskId() {
		return oldTaskId;
	}
	public void setOldTaskId(Long oldTaskId) {
		this.oldTaskId = oldTaskId;
	}
}
