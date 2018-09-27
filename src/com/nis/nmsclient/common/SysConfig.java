package com.nis.nmsclient.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;


public class SysConfig {
	static Logger logger = Logger.getLogger(SysConfig.class);
	
	/** ==============myconfig.properties文件获取参数=============== **/
	private static Properties myProperties;
	private static String url = null;
	static {
		URL urlObj = SysConfig.class.getClassLoader().getResource("myconfig.properties");
		if(urlObj==null){
//			JOptionPane.showMessageDialog(null, "缺少配置文件，程序无法执行！\n请先执行参数配置程序进行配置", "错误", JOptionPane.ERROR_MESSAGE);
			JOptionPane.showMessageDialog(null, "i18n_client.Sysconfig.init_n81i", "i18n_client.Sysconfig.error_n81i", JOptionPane.ERROR_MESSAGE);
			logger.error("NMSClient program termination: lack of configuration files, programs can not be executed! Please execute the configuration program for configuration first");
			System.exit(0);
		}else{
			url = urlObj.getPath().replaceAll("%20", " ");
		}
		myProperties = new Properties();
		
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(url);
			myProperties.load(fis);
		} catch (IOException e) {
			logger.error("Reading myconfig.properties file error", e);
		}finally{
			try {
				if(fis!= null)fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// 升级时更新参数配置使用
		updateConfigFile();
	}
	
	 /**
	  * 向资源配置文件host_uuid.properties中更新UUID键值
	  */
	 public static void setInterfaceNameValue(String value) {
	  try {
	   Properties properties = new Properties();
	   if (uuidUrl != null && !"".equals(uuidUrl)) {
	    properties.load(new FileInputStream(uuidUrl));
	    // 添加或更新键值对
	    properties.setProperty(AGENT_INTERFACE_NAME_KEY, value);
	    // 保存到文件
	    properties.store(new FileOutputStream(uuidUrl), "");
	   }
	   properties.clear();
	   
	   if(value != null && !"".equals(value)){
	    Contants.AGENT_INTERFACE_NAME_KEY = value;
	   }
	  } catch (Exception e) {
	   logger.error("Setting the network port name eth* attribute error", e);
	  } 
	 }

	
	/**
	 * 根据相应的参数配置来更新配置并写入文件
	 */
	private static void updateConfigFile(){
		FileInputStream fis = null;
		BufferedReader reader = null;
		BufferedWriter writer = null;
		try {
			ResourceBundle resource = ResourceBundle.getBundle(UpdateParams.class.getName());
			//判断是否更新properties
			String updateFlag = myProperties.getProperty(UpdateParams.CONFIG_UPDATE_FLAG,"-1");
			if(updateFlag.equals(resource.getString(UpdateParams.CONFIG_UPDATE_FLAG))){	//配置文件已经更新，退出操作
				return;
			}
			
			List<String> proList = new LinkedList<String>();
			
			String encode = System.getProperty("file.encoding");
			logger.debug("----file.encoding----" + encode);
			
			//读取配置文件原有的参数到proList
			fis = new FileInputStream(url);
			reader = new BufferedReader(new InputStreamReader(fis,Charset.forName(encode)));
			String str =null;
			while((str = reader.readLine() )!=null){
				proList.add(str);
			}
			
			//将UpdateParams中的值更新到proList和myProperties中
			Enumeration<String> en = resource.getKeys();
			while (en.hasMoreElements()) {
				String elem = (String) en.nextElement();
				String value = resource.getString(elem);
				boolean addFlag = true;
				try {
					for (int i = 0; i < proList.size(); i++) {
						String strV = proList.get(i);
						if (StringUtils.isEmpty(strV)) {
							continue;
						}
						if(strV.split("=", 2)[0].trim().equals(elem)){
							if(elem.equalsIgnoreCase(UpdateParams.CONFIG_UPDATE_FLAG)){
								proList.set(i, elem + " = " + value);// 更新配置文件中某属性的值
								logger.info("参数更新：" + elem + " = " + (StringUtils.isBlank(value) ? "" : value));
								myProperties.put(elem, value); 
							}
							addFlag = false;
							break ;
						}
					}
					
					if(addFlag){
						proList.add(elem + " = " + value);
						logger.info("参数新增：" + elem + " = " + (StringUtils.isBlank(value) ? "" : value));
						myProperties.put(elem, value); 
					}
				} catch (Exception e) {
					logger.error("Update the configuration file myconfig.properties parameter " + elem + "error", e);
				}
			}
			
			//将文件信息写入到文件中
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(url),Charset.forName(encode)));
			Iterator<String> it = proList.iterator();
			while (it.hasNext()) {
				String elem = (String) it.next();
				writer.write((elem==null?"":elem)+"\r\n");
			}
			writer.flush();
			
		} catch (Exception e) {
			logger.error("Update configuration file myconfig.properties exception", e);
		} finally{
			try {
				if(reader!= null)reader.close();
				if(fis!= null)fis.close();
				if(writer!= null ){
					writer.close();
					writer = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void updateConfigFile(String key, String value){
		FileInputStream fis = null;
		BufferedReader reader = null;
		BufferedWriter writer = null;
		try {
			List<String> proList = new LinkedList<String>();
			
			String encode = System.getProperty("file.encoding");
			logger.debug("----file.encoding----" + encode);
			
			//读取配置文件原有的参数到proList
			fis = new FileInputStream(url);
			reader = new BufferedReader(new InputStreamReader(fis,Charset.forName(encode)));
			String str =null;
			while((str = reader.readLine() )!=null){
				proList.add(str);
			}
			
			//将值更新到proList中
			try {
				for (int i = 0; i < proList.size(); i++) {
					String strV = proList.get(i);
					if (StringUtils.isEmpty(strV)) {
						continue;
					}
					if(strV.split("=", 2)[0].trim().equals(key)){
						proList.set(i, key + " = " + value);// 更新配置文件中某属性的值
						logger.info("参数更新：" + key + " = " + (StringUtils.isBlank(value) ? "" : value));
						break ;
					}
				}
			} catch (Exception e) {
				logger.error("Update the configuration file myconfig.properties parameter " + key + "error", e);
			}
			
			//将文件信息写入到文件中
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(url),Charset.forName(encode)));
			Iterator<String> it = proList.iterator();
			while (it.hasNext()) {
				String elem = (String) it.next();
				writer.write((elem==null?"":elem)+"\r\n");
			}
			writer.flush();
			
		} catch (Exception e) {
			logger.error("Update configuration file myconfig.properties exception", e);
		} finally{
			try {
				if(reader!= null)reader.close();
				if(fis!= null)fis.close();
				if(writer!= null ){
					writer.close();
					writer = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static String getStringVal(String key, String... defaultVal){
		String dStr = "";
		if(defaultVal!=null && defaultVal.length>=1 && defaultVal[0]!=null && defaultVal.length>0){
			dStr = defaultVal[0];
		}
		return myProperties.getProperty(key,dStr).trim();
	}
	
	public static Integer getIntegerVal(String name, String... defaultVal){
		try {
			String val = getStringVal(name, defaultVal);
			return StringUtils.isEmpty(val)? null : Integer.parseInt(val);
		} catch (Exception e) {
			logger.error("Digital formatting error", e);
		}
		return null;
	}
	
	public static String getSystemDir() {
		return System.getProperty("user.dir");
	}

	public static String getLogPath(){
		FileInputStream fis = null;
		try {
			String log4jUrl = SysConfig.class.getClassLoader().getResource("log4j.properties").getPath().replaceAll("%20", " ");
			fis = new FileInputStream(log4jUrl);
			Properties log4jProperties = new Properties();
			log4jProperties.load(fis);
			String logFile = log4jProperties.getProperty("log4j.appender.logfile.File");
			if(logFile!=null){
				return new File(logFile).getParent();
			}
		} catch (IOException e) {
			logger.error("Reading log4j.properties file error"+"",e);
		}finally{
			try {
				if(fis!= null)fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return "../logs";
	}
	
	/** ==============host_uuid.properties文件获取参数=============== **/
	static final String AGENT_HOST_UUID_KEY = "agent_host_uuid";
	static final String AGENT_OPERATE_SYSTEM_KEY = "agent_operate_system";
	static final String AGENT_LOCAL_IP_KEY = "agent_local_ip";
	static String uuidUrl = null;
	static final String AGENT_INTERFACE_NAME_KEY = "agent_interface_name";
	static {
		String name = "host_uuid.properties";
		// 2014-11-13 jzz modify 由于将该配置文件放于系统安装目录中容易使节点混乱，故将其放于nmsdata指定目录
		/*URL urlObj = Contants.class.getClassLoader().getResource(name);
		if(urlObj==null){
			uuidUrl = new File(url).getParent() + File.separator + name;
		}else{
			uuidUrl = urlObj.getPath().replaceAll("%20", " ");
		}*/
		String path = SysConfig.getStringVal("local.data.path") + File.separator + "nc_sysconf";
		File filePath = new File(path);
		if(!filePath.exists()){
			filePath.mkdirs();
		}		
		
		uuidUrl = path + File.separator + name;
		
		Properties properties = new Properties();
		try {
			File file = new File(uuidUrl);
			if(!file.exists()){
				file.createNewFile();
			}
			properties.load(new FileInputStream(uuidUrl));
			String uuidStr = properties.getProperty(AGENT_HOST_UUID_KEY);
			if(uuidStr != null && !"".equals(uuidStr.trim())){
				Contants.AGENT_HOST_UUID = Long.parseLong(uuidStr.trim());
			}
			Contants.AGENT_OPERATE_SYSTEM = properties.getProperty(AGENT_OPERATE_SYSTEM_KEY);
			Contants.AGENT_LOCAL_IP = properties.getProperty(AGENT_LOCAL_IP_KEY);
			Contants.AGENT_INTERFACE_NAME_KEY = properties.getProperty(AGENT_INTERFACE_NAME_KEY);
		} catch (IOException e) {
			logger.error("Reading host_uuid.properties file error", e);
		}
		properties.clear();
	}
	
	/**
	 * 向资源配置文件host_uuid.properties中更新UUID键值
	 */
	public static void setUUIDValue(String value) {
		try {
			Properties properties = new Properties();
			if (uuidUrl != null && !"".equals(uuidUrl)) {
				properties.load(new FileInputStream(uuidUrl));
				// 添加或更新键值对
				properties.setProperty(AGENT_HOST_UUID_KEY, value);
				// 保存到文件
				properties.store(new FileOutputStream(uuidUrl), "");
			}
			properties.clear();
			
			if(value != null && !"".equals(value)){
				Contants.AGENT_HOST_UUID = Long.parseLong(value);
			}
		} catch (Exception e) {
			logger.error("Setting the UUID attribute error", e);
		} 
	}
	
	/**
	 * 向资源配置文件host_uuid.properties中更新OperateSystem和LocalIP键值
	 */
	public static void setUUIDValue(String sysTypeValue,String localIpValue) {
		try {
			Properties properties = new Properties();
			if (uuidUrl != null && !"".equals(uuidUrl)) {
				properties.load(new FileInputStream(uuidUrl));
				// 添加或更新键值对
				properties.setProperty(AGENT_OPERATE_SYSTEM_KEY, sysTypeValue);
				properties.setProperty(AGENT_LOCAL_IP_KEY, localIpValue);
				// 保存到文件
				properties.store(new FileOutputStream(uuidUrl), "");
			}
			properties.clear();
			Contants.AGENT_OPERATE_SYSTEM = sysTypeValue;
			Contants.AGENT_LOCAL_IP = localIpValue;
		} catch (Exception e) {
			logger.error("Setting OperateSystemType and LocalIP attribute errors",e);
		} 
	}
}
