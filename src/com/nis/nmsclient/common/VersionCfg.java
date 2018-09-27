package com.nis.nmsclient.common;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * 获取和保存version信息的类
 * 
 */
public class VersionCfg {
	private static Logger logger = Logger.getLogger(VersionCfg.class);
	public static final String NAGENT_VERSION = "NA_version";
	public static final String NSERVER_VERSION = "NS_version";
	
	private static String url = null;
	private static Properties properties;
	
	static {
		url = VersionCfg.class.getClassLoader().getResource("version.properties").getPath().replaceAll("%20", " ");
		
		FileInputStream fis = null;
		properties = new Properties();
		try {
			fis = new FileInputStream(url);
			properties.load(fis);
		} catch (IOException e) {
			logger.error("Reading version.properties file error", e);
		}finally{
			try{
				if(fis!=null){
					fis.close();
				}
			}catch (Exception e) {}
		}
	}

	/**
	 * 获取version值
	 * 
	 */
	public static String getValue(String key) {
		return properties.getProperty(key);
	}
	
	/**
	 * 向资源配置文件中添加或更新version键值对
	 */
	public static void setValue(String key, String value) {
		logger.debug("setVersion----->" + key + "="  + value);
		// 添加或更新键值对
		properties.setProperty(key, value);
		logger.debug("properties.getProperty(\"" + key + "\")----->" + value);
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(url);
			// 保存到文件
			if (url != null && !"".equals(url)) {
				properties.store(fos, "");
			}
		} catch (Exception e) {
			logger.error(e);
		} finally{
			try{
				if(fos!=null){
					fos.flush();
					fos.close();
				}
			}catch (Exception e) {}
		}
	}
}
