package test;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.nis.nmsclient.common.Common;


public class Test1 {

	private Thread singleThread;
	public  void testThread(){
		int i = 0;
		final long loopDelay = 1l;
		final long delay = 0;
		ScheduledFuture<?> taskFuture = null;
		final String threadName = "测试线程";
		taskFuture = Common.scheduled.schedule(new Runnable() {
			public void run() {
				synchronized (singleThread) {
					singleThread = Thread.currentThread();
				}
				Thread.currentThread().setName(threadName + " 周期单次");
				System.out.println("new AgentCommand(command).exec();");
			}
		}, 0, TimeUnit.MILLISECONDS);
		ThreadNN tt = new ThreadNN(taskFuture, threadName, singleThread);
		taskFuture = Common.scheduled.scheduleAtFixedRate(
				tt, delay, 6 * 1000, TimeUnit.MILLISECONDS);
		try {
			Thread.sleep(10*1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//if(i>1){
			tt.cancle();
			System.out.println(i+";taskFuture.isCancelled():"+taskFuture.isCancelled());
			System.out.println(i+";taskFuture.isDone():"+taskFuture.isDone());
			System.out.println(i+";taskFuture.cancel(true):"+taskFuture.cancel(true));
			System.out.println(i+";taskFuture.cancel(true):"+taskFuture.cancel(false));
			System.out.println(i+";taskFuture.isCancelled():"+taskFuture.isCancelled());
			System.out.println(i+";taskFuture.isDone():"+taskFuture.isDone());
			//System.out.println(i+";runnable.stop()before;runnable.getState():"+runnable.getState());
			//runnable.notify();

			//runnable.destroy();
			//runnable.stop();
			
			//System.out.println(i+";runnable.stop();runnable.getState():"+runnable.getState());
			//System.out.println(i+";runnable.stop();runnable.getState():"+runnable.getState());
			//System.out.println(i+";runnable.stop();taskFuture.isCancelled():"+taskFuture.isCancelled());
			System.out.println(i+";runnable.stop();taskFuture.isDone():"+taskFuture.isDone());
			//taskFuture = null;
			//StringUtils.isNotEmpty(new StringBuilder());
			
		//}
	}
	public static void main(String args[]){
		Test1 t = new Test1();
		t.testThread();
	}
	
	static class ThreadNN extends Thread{
		private ScheduledFuture<?> singleFuture;
		private String threadName;
		private ScheduledFuture<?> lastFuture = null;
		private long i = 0;
		private Thread thread;
		private Thread singleThread;
		
		public ThreadNN(ScheduledFuture<?> singleFuture, String threadName, Thread singleThread){
			this.singleFuture = singleFuture;
			this.threadName = threadName;
			this.singleThread = singleThread;
		}
		
		public synchronized void cancle() {
			if(thread!=null && thread.isAlive()){
				System.out.println("------thread.isAlive()------" + thread.isAlive());
				thread.stop();
				System.out.println("------thread.isAlive()------" + thread.isAlive());
			}
		}
		
		public void run() {
			i++;
			Thread.currentThread().setName(threadName + " 周期" + i);
			System.out.println(i+"--start====="+this.getState());
			long et = System.currentTimeMillis();// 本次开始时间，即上次执行结束时间
			long st = et;// - (i * loopDelay * 60 * 1000);// 上次开始时间
			try {
				if (i == 1 && singleFuture != null
						&& !singleFuture.isCancelled()
						&& !singleFuture.isDone()) {
					synchronized (singleThread) {
						if(singleThread!=null){
							singleThread.stop();
							System.out.println("singleThread Timeout stop thread--" + thread.isAlive());
							singleThread = null;
						}
					}
					singleFuture.cancel(true);
				}
				if (lastFuture != null
						&& !lastFuture.isCancelled()
						&& !lastFuture.isDone() && thread!=null) {
					thread.stop();
					System.out.println(i+"--LoopTaskThread run Timeout stop thread--" + thread.isAlive());
					lastFuture.cancel(true);
					lastFuture = null;
					thread = null;
					System.out.println(i+"--TaskResultOper.sendTaskResult(command.getExecId(),command.getExecType(),AgentCommand.RESULT_FAIL,本周期任务执行超时, , new Date(st),new Date(et), command.getIsLoop());");
				}
				lastFuture = Common.scheduled.schedule(new Runnable() {
						public void run() {
							thread = Thread.currentThread();
							Thread.currentThread().setName(threadName + " 周期" + i);
							System.out.println(i+"---新周期的执行new AgentCommand(command).exec()");
							
							for(long j=0l;j<=2000000000; j++){
								if(j==0 || j==100 || j==1000 || j==10000 || j==100000 || j==1000000 || j==2000000000){
									System.out.println(i+"--------"+j);
								}
							}
						}
					}, 0, TimeUnit.MILLISECONDS);
				System.out.println(i+"--end====="+this.getState());
				//lastFuture.get();
				System.out.println(i+"--last====="+this.getState());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	/**
	 * 回传文件：先将信息写入临时文件，再判断与Server端通信是否成功
	 *    1、通信成功：回传所有文件，发送最终结果；删除临时文件
	 *    2、通信失败：保留临时文件，直接返回
	 * @param rfPo 回传文件实体类
	 */
//	public void sendTaskReturnFile(ReturnFilePO rfPo) {
//		if(rfPo==null || rfPo.getFilePathMap()==null || rfPo.getFilePathMap().size()<=0){
//			logger.warn("无回传文件, 不用回传");
//			return;
//		}
//		File tempDir = null;
//		try {
//			/**
//			 * 步骤1、将信息写入文件
//			 */
//			final File file = new File(getTaskReturnFile(rfPo.getTaskType(),
//					rfPo.getTaskId()));
//			if (!file.exists()) {
//				String[] values = new String[] { JSONObject.fromObject(
//						rfPo).toString() };
//				FileWrUtil.cfgFilePrinter(file, Contants.charset,
//						values);
//			}
//			/**
//			 * 步骤2、与Server通信
//			 */
//			Future<?> serFuture = ThreadPool.service.submit(new SSLClient(
//					Thread.currentThread().getName(),
//					CommonSocket.REQ_HAND_SHAKE, null));
//			if (!SSLClient.isSucessByResult((String) serFuture.get())) {
//				return;
//			}
//			/**
//			 * 步骤3、回传文件
//			 */
//			StringBuffer sb = new StringBuffer();
//			if(rfPo.getResDesc()!=null){//取已有的结果描述信息
//				sb.append(rfPo.getResDesc());
//			}
//			if(rfPo.getFilePathMap()!=null && rfPo.getFilePathMap().size()>0){
//				tempDir = new File(Contants.localTempPath + File.separator
//						+ "return_" + rfPo.getTaskId());
//				if(!tempDir.exists()){
//					tempDir.mkdirs();
//				}
//				/**
//				 * 步骤3-1 处理回传文件：1、过滤不存在或文件路径为空的文件 2、处理文件别名并对文件夹压缩，或对文件重命名
//				 */ 
//				Set<Map.Entry<String, String>> entrySet = rfPo.getFilePathMap().entrySet();
//				for(Map.Entry<String, String> entry : entrySet){
//					String returnPath = entry.getKey();
//					String aliasName = entry.getValue();
//					if (StringUtils.isEmpty(returnPath)) {
//						sb.append("回传“" + returnPath + "”失败，回传文件路径值为空;");
//						rfPo.getFilePathMap().remove(returnPath);
//						continue;
//					}
//					File returnFile = new File(returnPath);
//					if (!returnFile.exists()) {
//						sb.append("回传“" + returnPath + "”失败，回传文件不存在;");
//						rfPo.getFilePathMap().remove(returnPath);
//						continue;
//					}
//					//回传文件取别名，用于Server端断点续传
//					if(aliasName==null || aliasName.length()<=0){
//						aliasName = CommonSocket.addTimeTagForFileName(
//								returnFile.getName(), rfPo.getTaskId());
//						if(returnFile.isDirectory()){
//							aliasName += CompressFileMgr.getCompressSuffixByOs(false);
//						}
//						rfPo.getFilePathMap().put(returnPath, aliasName);
//					}
//					// 文件夹的话压缩，文件直接回传
//					File returnTmpFile = new File(tempDir.getCanonicalPath()
//							+ File.separator
//							+ aliasName);
//					if(!returnTmpFile.exists()){
//						if (returnFile.isDirectory()) {
//							new CompressFileMgr().compressFile(returnFile
//									.getAbsolutePath(), returnTmpFile
//									.getAbsolutePath(), null, false);
//						} else {
//							FileUtils.copyFile(returnFile, returnTmpFile);
//						}
//					}
//				}
//				
//				/**
//				 * 步骤3-2 将处理后的回传文件信息重新写入文件
//				 */
//				rfPo.setResDesc(sb.toString());
//				String[] values = new String[] { JSONObject.fromObject(
//						rfPo).toString() };
//				FileWrUtil.cfgFilePrinter(file, Contants.charset,
//						values);
//				
//				/**
//				 * 步骤3-3 开始回传文件
//				 */
//				entrySet = rfPo.getFilePathMap().entrySet();
//				List<String> successKeys = new ArrayList<String>();
//				for(Map.Entry<String, String> entry : entrySet){
//					String key = entry.getKey();
//					File returnFile = new File(tempDir.getCanonicalPath()
//							+ File.separator
//							+ entry.getValue());
//					rfPo.setCurRetrunFile(returnFile);
//					
//					for(int i=0; i<Contants.max_times; i++){//回传失败，尝试几次
//						Future<?> future = ThreadPool.service.submit(new SSLClient(
//								Thread.currentThread().getName(),
//								CommonSocket.REQ_TASK_RETURNFILE, rfPo));
//						String msg = (String) future.get();
//						if(SSLClient.isSucessByResult(msg)){
//							sb.append("回传“" + key + "”成功;");
//							successKeys.add(key);
//							break;
//						}
//						
//						try {// 如果更新失败，让当前线程暂停几秒，再重试
//							Thread.sleep(1000 * Contants.max_delay_seconds);
//						} catch (InterruptedException e) {
//							logger.error(Utils.printExceptionStack(e));
//							continue;
//						}
//					}
//					//sb.append("回传“" + returnPath + "”" + (resultDesc==null ? "失败" : resultDesc) + ";");
//				}// for end
//				for(String key : successKeys){
//					rfPo.getFilePathMap().remove(key);
//				}
//			}
//			
//			/**
//			 * 步骤4、判断文件是否全部回传完成
//			 */
//			if(rfPo.getFilePathMap().size()==0){
//				/**
//				 * 步骤4-1、发送任务结果
//				 */
//				TaskResultOper.sendTaskResult(rfPo.getTaskId(), rfPo.getTaskType(),
//						rfPo.getState(), sb.toString(), "", rfPo.getStartTime(),
//						rfPo.getEndTime(), rfPo.getIsLoop());
//				/**
//				 * 步骤4-2、删除保存回传文件信息的文件
//				 */
//				if(file.exists()){
//					file.delete();
//				}
//				/**
//				 * 步骤4-3、正常回传完成，删除临时文件
//				 */
//				if(tempDir!=null && tempDir.exists()){
//					try {
//						FileUtils.deleteDirectory(tempDir);
//					} catch (IOException e) {
//					}
//				}
//			}else{
//				/**
//				 * 步骤4-1 将下次需要回传的文件重新写入文件
//				 */
//				rfPo.setResDesc(sb.toString());
//				String[] values = new String[] { JSONObject.fromObject(
//						rfPo).toString() };
//				FileWrUtil.cfgFilePrinter(file, Contants.charset,
//						values);
//			}
//		} catch (Exception e) {
//			logger.error(Utils.printExceptionStack(e));
//		}finally{
//			if(tempDir!=null && tempDir.exists() && tempDir.listFiles().length==0){
//				try {
//					FileUtils.deleteDirectory(tempDir);
//				} catch (IOException e) {
//				}
//			}
//		}
//		return;	
//	}
	
	
	//----------打包上传数据中的部分代码
	//移动文件到临时文件
	/*File destDir = new File(Contants.localDataCollection + File.separator + "temp_data");
	for(File dir : dataDirs){
		File[] files = FileUtil.getFilesEndWith(dir, ".csv");
		if (files == null || files.length <= 0) {
			continue;
		}
		for(File file : files){
			FileUtil.moveFile(file, destDir.getAbsolutePath(), dir.getName(), true);
		}
	}
	//压缩文件
	String compressFileStr = Contants.localDataCollection
			+ File.separator
			+ CommonSocket.addTimeTagForFileName("detectdata",
					null)
			+ CompressFileMgr.getCompressSuffixByOs(false);
	new CompressFileMgr().compressFile(destDir
			.getAbsolutePath(), compressFileStr, null, false);
	//删除临时文件
	FileUtils.deleteDirectory(destDir);*/
}
