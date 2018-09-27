package com.nis.nmsclient.util.log4j;

import org.apache.log4j.Priority;
import org.apache.log4j.RollingFileAppender;

public class MyRollingFileAppender extends RollingFileAppender {

	@Override
	public boolean isAsSevereAsThreshold(Priority priority) {
		// 只判断相等，不判断优先级
		return this.getThreshold().equals(priority);
	}

}
