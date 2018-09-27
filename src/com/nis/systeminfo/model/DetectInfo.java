package com.nis.systeminfo.model;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DetectInfo {
	/*
	 * 该字段是针对，同一时间点多条具体数据（写多个文件），比如：网卡监测（多个网卡问题）
	 */
	private List<String> detailDatas = new LinkedList<String>();
	/*
	 * 针对公共数据的描述信息
	 */
	private String descInfo;
	/*
	 * 该字段是针对一条数据，关联的其他表的数据；写入到一个文件的： String 是解析标识， List<String[]>是多条数据（String[]存入表中的数据）
	 * 如：系统信息监测，数据中有网卡数量，网卡数量关联着其他表（表中又有多个字段，如网卡名称，IP，子网掩码等等）
	 */
	private Map<String, List<String[]>> relatedDatas;
	
	private String diskMsg;//用于存储硬盘只读和磁盘满的异常信息
	
	public List<String> getDetailDatas() {
		return detailDatas;
	}
	public void setDetailDatas(List<String> detailDatas) {
		this.detailDatas = detailDatas;
	}
	public String getDescInfo() {
		return descInfo;
	}
	public void setDescInfo(String descInfo) {
		this.descInfo = descInfo;
	}
	public Map<String, List<String[]>> getRelatedDatas() {
		return relatedDatas;
	}
	public void setRelatedDatas(Map<String, List<String[]>> relatedDatas) {
		this.relatedDatas = relatedDatas;
	}
	public String getDiskMsg() {
		return diskMsg;
	}
	public void setDiskMsg(String diskMsg) {
		this.diskMsg = diskMsg;
	}
	
}
