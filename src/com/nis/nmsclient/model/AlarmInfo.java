package com.nis.nmsclient.model;

/**
 * 监测信息报警相关信息实体
 * 
 */
public class AlarmInfo {
	/**
	 * 告警设置id
	 */
	private Long id;
	/**
	 * 监测设置id
	 */
	private Long setInfoId;
	/**
	 * 监测类型
	 */
	private String checkType;
	/**
	 * 进程设置名称
	 */
	private String processIden;
	/**
	 * 字段Id
	 */
	private Long metadataId;
	/**
	 * 字段描述
	 */
	private String filedCommonts;
	/**
	 * 字段序号
	 */
	private Integer showNum;
	/**
	 * 报警状态
	 */
	private String policeState;
	/**
	 * 报警值
	 */
	private String policeValue;
	/**
	 * 报警单位
	 */
	private String policeUnit;
	/**
	 * 报警比较符：针对number型数据>、<、>=、<=、=   针对字符串类型数据equal、 include、exclude
	 */
	private String policeSysmbols;
	/**
	 * 报警等级
	 */
	private Integer policeLevel;
	/**
	 * 设置告警时，指定多个标识符（如多个盘符、多个CPU、多个网卡），如硬盘使用率，空：所有盘存在一个盘使用率超过告警值，则告警；all：所有盘总的使用率超过告警值，则告警；指定多个盘符：指定盘存在一个盘使用率超过告警值，则告警
	 */
	private String marker;
	/**
	 * 标识符对应字段在metadata表中的id
	 */
	private Integer markerFiledId;;
	/**
	 * 标识符对应字段在metadata表中的showNum
	 */
	private Integer markerFiledShowNum;
	
	
	public Long getSetInfoId() {
		return setInfoId;
	}
	public void setSetInfoId(Long setInfoId) {
		this.setInfoId = setInfoId;
	}
	public Long getMetadataId() {
		return metadataId;
	}
	public void setMetadataId(Long metadataId) {
		this.metadataId = metadataId;
	}
	public Integer getShowNum() {
		return showNum;
	}
	public void setShowNum(Integer showNum) {
		this.showNum = showNum;
	}
	public String getPoliceState() {
		return policeState;
	}
	public void setPoliceState(String policeState) {
		this.policeState = policeState;
	}
	public String getPoliceUnit() {
		return policeUnit;
	}
	public void setPoliceUnit(String policeUnit) {
		this.policeUnit = policeUnit;
	}
	public String getPoliceSysmbols() {
		return policeSysmbols;
	}
	public void setPoliceSysmbols(String policeSysmbols) {
		this.policeSysmbols = policeSysmbols;
	}

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Integer getPoliceLevel() {
		return policeLevel;
	}
	public void setPoliceLevel(Integer policeLevel) {
		this.policeLevel = policeLevel;
	}
	public String getCheckType() {
		return checkType;
	}
	public void setCheckType(String checkType) {
		this.checkType = checkType;
	}
	public String getProcessIden() {
		return processIden;
	}
	public void setProcessIden(String processIden) {
		this.processIden = processIden;
	}
	public String getFiledCommonts() {
		return filedCommonts;
	}
	public void setFiledCommonts(String filedCommonts) {
		this.filedCommonts = filedCommonts;
	}
	public String getPoliceValue() {
		return policeValue;
	}
	public void setPoliceValue(String policeValue) {
		this.policeValue = policeValue;
	}
	
	public String toString() {
		return "showNum=" + showNum + ",policeSysmbols=" + policeSysmbols
				+ ",policeValue=" + policeValue + ",policeLevel=" + policeLevel;
	}
	
	public String toStringVal(){
		return showNum + "|" + policeSysmbols + "|" + policeValue + "|"
				+ policeLevel + "|" + filedCommonts;
	}
	
	public String getMarker()
	{
		return marker;
	}
	
	public void setMarker(String marker)
	{
		this.marker = marker;
	}
	
	public Integer getMarkerFiledId()
	{
		return markerFiledId;
	}
	
	public void setMarkerFiledId(Integer markerFiledId)
	{
		this.markerFiledId = markerFiledId;
	}
	
	public Integer getMarkerFiledShowNum()
	{
		return markerFiledShowNum;
	}
	
	public void setMarkerFiledShowNum(Integer markerFiledShowNum)
	{
		this.markerFiledShowNum = markerFiledShowNum;
	}
	
}
