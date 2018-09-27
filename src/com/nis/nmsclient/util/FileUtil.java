package com.nis.nmsclient.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.nis.nmsclient.common.Contants;

/**
 * 文件操作类：检索、排序
 *
 */
public class FileUtil {
	static Logger logger = Logger.getLogger(FileUtil.class);

	/**
	 * 获取子目录列表
	 * @param file
	 * @return
	 */
	public static File[] getDirectoryArray(File file) {
		if (!file.isDirectory()) {
			new Exception(file.getAbsolutePath() + " Not A Directory");
		}
		return file.listFiles(new FilenameFilter() {
			// 使用匿名内部类重写accept方法;
			public boolean accept(File dir, String name) {
				if (new File(dir, name).isDirectory()) {
					return true;
				} else
					return false;
			}
		});
	}
	
	
	/**
	 * 获取指定全部或部分名称的文件
	 * @param file
	 * @param azz
	 * @return
	 */
	public static File[] getFilesArray(File file, final String azz) {
		if (!file.isDirectory()) {
			new Exception(file.getAbsolutePath() + " Not A Directory");
		}
		return file.listFiles(new FilenameFilter() {
			// 使用匿名内部类重写accept方法;
			public boolean accept(File dir, String name) {
				if (new File(dir, name).isDirectory()) {
					return false;
				}
				return name.indexOf(azz) != -1; // 过滤出指定全部名称或部分名称的文件
			}
		});
	}
	
	/**
	 * 获取指定前缀的文件
	 * @param file
	 * @param suffix
	 * @return
	 */
	public static File[] getFilesStartWith(File file, final String prefix) {
		if (!file.isDirectory()) {
			new Exception(file.getAbsolutePath() + " Not A Directory");
		}
		return file.listFiles(new FilenameFilter() {
			// 使用匿名内部类重写accept方法;
			public boolean accept(File dir, String name) {
				if (new File(dir, name).isDirectory()) {
					return false;
				}
				return name.startsWith(prefix); // 过滤出注定前缀名称的文件
			}
		});
	}
	
	/**
	 * 获取指定时间段指定前缀的文件
	 * @param file
	 * @param suffix
	 * @return
	 */
	public static File[] getFilesStartWithByMillis(File file, final String prefix, final long sTimeMillis, final long eTimeMillis) {
		if (!file.isDirectory()) {
			new Exception(file.getAbsolutePath() + " Not A Directory");
		}
		return file.listFiles(new FilenameFilter() {
			// 使用匿名内部类重写accept方法;
			public boolean accept(File dir, String name) {
				if (new File(dir, name).isDirectory()) {
					return false;
				}
				if (sTimeMillis > 0 && eTimeMillis > 0) {
					if (new File(dir, name).lastModified() < sTimeMillis
							|| new File(dir, name).lastModified() > eTimeMillis) {// 不在指定时间段
						return false;
					}
				}
				return name.startsWith(prefix); // 过滤出注定前缀名称的文件
			}
		});
	}
	
	/**
	 * 获取指定后缀名的文件
	 * @param file
	 * @param suffix
	 * @return
	 */
	public static File[] getFilesEndWith(File file, final String suffix) {
		if (!file.isDirectory()) {
			new Exception(file.getAbsolutePath() + " Not A Directory");
		}
		return file.listFiles(new FilenameFilter() {
			// 使用匿名内部类重写accept方法;
			public boolean accept(File dir, String name) {
				if (new File(dir, name).isDirectory()) {
					return false;
				}
				return name.endsWith(suffix); // 过滤出注定后缀名称的文件
			}
		});
	}
	
	/**
	 * 获取指定时间修改的指定后缀名的文件
	 * @param file
	 * @param suffix
	 * @return
	 */
	public static File[] getFilesEndWithBeforeMillis(File file, final String suffix, final long timeMillis) {
		if (!file.isDirectory()) {
			new Exception(file.getAbsolutePath() + " Not A Directory");
		}
		return file.listFiles(new FilenameFilter() {
			// 使用匿名内部类重写accept方法;
			public boolean accept(File dir, String name) {
				if (new File(dir, name).isDirectory() || new File(dir,name).lastModified() > timeMillis) {
					return false;
				}
				return name.endsWith(suffix); // 过滤出注定后缀名称的文件
			}
		});
	}
	
	/**
	 * 获取指定时间修改的文件
	 * @return
	 */
	public static File[] getFilesBeforeMillis(File file, final long timeMillis) {
		if (!file.isDirectory()) {
			new Exception(file.getAbsolutePath() + " Not A Directory");
		}
		return file.listFiles(new FilenameFilter() {
			// 使用匿名内部类重写accept方法;
			public boolean accept(File dir, String name) {
				if (new File(dir, name).isDirectory() || new File(dir,name).lastModified() > timeMillis) {
					return false;
				}
				return true;
			}
		});
	}
	
	/**
	 * 获取指定毫秒之后修改过的文件
	 * @param file
	 * @param timeMillis
	 * @return
	 */
	public static File[] getFilesAfterMillis(File file, final long timeMillis) {
		if (!file.isDirectory()) {
			new Exception(file.getAbsolutePath() + " Not A Directory");
		}
		return file.listFiles(new FilenameFilter() {
			// 使用匿名内部类重写accept方法;
			public boolean accept(File dir, String name) {
				if (new File(dir, name).lastModified() >= timeMillis) {
					return true;
				} else
					return false;
			}
		});
	}
	
	/**
	 * 获取指定时间名称及之前的文件夹
	 * @param file
	 * @param suffix
	 * @return
	 */
	public static File[] getDirsBeforeDateName(File file, final String yyyyMMddName) {
		if (!file.isDirectory()) {
			new Exception(file.getAbsolutePath() + " Not A Directory");
		}
		return file.listFiles(new FilenameFilter() {
			// 使用匿名内部类重写accept方法;
			public boolean accept(File dir, String name) {
				if (new File(dir, name).isDirectory() && name.compareTo(yyyyMMddName)<=0) {
					return true;
				}
				return false;
			}
		});
	}
	
	/**
	 * 获取指定时间名称及之后的文件夹
	 * @param file
	 * @param suffix
	 * @return
	 */
	public static File[] getDirsAfterDateName(File file, final String yyyyMMddName) {
		if (!file.isDirectory()) {
			new Exception(file.getAbsolutePath() + " Not A Directory");
		}
		return file.listFiles(new FilenameFilter() {
			// 使用匿名内部类重写accept方法;
			public boolean accept(File dir, String name) {
				if (new File(dir, name).isDirectory() && name.compareTo(yyyyMMddName)>=0) {
					return true;
				}
				return false;
			}
		});
	}

	/**
	 * 文件数组按最后修改日期排序，升序
	 * @param files
	 * @return
	 */
	public static File[] sortASCByModify(File[] files) {
		Arrays.sort(files, new Comparator<File>(){
			public int compare(File arg0, File arg1) {
				if(arg0.lastModified() > arg1.lastModified()){
					return 1;
				}else if (arg0.lastModified() < arg1.lastModified()){
					return -1;
				}else {
					return 0;
				}
			}
		});
		return files;
	}
	
	/**
	 * 文件数组按最后修改日期排序，降序
	 * @param files
	 * @return
	 */
	public static File[] sortDescByModify(File[] files) {
		Arrays.sort(files, new Comparator<File>(){
			public int compare(File arg0, File arg1) {
				if(arg0.lastModified() > arg1.lastModified()){
					return -1;
				}else if (arg0.lastModified() < arg1.lastModified()){
					return 1;
				}else {
					return 0;
				}
			}
		});
		return files;
	}
	
	/**
	 * 文件数组按名称排序，升序
	 * @param files
	 * @return
	 */
	public static File [] sortASCByFileName(File[] files) {
		Arrays.sort(files, new Comparator<File>(){
			public int compare(File arg0, File arg1) {
				return arg0.getName().compareTo(arg1.getName());
			}
		});
		return files;
	}
	/**
	 * 文件数组按名称排序，降序
	 * @param files
	 * @return
	 */
	public static File [] sortDescByFileName(File[] files) {
		Arrays.sort(files, new Comparator<File>(){
			public int compare(File arg0, File arg1) {
				return arg1.getName().compareTo(arg0.getName());
			}
		});
		return files;
	}

	/**
	 * 将文件移动到指定文件夹
	 * 
	 * @param f
	 *            源文件
	 * @param newDir
	 *            新目录
	 * @param flag
	 *            是否覆盖 true:覆盖 false:不覆盖
	 */
	public static void moveFile(File f, String newDir, boolean flag) {
		if (!f.exists()) {
			logger.debug("文件已不存在，无法移动！" + f.getAbsolutePath());
			return;
		}
		File fileDir = new File(newDir); // 备份目录
		if (!fileDir.exists()) { // 检查并创建新目录
			fileDir.mkdirs();
		}
		File dtnFile = new File(fileDir.getAbsolutePath() + File.separator
				+ f.getName()); // 制定目标文件路径以及文件名
		if (dtnFile.exists() && flag) { // 检查并删除已存在文件
			//dtnFile.delete_bak();
			//使用删除文件公共方法
			FileUtil.delDir(dtnFile);
			logger.debug("moveFile 删除已存在文件--" + dtnFile.getAbsolutePath());
			//FileUtil.checkParentDirExist(dtnFile);
		}
		try {
			FileUtils.copyFile(f, dtnFile); // 移动文件
			logger.debug("文件 From 》 " + f.getAbsolutePath()
					+ "\n    To 》 " + dtnFile.getAbsolutePath());
			if (f.exists()) {
				logger.debug("删除源文件" + f.getAbsolutePath());
				//f.delete_bak();
				//使用删除文件公共方法
				FileUtil.delDir(f);
				//FileUtil.checkParentDirExist(f);
			} else {
				logger.debug("源文件不存在！不用删了！" + f.getAbsolutePath());
				return;
			}
		} catch (IOException e) {
			logger.error(Utils.printExceptionStack(e));
		}
	}
	
	 /**
	 * 删除文件或文件夹
	 */
	public static void delDir(File file) {
		try {//文件在可删范围并且不在禁删范围
			if (file.exists() && isIncludeDelRange(file) && !isExcludeDelRange(file)) {
				if (!file.isDirectory()) {
					file.delete();
					logger.debug("FileUtil.delDir(File file)删除文件--" + file.getAbsolutePath());
					FileUtil.checkParentDirExist(file);
				} else if (file.isDirectory()) {
					delDir2(file);
				}
			}
		} catch (IOException e) {
			logger.error(Utils.printExceptionStack(e));
		}
	}
	
	 /**
	 * 递归删除文件夹及其下的所有文件
	 */
	private static void delDir2(File file) throws IOException {
		if (!file.exists()) {
			return;
		}
		if (!file.isDirectory()) {
			file.delete();
			logger.debug("FileUtil.delDir2(File file)删除文件--" + file.getAbsolutePath());
			FileUtil.checkParentDirExist(file);
		} else if (file.isDirectory()) {
			if (!isExcludeDelRange(file)) {// 文件不在禁删范围
				for (File f : file.listFiles()) {
					delDir2(f);
				}
				file.delete();
				logger.debug("FileUtil.delDir2(File file)删除文件夹--" + file.getAbsolutePath());
				FileUtil.checkParentDirExist(file);
			}
		}
	}
	
	/**
	 * 检查文件是否在可删范围内
	 * @param file
	 * @return true:是，false:否
	 */
	public static boolean isIncludeDelRange(File file) throws IOException{
		boolean flag = false;
		String path = file.getCanonicalPath();
		if(Contants.COMMON_DEL_PATH_INCLUDE!=null){
			for(String include : Contants.COMMON_DEL_PATH_INCLUDE){
				if(path.startsWith(include)){
					flag = true;
					break;
				}
			}
		}else{//删除范围参数为空，则返回在删除范围
			flag = true;
		}
		return flag;
	}
	
	/**
	 * 检查文件是否在禁删范围内
	 * @param file
	 * @return true:是，false:否
	 */
	public static boolean isExcludeDelRange(File file) throws IOException{
		boolean flag = false;
		String path = file.getCanonicalPath();
		if(Contants.COMMON_DEL_PATH_EXCLUDE!=null){
			for(String exclude : Contants.COMMON_DEL_PATH_EXCLUDE){
				if(path.startsWith(exclude)){
					flag = true;
					break;
				}
			}
		}else{//禁删参数为空，则返回不在禁删范围
			flag = false;
		}
		return flag;
	}
	
	public static void checkParentDirExist(File file){
		logger.debug("检查父目录是否存在---" +file.getParentFile().exists() + "---" + file.getParent());
	}
	
	/**
	 * 格式化处理路径
	 * @param path
	 * @return
	 */
	public static String handlerPath(String path){
		File file = new File(path);
		try {
			path = file.getCanonicalPath();
		} catch (IOException e) {
			logger.error(Utils.printExceptionStack(e));
		}
		
		return path;
	}
	
	public static File[] getAll(File dir, int level) {
		logger.info(getLevel(level) + dir.getName());

		level++;
		File[] files = dir.listFiles();
		for (int x = 0; x < files.length; x++) {
			if (files[x].isDirectory())
				getAll(files[x], level);
			else
				logger.debug(getLevel(level) + files[x].getName());
		}
		return files;
	}

	public static String getLevel(int level) {
		StringBuilder sb = new StringBuilder();
		sb.append("|--");

		for (int x = 0; x < level; x++) {
			sb.insert(0, "|  ");
		}
		return sb.toString();
	}
	
}
