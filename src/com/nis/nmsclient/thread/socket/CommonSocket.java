package com.nis.nmsclient.thread.socket;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.nis.nmsclient.common.Contants;
import com.nis.nmsclient.util.FileUtil;
import com.nis.nmsclient.util.MD5Util;
import com.nis.nmsclient.util.Utils;
import com.nis.nmsclient.util.file.BufferedRandomAccessFile;
import com.socket.utils.FileComment;

public class CommonSocket{
	static Logger logger = Logger.getLogger(CommonSocket.class);
	protected static final String TEMP_SUFFIX = ".tp"; 
	//缓存字节长度
	protected static final int BUFF_SIZE = 1024;
	public static final String SUCCESS = "success";
	public static final String FAIL = "fail";
	public static final String END = "end";
	/**
	 * 与Server握手请求
	 */
	public static final String REQ_HAND_SHAKE = "char:handshake";
	/**
	 * 获取本机标志UUID请求
	 */
	public static final String REQ_LOCAL_UUID = "char:uuid";
	/**
	 * 发送本机变更信息请求
	 */
	public static final String REQ_LOCAL_CHANGE = "char:agentChange";
	/**
	 * 初始化配置请求
	 */
	public static final String REQ_INIT_CONFIG = "char:init";
	/**
	 * 初始化任务请求
	 */
	public static final String REQ_INIT_TASK = "char:initTask";
	/**
	 * 主动告警请求
	 */
	public static final String REQ_ALARM = "char:alarm";
	/**
	 * 获取Server端系统时间请求
	 */
	public static final String REQ_SERVER_SYSTEMDATE = "char:systemdate";
	/**
	 * Server升级请求
	 */
	public static final String REQ_SERVER_UPGRADE = "char:upgradeServer";
	/**
	 * NC向DC发送错误信息
	 */
	public static final String REQ_ERROR_INFO = "char:ncErrorInfo";
	// ========== 现由DC主动获取数据，以下命令暂留
	/**
	 * 批量上传数据文件请求
	 */
	public static final String REQ_UPLOAD_DATAS ="byte:datas";
	/**
	 * 回传文件请求
	 */
	public static final String REQ_TASK_RETURNFILE = "byte:taskReturn";
	// ========== 现由DC主动获取数据，以上命令暂留
	/**
	 * 发送任务结果请求【数据收集方式改为DC主动后，此请求只在NC启动时发送所有任务结果使用】
	 */
	public static final String REQ_TASK_RESULT = "char:taskResult";
	/**
	 * 上传回传文件、任务结果文件、数据文件的打包文件请求【数据收集方式改为DC主动后，此请求只在NC启动时发送所有任务结果使用】
	 */
	public static final String REQ_BP_UPLOAD_FIFE = "byte:bpUploadFile";
	//bpUploadFile的子命令类型
	public static final String BP_TYPE_TASK_RESULT = "taskresult";
	public static final String BP_TYPE_TASK_RETURN = "taskreturn";
	public static final String BP_TYPE_DETECT_DATA = "detectdata";
	/**
	 * 交换证书通信命令
	 */
	public static final String REQ_CERT = "byte:cert";
	/**
	 * 更新监测设置信息通信命令
	 */
	public static final String SERVER_UPDATE_CONFIG = "char:updateConfig";
	/**
	 * 下发第三方监测脚本命令
	 */
	public static final String SEND_PLUGIN_SCRIPT_FILE = "char:sendPluginScriptFile";
	/**
	 * NC端是否报主动告警 变更
	 */
	public static final String ACTIVE_ALARM_START_ALERT = "char:isActiveAlarmStart";
	/**
	 * 文件推送通信命令
	 */
	public static final String SERVER_FILE_PUSH = "byte:filePush";
	/**
	 * 升级通信命令
	 */
	public static final String SERVER_UPGRADE = "byte:upgrade";
	/**
	 * 下发任务通信命令
	 */
	public static final String SERVER_TASK = "char:task";
	/**
	 * 任务撤消命令
	 */
	public static final String SERVER_TASK_CANCEL = "char:taskCancel";
	/**
	 * DC主动向NC再次获取任务结果
	 */
	public static final String SERVER_GET_TASKRESULT = "char:collectNonRltTaskResult";
	/**
	 * DC主动向NC收集监测数据
	 */
	public static final String SERVER_COLLECT_DATA = "byte:collectData";
	//收集数据，Agent发送的类型
	public static final String DATA_TYPE_ZIP_DETECT = "zipDetectData";//监测数据zip
	public static final String DATA_TYPE_CSV_DETECT = "csvDetectData";//批量上传csv监测数据
	public static final String DATA_TYPE_ZIP_TASKRESULT = "zipTaskResult";//任务结果zip
	public static final String DATA_TYPE_OBJ_TASKRESULT = "objTaskResult";//批量上传任务结果obj
	public static final String DATA_TYPE_ZIP_TASKRETURN = "zipTaskReturn";//任务回传文件zip
	public static final String DATA_TYPE_FILE_TASKETURN = "fileTaskReturn";//单个任务回传文件
	
	protected Socket socket = null;
	protected OutputStream out = null;
	protected InputStream in = null;

	public CommonSocket() {
		super();
	}

    public CommonSocket(Socket client) throws Exception {
    	socket = client;
		out = socket.getOutputStream();
		in = socket.getInputStream();
    }
	
	/**
	 * 发送消息，以字符形式，发送一行信息
	 **/
	public boolean sendMessageByChar(String msg) throws Exception {
		logger.debug("sendMessageByChar---" + msg);
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(out,
				Contants.charset));
		pw.println(msg);
		pw.flush();

		return true;
	}
	
	
	/**
	 * 接收信息，以字符形式，接收一行信息
	 * 
	 */
	public String receiveMessageByChar() throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(in,
				Contants.charset));
		String str = br.readLine();
		logger.debug("receiveMessageByChar---" + str);
		return str;
	}
	
	/**
	 * 发送单个文件
	 **/
	public boolean sendFileByByte(File file) throws Exception {
		ObjectOutputStream oos = null;
		FileInputStream fis = null;
		try {
			//发送文件大小和文件名
			oos = new ObjectOutputStream(out);
			String[] strArr = new String[]{
				file.length() + "", file.getName()
			};
			oos.writeObject(strArr);
			//发送文件内容
			byte[] buff = new byte[BUFF_SIZE];
			int len = 0;
			fis = new FileInputStream(file);
			while ((len = fis.read(buff)) != -1) {
				//将读取的内容写入文件
				out.write(buff, 0, len);
			}
			out.flush();
		} catch (Exception e) {
			logger.error("Single file sending failure");
			throw e;
		} finally{
			if(fis!=null){
				try {
					fis.close();
				} catch (IOException e) {
					logger.error(Utils.printExceptionStack(e));
				}
			}
		}
		
		return true;
	}
	
	/**
	 * 接收单个文件
	 * 
	 */
	public boolean receiveFileByByte(String filePath) throws Exception {
		ObjectInputStream ois = null;
		FileOutputStream fos = null;
		try {
			
			ois = new ObjectInputStream(in);
			String[] strArr = (String[])ois.readObject();
			//接收文件大小
			long fileSize = Long.parseLong(strArr[0]);
			//接收文件名
			String fileName = strArr[1];
			
			//接收文件内容 
			byte[] buff = new byte[BUFF_SIZE];
			fos = new FileOutputStream(filePath + File.separator + fileName);
			int nRead = 0;			
			
			//单个文件循环读取
			while ((nRead = in.read(buff, 0, (int)(BUFF_SIZE<fileSize?BUFF_SIZE:fileSize))) > 0) {
				fos.write(buff,0,nRead);
				fos.flush();
				fileSize -= nRead;
				if(fileSize<=0){
					break;
				}
			}
			fos.close();
		} catch (Exception e) {
			logger.error("Single file receiving failure");
			throw e;
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (IOException e) {
				logger.error(Utils.printExceptionStack(e));
			}

		}
		
		return true;

	}
	
	/**
	 * 批量上传文件
	 * @param dir	本地文件集合根目录绝对路径
	 * @param fileList	上传的文件列表
	 */
	public void sendFileByBath(String dir, List<File> fileList) throws Exception {
		ObjectOutputStream oos = null;
		FileInputStream fis = null;

		try {
			// 第一步发送本地根目录地址（用于地址截取）保证fileList的目录结构完整性
			this.sendMessageByChar(dir);
			String result = this.receiveMessageByChar();
			logger.debug("根目录地址发送状态: " + result);
			// 第二步 用ObjectOutputStream工具类 发送file对象信息 用于文件名，文件目录，文件大小的获取
			oos = new ObjectOutputStream(out);
			List<String[]> fileStrList = new ArrayList<String[]>(); 
			if(fileList!=null && fileList.size()>0){
				for (File f : fileList) {
					if (f.exists()) {
						String[] tmpArr = new String[] { f.getAbsolutePath(),
								f.length() + "" };
						fileStrList.add(tmpArr);
					} else {
						logger.warn("File:>" + f.getAbsolutePath()
								+ " do not exist, can not send");
					}
				}
				oos.writeObject(fileStrList);
				// 第三部，发送文件
				byte[] buff = new byte[BUFF_SIZE];
				int len = 0;
				// 循环上传文件
				for (File file : fileList) {
					logger.debug("--sendFileByBath---" + file.getName() + "---length=" + file.length());
					fis = new FileInputStream(file);
					while ((len = fis.read(buff)) != -1) {// 将读取的内容输出流
						out.write(buff, 0, len);
					}
					out.flush();
					fis.close();
					fis = null;
				}
			}
			logger.debug("批量发送文件个数：" + (fileList==null ? 0 : fileList.size()));
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				if (fis != null) {
					fis.close();
					fis = null;
				}
			} catch (IOException e) {
			}
		}
	}

	/**
	 * 批量接收文件
	 * @param newDir
	 */
	public boolean receiveFileByBath(String newDir) throws Exception {
		ObjectInputStream ois = null;
		FileOutputStream fos = null;
		try {
			//获取集合文件路径
			String oldDir = this.receiveMessageByChar();
			//logger.info("旧上传文件集合根目录: " + oldDir);
			this.sendMessageByChar("success");
			ois = new ObjectInputStream(in);
			List<String[]> fileList = (List<String[]>)ois.readObject();
			if(fileList != null && fileList.size()>0){
				for(String[] arr : fileList){
					String newUrl = arr[0].replace(oldDir, newDir);//新路径
					newUrl = newUrl.replaceAll("\\\\", "/");
					int fileLength = Integer.parseInt(arr[1]);		//大小
					File newFile = new File(newUrl);
					if(newFile.exists()){
						FileUtil.delDir(newFile);
						logger.debug("receiveFileByBath delete file---" + newFile.getAbsolutePath());
					}
					if(!newFile.getParentFile().exists()){
						newFile.getParentFile().mkdirs();
					}
					fos = new FileOutputStream(newUrl+TEMP_SUFFIX);
					
					int nRead = 0;
					byte[] buff = new byte[BUFF_SIZE];
					//单个文件循环读取
					while ((nRead = in.read(buff, 0, (int)(BUFF_SIZE<fileLength?BUFF_SIZE:fileLength))) > 0) {
						fos.write(buff,0,nRead);
						fos.flush();
						fileLength -= nRead;
						if(fileLength<=0){
							break;
						}
					}
					fos.close();
					fos = null;
					File newFile2 = new File(newUrl+TEMP_SUFFIX);					
					//newFile2.renameTo(newFile);
					FileUtils.copyFile(newFile2, newFile);//将临时文件名改为正式文件名，即去掉.tp后缀
					newFile2.delete();// 将临时文件删除
				}
			}
			logger.debug("批量接收文件个数：" + (fileList==null ? 0 : fileList.size()));
		} catch (Exception e) {
			throw e;
		}finally{
			if(fos!=null){
				try {
					fos.close();
					fos = null;
				} catch (IOException e) {
				}
			}
		}
		
		return true;

	}
	
	 /**
	 * 批量上传文件, 并传入文件的Md5值
	 * @param fileList	上传的文件列表
	 */
	protected void sendFileWithMd5ByBath(List<String[]> fileCommentsList) throws IOException {
		ObjectOutputStream oos = null;
		FileInputStream fis = null;

		try {
			// 第一步 用ObjectOutputStream工具类 发送file对象信息 用于文件名，文件目录，文件大小的获取
			oos = new ObjectOutputStream(out);
			List<File> fileList = new LinkedList<File>();
			List<String[]> fileStrList = new ArrayList<String[]>();
			for(String[] fileComments : fileCommentsList){
				File file = new File(fileComments[0]);
				if(file.exists()){
					String[] tmpArr = new String[]{
							file.getName(), file.length() + "",fileComments[1]
					};
					fileList.add(file);
					fileStrList.add(tmpArr);
				}else {
					logger.warn("File:>"+file.getAbsolutePath()+" do not exist, can not send");
				}
			}
			oos.writeObject(fileStrList);
			// 第三部，发送文件
			byte[] buff = new byte[BUFF_SIZE];
			int len = 0;
			// 循环上传文件
			for (File file : fileList) {
				fis = new FileInputStream(file);
				while ((len = fis.read(buff)) != -1) {// 将读取的内容输出流
					out.write(buff, 0, len);
				}
				out.flush();
				fis.close();
				fis = null;
			}
			logger.debug("批量发送文件结束，共 "+(fileList==null ? 0 : fileList.size())+ "个文件");
		} catch (IOException e) {
			logger.error("Batch file failed!");
			throw new IOException(e);
		} finally {
			try {
				if (fis != null) {
					fis.close();
					fis = null;
				}
			} catch (IOException e) {
			}
		}
	}
	
	/**
	 * 批量接收文件, 使用Md5校验文件是否完整
	 * @param newDir
	 */
	public boolean receiveFileWithMd5ByBath(String newDir) throws IOException {
		boolean flag = true;
		ObjectInputStream ois = null;
		FileOutputStream fos = null;
		try {
			ois = new ObjectInputStream(in);
			List<String[]> fileList = (List<String[]>)ois.readObject();
			if(fileList != null && fileList.size()>0){
				int sucessCnt = 0;
				int failCnt = 0;
				for(int i=0; i<fileList.size(); i++){
					String[] arr = fileList.get(i);//arr[0] 存放文件名，arr[1] 存放文件长度，arr[2] 存放Md5值
					String newUrl = newDir + File.separator + arr[0];
					int fileLength = Integer.parseInt(arr[1]);//大小
					String md5Val = arr[2];
					File newFile = new File(newUrl);
					if(newFile.exists()){
						FileUtil.delDir(newFile);
						logger.debug("receiveFileWithMd5ByBath delete file--" + newFile.getAbsolutePath());
					}
					if(!newFile.getParentFile().exists()){
						newFile.getParentFile().mkdirs();
					}
					fos = new FileOutputStream(newUrl+TEMP_SUFFIX);
					
					//接收文件内容 
					int nRead = 0;
					byte[] buff = new byte[BUFF_SIZE];
					
					//单个文件循环读取
					while ((nRead = in.read(buff, 0, (int)(BUFF_SIZE<fileLength?BUFF_SIZE:fileLength))) > 0) {
						fos.write(buff,0,nRead);
						fos.flush();
						fileLength -= nRead;
						if(fileLength<=0){
							break;
						}
					}
					fos.close();
					fos = null;
					File newFile2 = new File(newUrl+TEMP_SUFFIX);
					if (md5Val != null
							&& md5Val
									.equals(MD5Util.getFileMD5String(newFile2))) {
						logger.debug("接收文件" + (i+1) + "“" + newFile.getAbsolutePath() + "”完整");
						//newFile2.renameTo(newFile);
						FileUtils.copyFile(newFile2, newFile);//将临时文件名改为正式文件名，即去掉.tp后缀
						newFile2.delete();// 将临时文件删除
						sucessCnt ++ ;
					} else {
						logger.debug("接收文件" + (i+1) + "“" + newFile.getAbsolutePath() + "”不完整，失败");
						failCnt ++ ;
					}
				}
				logger.info("批理接收文件个数：" + fileList.size() + ", 成功：" + sucessCnt + ", 失败：" + failCnt);
				if(failCnt > 0) {
					flag = false;
				}
			}else{
				logger.info("批量接收文件列表为空");
			}
		} catch (Exception e) {
			logger.error("Batch file failure");
			throw new IOException(e);
		}finally{
			if(fos!=null){
				try {
					fos.close();
					fos = null;
				} catch (IOException e) {
				}
			}
		}
		
		return flag;
	}
	
	/**
	 * 断点续传 发送方法
	 * @time Mar 2, 2012-2:30:16 PM
	 * @param filePath
	 */
	protected boolean bpSendFile (String filePath) throws Exception {
		File file = new File(filePath);
		
		//发送长度 end
		this.sendMessageByChar(file.length()+"");
		
		String msg = this.receiveMessageByChar();
		long start = Long.parseLong(msg);
		long end = file.length();
		logger.debug("start "+start);
		logger.debug("end "+end);
		bpSendFile(filePath, start, end);
		
		return true;
	}
	
	/**
	 * 断点续传 接收方法
	 * @time Mar 2, 2012-2:30:16 PM
	 * @param filePath
	 * @param start
	 * @param end
	 */
	protected int bpReceiveFile (String filePath) throws Exception {
		File file = new File(filePath);
		if(!file.exists()){
			file = new File(filePath+TEMP_SUFFIX);
		}
		String msg = this.receiveMessageByChar();
		long start = file.length();
		long end = Long.parseLong(msg);
		this.sendMessageByChar(start+"");
		
		logger.debug("start "+start);
		logger.debug("end "+end);
		bpReceiveFile(file.getAbsolutePath(), start, end);
		
		//file.renameTo(new File(filePath));
		FileUtils.copyFile(file, new File(filePath));//将临时文件名改为正式文件名，即去掉.tp后缀
		file.delete();// 将临时文件删除
		
		logger.debug("bpReceiveFile sucess");
		return 0;
	}
	
	/**
	 * 断点续传 发送方法
	 * @time Mar 2, 2012-2:30:16 PM
	 * @param filePath
	 * @param start
	 * @param end
	 */
	protected void bpSendFile (String filePath,long start,long end) throws Exception {
		if (start == end) {
			return;
		}
        BufferedRandomAccessFile braf = null;
        try {
            File file = new File(filePath);
            
            //- 不存在，终止； 存在则继续
            if(!file.exists()){
            	this.sendMessageByChar(FAIL);
            	return ;
            }else {
            	this.sendMessageByChar(SUCCESS);
            }
            
            String msg = this.receiveMessageByChar();
            logger.debug("Recive:   " + msg); 
            
            //- BufferedRandomAccessFile 读取指定位置的文件字节数组，写入输出通讯
            byte[] b = new byte[BUFF_SIZE];
            braf = new BufferedRandomAccessFile(file,"r");  
            braf.seek(start);
            int nRead;
            while ((nRead = braf.read(b, 0, BUFF_SIZE)) > 0) {
            	
            	out.write(b, 0, nRead);
            	start += nRead;
            	
            	//-- 读取完成 跳出
            	if(start==end){break;}
            	
            }
            
        }catch (Exception e) {
        	throw e;
		}finally{
			try {
				//- 关闭 随机访问文件对象（关闭流）
				if(braf!= null){braf.close();}
			} catch (IOException e) {
				logger.error(Utils.printExceptionStack(e));
			}
			
		}
	}
	/**
	 * 断点续传 接收方法
	 * @time Mar 2, 2012-2:30:16 PM
	 * @param filePath
	 * @param start
	 * @param end
	 */
	protected void bpReceiveFile (String filePath,long start,long end) throws Exception {
		if(StringUtils.isEmpty(filePath)){
			return;
		}
		if (start == end) {
			return;
		}
		BufferedRandomAccessFile raf = null;
		try {
			File file = new File(filePath);
			
			//- 文件路径不存在 则创建
			if (!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			
			//- 文件不存在 则创建
			if (!file.exists()) {
				file.createNewFile();
			}
			
			//- 接收发送端 发送数据准备 确认信息
			String msg = this.receiveMessageByChar();
			
			if (FAIL.equals(msg)) {			//结束操作
				return;
			} else { 
				this.sendMessageByChar(SUCCESS);	// 通知发送端 接收数据准备完成 确认信息
			}

			// 将通信中读出的数据 写入文件指定位置
			byte[] b = new byte[BUFF_SIZE];
			raf = new BufferedRandomAccessFile(file, "rw");
			raf.seek(start);
			int nRead;
			
			while ((nRead = in.read(b, 0, BUFF_SIZE)) > 0) {
				raf.write(b, 0, nRead);
				start += nRead;
				
				if (start == end) {		//写完跳出
					break;
				}
			}
		} catch (Exception e) {
			throw e;
		}finally{
			try {
				//- 关闭 随机访问文件对象（关闭流）
				if(raf!= null){raf.close();}
			} catch (IOException e) {
				logger.error(Utils.printExceptionStack(e));
			}
		}
	}
	
	/**
	 * 断点续传 批量上传文件
	 * @param fileList	上传的文件列表
	 * @param dir	本地文件集合根目录绝对路径
	 */
	protected void bpSendFileByBath(List<File> fileList,String dir) throws Exception {
		BufferedRandomAccessFile oReadFile =  null;

		try {
			// 第一步发送本地根目录地址（用于地址截取）保证fileList的目录结构完整性
			this.sendMessageByChar("abs:"+(dir==null?"":dir));
			String result = this.receiveMessageByChar();
			logger.debug("根目录路径通信状态: " + result);
			// 第二步 用ObjectOutputStream工具类 发送file对象信息 用于文件名，文件目录，文件大小的获取

			//原文件文件名 和 大小（即end长度）
			List<String[]> sourceFileList = new ArrayList<String[]>(); 
			for(File f : fileList){
				String[] tmpArr = new String[]{
						f.getAbsolutePath(),0+"",f.length() + ""
				};
				sourceFileList.add(tmpArr);
			}

			logger.debug("发送信息: " + Arrays.toString(sourceFileList.toArray()));
			this.sendObject(sourceFileList);
		
			//得到需要下载的文件信息
			List<String[]> sendFileList = (List<String[]>)receiveObject();
			
			// 第三部，发送文件
			byte[] buff = new byte[BUFF_SIZE];
			
			// 循环上传文件
			for (String[] sendFile: sendFileList) {
				long start = Long.parseLong(sendFile[1]);
				long end = Long.parseLong(sendFile[2]);
				if(start >= end){
					continue;
				}
				File file = new File(sendFile[0]);
				oReadFile = new BufferedRandomAccessFile(file,"r");
				
				// 定位文件指针到nPos位置
				oReadFile.seek(start);		//从0开始
				int nRead;
				
				// 从输入流中读入字节流，然后写到文件中
			    while ((nRead = oReadFile.read(buff, 0, BUFF_SIZE)) > 0) {
			    	
			    	out.write(buff, 0, nRead);
			    	start += nRead;		//调整为从1开始
			        if(start >= end){
			        	break;
			        }
			        
			    }
			    oReadFile.close();
			    oReadFile = null;
			}
			logger.debug("多文件上传结束，共 "+(fileList==null ? 0 : fileList.size())+ "个文件");
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				if (oReadFile != null) {
					oReadFile.close();
					oReadFile = null;
				}
			} catch (IOException e) {
				logger.error(Utils.printExceptionStack(e));
			}
		}
	}
	
	/**
	 * 断点续传 批量接收文件
	 * @param newDir
	 */
	protected void bpReceiveFileByBath(String newDir) throws Exception {

		BufferedRandomAccessFile oSavedFile =  null;
		try {
			//获取集合文件路径
			String oldDir = this.receiveMessageByChar();
			int headLength = "abs:".length();
			oldDir = ((StringUtils.isNotEmpty(oldDir) 
						&& oldDir.length()>=headLength)
						?oldDir.substring(headLength,oldDir.length())
								:oldDir);
			if(StringUtils.isEmpty(oldDir)){
				logger.debug("远程 目录根路径为空 接收文件不保留目录格式 统一存放到本地目录：》"+newDir);
			}else{
				logger.debug("根目录 记录: " + oldDir+" VS "+newDir);
			}
			this.sendMessageByChar(SUCCESS);

			List<String[]> remoteFileList = (List<String[]>)receiveObject();
			List<String[]> receiveFileList = new LinkedList<String[]>();
			byte[] buff = new byte[BUFF_SIZE];
			if(remoteFileList != null && remoteFileList.size()>0){
				for(String[] arr : remoteFileList){
					String newUrl = null;
					if(StringUtils.isEmpty(oldDir)){
						newUrl = newDir+(new File(arr[0].replaceAll("\\\\", "/")).getName());
					}else{
						newUrl = arr[0].replace(oldDir, newDir);//新路径
						newUrl = newUrl.replaceAll("\\\\", "/");
					}
					
					File newFile = new File(newUrl);
					
					//该文件已存在
					if(newFile.exists()){
						continue;
					}

					newFile = new File(newUrl+TEMP_SUFFIX);
					arr[1] = newFile.length()+"";
					receiveFileList.add(arr);
				}
			}
			this.sendObject(receiveFileList);
			
			if(receiveFileList != null && receiveFileList.size()>0){
				for(String[] arr : receiveFileList){
					String newUrl = null;
					if(StringUtils.isEmpty(oldDir)){
						newUrl = newDir+(new File(arr[0].replaceAll("\\\\", "/")).getName());
					}else{
						newUrl = arr[0].replace(oldDir, newDir);//新路径
						newUrl = newUrl.replaceAll("\\\\", "/");
					}

					File newFile = new File(newUrl+TEMP_SUFFIX);
					
					if(!newFile.getParentFile().exists()){
						newFile.getParentFile().mkdirs();
					}
					
					if(!newFile.exists()){
						newFile.createNewFile();
					}
					
					int start = Integer.parseInt(arr[1]);		// 起始
					int end = Integer.parseInt(arr[2]);		// 结束
					if(start<end){
						oSavedFile = new BufferedRandomAccessFile(newFile,"rw");
						oSavedFile.seek(start);
						int nRead;
						rfile:while ((nRead = in.read(buff, 0, BUFF_SIZE<end?BUFF_SIZE:end)) > 0) {
							oSavedFile.write(buff,0,nRead);
							end -= nRead;
							if(end<=0){
								break rfile;
							}
						}
						oSavedFile.close();
						oSavedFile = null;
					}
//					newFile.renameTo(new File(newUrl));		//将临时文件名改为正式文件名，即去掉.tp后缀
					FileUtils.copyFile(newFile, new File(newUrl));	//将临时文件名改为正式文件名，即去掉.tp后缀
					newFile.delete();
				}
			}
			logger.debug("多文件接收结束，共 "+(remoteFileList==null ? 0 : remoteFileList.size())+ "个文件");
//		} catch (IOException e) {
//			logger.error("",e);
//		} catch (ClassNotFoundException e) {
//			logger.error("",e);
		}finally{
			if(oSavedFile!=null){
//				try {
					oSavedFile.close();
					oSavedFile = null;
//				} catch (IOException e) {
//					logger.error("",e);
//				}
			}
		}
	}
	
	/**
	 * 断点续传 批量上传文件, 并传入文件的Md5值
	 * @param fileList	上传的文件列表
	 * @param dir	本地文件集合根目录绝对路径
	 */
	protected void bpSendFileByBathMD5(List<FileComment> fileCommentsList) throws Exception {
		BufferedRandomAccessFile oReadFile =  null;

		try {
			//原文件文件名 和 大小（即end长度）
			List<FileComment> sourceFileList = new ArrayList<FileComment>(); 
			if(fileCommentsList !=null && fileCommentsList.size()!=0){
				for(FileComment fileComment : fileCommentsList){
					File f = new File(fileComment.getFileName());
					if(!f.exists()){
						sourceFileList.add(new FileComment(f.getAbsolutePath(),0,-1,fileComment.getMd5Val()));
					}else {
						String md5Val = StringUtils.isEmpty(fileComment.getMd5Val())?MD5Util.getFileMD5String(f):fileComment.getMd5Val();
						sourceFileList.add(new FileComment(f.getAbsolutePath(),0,f.length(),md5Val));
					}
				}
			}

			logger.debug("发送信息: " + Arrays.toString(sourceFileList.toArray()));
			this.sendObject(sourceFileList);
		
			//得到需要下载的文件信息
			List<FileComment> sendFileList = (List<FileComment>)receiveObject();
			
			// 第三部，发送文件
			byte[] buff = new byte[BUFF_SIZE];
			
			// 循环上传文件
			for (FileComment sendFile: sendFileList) {
				long start = sendFile.getStart();
				long end = sendFile.getEnd();
				if(start >= end){
					continue;
				}
				File file = new File(sendFile.getFileName());
				oReadFile = new BufferedRandomAccessFile(file,"r");
				
				// 定位文件指针到nPos位置
				oReadFile.seek(start);		//从0开始
				int nRead;
				
				// 从输入流中读入字节流，然后写到文件中
			    while ((nRead = oReadFile.read(buff, 0, BUFF_SIZE)) > 0) {
			    	
			    	out.write(buff, 0, nRead);
			    	start += nRead;		//调整为从1开始
			        if(start >= end){
			        	break;
			        }
			        
			    }
			    oReadFile.close();
			    oReadFile = null;
			}
			logger.debug("多文件上传结束，共 "+(sendFileList==null ? 0 : sendFileList.size())+ "个文件");
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				if (oReadFile != null) {
					oReadFile.close();
					oReadFile = null;
				}
			} catch (IOException e) {
				logger.error(Utils.printExceptionStack(e));
			}
		}
	}

	
	/**
	 * 断点续传 批量接收文件, 使用Md5校验文件是否完整
	 * @param newDir
	 */
	public int bpReceiveFileByBathMd5(String newDir) throws Exception{
		if(newDir!=null){
			newDir += File.separator;
		}
		int rFlag = 0; //0 OK  -1 MD5 ERROR -2 Function ERROR -3 文件不存在
		BufferedRandomAccessFile oSavedFile =  null;	//有缓存的 随机文件IO对象
		try {

			List<FileComment> remoteFileList = (List<FileComment>)receiveObject();	//接收可接收的文件信息 string[]{fileName,start,end,MD5}
			List<FileComment> receiveFileList = new LinkedList<FileComment>();		//需要续传的文件及其索引信息 string[]{fileName,start,end,MD5}
			byte[] buff = new byte[BUFF_SIZE];	//缓存 大小

			//- 检查实际接收文件大小
			if(remoteFileList != null && remoteFileList.size()>0){
				for(FileComment arr : remoteFileList){
					//String newUrl = newDir+removeTimeTagFileName(new File(arr.getFileName()).getName(),null);
					String filePath = arr.getFileName().replaceAll("\\\\", "/");
					String fileName = filePath.substring(filePath.lastIndexOf("/")+1, filePath.length());
					String newUrl = newDir+removeTimeTagFileName(fileName,null);
					
					File newFile = new File(newUrl);
					
					//-- 已接收完成
					if(newFile.exists()){
						// continue;
						// 2013-1-6 jzz 如果接收完成也比较MD5值，主要是针对再次执行任务，直接拷来的文件
						//-- MD5为空 无需校验
						if(StringUtils.isEmpty(arr.getMd5Val())){
							continue;
						}
						//-- MD5相等, 接收完成
						if(arr.getMd5Val().equals(MD5Util.getFileMD5String(newFile))){
							logger.debug("1--" + newFile.getAbsolutePath()+" MD5值校验一致");
							continue;
						} else {//-- MD5不相等，则删除该文件，下面重新接收
							FileUtil.delDir(newFile);
							logger.debug("1--bpReceiveFileByBathMd5 delete file ---" + newFile.getAbsolutePath());
							logger.debug("1--" + newFile.getAbsolutePath()+" MD5值校验不一致");
						}
						// 2013-1-6 jzz 修改结束
					}
					
					//-- 续传文件及起始长度
					newFile = new File(newUrl+TEMP_SUFFIX);
					arr.setStart(newFile.length());
					receiveFileList.add(arr);
				}
			}
			this.sendObject(receiveFileList);
			
			//- 接收文件
			if(receiveFileList != null && receiveFileList.size()>0){
				for(FileComment arr : receiveFileList){
					//String newUrl = newDir+removeTimeTagFileName(new File(arr.getFileName()).getName(),null);
					String filePath = arr.getFileName().replaceAll("\\\\", "/");
					String fileName = filePath.substring(filePath.lastIndexOf("/")+1, filePath.length());
					String newUrl = newDir+removeTimeTagFileName(fileName,null);
					
					File newFile = new File(newUrl+TEMP_SUFFIX);

					if(!newFile.getParentFile().exists()){
						newFile.getParentFile().mkdirs();
					}
					
					//创建空文件
					if (!newFile.exists()) {
						newFile.createNewFile();
					}
					
					long start = arr.getStart();		// 起始
					long end = arr.getEnd();		// 结束
					if(end == -1){
						return -3;
					}
					
					if(start<end){
						oSavedFile = new BufferedRandomAccessFile(newFile,"rw");
						oSavedFile.seek(start);
						int nRead;
						rfile:while ((nRead = in.read(buff, 0, (int)(BUFF_SIZE<end?BUFF_SIZE:end))) > 0) {
							oSavedFile.write(buff,0,nRead);
							end -= nRead;
							if(end<=0){
								break rfile;
							}
						}
						oSavedFile.close();
						oSavedFile = null;
					}
					//newFile.renameTo(new File(newUrl));
					FileUtils.copyFile(newFile, new File(newUrl));//将临时文件名改为正式文件名，即去掉.tp后缀
					newFile.delete();// 将临时文件删除
					logger.debug(newFile.getAbsolutePath()+" 下载完成！");
					
					//-- MD5为空 无需校验
					if(StringUtils.isEmpty(arr.getMd5Val())){
						continue;
					}

					File newFile2 = new File(newUrl);
					//-- MD5不相等，则删除该文件 返回-1
					if(!arr.getMd5Val().equals(MD5Util.getFileMD5String(newFile2))){
						//newFile.delete_bak();
						//使用删除文件公共方法
						FileUtil.delDir(newFile);
						logger.debug("bpReceiveFileByBathMd5 delete file ---" + newFile.getAbsolutePath());
						//FileUtil.checkParentDirExist(newFile);
						logger.debug(newFile.getAbsolutePath()+" MD5值校验不一致");
						return -1;
					} else {//-- MD5相等
						logger.debug(newFile.getAbsolutePath()+" MD5值校验一致");
					}
				}
			}
			logger.debug("多文件接收结束，共 "+(remoteFileList==null ? 0 : remoteFileList.size())+ "个文件");
			return rFlag;
		} catch (Exception e) {
			//return -2;
			throw e;
		}finally{
			if(oSavedFile!=null){
				try {
					oSavedFile.close();
					oSavedFile = null;
				} catch (IOException e) {
					logger.error(Utils.printExceptionStack(e));
				}
			}
		}
	}
	
	/**
	 * Object 形式 发送信息
	 */
	protected void sendObject(Object object) throws Exception {
		ObjectOutputStream oos = new ObjectOutputStream(out);
		oos.writeObject(object);
		oos.flush();
	}
	
	/**
	 * Object 形式 接收信息
	 */
	protected Object receiveObject() throws Exception {
		ObjectInputStream ois = new ObjectInputStream(in);
		return ois.readObject();
	}
	
	/**
	 * 关闭通讯
	 */
	public void close() {
			try {
				if(out!=null){
					out.close();
					out = null;
				}
				if(in!=null){
					in.close();
					in = null;
				}
				if(socket!=null && socket.isConnected()){
					socket.close();
					socket = null;
				}
			} catch (Exception e) {
				logger.error(Utils.printExceptionStack(e));
			}
	}
	
	/**
	 * 删除addTimeTagForFileName()方法 所添加的时间戳
	 * @time Mar 12, 2012-3:36:16 PM
	 * @param fileName
	 * @return
	 */
	public static String removeTimeTagFileName(String fileName, String taskId) {
		
		if (StringUtils.isNotBlank(fileName) && fileName.contains("_")) {
			
			String timeTag = fileName.substring(fileName.lastIndexOf("_"),
					fileName.lastIndexOf(".")==-1?fileName.length():fileName.lastIndexOf("."));		//针对无后缀名文件，时间戳截取校验
			fileName = fileName.replace(timeTag, "");
			
			if(taskId!=null){
				fileName = fileName.replace("_" + taskId, "");
			}
			
		}
		
		return fileName;
		
	}
	
	/**
	 * 上传文件时，判断该文件是否已存在，如存在，则在后面加入时间戳
	 * 
	 * @param fileName
	 *            单纯的文件名
	 * @param taskId 标识ID
	 * @return
	 * @throws UnknownHostException 
	 */
	public static String addTimeTagForFileName(String fileName, String taskId, boolean isFile){
		
		try
		{
			Calendar calendar = new GregorianCalendar();
			long timestamp = calendar.getTimeInMillis();
			
			// 文件后缀
			String fielType = "";
			
			if (isFile) {// 只是文件做名称处理，目录的话不用处理，直接使用原名称
				if (fileName.lastIndexOf(".") != -1) {
					fielType = fileName.substring(fileName.lastIndexOf("."));
					fileName = fileName.substring(0, fileName.lastIndexOf("."));
				}
			}
			
			if(taskId!=null){
				fileName += "_" + taskId;
			}
			fileName += "_" + timestamp+""+((int)(Math.random()*1000));
			
			if(StringUtils.isNotBlank(Contants.AGENT_LOCAL_IP)) {
				fileName = fileName+"_"+Contants.AGENT_LOCAL_IP;
			}
			fileName += fielType;
			logger.debug("回传文件名称为： "+fileName);
		} catch (Exception e)
		{
			logger.error("Generating the name exception of the return file", e);
		}
		
		return fileName;
	}
}
