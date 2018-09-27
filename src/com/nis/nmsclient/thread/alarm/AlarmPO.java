package com.nis.nmsclient.thread.alarm;


public class AlarmPO {
	/**
	 * 监测ID
	 */
	private long id;
	/**
	 * 监测类型
	 */
	private String type;
	/**
	 * 监测设置名称
	 */
	private String procIden;
	/**
	 * 时间间隔(单位:分钟)
	 */
	private long checkGap;
	/**
	 * 是否是进程
	 */
	private boolean isproc;
	/**
	 * 进程路径与名称
	 */
	private String proc;
	/**
	 * PID文件
	 */
	private String pidFileStr;
	/**
	 * 进程搜索关键字
	 */
	private String processSearchKeyCode;
	/**
	 * 控制启动时间
	 */
	private Long controlStartTime;
	/**
	 * 是否系统启动（NMSAgent启动/第三方自己启动）；默认0自启动；1NMSAgent启动
	 */
	private String IsControlStart;

	public AlarmPO() {
		super();
	}

	public AlarmPO(long id, String type, String procIden, long checkGap,
			boolean isproc, String proc, String pidFileStr,
			Long controlStartTime, String processSearchKeyCode, String IsControlStart) {
		super();
		this.id = id;
		this.type = type;
		this.procIden = procIden;
		this.checkGap = checkGap;
		this.isproc = isproc;
		this.proc = proc;
		this.pidFileStr = pidFileStr;
		this.controlStartTime = controlStartTime;
		this.processSearchKeyCode = processSearchKeyCode;
		this.IsControlStart = IsControlStart;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getProcIden() {
		return procIden;
	}

	public void setProcIden(String procIden) {
		this.procIden = procIden;
	}

	public long getCheckGap() {
		return checkGap;
	}

	public void setCheckGap(long checkGap) {
		this.checkGap = checkGap;
	}

	public boolean isIsproc() {
		return isproc;
	}

	public void setIsproc(boolean isproc) {
		this.isproc = isproc;
	}

	public String getProc() {
		return proc;
	}

	public void setProc(String proc) {
		this.proc = proc;
	}

	public String getPidFileStr() {
		return pidFileStr;
	}

	public void setPidFileStr(String pidFileStr) {
		this.pidFileStr = pidFileStr;
	}

	public Long getControlStartTime() {
		return controlStartTime;
	}

	public void setControlStartTime(Long controlStartTime) {
		this.controlStartTime = controlStartTime;
	}

	public String getProcessSearchKeyCode() {
		return processSearchKeyCode;
	}

	public void setProcessSearchKeyCode(String processSearchKeyCode) {
		this.processSearchKeyCode = processSearchKeyCode;
	}
	
	public String getIsControlStart() {
		return IsControlStart;
	}
	
	public void setIsControlStart(String isControlStart) {
		IsControlStart = isControlStart;
	}

}
