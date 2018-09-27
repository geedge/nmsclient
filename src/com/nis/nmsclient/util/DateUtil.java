package com.nis.nmsclient.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

public class DateUtil {
	static Logger logger = Logger.getLogger(DateUtil.class);
	/**
	 * 处理日期时，用到参数。格式24小时制yyyy-MM-dd HH:mm:ss
	 */
	public static final SimpleDateFormat YYYY_MM_DD_HH24_MM_SS = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");
	/**
	 * 处理日期时，用到参数。格式24小时制yyyyMMddHHmmss
	 */
	public static final SimpleDateFormat YYYYMMDDHH24MMSS = new SimpleDateFormat(
			"yyyyMMddHHmmss");
	/**
	 * 处理日期时，用到参数。格式为yyyy-MM-dd
	 */
	public static final SimpleDateFormat YYYY_MM_DD = new SimpleDateFormat(
			"yyyy-MM-dd");
	/**
	 * 处理日期时，用到参数。格式为yyyyMMdd
	 */
	public static final SimpleDateFormat YYYYMMDD = new SimpleDateFormat(
			"yyyyMMdd");
	

	/**
	 * 获得当前日期
	 * 
	 * @return
	 */
	public static String getCurrentDate(SimpleDateFormat sf) {
		if(sf==null){
			sf = YYYY_MM_DD;
		}
		return sf.format(new Date());
	}
	
	/**
	 * 获得某日期的指定格式的字符串
	 * @param sf
	 * @param date
	 * @return
	 */
	public static String getStingDate(SimpleDateFormat sf, Date date) {
		if(date==null){
			date = new Date();
		}
		if(sf==null){
			sf = YYYY_MM_DD;
		}
		
		return sf.format(date);
	}
	
	public static long getDaysFromBeginToEnd(SimpleDateFormat dateFormat,
			String begin, String end) {
		Date beginD = null;
		Date endD = null;
		long days = 0;
		try {
			if (begin != null && !"".equals(begin)) {
				beginD = dateFormat.parse(begin);
			} else {
				beginD = new Date();
			}
			if (end != null && !"".equals(end)) {
				endD = dateFormat.parse(end);
			} else {
				endD = new Date();
			}
			days = getDaysFromBeginToEnd(beginD, endD);
		} catch (ParseException e) {
			logger.error(Utils.printExceptionStack(e));
			return days;
		}

		return days;

	}
	
	public static long getDaysFromBeginToEnd(Date begin, Date end) {
		long days = 0;
		if (begin != null && end != null) {
			days = getDaysFromBeginToEnd(begin.getTime(), end.getTime());
		}

		return days;

	}
	
	public static long getDaysFromBeginToEnd(long begin, long end) {
		return (end - begin) / (24 * 60 * 60 * 1000);
	}
	
	public static long getMinutesFromBeginToEnd(SimpleDateFormat dateFormat,
			String begin, String end) {
		Date beginD = null;
		Date endD = null;
		long days = 0;
		try {
			if (begin != null && !"".equals(begin)) {
				beginD = dateFormat.parse(begin);
			} else {
				beginD = new Date();
			}
			if (end != null && !"".equals(end)) {
				endD = dateFormat.parse(end);
			} else {
				endD = new Date();
			}
			days = getMinutesFromBeginToEnd(beginD, endD);
		} catch (ParseException e) {
			logger.error(Utils.printExceptionStack(e));
			return days;
		}

		return days;

	}
	
	public static long getMinutesFromBeginToEnd(Date begin, Date end) {
		long days = 0;
		if (begin != null && end != null) {
			days = getMinutesFromBeginToEnd(begin.getTime(), end.getTime());
		}

		return days;

	}
	
	public static long getMinutesFromBeginToEnd(long begin, long end) {
		return (end - begin) / (60 * 1000);
	}
	
}
