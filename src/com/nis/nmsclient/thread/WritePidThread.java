package com.nis.nmsclient.thread;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import com.nis.nmsclient.common.Contants;

public class WritePidThread implements Runnable{

	@Override
	public void run() {
		String SYSTEM_PATH = System.getProperty("user.dir");
//		Thread.currentThread().setName("写PID线程");
		Thread.currentThread().setName("Write The PID Thread");
		Logger logger = Logger.getLogger(WritePidThread.class);
		
		/* 获取程序运行PID */
		String path = Contants.localAgentPidFile;//2015-11-25:之前一直写的是"NMSClientPid.temp"，不对配置的是agentPid.temp
		String name = ManagementFactory.getRuntimeMXBean().getName();
		logger.info("当前程序PID:>"+(name.split("@")[0]));
		
		/* 判断系统类型是否写文件 */
		String os =  System.getProperty("os.name");
		if(os!=null && !os.toLowerCase().startsWith("windows")){
			logger.info("非Windows系统 结束执行");
			return ;
		}
		/* 获取输出文件并检查文件路径是否存在 */
		File file = new File(path);
		if(!file.getParentFile().exists()){
			file.getParentFile().mkdirs();
		}
		
		/* 将PID写入文件 */
		FileWriter writer = null;
		try {
			writer = new FileWriter(file);
			writer.write(name.split("@")[0]);
			writer.flush();
			logger.info("写PID完成");
		} catch (IOException e) {
			logger.error("Write PID failure", e);
		}finally{
			try {
				if(writer!=null) 
					writer.close();
				writer = null;
			} catch (IOException e) {
				logger.error("", e);
			}
			logger.info("线程关闭");
		}
	}
	public static void main(String [] args) {
		new Thread(new WritePidThread()).start();
	}
	public static void pl (Object obj) {
		System.out.println(obj==null?null:obj.toString());
	}

}
