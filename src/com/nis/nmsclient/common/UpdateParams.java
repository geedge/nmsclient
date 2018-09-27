package com.nis.nmsclient.common;

public class UpdateParams extends java.util.ListResourceBundle {
	public static String CONFIG_UPDATE_FLAG = "config.update.flag";		//更新标示 固定 判断配置文件指定值更新 建议自增1操作
	public static String CONFIG_UPDATE_FLAG_VALUE = "5";		//更新标示 该值 缺省值为0 每次修改都要
    static final String[][] contents = new String[][]{
    	{CONFIG_UPDATE_FLAG,CONFIG_UPDATE_FLAG_VALUE},		//更新标示 固定 判断配置文件指定值更新 建议自增1操作
    	{"socket.timeout.minutes", "30"},		//	Socket通信超时时间设置
    	{"common.zip.min.size", "1000"},		//  压缩文件最少包含文件个数
    	{"common.zip.max.size", "2000"},			//  压缩文件最多包含文件个数
//    	{"active.alarm.start","false"},    	//是否启用NC的主动告警（只针对监测数据超过设置的值时的主动告警，避免异常信息重复）：true：启用，false：停用-暂不使用，修改为web端控制nc是否报主动告警
    	{"alarm.set.marker.separator","|"}  //监测数据设置告警时，对于指定多个标识符（如多个盘符、多个CPU、多个网卡）的分隔符 
    };
    
    public Object[][] getContents() {
        return contents;
    }
}
