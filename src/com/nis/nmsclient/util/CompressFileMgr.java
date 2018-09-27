package com.nis.nmsclient.util;

import java.io.File;

import org.apache.log4j.Logger;

import com.nis.nmsclient.util.GzipUtil;
import com.nis.nmsclient.util.RarUtil;
import com.nis.nmsclient.util.ZipUtil;

/**
 * 文件解压缩的统一处理
 * @date Mar 20, 2012
 * @version
 */
public class CompressFileMgr {
	static Logger logger = Logger.getLogger(CompressFileMgr.class);
	
	/**
	 * 文件压缩
	 * @param sourcePath 源文件或目录
	 * @param destFile 压缩后的文件
	 * @param excludes 源目录中不要压缩的文件列表
	 * @param isAbs 相对路径还是绝对路径压缩
	 * @return
	 * @throws Exception
	 */
	public boolean compressFile(String sourcePath, String destFile,
			String[] excludes, boolean isAbs) throws Exception {
		boolean returnFlag = true;
		File folder = new File(sourcePath);
		if (!folder.exists()) {
			return false;
		}
		if (destFile.length() == (destFile.lastIndexOf(".zip") + 4)) {// 判断是否zip包
			ZipUtil.zip(sourcePath, destFile, excludes);
		} else if (destFile.length() == (destFile.lastIndexOf(".gz") + 3)) {// 判断是否gzip包
			GzipUtil.gzipByCmd(sourcePath, destFile, excludes, isAbs);
		}  else if (destFile.length() == (destFile.lastIndexOf(".tar") + 4)) {// 判断是否tar包
			GzipUtil.tarByCmd(sourcePath, destFile, excludes, isAbs);
		} else {
			throw new Exception("unable to compress this compressed file");
		}

		return returnFlag;
	}

	/**
	 * 文件解压，如果sourcePath是一个目录，则解压它下面的所有压缩文件
	 * @param sourcePath 需要解压的文件或需要解压文件的目录
	 * @param destPath 解压到的目标路径
	 * @param isAbs 相对路径还是绝对路径解压
	 * @return
	 * @throws Exception
	 */
	public boolean decompressFile(String sourcePath, String destPath, boolean isAbs)
			throws Exception {
		logger.debug("decompressFile start……");
		boolean returnFlag = true;
		logger.debug("decompressFile sourcePath-----" + sourcePath);
		logger.debug("decompressFile destPath-----" + destPath);
		File folder = new File(sourcePath);
		if (!folder.exists()) {
			return false;
		}
		if (folder.isDirectory()) {
			String files[] = folder.list();
			String fileAbs = null;
			for (int i = 0; i < files.length; i++) {
				fileAbs = sourcePath + File.separator + files[i];
				decompressFile(fileAbs, destPath, isAbs);
			}
		} else {
			decompress(sourcePath, destPath, isAbs);
		}

		logger.debug("decompressFile end!");
		return returnFlag;
	}
	
	/**
	 * 解压文件，只针对单个压缩文件
	 * @param sourceFile 需要解压的文件
	 * @param destPath 解压到的目标路径
	 * @param isAbs 相对路径还是绝对路径解压
	 * @throws Exception
	 */
	private void decompress(String sourceFile, String destPath, boolean isAbs)
			throws Exception {
		logger.debug("decompress start……");
		logger.debug("decompress sourceFile---" + sourceFile);
		logger.debug("decompress 1-----"+(sourceFile.length() == (sourceFile.lastIndexOf(".zip") + 4)));
		logger.debug("decompress 2-----"+(sourceFile.length() == (sourceFile.lastIndexOf(".rar") + 4)));
		logger.debug("decompress 3-----"+(sourceFile.length() == (sourceFile.lastIndexOf(".gz") + 3)));
		if (sourceFile.length() == (sourceFile.lastIndexOf(".zip") + 4)) {// 判断是否zip包
			 ZipUtil.unZip(sourceFile, destPath);
		} else if (sourceFile.length() == (sourceFile.lastIndexOf(".rar") + 4)) {// 判断是否rar包
			RarUtil.unRar(sourceFile, destPath);
		} else if (sourceFile.length() == (sourceFile.lastIndexOf(".gz") + 3)) {// 判断是否gzip包
			GzipUtil.unGzipByCmd(sourceFile, destPath, isAbs);
		}  else if (sourceFile.length() == (sourceFile.lastIndexOf(".tar") + 4)) {// 判断是否tar包
			GzipUtil.unTarByCmd(sourceFile, destPath, isAbs);
		} else {
			throw new Exception("unable to decompress this decompressed file");
		}
		logger.debug("decompress end!");
	}
	
	public static String getCompressSuffixByOs(boolean isAbs){
		String suffix = ".zip";
		//根据操作系统确定获取进程ID的方式
		String os = System.getProperty("os.name");
		if (os.startsWith("Windows")) {
		     suffix = ".zip";
		}else if (os.startsWith("Linux")){
			suffix = (isAbs ? "_absolute" : "_relative") + ".tar.gz";
		} 
		return suffix;
	}
	
	public static boolean isCompressFile(File file){
		boolean flag = false;
		if(file.isFile()){
			String fileName = file.getName();
			if (".rar".equalsIgnoreCase(fileName.substring(fileName.length() - 4))
					|| ".zip".equalsIgnoreCase(fileName.substring(fileName.length() - 4))
					|| ".tar".equalsIgnoreCase(fileName.substring(fileName.length() - 4))
					|| ".gz".equalsIgnoreCase(fileName.substring(fileName.length() - 3))) {
				flag = true;
			}
		}
		return flag;

	}
}
