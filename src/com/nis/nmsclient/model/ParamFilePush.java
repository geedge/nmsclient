package com.nis.nmsclient.model;

/**
 * 文件推送的参数实体类
 * @date Mar 23, 2012
 * @author zhenzhen
 * @version
 */
public class ParamFilePush {
	/**
	 * 推送文件名
	 */
	private String fileName;
	/**
	 * 推送目的地
	 */
	private String destPath;
	/**
	 * 是否覆盖：Y是，N否
	 */
	private String isCover;
	/**
	 * 推送文件的属主（只针对Linux有效）
	 */
	private String username;
	/**
	 * 属主密码（只针对Linux有效）
	 */
	private String param1;
	/**
	 * 推送文件的属组（只针对Linux有效）
	 */
	private String groupName;
	/**
	 * 推送文件拥有的权限（只针对Linux有效）
	 */
	private String permisson;
	
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getDestPath() {
		return destPath;
	}
	public void setDestPath(String destPath) {
		this.destPath = destPath;
	}
	public String getIsCover() {
		return isCover;
	}
	public void setIsCover(String isCover) {
		this.isCover = isCover;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getGroupName() {
		return groupName;
	}
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	public String getPermisson() {
		return permisson;
	}
	public void setPermisson(String permisson) {
		this.permisson = permisson;
	}
	public String getParam1() {
		return param1;
	}
	public void setParam1(String param1) {
		this.param1 = param1;
	}
}
