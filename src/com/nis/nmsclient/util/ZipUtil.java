package com.nis.nmsclient.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.apache.tools.zip.ZipOutputStream;

import com.nis.nmsclient.thread.socket.CommonSocket;
import com.nis.nmsclient.thread.socket.ServerCollectData;

public class ZipUtil {
	static Logger logger = Logger.getLogger(ZipUtil.class);
	/**
	 * java 解压缩单个zip压缩包 存在文件和目录的汉字处理问题
	 * 
	 * @param zipFileName 需要解压的压缩包文件
	 * @param destDir 解压缩后的目标路径
	 * @throws Exception 
	 */
	public static void unZip(String zipFileName, String destDir) throws Exception {
		logger.debug("unZip start……");
		ZipFile zipFile = null;
		try{
			zipFile = new ZipFile(zipFileName);
			Enumeration en = zipFile.getEntries();
			ZipEntry zipEntry = null;
			while (en.hasMoreElements()) {
				zipEntry = (ZipEntry) en.nextElement();
				String path = (destDir + File.separator + zipEntry.getName()).replaceAll("\\\\", "/");
				if (zipEntry.isDirectory()) {// 如果得到的是目录
					int end = path.lastIndexOf("/");
					if (end != -1) {
						File dir = new File(path.substring(0, end));
						if (!dir.exists()) {
							dir.mkdir();
						}
					}
				} else {
					InputStream in = null;
					FileOutputStream out = null;
					try{
						File f = new File(path);
						if(!f.getParentFile().exists()){
							f.getParentFile().mkdirs(); 
						}
		                f.createNewFile();  
						in = zipFile.getInputStream(zipEntry);
						out = new FileOutputStream(f);
						IOUtils.copy(in, out);
					}catch (Exception e) {
						throw e;
					}finally{
						if(in!=null) in.close();
						if(out!=null) out.close();
					}
				}
			}// while end
		} catch (Exception e) {
			throw e;
		}finally {
			if(zipFile != null){
				zipFile.close();
				zipFile = null;
			}
		}
		logger.debug("unZip end……");
	}
	
	/**
	 * 将单个文件或一个目录打成zip包
	 * 
	 * @param srcDir 需要压压缩的文件目录
	 * @param destFile 压缩后的目标文件
	 * @throws Exception 
	 */
	public static void zip(String srcDir, String destFile, String[] excludes) throws Exception {  
		ZipOutputStream out = null;
		try{
			out = new ZipOutputStream(new FileOutputStream(destFile));  
			//out.setEncoding("GBK");//解决linux乱码
	        File srcFile = new File(srcDir);
	        if (!srcFile.exists()) {
//	            throw new Exception("压缩目录或文件不存在: " + srcFile.getAbsolutePath());
	            throw new Exception("Compressed directories or files do not exist: " + srcFile.getAbsolutePath());
	        }
	        if(excludes!=null && excludes.length>0){
	        	for (int i=0; i<excludes.length; i++) {
	        		File excudeFile = new File(excludes[i]);
	        		excludes[i] = excudeFile.getCanonicalPath();
				}
	        }
			
	       /* if(srcFile.isDirectory()){
	        	zip(out,srcFile,srcFile.getName(), excludes); 
	        }else{
	        	zip(out,srcFile,"", excludes);
	        }*/
	        zip(out,srcFile,"", excludes);
        }catch (Exception e) {
        	throw e;
        }finally {
        	if(out!=null)  out.close();
        }
    }  
      
     /**
      * zip压缩
      * @param out： Zip压缩流
      * @param srcFile： 要压缩的目录或文件
      * @param base： 要压缩文件在zip包内的路径
      * @throws Exception
    */
    protected static void zip(ZipOutputStream out, File srcFile, String base, String[] excludes) throws Exception{
        if (!srcFile.exists()) {
//            throw new Exception("压缩目录或文件不存在: " + srcFile.getAbsolutePath());
            throw new Exception("Compressed directories or files do not exist: " + srcFile.getAbsolutePath());
        }
        
        if(excludes!=null && excludes.length>0){
    		for(String exclude : excludes){
    			if(exclude.equalsIgnoreCase(srcFile.getCanonicalPath())){
    				return;
    			}
    		}
    	}
        
        if (srcFile.isDirectory()) {
            File[] files = srcFile.listFiles();
            base = base.length() == 0 ? "" : base + "/";
            if (base.length() > 0) {
                out.putNextEntry(new ZipEntry(base));
               /* ZipEntry zipEntry = new ZipEntry(base);
				zipEntry.setUnixMode(755);// 解决linux乱码
				out.putNextEntry(zipEntry);*/
            }
            for (int i = 0; i < files.length; i++) {
                zip(out, files[i], base + files[i].getName(), excludes);
            }
        } else {
            base = base.length() == 0 ? srcFile.getName() : base;
            out.putNextEntry(new ZipEntry(base));
            /*ZipEntry zipEntry=new ZipEntry(base);
            zipEntry.setUnixMode(644);//解决linux乱码
            out.putNextEntry(zipEntry);*/
            FileInputStream fis = null;
            try{
	            fis = new FileInputStream(srcFile);
				IOUtils.copy(fis, out);
            }catch (IOException e) {
				throw e;
			}finally{
				if(fis!=null) fis.close();
			}
        }
        out.closeEntry();
    }
    
    /**
	 * 将单个文件或一个目录打成zip包，并将原文件删除
	 * 
	 * @param srcFiles 需要压压缩的文件列表
	 * @param destFile 压缩后的目标文件
	 * @param isAddPrefix 是否要添加文件前缀
	 * @throws Exception 
	 */
	public static void zipWithDelFile(File[] srcFiles, String destFile, boolean isAddPrefix) throws Exception {
		logger.debug("pass ZipUtil zipWithDelFile(File[] srcFiles, String destFile, boolean isAddPrefix):");
		if(srcFiles!=null){
			logger.debug("ZipUtil zipWithDelFile(File[] srcFiles, String destFile, boolean isAddPrefix) srcFiles:"+srcFiles.length);
		}
		ZipOutputStream out = null;
		try{
			out = new ZipOutputStream(new FileOutputStream(destFile));  
			//out.setEncoding("GBK");//解决linux乱码
	        if (srcFiles==null || srcFiles.length==0) {
//	            throw new Exception("压缩文件列表为空");
	            throw new Exception("The list of compressed files is empty");
	        }
	        for(File file : srcFiles){
	        	zip(out,file,"", true, isAddPrefix, null);
	        }
        }catch (Exception e) {
        	throw e;
        }finally {
        	if(out!=null)  out.close();
        }
    }
	
	/**
	 * 将单个文件或一个目录打成zip包，并将原文件删除或移动
	 * 
	 * @param srcFiles 需要压压缩的文件列表
	 * @param destFile 压缩后的目标文件
	 * @param isAddPrefix 是否要添加文件前缀
	 * @throws Exception 
	 */
	public static void zipWithMoveFile(File[] srcFiles, String destFile, boolean isAddPrefix) throws Exception {
		ZipOutputStream out = null;
		try{
			out = new ZipOutputStream(new FileOutputStream(destFile));  
			//out.setEncoding("GBK");//解决linux乱码
	        if (srcFiles==null || srcFiles.length==0) {
//	            throw new Exception("压缩文件列表为空");
	            throw new Exception("The list of compressed files is empty");
	        }
	        // 2013-03-22 由于DC再次获取未保存任务结果这个功能的实现，现修改将任务结果和回传文件压缩时不删除文件，而是将其移动到相应的日期目录
	        String dataType = null;
	        String destFileName = new File(destFile).getName();
	        if(destFileName.startsWith(CommonSocket.BP_TYPE_TASK_RESULT)){
	        	dataType = CommonSocket.BP_TYPE_TASK_RESULT;
	        }else if(destFileName.startsWith(CommonSocket.BP_TYPE_TASK_RETURN)){
	        	dataType = CommonSocket.BP_TYPE_TASK_RETURN;
	        }else if(destFileName.startsWith(CommonSocket.BP_TYPE_DETECT_DATA)){
	        	dataType = CommonSocket.BP_TYPE_DETECT_DATA;
	        }
	        logger.debug("zipWithMoveFile --- dataType = " + dataType);
	        for(File file : srcFiles){
	        	// 2013-5-6 针对监测数据打包文件个数的限制，即srcFiles传来的是一定个数的文件，不是所有监测的目录，所以压缩文件中的文件名要加上文件的父目录名
	        	if(CommonSocket.BP_TYPE_DETECT_DATA.equalsIgnoreCase(dataType) && file.isFile()){
	        		zip(out, file, file.getParentFile().getName() + "_" + file.getName(), true, isAddPrefix, dataType);
	        	}else{
	        		zip(out,file,"", true, isAddPrefix, dataType);
	        	}
	        }
        }catch (Exception e) {
        	throw e;
        }finally {
        	if(out!=null)  out.close();
        }
    }  
	
    /**
     * zip压缩
     * @param out： Zip压缩流
     * @param srcFile： 要压缩的文件或目录
     * @param base： 要压缩文件在zip包内的路径
     * @param isDel 是否删除压缩的文件, 若dataType为null，则直接删除，若dataType不为null, 则移动到指定目录
     * @param isAddPrefix 是否要添加文件前缀，true 如果是文件则将父文件夹的名称加到文件名的前缀，作为压缩后的文件名
     * @throws Exception
     */
    protected static void zip(ZipOutputStream out, File srcFile, String base, boolean isDel, boolean isAddPrefix, String dataType) throws Exception{
        if (!srcFile.exists()) {
//            throw new Exception("压缩目录或文件不存在: " + srcFile.getAbsolutePath());
            throw new Exception("Compressed directories or files do not exist: " + srcFile.getAbsolutePath());
        }
        logger.debug("pass ZipUtil zip");
		
        if (srcFile.isDirectory()) {
            File[] files = srcFile.listFiles();
            base = base.length() == 0 ? "" : base + "/";
            if (base.length() > 0) {
                out.putNextEntry(new ZipEntry(base));
               /* ZipEntry zipEntry = new ZipEntry(base);
				zipEntry.setUnixMode(755);// 解决linux乱码
				out.putNextEntry(zipEntry);*/
            }
            for (int i = 0; i < files.length; i++) {
            	String fileName = files[i].getName();
            	if(isAddPrefix){
            		fileName = files[i].getParentFile().getName() + "_" + files[i].getName();
            	}
                zip(out, files[i], base + fileName, isDel, isAddPrefix, dataType);
            }
        } else{
            base = base.length() == 0 ? srcFile.getName() : base;
            out.putNextEntry(new ZipEntry(base));
            /*ZipEntry zipEntry=new ZipEntry(base);
            zipEntry.setUnixMode(644);//解决linux乱码
            out.putNextEntry(zipEntry);*/
            FileInputStream fis = null;
            try{
	            fis = new FileInputStream(srcFile);
				IOUtils.copy(fis, out);
            }catch (IOException e) {
				throw e;
			}finally{
				if(fis!=null) fis.close();
			}
			if(srcFile.exists() && isDel){
				 // 2013-03-22 由于DC再次获取未保存任务结果这个功能的实现，现修改将任务结果和回传文件压缩时不删除文件，而是将其移动到相应的日期目录
				if(CommonSocket.BP_TYPE_TASK_RESULT.equalsIgnoreCase(dataType)){
					ServerCollectData.moveTaskResultToDateDir(srcFile);
				}else if(CommonSocket.BP_TYPE_TASK_RETURN.equalsIgnoreCase(dataType)){
					ServerCollectData.moveTaskReturnToDateDir(srcFile, null);
				}else if(CommonSocket.BP_TYPE_DETECT_DATA.equalsIgnoreCase(dataType)){
					ServerCollectData.moveDetecDataToDateDir(srcFile);
				}else {
					FileUtil.delDir(srcFile);
		            logger.debug("ZipUtil.zip()----delete srcFile=" + srcFile.getAbsolutePath());
				}
	        }
        }
        out.closeEntry();
    }

	public static void main(String[] args) {
		try {
			File f = new File("D:/work/create_tablespace");
			System.out.println(f.getPath());
			long tt = System.currentTimeMillis();
			/*String[] excludes = new String[3];
			excludes[0] = "D:\\temp\\t1\\test\\src";
			excludes[1] = "D:\\temp\\t1\\test\\WebRoot\\WEB-INF\\lib\\jdom.jar";
			excludes[2] = "D:\\temp\\t1\\test\\ziptest_code.zip";
			ZipUtil.zip("D:\\temp\\t1", "D:\\temp\\test\\t1.zip", excludes);
			//ZipUtil.unZip("D:\\temp\\test\\ziptest_code.zip", "D:\\temp");
			System.out.println(System.currentTimeMillis()-tt);*/
			ZipUtil.zipWithDelFile(new File("D:\\nmstest\\logs\\").listFiles(), "D:\\test\\nmsserver.log.zip", true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
