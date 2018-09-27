package com.nis.nmsclient.model;

/**
 * 覆盖更新的参数实体类
 * @date Mar 23, 2012
 * @author zhenzhen
 * @version
 */
public class ParamCoverUpdate{
	/**
	 * 指定更新需要的文件
	 */
	private String source = null;
	/**
	 * 指定文件覆盖目录
	 */
	private String cover = null;
	/**
	 * 是否创建覆盖目录
	 */
	private String isCreateCover = null;
	/**
	 * 删除的文件或目录，如果是相对路径，则相对需要更新的目录
	 */
	private String[] delete = null;
	/**
	 * 如果更新源文件是个压缩包：Y 按绝对路径解压即在根目录下解压，N 按进入备份目录解压（这个属性只针对Linux有效，Windows下只按进入备份目录解压）
	 */
	private String isAbs = null;
	
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public String getCover() {
		return cover;
	}
	public void setCover(String cover) {
		this.cover = cover;
	}
	public String getIsCreateCover() {
		return isCreateCover;
	}
	public void setIsCreateCover(String isCreateCover) {
		this.isCreateCover = isCreateCover;
	}
	public String[] getDelete() {
		return delete;
	}
	public void setDelete(String[] delete) {
		this.delete = delete;
	}
	public String getIsAbs() {
		return isAbs;
	}
	public void setIsAbs(String isAbs) {
		this.isAbs = isAbs;
	}
	
}