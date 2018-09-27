package com.nis.nmsclient.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.ice.tar.TarEntry;
import com.ice.tar.TarInputStream;
import com.ice.tar.TarOutputStream;
import com.nis.nmsclient.common.Contants;

/**
 * gzip文件压缩与解压缩，对文件tar归档与解归档工具类
 * 
 */
public class GzipUtil {
	static Logger logger = Logger.getLogger(GzipUtil.class);
	
	/**
	 * 使用命令对单个gz压缩包解压
	 * @param srcFileStr 需要解压的gz文件
	 * @param destDir 解压后的目标路径
	 * @param isAbs 解压文件时 true 按绝对路径解压， false 进入目标路径后解压
	 * @throws Exception
	 */
	public static void unGzipByCmd(String srcFileStr, String destDir, boolean isAbs) throws Exception{
		logger.debug("unGzip start……");
		BufferedReader bReader = null;
		BufferedReader errorReader = null;
		Process process = null;
		try {
			logger.debug("unGzipByCmd srcFile---" + srcFileStr);
			logger.debug("unGzipByCmd destDir---" + destDir);
			
			File srcFile = new File(srcFileStr);
			File descFile = new File(destDir);
			if(!srcFile.exists()){
				throw new Exception("source file not exist");
			}
			if(!srcFile.isFile()){
				throw new Exception("source file is not a file");
			}
			if(!descFile.exists()){
				descFile.mkdirs();
			}
			if(!descFile.isDirectory()){
				throw new Exception("compress destination path is not a directory");
			}
			String tarFile = srcFile.getParent() + File.separator +  srcFile.getName().substring(0, srcFile.getName().length()-3);
			StringBuffer sb = new StringBuffer();
			sb.append("gunzip " + srcFileStr);
			sb.append(";tar xvpPf " + tarFile);
			if(!isAbs){
				sb.append(" -C " + destDir);
			}else{
				sb.append(" -C /");
			}
			sb.append(";gzip " + tarFile);
			logger.debug("unGzipByCmd cmd--->" + sb.toString());
			
			String[] shellCmd = new String[] { "/bin/bash", "-c", sb.toString() };
			process = Runtime.getRuntime().exec(shellCmd);
			process.getOutputStream().close();

			bReader = new BufferedReader(new InputStreamReader(process
					.getInputStream()));
			errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			String line = null;
			while ((line = bReader.readLine()) != null) {
				logger.debug("unGzipByCmd--->" + line);
			}
			while ((line = errorReader.readLine()) != null) {
				logger.debug("unGzipByCmd error--->" + line);
				throw new IOException(line);
			}
			
			logger.debug("unGzipByCmd end……");
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				if (bReader != null) {
					bReader.close();
				}
				if (errorReader != null) {
					errorReader.close();
				}
				if (process != null) {
					process.destroy();
				}
			} catch (Exception e) {
			}
		}

	}
	
	/**
	 * 使用命令将单个文件或一个目录压缩成Gzip格式
	 * @param srcPath 需要压缩的文件目录或文件
	 * @param destFileStr 压缩后的目标文件
	 * @param excludes 排除的目录或文件(全是绝对路径)，不包含到压缩包的
	 * @param isAbs 压缩文件时 true 按绝对路径压缩（含有完成的路径压缩包）， false 进入目标路径后压缩
	 * @throws Exception
	 */
	public static void gzipByCmd(String srcPath, String destFileStr, String[] excludes, boolean isAbs) throws Exception{
		logger.debug("gzipByCmd start……");
		OutputStreamWriter fos = null;
		BufferedReader bReader = null;
		BufferedReader errorReader = null;
		Process process = null;
		File excludeFile = null;
		boolean flag = true;
		try {
			logger.debug("gzipByCmd srcPath---" + srcPath);
			logger.debug("gzipByCmd destFileStr---" + destFileStr);
			
			File srcFile = new File(srcPath);
			File descFile = new File(destFileStr);
			if(!srcFile.exists()){
				throw new Exception("decompress file not exist");
			}
			if(!descFile.getParentFile().exists()){
				descFile.getParentFile().mkdirs();
			}
			StringBuffer sb = new StringBuffer();
			if(!isAbs){
				if(srcFile.isFile()){
					sb.append("cd " + srcFile.getParentFile().getCanonicalPath() + ";");
				}else{
					sb.append("cd " + srcFile.getCanonicalPath() + ";");
				}
			}
			sb.append("tar -czvpPf " + destFileStr);
			if(excludes!=null && excludes.length>0){
				if(!isAbs){
					for (int i = 0; i < excludes.length; i++) {
						int index = excludes[i].indexOf(srcFile
								.getCanonicalPath());
						logger.debug("gzipByCmd index--->" + index + "---src=" +srcFile
								.getCanonicalPath());
						int start = index + srcFile.getCanonicalPath().length() + 1;
						logger.debug("gzipByCmd start--->" + start + "--length=" + excludes[i].length() +"---exclude=" + excludes[i]);
						if (index != -1 && start <= excludes[i].length()) {
							excludes[i] = excludes[i].substring(start,
									excludes[i].length());
							logger.debug("gzipByCmd exclude--->" + excludes[i]);
						}
					}
				}
				excludeFile = new File(Contants.localTempPath + File.separator + "excludelist_" + System.currentTimeMillis() + ".temp");
				// 2012-11-21 修改 由于使用FileWrUtil写的文件（有文件头的），Linux下无法正确识别
				fos = new OutputStreamWriter(
        				new FileOutputStream(excludeFile, true), System.getProperty("file.encoding"));
				logger.debug("gzipByCmd ---- " + System.getProperty("file.encoding"));
	        	for(String val : excludes){
	        		fos.write(val);
	        		fos.write("\n");
	        	}
				fos.flush();
				//FileWrUtil.cfgFilePrinter(excludeFile, Contants.charset, excludes);
				sb.append(" -X " + excludeFile.getCanonicalPath());
			}
			if(!isAbs){
				if(srcFile.isFile()){
					sb.append(" " + srcFile.getName());
				}else{
					sb.append(" *");
				}
			}else{
				sb.append(" " + srcFile.getCanonicalPath());
			}
			logger.debug("gzipByCmd cmd--->" + sb.toString());
			
			String[] shellCmd = new String[] { "/bin/bash", "-c", sb.toString() };
			process = Runtime.getRuntime().exec(shellCmd);
			process.getOutputStream().close();

			bReader = new BufferedReader(new InputStreamReader(process
					.getInputStream()));
			errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			String line = null;
			while ((line = bReader.readLine()) != null) {
				logger.debug("gzipByCmd--->" + line);
			}
			while ((line = errorReader.readLine()) != null) {
				logger.debug("gzipByCmd error--->" + line);
				flag = false;
				throw new IOException(line);
			}
			
			logger.debug("gzipByCmd end……");
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				if(fos!=null){
					fos.close();
				}
				if(excludeFile!=null && excludeFile.exists()){
					//excludeFile.delete_bak();
					//使用删除文件公共方法
					FileUtil.delDir(excludeFile);
					logger.debug("GzipUtil.gzipByCmd()--delete excludeFile=" + excludeFile.getAbsolutePath());
					//FileUtil.checkParentDirExist(excludeFile);
				}
				if (bReader != null) {
					bReader.close();
				}
				if (errorReader != null) {
					errorReader.close();
				}
				if (process != null) {
					process.destroy();
				}
				if(!flag){
					File destFile = new File(destFileStr);
					if(destFile.exists()){
						//destFile.delete_bak();
						//使用删除文件公共方法
						FileUtil.delDir(destFile);
						logger.debug("GzipUtil.gzipByCmd()--delete destFile=" + destFile.getAbsolutePath());
						//FileUtil.checkParentDirExist(destFile);
					}
				}
			} catch (Exception e) {
			}
		}

	}
	
	/**
	 * 使用命令对单个tar压缩包解压
	 * @param srcFileStr 需要解压的gz文件
	 * @param destDir 解压后的目标路径
	 * @param isAbs 解压文件时 true 按绝对路径解压， false 进入目标路径后解压
	 * @throws Exception
	 */
	public static void unTarByCmd(String srcFileStr, String destDir, boolean isAbs) throws Exception{
		logger.debug("unTarByCmd start……");
		BufferedReader bReader = null;
		BufferedReader errorReader = null;
		Process process = null;
		try {
			logger.debug("unTarByCmd srcFile---" + srcFileStr);
			logger.debug("unTarByCmd destDir---" + destDir);
			
			File srcFile = new File(srcFileStr);
			File descFile = new File(destDir);
			if(!srcFile.exists()){
				throw new Exception("source file not exist");
			}
			if(!srcFile.isFile()){
				throw new Exception("source file is not a file");
			}
			if(!descFile.exists()){
				descFile.mkdirs();
			}
			if(!descFile.isDirectory()){
				throw new Exception("compress destination path is not a directory");
			}
			StringBuffer sb = new StringBuffer();
			sb.append("tar xvpPf " + srcFileStr);
			if(!isAbs){
				sb.append(" -C " + destDir);
			}else{
				sb.append(" -C /");
			}
			logger.debug("unTarByCmd cmd--->" + sb.toString());
			
			String[] shellCmd = new String[] { "/bin/bash", "-c", sb.toString() };
			process = Runtime.getRuntime().exec(shellCmd);
			process.getOutputStream().close();

			bReader = new BufferedReader(new InputStreamReader(process
					.getInputStream()));
			errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			String line = null;
			while ((line = bReader.readLine()) != null) {
				logger.debug("unTarByCmd--->" + line);
			}
			while ((line = errorReader.readLine()) != null) {
				logger.debug("unTarByCmd--->" + line);
				throw new IOException(line);
			}
			
			logger.debug("unTarByCmd end……");
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				if (bReader != null) {
					bReader.close();
				}
				if (errorReader != null) {
					errorReader.close();
				}
				if (process != null) {
					process.destroy();
				}
			} catch (Exception e) {
			}
		}

	}
	
	/**
	 * 使用命令将单个文件或一个目录压缩成tar格式
	 * @param srcPath 需要压缩的文件目录或文件
	 * @param destFileStr 压缩后的目标文件
	 * @param excludes 排除的目录或文件(全是绝对路径)，不包含到压缩包的
	 * @param isAbs 压缩文件时 true 按绝对路径压缩（含有完成的路径压缩包）， false 进入目标路径后压缩
	 * @throws Exception
	 */
	public static void tarByCmd(String srcPath, String destFileStr, String[] excludes, boolean isAbs) throws Exception{
		logger.debug("tarByCmd start……");
		BufferedReader bReader = null;
		BufferedReader errorReader = null;
		Process process = null;
		File excludeFile = null;
		boolean flag = true;
		try {
			logger.debug("tarByCmd srcPath---" + srcPath);
			logger.debug("tarByCmd destFileStr---" + destFileStr);
			
			File srcFile = new File(srcPath);
			File descFile = new File(destFileStr);
			if(!srcFile.exists()){
				throw new Exception("decompress file not exist");
			}
			if(!descFile.getParentFile().exists()){
				descFile.getParentFile().mkdirs();
			}
			StringBuffer sb = new StringBuffer();
			if(!isAbs){
				if(srcFile.isFile()){
					sb.append("cd " + srcFile.getParentFile().getCanonicalPath() + ";");
				}else{
					sb.append("cd " + srcFile.getCanonicalPath() + ";");
				}
			}
			sb.append("tar -cvpPf " + destFileStr);
			if(excludes!=null && excludes.length>0){
				if(!isAbs){
					for (int i = 0; i < excludes.length; i++) {
						int index = excludes[i].indexOf(srcFile
								.getCanonicalPath());
						logger.debug("tarByCmd index--->" + index + "---src=" +srcFile
								.getCanonicalPath());
						int start = index + srcFile.getCanonicalPath().length() + 1;
						logger.debug("tarByCmd start--->" + start + "--length=" + excludes[i].length() +"---exclude=" + excludes[i]);
						if (index != -1 && start <= excludes[i].length()) {
							excludes[i] = excludes[i].substring(start,
									excludes[i].length());
							logger.debug("tarByCmd exclude--->" + excludes[i]);
						}
					}
				}
				excludeFile = new File(Contants.localTempPath + File.separator + "excludelist_" + System.currentTimeMillis() + ".temp");
				FileWrUtil.cfgFilePrinter(excludeFile, Contants.charset, excludes);
				sb.append(" -X " + excludeFile.getCanonicalPath());
			}
			if(!isAbs){
				if(srcFile.isFile()){
					sb.append(" " + srcFile.getName());
				}else{
					sb.append(" *");
				}
			}else{
				sb.append(" " + srcFile.getCanonicalPath());
			}
			logger.debug("tarByCmd cmd--->" + sb.toString());
			
			String[] shellCmd = new String[] { "/bin/bash", "-c", sb.toString() };
			process = Runtime.getRuntime().exec(shellCmd);
			process.getOutputStream().close();

			bReader = new BufferedReader(new InputStreamReader(process
					.getInputStream()));
			errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			String line = null;
			while ((line = bReader.readLine()) != null) {
				logger.debug("tarByCmd--->" + line);
			}
			while ((line = errorReader.readLine()) != null) {
				logger.debug("tarByCmd error--->" + line);
				flag = false;
				throw new IOException(line);
			}
			
			logger.debug("tarByCmd end……");
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				if(excludeFile!=null && excludeFile.exists()){
					//excludeFile.delete_bak();
					//使用删除文件公共方法
					FileUtil.delDir(excludeFile);
					logger.debug("GzipUtil.tarByCmd()--delete excludeFile=" + excludeFile.getAbsolutePath());
					//FileUtil.checkParentDirExist(excludeFile);
				}
				if (bReader != null) {
					bReader.close();
				}
				if (errorReader != null) {
					errorReader.close();
				}
				if (process != null) {
					process.destroy();
				}
				if(!flag){
					File destFile = new File(destFileStr);
					if(destFile.exists()){
						//destFile.delete_bak();
						//使用删除文件公共方法
						FileUtil.delDir(destFile);
						logger.debug("GzipUtil.tarByCmd()--delete destFile=" + destFile.getAbsolutePath());
						//FileUtil.checkParentDirExist(destFile);
					}
				}
			} catch (Exception e) {
			}
		}
	}
	
	/**
	 * 对单个gz压缩包解压
	 * @param srcFile 需要解压的gz文件
	 * @param destDir 解压后的目标路径
	 * @throws Exception
	 */
	public static void unGzip(String srcFile, String destDir) throws Exception{
		logger.debug("unGzip start……");
		OutputStream out = null;
		GZIPInputStream gis = null;
		File tempFile = new File(destDir + File.separator + System.currentTimeMillis() + ".tar");
		try {
			logger.debug("unGzip tempFile----" + tempFile.getAbsolutePath());
			logger.debug("unGzip srcFile---" + srcFile);
			logger.debug("unGzip destDir---" + destDir);
			gis = new GZIPInputStream(new FileInputStream(new File(srcFile)));
			
			if(!tempFile.getParentFile().exists()){
				tempFile.getParentFile().mkdirs();
			}
			out = new FileOutputStream(tempFile);
			logger.debug("unGzip tempFile---------" + tempFile.getAbsolutePath());
			byte[] buf = new byte[1024 * 2];
			int len = 0;
			while ((len = gis.read(buf)) != -1) {
				logger.debug("unGzip len---------" + len);
				out.write(buf, 0, len);
			}
			out.flush();
			
			//tar包解归档
			GzipUtil.unTar(tempFile.getAbsolutePath(), destDir);
			
			logger.debug("unGzip end……");
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				if(gis!=null) gis.close();
				if(out!=null) out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			logger.debug("tempFile.delete() start……" + tempFile.getAbsolutePath());
			if(tempFile.exists()){
				//tempFile.delete_bak();
				//使用删除文件公共方法
				FileUtil.delDir(tempFile);
				logger.debug("unGzip delete file --" + tempFile.getAbsolutePath());
				//FileUtil.checkParentDirExist(tempFile);
			}
			logger.debug("tempFile.delete() end……" + tempFile.getAbsolutePath());
		}

	}
	
	/**
	 * 将单个文件或一个目录压缩成Gzip格式
	 * 
	 * @param srcPath 需要压缩的文件目录或文件
	 * @param destFile 压缩后的目标文件
	 * @throws Exception
	 */
	public static void gzip(String srcPath, String destFile) throws Exception {  
		File tempFile = new File(new File(destFile).getParent() + File.separator + System.currentTimeMillis() + ".tar");
		GZIPOutputStream gzout = null;
		FileInputStream tarin = null;
		try{
			//tar包归档    
	        GzipUtil.tar(srcPath, tempFile.getAbsolutePath());
	        
			//建立gzip压缩输出流    
			gzout = new GZIPOutputStream(new FileOutputStream(destFile));
			//打开需压缩文件作为文件输入流    
			tarin = new FileInputStream(tempFile);    
			int len;
			byte[] buf = new byte[1024 * 2];
			while ((len = tarin.read(buf)) != -1) {
				gzout.write(buf, 0, len);
			}
			gzout.flush();
			
        }catch (Exception e) {
        	throw e;
        }finally {
        	if(tarin!=null) tarin.close();
        	if(gzout!=null) gzout.close();
        	if(tempFile.exists()){
				//tempFile.delete_bak();
        		//使用删除文件公共方法
				FileUtil.delDir(tempFile);
				logger.debug("GzipUtil.gzip()--delete tempFile=" + tempFile.getAbsolutePath());
				//FileUtil.checkParentDirExist(tempFile);
			}
        }
    } 
	
	
	/**
	 * 对单个tar压缩包解压，即解归档
	 * 
	 * @param srcFile 需要解压的tar文件
	 * @param outpath  解压后的目标路径
	 * @throws Exception
	 */
	public static void unTar(String srcFile, String outpath) throws Exception{
		logger.debug("unTar start……");
		TarInputStream tis = null;
		try {
			logger.debug("unTar srcFile---" + srcFile);
			logger.debug("unTar outpath---" + outpath);
			File file = new File(srcFile);
			if(!file.exists()){
//				 throw new Exception("解压源文件不存在: " + file.getAbsolutePath());
				 throw new Exception("Unzip source file does not exist: " + file.getAbsolutePath());
			}
			tis = new TarInputStream(new FileInputStream(file));
			/*关键在于这个TarEntry 的理解， 实际你的tar包里有多少文件就有多少TarEntry*/
			TarEntry tarEntry = null;
			while ((tarEntry = tis.getNextEntry()) != null) {
				logger.debug("unTar tarEntry---" + tarEntry);
				String tempFileName = (outpath + File.separator + tarEntry.getName()).replaceAll("\\\\", "/");
				logger.debug("unTar tempFileName---" + tempFileName);
				if (tarEntry.isDirectory()) {
					logger.debug("unTar tarEntry is Dir");
					int end = tempFileName.lastIndexOf("/");
					if (end != -1) {
						File dir = new File(tempFileName.substring(0, end));
						if (!dir.exists()) {
							dir.mkdirs();
						}
					}
				} else {
					logger.debug("unTar tarEntry is file");
					File tempFile = new File(tempFileName);
					if(!tempFile.getParentFile().exists()){
						tempFile.getParentFile().mkdirs();
					}
					tempFile.createNewFile();
					OutputStream out = new FileOutputStream(tempFile);
					IOUtils.copy(tis, out);
					out.close();
				}
			}
			logger.debug("unTar end……");
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				if(tis!=null) tis.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}
	
	/**
	 * 将单个文件或一个目录打成tar包，即归档
	 * 
	 * @param srcDir 需要压缩的文件目录或文件
	 * @param destFile 压缩后的目标文件
	 * @throws Exception
	 */
	public static void tar(String srcDir, String destFile) throws Exception {  
		TarOutputStream tout = null;
		try{
			//建立tar压缩输出流    
			tout = new TarOutputStream(new FileOutputStream(destFile));
	        File srcFile = new File(srcDir); 
	        if (!srcFile.exists()) {
//	            throw new Exception("压缩目录或文件不存在: " + srcFile.getAbsolutePath());
	            throw new Exception("Compressed directories or files do not exist: " + srcFile.getAbsolutePath());
	        }
	        if(srcFile.isDirectory()){
	        	tar(tout,srcFile,srcFile.getName()); 
	        }else{
	        	tar(tout,srcFile,"");
	        }
        }catch (Exception e) {
        	throw e;
        }finally {
        	if(tout!=null)  tout.close();
        }
    }  
	
     /**
      * tar压缩
      * @param out： tar压缩流
      * @param srcFile： 需要压缩的目录或文件
      * @param base： 需要压缩的文件在压缩后的tar包内的路径
      * @throws Exception
    */
    public static void tar(TarOutputStream tout, File srcFile, String base) throws Exception{
        if (!srcFile.exists()) {
//            throw new Exception("压缩目录或文件不存在: " + srcFile.getAbsolutePath());
            throw new Exception("Compressed directories or files do not exist: " + srcFile.getAbsolutePath());
        }
        if (srcFile.isDirectory()) {
            File[] files = srcFile.listFiles();
            base = base.length() == 0 ? "" : base + "/";
            if (base.length() > 0) {
                TarEntry tarEn = new TarEntry(srcFile); //此处必须使用new TarEntry(File file);    
				tarEn.setName(base); //此处需重置名称，默认是带全路径的，否则打包后会带全路径    
				tout.putNextEntry(tarEn);
            }
            for (int i = 0; i < files.length; i++) {
                tar(tout, files[i], base + files[i].getName());
            }
        } else {
            base = base.length() == 0 ? srcFile.getName() : base;
            TarEntry tarEn = new TarEntry(srcFile);    
			tarEn.setName(base); 
			tout.putNextEntry(tarEn);
            FileInputStream fis = null;
            try{
	            fis = new FileInputStream(srcFile);
	            IOUtils.copy(fis, tout);
            }catch (IOException e) {
				throw e;
			}finally{
				if(fis!=null) fis.close();
			}
        }
        tout.closeEntry();
    }
    
    
	public static void main(String[] args) {
		try {
			long tt = System.currentTimeMillis();
			//tar("D:\\temp\\test", "d:\\temp\\t1.tar");
			//unTar("D:\\temp\\t1.tar", "D:\temp\\t1");
			
			//gzip("D:\\temp\\t1\\gd-2.0.350000", "d:\\temp\\test\\t1.tar.gz");
			unGzip("d:\\temp\\nms.tar.gz", "D:\\temp\\t1");
			System.out.println(System.currentTimeMillis()-tt);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
 
