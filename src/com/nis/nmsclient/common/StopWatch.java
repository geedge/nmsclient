package com.nis.nmsclient.common;

import java.util.LinkedHashMap;

/**
 * 秒表计时器
 * @author fang
 *
 */
public class StopWatch {
	private static final long SEC_MILL = 1000;
	private static final long MIN_MILL = 60 * SEC_MILL;
	private static final long HOUR_MILL = 60 * MIN_MILL;
	private static final long DAY_MILL = 24 * HOUR_MILL;
	private long start;
	private long end;
	private LinkedHashMap<String,Long> tagMap = new LinkedHashMap<String,Long>();
	
	public StopWatch(){
		start();
	}
	
	public static StopWatch newStopWacth(){
		return new StopWatch();
	}

	/**
	 * 计时器开始
	 * @return
	 */
	public long start(){
		this.start = System.currentTimeMillis();
		return start;
	}
	
	/**
	 * 计时器结束
	 * @return
	 */
	public long end(){
		this.end = System.currentTimeMillis();
		return end;
	}
	
	
	public long tag(String tag){
		long l = System.currentTimeMillis();
		this.tagMap.put(tag, l);
		return l;
	}
	
	/**
	 * 计算两个 tag 之间的时间差
	 * @param b
	 * @param a
	 * @return
	 */
	public long between(String b,String a){
		Long l1 = this.tagMap.get(b);
		Long l2 = this.tagMap.get(a);
		if(l1 != null && l2 != null){
			return l1-l2;
		}
		return -1;
	}
	
	public static String toString(long l){
		StringBuilder sb = new StringBuilder();
		if(l >= DAY_MILL){
			sb.append((l/DAY_MILL));
			sb.append( "天");
			l = l % DAY_MILL;
		}
		if(l >= HOUR_MILL){
			sb.append((l/HOUR_MILL));
			sb.append( "小时");
			l = l % HOUR_MILL;
		}
		if(l >= MIN_MILL){
			sb.append((l/MIN_MILL));
			sb.append( "分");
			l = l % MIN_MILL;
		}
		if(l >= SEC_MILL){
			sb.append((l/SEC_MILL));
			sb.append( "秒");
			l = l % SEC_MILL;
		}
		
		sb.append((l));
		sb.append( "毫秒");
		
		return sb.toString();
	}
	
	public String toString(){
		
		return "";
	}
	
	/**
	 * 从开始到结束总耗时
	 * @return
	 */
	public long total(){
		long temp = System.currentTimeMillis();
		if(this.end < this.start){
			this.end = temp;
		}
		return end - start;
	}
	
	
	public void reset(){
		this.tagMap.clear();
		this.start();
	}
	
	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public long getEnd() {
		return end;
	}

	public void setEnd(long end) {
		this.end = end;
	}

	public LinkedHashMap<String, Long> getTag() {
		return tagMap;
	}

	public void LinkedHashMap(LinkedHashMap<String, Long> tag) {
		this.tagMap = tag;
	}
	
	public static void main(String[] args) {
		long s = System.currentTimeMillis();
		long end =  s +2*DAY_MILL+ 12 * MIN_MILL + 30*SEC_MILL + 388;
		
		String string = StopWatch.toString(end -s);
		System.out.println(string);
		
	}
	
}
