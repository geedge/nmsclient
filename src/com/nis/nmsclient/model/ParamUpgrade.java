package com.nis.nmsclient.model;


/**
 * 升级命令的参数实体类
 * @date Mar 23, 2012
 * @author zhenzhen
 * @version
 */
public class ParamUpgrade {
	/**
	 * 指定更新需要的文件，如果是相对路径，则相对推送目的地
	 */
	private String fileName = null;
	/**
	 * 指定文件覆盖目录
	 */
	private String cover = null;
	/**
	 * 是否创建覆盖目录
	 */
	private String isCreateCover = null;
	/**
	 * 如果更新源文件是个压缩包：Y 按绝对路径解压即在根目录下解压，N 按进入备份目录解压（这个属性只针对Linux有效，Windows下只按进入备份目录解压）
	 */
	private String isAbs = null;
	/**
	 * 需要删除的目录，如果是相对路径，则相对覆盖目录
	 */
	private String []  delete = null;
	/**
	 * 该命令是否强制执行：Y是，N否
	 */
	private String forceExec;
	/**
	 * 文件的属主或运行的用户（只针对Linux有效）
	 */
	private String username;
	/**
	 * 用户密码（只针对Linux有效）
	 */
	private String param1;
	/**
	 * 文件的属组（只针对Linux有效）
	 */
	private String groupName;
	/**
	 * 权限设置
	 */
	private String permisson = null;
	//以上是升级用到的参数属性，以下是升级和恢复都有的参数属性
	/**
	 * 备份目录，如果是相对路径，则相对覆盖目录
	 */
	private ParamBackup [] backups = null;
	/**
	 * 恢复目录，逆向任务时使用
	 */
	private ParamCoverUpdate[] recoverys;
	/**
	 * 进程PID文件全路径
	 */
	private String pidFile = null;
	/**
	 * 进程启动文件全路径
	 */
	private String startupFile = null;
	/**
	 * 执行文件的参数序列
	 */
	private String[] execParams;
	/**
	 * 最终结果获取最大等待时间（单位：秒）
	 */
	private String maxWaitTime;

	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getCover() {
		return cover;
	}
	public void setCover(String cover) {
		this.cover = cover;
	}
	public ParamBackup[] getBackups() {
		return backups;
	}
	public void setBackups(ParamBackup[] backups) {
		this.backups = backups;
	}
	public String getPidFile() {
		return pidFile;
	}
	public void setPidFile(String pidFile) {
		this.pidFile = pidFile;
	}
	public String getStartupFile() {
		return startupFile;
	}
	public void setStartupFile(String startupFile) {
		this.startupFile = startupFile;
	}
	public String[] getDelete() {
		return delete;
	}
	public void setDelete(String[] delete) {
		this.delete = delete;
	}
	public String getIsCreateCover() {
		return isCreateCover;
	}
	public void setIsCreateCover(String isCreateCover) {
		this.isCreateCover = isCreateCover;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPermisson() {
		return permisson;
	}
	public void setPermisson(String permisson) {
		this.permisson = permisson;
	}
	public String getGroupName() {
		return groupName;
	}
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	public String getIsAbs() {
		return isAbs;
	}
	public void setIsAbs(String isAbs) {
		this.isAbs = isAbs;
	}
	public String getForceExec() {
		return forceExec;
	}
	public void setForceExec(String forceExec) {
		this.forceExec = forceExec;
	}
	public String getMaxWaitTime() {
		return maxWaitTime;
	}
	public void setMaxWaitTime(String maxWaitTime) {
		this.maxWaitTime = maxWaitTime;
	}
	public void setExecParams(String[] execParams) {
		this.execParams = execParams;
	}
	public String[] getExecParams() {
		return execParams;
	}
	public ParamCoverUpdate[] getRecoverys() {
		return recoverys;
	}
	public void setRecoverys(ParamCoverUpdate[] recoverys) {
		this.recoverys = recoverys;
	}
	public String getParam1() {
		return param1;
	}
	public void setParam1(String param1) {
		this.param1 = param1;
	}
	
}
