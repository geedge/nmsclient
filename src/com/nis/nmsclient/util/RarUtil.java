package com.nis.nmsclient.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import de.innosystec.unrar.Archive;
import de.innosystec.unrar.rarfile.FileHeader;

/**
 * 对rar进行解压缩
 * 
 */
public class RarUtil {
	static Logger logger = Logger.getLogger(RarUtil.class);
	/**
	 * 对单个rar压缩包解压
	 * @param rarFileName 需要解压的rar文件
	 * @param destDir 解压后的目标目录
	 * @throws Exception
	 */
	public static void unRar(String rarFileName, String destDir)
			throws Exception {
		logger.debug("unRar start……");
		Archive archive = null;
		FileHeader fh = null;
		try {
			logger.debug("unRar rarFileName---" + rarFileName);
			logger.debug("unRar destDir---" + destDir);
			File file = new File(rarFileName);
			if(!file.exists()){
//				 throw new Exception("解压源文件不存在: " + file.getAbsolutePath());
				 throw new Exception("Unzip source file does not exist: " + file.getAbsolutePath());
			}
			logger.debug("unRar: get archive……");
			archive = new Archive(file);
			logger.debug("unRar: archive =" + archive);
			if (archive != null) {
				logger.debug("unRar: while start");
				int index = 0;
				while ((fh = archive.nextFileHeader()) != null) {
					logger.debug("unRar: " + ++index + " start");
					String fileName = fh.getFileNameW().trim();
					if (!existZH(fileName)) {
						fileName = fh.getFileNameString().trim();
					}
					String path = (destDir + File.separator + fileName)
							.replaceAll("\\\\", "/");
					
					logger.debug("unRar: path---" + path);
					if (fh.isDirectory()) {
						//logger.debug("unRar: FileHeader is directory");
						int end = path.lastIndexOf("/");
						if (end != -1) {
							File dir = new File(path.substring(0, end));
							if (!dir.exists()) {
								dir.mkdirs();
							}
						}
					}else{
						//logger.debug("unRar: FileHeader is file");
						FileOutputStream os = null;
						try{
							File out = new File(path);
							if(!out.getParentFile().exists()){
								out.getParentFile().mkdirs();
							}
							os = new FileOutputStream(out);
							archive.extractFile(fh, os);
						}catch (Exception e) {
							throw e;
						}finally{
							fh = null;
							if (os != null) {
								os.close();
								os = null;
							}
						}
					}
					logger.debug("unRar: " + index + "  end");
				}
				logger.debug("unRar: while end");
			}
			logger.debug("unRar end……");
		} catch (Exception e) {
			throw e;
		} finally {
			fh = null;
			try {
				if (archive != null) {
					archive.close();
					archive = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}
	
	public static boolean existZH(String str) {
		String regEx = "[\\u4e00-\\u9fa5]";
		Pattern p = Pattern.compile(regEx);
		Matcher m = p.matcher(str);
		while (m.find()) {
			return true;
		}
		return false;
	}

	public static void main(String[] args) {
		try {
			unRar("D:\\temp\\logs.rar", "D:\\temp\\log");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
