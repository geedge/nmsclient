package com.nis.nmsclient.model;

/**
 * 备份命令的参数实体类
 * @date Mar 23, 2012
 * @author zhenzhen
 * @version
 */
public class ParamBackup{
	/**
	 * 需要备份的文件或目录
	 */
	private String  backup = null;
	/**
	 * 备份文件到的目的路径
	 */
	private String  backupTo = null;
	/**
	 * 排除的文件或目录，即不需要备份的，如果是相对路径，则相对需要备份的文件或目录
	 */
	private String[] except = null;
	/**
	 * 备份压缩包时：Y 按绝对路径压缩，N 按进入备份目录压缩（这个属性只针对Linux有效，Windows下只按进入备份目录压缩）
	 */
	private String isAbs = null;

	public String getBackup() {
		return backup;
	}
	public void setBackup(String backup) {
		this.backup = backup;
	}
	public String getBackupTo() {
		return backupTo;
	}
	public void setBackupTo(String backupTo) {
		this.backupTo = backupTo;
	}
	public String[] getExcept() {
		return except;
	}
	public void setExcept(String[] except) {
		this.except = except;
	}
	public String getIsAbs() {
		return isAbs;
	}
	public void setIsAbs(String isAbs) {
		this.isAbs = isAbs;
	}
	
}