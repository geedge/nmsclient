package com.nis.nmsclient.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import com.nis.nmsclient.common.Contants;

/**
 * 客户端用到的监测设置信息实体
 * 
 */
public class SetInfo {
	/**
	 * 监测设置信息ID
	 */
	private Long id;
	/**
	 * 检测类型
	 */
	private String checkTypeName;//如：CPU、DISK等
	/**
	 * 检测类型的ID，预留
	 */
	private Long checkTypeId;
	/**
	 * 最大测试次数
	 */
	private Long checkMaxTimes;
	/**
	 * 时间间隔(单位:分钟)
	 */
	private Long checkGap;
	/**
	 * 超时时间(单位:秒)
	 */
	private Long checkOutTime;
	/**
	 * 监测状态：0无效；1有效
	 */
	private String checkState;
	/**
	 * 监测方式：0主动(DC执行)，1被动(NC执行)
	 */
	private String checkWay;
	/**
	 * 设置标志
	 */
	private String processIden;
	/**
	 * 进程PID存放文件
	 */
	private String processFile;
	/**
	 * 进程执行文件
	 */
	private String processPath;
	/**
	 * 是否系统启动（NMSAgent启动/第三方自己启动）；默认0自启动；1NMSAgent启动
	 */
	private String IsControlStart;
	/**
	 * 控制启动时间
	 */
	private Long controlStartTime;
	/**
	 *上传数据时间间隔单位分钟：不能为空，默认15分钟。监测数据上传到NMSServer周期。
	 */
	private Long uploadGap;
	
	/**
	 * 计划检测时间:针对当前配置信息首次执行时间
	 */
	private Long planCheckTime;
	/**
	 * 是否预置监测,0是，1否
	 */	
	private String isSchedule;
	/**
	 * 进程搜索关键字
	 */
	private String processSearchKeyCode;
	
	// ====== 以下信息仅供DC端使用，这里添加是为了不出现WARN信息
	private String nodeGroupsId;
	private String nodeIpsId;
	private Long viewLevel;
	private String nodeIp2;
	private Long isSNMP;
	
	// ======= 保存最后合并的临时结果文件的检测时间（仅用于NC）
	private Long lastMergeDetecTime;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getCheckTypeName() {
		return checkTypeName;
	}
	public void setCheckTypeName(String checkTypeName) {
		this.checkTypeName = checkTypeName;
	}
	public Long getCheckTypeId() {
		return checkTypeId;
	}
	public Long getCheckMaxTimes() {
		return checkMaxTimes;
	}
	public void setCheckMaxTimes(Long checkMaxTimes) {
		this.checkMaxTimes = checkMaxTimes;
	}
	public Long getCheckGap() {
		return checkGap;
	}
	public void setCheckGap(Long checkGap) {
		this.checkGap = checkGap;
	}
	public Long getCheckOutTime() {
		return checkOutTime;
	}
	public void setCheckOutTime(Long checkOutTime) {
		this.checkOutTime = checkOutTime;
	}
	public String getCheckState() {
		return checkState;
	}
	public void setCheckState(String checkState) {
		this.checkState = checkState;
	}
	public String getProcessIden() {
		return processIden;
	}
	public void setProcessIden(String processIden) {
		this.processIden = processIden;
	}
	public String getProcessFile() {
		return processFile;
	}
	public void setProcessFile(String processFile) {
		this.processFile = processFile;
	}
	public String getProcessPath() {
		return processPath;
	}
	public void setProcessPath(String processPath) {
		this.processPath = processPath;
	}
	public void setCheckTypeId(Long checkTypeId) {
		this.checkTypeId = checkTypeId;
	}
	public String getCheckWay() {
		return checkWay;
	}
	public void setCheckWay(String checkWay) {
		this.checkWay = checkWay;
	}
	
	public String[] getValArr(Long uuid, String dataFileDir) {
		List<String> list = new ArrayList<String>();
		list.add("pubInfo=" + uuid + "," + id + "," + checkTypeName + "," + processIden);
		list.add("checkState=" + checkState);
		//list.add("checkWay=" + checkWay);
		//list.add("isControlStart=" + IsControlStart);
		//list.add("controlStartTime=" + controlStartTime);
		list.add("checkGap=" + checkGap);
		list.add("checkOutTime=" + checkOutTime);
		list.add("checkMaxTimes=" + checkMaxTimes);
		//list.add("processFile=" + processFile);
		//list.add("processPath=" + processPath);
		list.add("planCheckTime=" + planCheckTime);
		list.add("uploadGap=" + uploadGap);
		list.add("dataFileDir=" + dataFileDir);
		
		try {
			if("1".equals(isSchedule)) { // 第三方监测
				File dataDir = new File(dataFileDir);
				File tempDataDir = new File(Contants.localTempDataIncomingPath, dataDir.getName());
				list.add("tempDataFileDir=" + tempDataDir.getCanonicalPath());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String[] val = new String[list.size()];
		return list.toArray(val);
	}
	public String getIsControlStart() {
		return IsControlStart;
	}
	public void setIsControlStart(String isControlStart) {
		IsControlStart = isControlStart;
	}
	public Long getUploadGap() {
		return uploadGap;
	}
	public void setUploadGap(Long uploadGap) {
		this.uploadGap = uploadGap;
	}
	public String getIsSchedule() {
		return isSchedule;
	}
	public void setIsSchedule(String isSchedule) {
		this.isSchedule = isSchedule;
	}
	public Long getControlStartTime() {
		return controlStartTime;
	}
	public void setControlStartTime(Long controlStartTime) {
		this.controlStartTime = controlStartTime;
	}
	public Long getPlanCheckTime() {
		return planCheckTime;
	}
	public void setPlanCheckTime(Long planCheckTime) {
		this.planCheckTime = planCheckTime;
	}
	public String getProcessSearchKeyCode() {
		return processSearchKeyCode;
	}
	public void setProcessSearchKeyCode(String processSearchKeyCode) {
		this.processSearchKeyCode = processSearchKeyCode;
	}
	public String getNodeGroupsId() {
		return nodeGroupsId;
	}
	public void setNodeGroupsId(String nodeGroupsId) {
		this.nodeGroupsId = nodeGroupsId;
	}
	public String getNodeIpsId() {
		return nodeIpsId;
	}
	public void setNodeIpsId(String nodeIpsId) {
		this.nodeIpsId = nodeIpsId;
	}
	public Long getViewLevel() {
		return viewLevel;
	}
	public void setViewLevel(Long viewLevel) {
		this.viewLevel = viewLevel;
	}
	public String getNodeIp2() {
		return nodeIp2;
	}
	public void setNodeIp2(String nodeIp2) {
		this.nodeIp2 = nodeIp2;
	}
	public Long getIsSNMP() {
		return isSNMP;
	}
	public void setIsSNMP(Long isSNMP) {
		this.isSNMP = isSNMP;
	}
	public void setLastMergeDetecTime(Long lastMergeDetecTime) {
		this.lastMergeDetecTime = lastMergeDetecTime;
	}
	public Long getLastMergeDetecTime() {
		return lastMergeDetecTime;
	}
	
	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}
	
	
}
