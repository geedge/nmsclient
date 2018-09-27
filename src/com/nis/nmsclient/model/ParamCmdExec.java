package com.nis.nmsclient.model;

/**
 * 命令执行(启动命令)的参数实体类
 * @date Mar 23, 2012
 * @author zhenzhen
 * @version
 */
public class ParamCmdExec {
	/**
	 * 执行文件或命令
	 */	
	private String execCmd;
	/**
	 * 执行文件或命令的参数序列
	 */
	private String[] execParams;
	/**
	 * 该命令是否强制执行：Y是，N否
	 */
	private String forceExec;
	/**
	 * 常驻内存标识: Y是，N否
	 */
	private String residentFlag;
	/**
	 * 存放执行结果标识的文件:1、常驻内存的，写入PID，2、非常驻的，写执行结果：结果标识（0 成功 1 失败）|结果描述
	 */
	private String execResult;
	/**
	 * 回传标识: Y是，N否
	 */
	private String returnFlag;
	/**
	 * 回传文件或目录路径
	 */
	private String returnPath;
	/**
	 * 最终结果获取最大等待时间（单位：秒）
	 */
	private String maxWaitTime;
	/**
	 * 执行文件或命令的用户名（只针对Linux有效）
	 */
	private String username;
	/**
	 * 执行用户的密码（只针对Linux有效）
	 */
	private String param1;
	
	public String getExecCmd() {
		return execCmd;
	}
	public void setExecCmd(String execCmd) {
		this.execCmd = execCmd;
	}
	public String getForceExec() {
		return forceExec;
	}
	public void setForceExec(String forceExec) {
		this.forceExec = forceExec;
	}
	public String getResidentFlag() {
		return residentFlag;
	}
	public void setResidentFlag(String residentFlag) {
		this.residentFlag = residentFlag;
	}
	public String getExecResult() {
		return execResult;
	}
	public void setExecResult(String execResult) {
		this.execResult = execResult;
	}
	public String getReturnFlag() {
		return returnFlag;
	}
	public void setReturnFlag(String returnFlag) {
		this.returnFlag = returnFlag;
	}
	public String getReturnPath() {
		return returnPath;
	}
	public void setReturnPath(String returnPath) {
		this.returnPath = returnPath;
	}
	public String getMaxWaitTime() {
		return maxWaitTime;
	}
	public void setMaxWaitTime(String maxWaitTime) {
		this.maxWaitTime = maxWaitTime;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String[] getExecParams() {
		return execParams;
	}
	public void setExecParams(String[] execParams) {
		this.execParams = execParams;
	}
	public String getParam1() {
		return param1;
	}
	public void setParam1(String param1) {
		this.param1 = param1;
	}
}
