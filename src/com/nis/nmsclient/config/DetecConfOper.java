package com.nis.nmsclient.config;

import java.io.File;

import org.apache.log4j.Logger;

import com.nis.nmsclient.common.Contants;
import com.nis.nmsclient.model.SetInfo;

public class DetecConfOper {
	static Logger logger = Logger.getLogger(DetecConfOper.class);
	
	public static File getConfigFile(File dir, String fileName) {
		File tmp = null;
		if (dir.isDirectory()) {
			for (File f : dir.listFiles()) {
				if (!f.isDirectory()) {
					if (f.getName().equals(fileName)) {
						tmp = f;
						break;
					}
				}
			}
		}
		return tmp;
	}

	public static String getFileName(String checkType, String processIden,
			String suffix) {
		String fileName = "";
		if (checkType != null && !"".equals(checkType)) { 
			fileName += checkType;
		}
		if (processIden != null && !"".equals(processIden)) {
			fileName += "_" + processIden;
		}
		if (suffix != null && !"".equals(suffix)) {
			fileName += suffix;
		}

		return fileName;
	}
	
	public static boolean isProcess(SetInfo setInfo){
		boolean flag = false;
		if("1".equals(setInfo.getIsSchedule())){//非预设监测
			flag = true;
		}else if(Contants.SYS_CHECK_TYPE_PROCESS.equalsIgnoreCase(setInfo.getCheckTypeName())){
			flag = true;
		}
		return flag;
	}
	
	public static String getProcess(SetInfo setInfo){
		return setInfo.getProcessPath();// + File.separator + setInfo.getProcessFile();
	}

}
