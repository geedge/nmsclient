package com.nis.nmsclient.thread.timer;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.nis.nmsclient.common.Contants;
import com.nis.nmsclient.util.DateUtil;
import com.nis.nmsclient.util.FileUtil;
import com.nis.nmsclient.util.Utils;
import com.sun.org.apache.bcel.internal.generic.NEW;

public class DelLocalFileThread implements Runnable {
	Logger logger = Logger.getLogger(DelLocalFileThread.class);
	private String name;
	
	public DelLocalFileThread(String name){
		this.name = name;
	}
	
	@Override
	public void run() {
		Thread.currentThread().setName(name);
		delLogFile();
		delDataFile();
		delTempFile();
		delUpgradeFile();
		
	}

	/**
	 * 删除日志文件
	 */
	public void delLogFile() {
		long curDate = System.currentTimeMillis();
		// 删除指定天数之前的日志文件
		File logs = new File(Contants.localLogsPath);
		if (!logs.exists() || !logs.isDirectory()) {
			return;
		}
		for (File f : logs.listFiles()) {
			long diff = DateUtil.getDaysFromBeginToEnd(f.lastModified(),
					curDate);

			if (f.isFile() && diff > Contants.COMMON_DEL_LOG_DAYS) {
				//f.delete_bak();
				//使用删除文件公共方法
				FileUtil.delDir(f);
				logger.info("删除日志文件" + f.getAbsolutePath());
				//FileUtil.checkParentDirExist(f);
			}
		}

	}
	
	/**
	 * 清理数据文件(包括监测数据和任务文件)
	 */
	public void delDataFile(){
		long curDateMillis = System.currentTimeMillis();
		// ==  1、删除指定分钟之前的所有监测文件
		File doneFile = new File(Contants.localDataDonePath);
		if (doneFile.exists()) {
			//---删除打包上传的数据
			long timeMillis = curDateMillis - Contants.COMMON_DEL_DATA_HOURS
					* 60 * 60 * 1000;
			File[] files = FileUtil.getFilesEndWithBeforeMillis(doneFile, ".zip",
					timeMillis);
			if (files != null && files.length > 0) {
				for (File f : files) {
					FileUtil.delDir(f);
					logger.debug("delDataFile 删除打包文件--" + f.getAbsolutePath());
				}
			}
			//---删除正常上传的数据文件
			File[] doneDirs = FileUtil.getDirectoryArray(doneFile);
			delDataFileCallBack(curDateMillis, doneDirs, Contants.COMMON_DEL_DATA_HOURS);
		}
		File errorFile = new File(Contants.localDataErrorPath);
		if (errorFile.exists()) {
			File[] errorDirs = FileUtil.getDirectoryArray(errorFile);
			delDataFileCallBack(curDateMillis, errorDirs, Contants.COMMON_DEL_DATA_HOURS);
		}
		
		// ==  2、删除指定分钟之前的所有任务相关的文件
		doneFile = new File(Contants.localTaskDonePath);
		if (doneFile.exists()) {
			//---删除打包上传的任务结果和回传文件
			long timeMillis = curDateMillis - Contants.COMMON_DEL_TASK_HOURS
					* 60 * 60 * 1000;
			File[] files = FileUtil.getFilesEndWithBeforeMillis(doneFile, ".zip",
					timeMillis);
			if (files != null && files.length > 0) {
				for (File f : files) {
					FileUtil.delDir(f);
					logger.debug("delDataFile 删除打包文件2--" + f.getAbsolutePath());
				}
			}
			//---删除正常上传的任务结果和回传文件
			File[] doneDirs = FileUtil.getDirectoryArray(doneFile);
			delDataFileCallBack(curDateMillis, doneDirs, Contants.COMMON_DEL_TASK_HOURS);
		}
		errorFile = new File(Contants.localTaskErrorPath);
		if (errorFile.exists()) {
			File[] errorDirs = FileUtil.getDirectoryArray(errorFile);
			delDataFileCallBack(curDateMillis, errorDirs, Contants.COMMON_DEL_TASK_HOURS);
		}
	}
	
	/**
	 * 清理数据文件的递归回调函数
	 */
	public void delDataFileCallBack(long curDateMillis, File[] dirs, int delHours) {
		if (dirs == null || dirs.length == 0) {
			return;
		}
		for (File dir : dirs) {
			long timeMillis = curDateMillis - delHours
					* 60 * 60 * 1000;
			File[] sonDirs = FileUtil.getDirsBeforeDateName(dir, DateUtil
					.getStingDate(DateUtil.YYYYMMDD, new Date(timeMillis)));
			delDataFileCallBack(curDateMillis, sonDirs, delHours);

			File[] files = FileUtil.getFilesBeforeMillis(dir,timeMillis);
			if (files != null && files.length > 0) {
				for (File f : files) {
					FileUtil.delDir(f);
					logger.debug("删除文件--" + f.getAbsolutePath());
				}
			}

			if (files == null && files.length > 0) {
				logger.info("删除文件夹" + dir.getAbsolutePath() + "下的文件：文件"
						+ files.length + "个");
			}

			if (dir.exists() && dir.listFiles().length <= 0) {
				FileUtil.delDir(dir);
				logger.info("删除文件夹" + dir.getAbsolutePath());
			}
		}
	}
	
	/**
	 * 清理临时目录的文件
	 */
	public void delTempFile() {
		try {
			long curDate = System.currentTimeMillis();
			// 删除指定日期之前的临时文件
			File temp = new File(Contants.localTempPath);
			if (!temp.exists() || !temp.isDirectory()) {
				return;
			}
			for (File f : temp.listFiles()) {
				long diff = DateUtil.getDaysFromBeginToEnd(f.lastModified(),
						curDate);
				if (f.exists() && diff > Contants.COMMON_DEL_TEMP_DAYS) {
					//使用删除文件公共方法
					FileUtil.delDir(f);
					logger.info("删除临时文件或文件夹" + f.getAbsolutePath());
				}
			}
		} catch (Exception e) {
			logger.error(Utils.printExceptionStack(e));
		}
	}
	
	/**
	 * 清理UPLOAD目录下的文件
	 */
	public void delUpgradeFile() {
		try {
			long curDate = System.currentTimeMillis();
			// 删除指定日期之前的升级文件
			File uploads = new File(Contants.localUploadsPath);
			if (!uploads.exists() || !uploads.isDirectory()) {
				return;
			}
			for (File f : uploads.listFiles()) {
				long diff = DateUtil.getDaysFromBeginToEnd(f.lastModified(),
						curDate);
				if (f.exists() && diff > Contants.COMMON_DEL_UPGRADEFILE_DAYS) {
					//使用删除文件公共方法
					FileUtil.delDir(f);
					logger.info("删除升级文件或文件夹" + f.getAbsolutePath());
				}
			}
		} catch (Exception e) {
			logger.error(Utils.printExceptionStack(e));
		}

	}
}
