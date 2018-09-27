package com.nis.nmsclient.thread.task;

import java.util.Date;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.nis.nmsclient.common.Common;
import com.nis.nmsclient.model.CommandPO;
import com.nis.nmsclient.util.Utils;

public class LoopTaskThread extends Thread{
	Logger logger = Logger.getLogger(LoopTaskThread.class);
	
	private String threadName;
	private long loopDelay;
	private CommandPO command;
	
	private long i = 0;
	private Future<?> future = null;
	private Thread thread;
	
	private boolean isCancle;

	public LoopTaskThread(String threadName, CommandPO command, long loopDelay, Future<?> singleFuture, Thread singleThread){
		this.threadName = threadName;
		this.loopDelay = loopDelay;
		this.command = command;
		this.future = singleFuture;
		this.thread = singleThread;
		
		isCancle = false;
	}
	
	public synchronized void cancle() {
		isCancle = true;
	}
	
	public void run() {
		i++;
//		Thread.currentThread().setName(threadName + " 周期" + i);
		Thread.currentThread().setName(threadName + " Cycle" + i);
		long et = System.currentTimeMillis();// 本次开始时间，即上次执行结束时间
		long st = et - (i * loopDelay * 60 * 1000);// 上次开始时间
		try {
			if (i == 1 && future != null
					&& !future.isCancelled()
					&& !future.isDone()) {
				try {
					future.get(1, TimeUnit.MICROSECONDS);
				} catch (TimeoutException e) {
					if(thread!=null && thread.isAlive()){
						thread.stop();
						logger.debug("LoopTaskThread run Timeout stop singleThread--" + thread.isAlive());
					}
					future.cancel(true);
				}
				
			}
			if(!isCancle){
				// 周期开始执行
				future = Common.scheduled.schedule(new Runnable() {
					public void run() {
						thread = Thread.currentThread();
//						Thread.currentThread().setName(threadName + " 周期" + i);
						Thread.currentThread().setName(threadName + " Cycle" + i);
						new AgentCommand(command).exec();
					}
				}, 0, TimeUnit.MILLISECONDS);
				try {
					future.get(loopDelay, TimeUnit.MINUTES);
				} catch (TimeoutException e) {
					if(thread!=null && thread.isAlive()){
						thread.stop();
						logger.debug("LoopTaskThread run Timeout stop thread--" + thread.isAlive());
					}
					future.cancel(true);
					logger.debug("---------LoopTaskThread run timeout----------");
					TaskResultOper.sendTaskResult(command.getExecId(),
							command.getExecType(),
							AgentCommand.RESULT_FAIL,
//							"本周期任务执行超时", "", new Date(st),
//							"this task execution timeout", "", new Date(st),
							"i18n_client.LoopTaskThread.loopTaskOuttime_n81i", "", new Date(st),
							new Date(et), false, command.getIsLoop());
				}catch (InterruptedException e) {
					if(thread!=null && thread.isAlive()){
						thread.stop();
						logger.debug("LoopTaskThread run Interrupted stop thread--" + thread.isAlive());
					}
					future.cancel(true);
					logger.debug("---------LoopTaskThread run Interrupted----------");
				}
			}
			
			if(thread!=null){
				logger.debug("LoopTaskThread run thread state--" + thread.isAlive());
			}
		} catch (Exception e) {
			logger.info(Utils.printExceptionStack(e));
		}
	}
}

