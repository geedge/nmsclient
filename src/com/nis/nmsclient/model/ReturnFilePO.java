package com.nis.nmsclient.model;

import java.util.Date;

public class ReturnFilePO {
	/**
	 * ID
	 */
	private Long taskId;
	/**
	 * 类型：4 命令执行，6 升级
	 */
	private long taskType;
	/**
	 * 唯一标志一台物理机（类似于IP）
	 */
	private long uuid;
	/**
	 * 回传路径集： key 文件实际路径 value 文件别名
	 */
//	private Map<String, String> filePathMap;
	/**
	 * 当前要回传的文件
	 */
//	private File curRetrunFile;
	/**
	 * 回传文件名（统一处理后一个任务只有一个回传文件）
	 */
	private String returnFileName;
	/**
	 * 回传状态：0 成功，1 失败 
	 */
	private long state;
	/**
	 * 开始时间
	 */
	private Date startTime;
	/**
	 * 结束时间
	 */
	private Date endTime;
	/**
	 * 是否循环任务
	 */
	private long isLoop;
	/**
	 * 回传文件的结果描述
	 */
	private String resDesc;
	
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
	public long getState() {
		return state;
	}
	public void setState(long state) {
		this.state = state;
	}
	public Date getStartTime() {
		return startTime;
	}
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	public Date getEndTime() {
		return endTime;
	}
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
	public long getIsLoop() {
		return isLoop;
	}
	public void setIsLoop(long isLoop) {
		this.isLoop = isLoop;
	}
	public String getResDesc() {
		return resDesc;
	}
	public void setResDesc(String resDesc) {
		this.resDesc = resDesc;
	}
	public String getReturnFileName() {
		return returnFileName;
	}
	public void setReturnFileName(String returnFileName) {
		this.returnFileName = returnFileName;
	}
	public long getUuid() {
		return uuid;
	}
	public void setUuid(long uuid) {
		this.uuid = uuid;
	}
	
}
