package com.nis.nmsclient.thread.upload;

/**
 * 消息类型
 * @author nanfang
 *
 */
public enum BusinessType {
	
	HeartBeat("HeartBeat",0),//心跳
	DetectZip("DetectZip",1), // 监测数据zip
	DetectData("DetectData",2),// 监测数据
	ObjTaskResult("ObjTaskResult",3),//任务结果
	FileTaskReturn("FileTaskReturn",4);//回传文件
	
	
	private final String name;
	private final int type;

	private BusinessType(String name ,int type) {
		this.type = type;
		this.name = name;
	}

	public int getType() {
		return this.type;
	}

	public String getName(){
		return this.name;
	}
	
	public static BusinessType getType(int type) {
		for (BusinessType t : BusinessType.values()) {
			if (t.getType() == type) {
				return t;
			}
		}
		return null;
	}
}