/*
 *  @(#)StringUtil.java	1.0
 *
 * Copyright 2010 NIS, Inc. All rights reserved.
 * 
 */
package com.nis.nmsclient.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import sun.misc.BASE64Encoder;


/**
* 
* <p>字符串处理工具类.</p>
* @author 中科智源育成信息有限公司  E-mail: doufengh@software.ict.ac.cn
* @version 1.0 创建时间：Nov 2, 2010 2:57:56 PM
*
*/
public final class StringUtil {
	/**
	 * ISO8859_1 编码集
	 */
	public static final String CODE_ISO8859_1 = "ISO8859_1";
	/**
	 * GB2312 编码集
	 */
	public static final String CODE_GB2312 = "GB2312";
	/**
	 * GBK 编码集
	 */
	public static final String CODE_GBK = "GBK";
	/**
	 * UTF-8 编码集
	 */
	public static final String CODE_UTF_8 = "UTF-8";
	
	private static int size = 0;
	
	private static final String[] SIMPLIFIED_CASE = { "O", "一", "二", "三", "四", "五",
		"六", "七", "八", "九", "十" };
//	private static final String[] SIMPLIFIED_CASE = { "Zero", "One", "Two", "Three", "Four", "Five",
//		"Six", "Seven", "Eight", "Nine", "Ten" };
	
	private static final String[] TRADITIONAL_CASE = { "零", "壹", "贰", "叁", "肆", "伍",
		"陆", "柒", "捌", "玖", "拾" };
//	private static final String[] TRADITIONAL_CASE = { "Zero", "One", "Two", "Three", "Four", "Five",
//			"Six", "Seven", "Eight", "Nine", "Ten" };
	
	/**
	 *<p>Description:抑制默认的构造器,避免实例化对象 </p>
	 */
	private StringUtil() {

	}

   /**
	* 
	* <p>判断一个对象是否为空</p>
	* <p>
	* <code>object</code>元素判断所有对象是否为空.
	* 另外对{@link String}、{@link Collection} 、{@link Map} 进行长度验证,如果长度为0,视为空对象.
	* </p>
	* <pre>
	* String aa = " ";
	* List list = new ArrayList()
	* LinkedHashSet set = new LinkedHashSet();
	* StringUtil.isEmpty(aa) = true
	* StringUtil.isEmpty(list) = true
	* StringUtil.isEmpty(set) = true
	* StringUtil.isEmpty("\t") = true
	* </pre> 
	* @param object 对象元素
	* @return  <code>true</code> 对象为<code>null</code>,<code>false</code> 对象不为<code>null</code>.
	*/
	public static boolean isEmpty(Object object) {
		initSize(object);
		return size==0;

	}
	
	
	/**
	 * 
	 * 判断对象是否有数据存在? 不存在为0、存在不为0的值.
	 * @param object 对象值
	 */
	private static  void initSize(Object object){
		
		if (object == null) {
			size = 0;
		} else {
			if (object instanceof String) {
				size = ((String)object).trim().length();
			} else if (object instanceof Collection) {
				size = ((Collection)object).size();
			} else if (object instanceof Map) {
				size = ((Map)object).size();
            //其他数据类型
			} else {
				size = 1;
			}

		}
		
	}
 
   /**
	* 
	* <p>如果对象为空时,返回默认值.</p>
	* @param object 要判断是否为空的对象
	* @param defaultValue 为空时设的默认值
	* @see #isEmpty(Object)
	* @return 获取处理后的数据
	*/
	public static Object setDefaultValueIfNull(Object object,Object defaultValue){
		if(isEmpty(object)){
			return defaultValue;
		}
		return object;
	}

   /**
	* 
	* <p>对字符串进行MD5加密.</p>
	* <p>
	* 一般作为密码的处理方式,首先通过MD5进行加密，然后将字符串进行Base64编码获得所需字符.
	* </p>
	* <pre>
	* String str = "ceshi";
	* StringUtil.md5(str) = "zBfDDNERxyFfyPUfh5Dg4Q=="
	* </pre> 
	* @param msg 要加密的字符串
	* @return 返回加密后的25位字符,如果解析出现异常将返回<code>null</code>.
	*/
	public static String md5(String msg) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] b = md.digest(msg.getBytes());
			BASE64Encoder encoder = new BASE64Encoder();
			String code = encoder.encode(b);
			return code;
		} catch (Exception e) {
			return null;
		}

	}
	
   /**
	* <p>截取处理字符串,当字符串超过指定的截取长度时,用“......”补充</p>
	* <pre>
	* String str = "中华人民共和国";
	* StringUtil.getMoreString(str, 6) = "中华人民共和......"
	* </pre>   
	* @param text 字符串数据
	* @param length 截取的长度值
	* @return  返回处理后字符
	*/
	public static String getMoreString(String text,int length){
		StringBuilder textBuilder = new StringBuilder();
		
		if(isEmpty(text)){
			return null;
		}
		if(text.length()>length){
			text = text.substring(0,length);
			textBuilder.append(text).append("......");
		}else {
			textBuilder.append(text);
		}
		
		
		return textBuilder.toString();
	}
	
	
   /**
	* 
	* <p>通过给定url获取域名地址.</p>
	* <p>
	* 获得域名信息需去除 http/https/ftp 与 www 一些头信息,获取有效地址. 
	* </p>
	* <pre>
	* StringUtil.getDomain("http://www.baidu.com") = "baidu.com"
	* </pre>   
	* @param webSiteUrl url 地址
	* @return  返回截取后的域名地址
	*/
	public static String getDomain(String webSiteUrl) {
		String url = "";
		
		if(isEmpty(webSiteUrl)){
			return url;
		}
		
		
		if (webSiteUrl.indexOf("http://") >= 0) {
			url = webSiteUrl.substring("http://".length(), webSiteUrl.length());
		} else if (webSiteUrl.indexOf("https://", 0) >= 0) {
			url = webSiteUrl.substring("https://".length(), webSiteUrl.length());
		} else if (webSiteUrl.indexOf("ftp://", 0) >= 0) {
			url = webSiteUrl.substring("ftp://".length(), webSiteUrl.length());
		} else {
			url = webSiteUrl;
		}

		if (url.indexOf("/", 0) >= 0) {
			url = url.substring(0, url.indexOf("/", 1));
		}
		if (url.indexOf("www.") == 0) {
			url = url.substring(url.indexOf(".") + 1, url.length());
		}
		if (url.indexOf("?") >= 0) {
			url = url.substring(0, url.indexOf("?"));
		}
		
		return url;
	}
	
   /**
	* 
	* <p>按照给定规则分隔字符串</p>
	* <pre>例子说明:</pre>   
	* @param str 需要分隔的字符串
	* @param regex 分隔规则
	* @return  返回字符串数组,如果分隔字符串为空返回<code>null</code>.
	*/
	public static String[] Split(String str,String regex) {
		
		if(StringUtil.isEmpty(str)){
			return null;
		}
		
        return str.split(regex);
	
    }
	
	/**
	* 
	* <p>字符编码转换，需要提供字符串的源编码格式.</p>
	* <p>
	* 字符串工具类中提供一些字符编码常量：
	* {@link #CODE_GB2312}\{@link #CODE_GBK}\{@link #CODE_ISO8859_1}\{@link #CODE_UTF_8}
	* </p>
	* @param value 要编码的值
	* @param sourceCodingFormat 字符的原始编码格式,具体编码格式可看本类提供的编码样式.
	* @param destCodingFormat 要转换字符的编码格式,具体编码格式可看本类提供的编码样式.
	* @return  返回编码后的字符串.
	* @throws UnsupportedEncodingException 
	*/
	public static String  getCodingConversionResult(String value,String sourceCodingFormat,
			String destCodingFormat ) throws UnsupportedEncodingException{
		
		if(isEmpty(value)){
			return null;
		}
		
	
		return new String(value.getBytes(sourceCodingFormat), destCodingFormat);
	}
	
   /**
	* 
	* <p>将url转为utf编码格式url,当url符合utf8格式,不会转换.</p>
	* <pre>
	* StringUtil.getUTF8URLEncode("http://www.baidu.com/s?param='中国'") = 
	*   "http%3A%2F%2Fwww.baidu.com%2Fs%3Fparam%3D%27%E4%B8%AD%E5%9B%BD%27"
	* </pre>
	* @see #getUTF8URLDecode(String)   
	* @param url 字符串url
	* @return 返回utf8转换后的字符url
	* @throws UnsupportedEncodingException 
	*/
	public static String getUTF8URLEncode(String url)throws UnsupportedEncodingException{
		if(isUtf8Url(url)){
			return url;
		}
		
		return URLEncoder.encode(url, StringUtil.CODE_UTF_8);
		
	}
	
   /**
    * 
    * <p>将utf8编码的url解析为原始url.当url不符合utf8格式时,不转换.</p>
    * <pre>例子说明:</pre>   
    * @see #getUTF8URLEncode(String)
    * @param url 字符串url
    * @return 返回解析后字符url
    * @throws UnsupportedEncodingException 
    */
	
	public static String getUTF8URLDecode(String url)throws UnsupportedEncodingException{
		
		/*if(!isUtf8Url(url)){
			return url;
		}*/
		
		return URLDecoder.decode(url, StringUtil.CODE_UTF_8);
		
	}
	
	
	/** * 编码是否有效 
	 * @param text 
	 * @return 
	 */
    private static boolean Utf8codeCheck(String text){
    	String sign = "";
    	if (text.startsWith("%e")){
    		
    		for (int i = 0, p = 0; p != -1; i++) {
    			p = text.indexOf("%", p);
    			
    			if (p != -1){
    				p++;
    			}
    			sign += p;
    		}
    	}
    		
    	return sign.equals("147-1");
    }
    
    /**
    * 是否Utf8Url编码
    * @param text
    * @return true or false
    */
    private static boolean isUtf8Url(String text) {
    	text = text.toLowerCase();
    	int p = text.indexOf("%");
    	
    	if (p != -1 && text.length() - p > 9) {
    		text = text.substring(p, p + 9);
    	}
    	
    	return Utf8codeCheck(text);
    }
    
    
   /**
    * 
    * <p>判断字符串是否是数字格式(包括小数形式).</p>
    * <pre>
    * String a1 = "12";
	* String a2 = "0.01";
	* String a3 = "0.0.1";
	* String a4 = "123a";
	* StringUtil.isNumeric(a1) = true
	* StringUtil.isNumeric(a2) = true
	* StringUtil.isNumeric(a3) = false
	* StringUtil.isNumeric(a4) = false
    * </pre>   
    * @param numberString 数字格式字符串
    * @return  <code>true</code> 符合数字格式(包括小数),<code>false</code> 不符合数字格式.
    */ 
    public static boolean isNumeric(String numberString){
    	
    	if(isEmpty(numberString)){
    		return false;
    	}
    	
    	if(numberString.startsWith(".")||numberString.endsWith(".")){
    		return  false;
    	}
    	
    	int length = numberString.split("\\.").length-1; //判断小数点在字符串中出现的次数。
    	
    	
    	if(length>1) { //小数点大于1次，不符合数字规范
    		
    		return false;
    	}
    	
    	
    	for(int i=0; i<numberString.length(); i++){
    		if(!Character.isDigit(numberString.charAt(i))&&!".".equals(String.valueOf(numberString.charAt(i)))){
    			return false;
    		}
    	}
    	
    	return true;
    }
    
   /**
    * 
    * <p>将字符串数字转换为简体大写中文格式.</p>
    * <pre>
    * StringUtil.convertSimplifiedCase("325") = ”三二五"
    * </pre>   
    * @param numberString 数字字符串
    * @return  返回简体大写后的数字
    */
    public static String convertSimplifiedCase(String numberString) {
    	
    	StringBuilder simplifiedBuilder = new StringBuilder();
		
    	if(isEmpty(numberString)){
    		return null;
    	}
    	
		
		for (int i = 0; i < numberString.length(); i++) {
			String tempNumberString = String.valueOf(numberString.charAt(i));
			if ("0123456789".indexOf(tempNumberString) >= 0) {
				int number = Integer.parseInt(tempNumberString);
				simplifiedBuilder.append(SIMPLIFIED_CASE[number]);
			} else {
				simplifiedBuilder.append(tempNumberString);
			}
		}
		
		return simplifiedBuilder.toString();
	}
    
    
   /**
    * 
    * <p>把字符串中的数字转换成繁体大写中文的格式.</p>
    * <pre>
    * StringUtil.convertTraditionalCase("325") = "叁贰伍"
    * </pre>   
    * @param numberString 数字字符串
    * @return  返回繁体大写后的数字
    */
    public static String convertTraditionalCase(String numberString) {
    	
    	StringBuilder simplifiedBuilder = new StringBuilder();
		
    	if(isEmpty(numberString)){
    		return null;
    	}
    	
		
		for (int i = 0; i < numberString.length(); i++) {
			String tempNumberString = String.valueOf(numberString.charAt(i));
			if ("0123456789".indexOf(tempNumberString) >= 0) {
				int number = Integer.parseInt(tempNumberString);
				simplifiedBuilder.append(TRADITIONAL_CASE[number]);
			} else {
				simplifiedBuilder.append(tempNumberString);
			}
		}
		
		return simplifiedBuilder.toString();
	}
    
  /**
   * 
   * <p>创建唯一标识字符串.</p>
   * <pre>
   * StringUtil.createUUID() = "00000DAF3CFC4E0B8DF2D5BEACB14D75" 
   * </pre>   
   * @return  返回标识符字符串 
   */
	public static String createUUID() {
		String uuid = UUID.randomUUID().toString();
		uuid = uuid.replace("-", "");
		return uuid.toUpperCase();
	}


   /**
	* 
	* <p>字符串过滤,负责将html的textarea属性获得的字符转换成html格式的字符集.</p>
	* @param str 要解析的字符串
	* @return  是解析后的字符串
	*/
	public static String htmlFilter(String str) {
		StringBuffer stringbuffer = new StringBuffer();
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			switch (c) {

			case 39:
				stringbuffer.append("&#039;");
				break;

			case 34:
				stringbuffer.append("&quot;");
				break;

			case 60:
				stringbuffer.append("&lt;");
				break;

			case 62:
				stringbuffer.append("&gt;");
				break;

			case 38:
				stringbuffer.append("&amp;");
				break;

			case 32:
				stringbuffer.append("&#32;");
				break;

			case 10:
				stringbuffer.append("<br>");
				break;

			case 8220:
				stringbuffer.append("&ldquo;");
				break;

			case 8221:
				stringbuffer.append("&rdquo;");
				break;

			default:
				stringbuffer.append(c);
				break;
			}
		}

		return stringbuffer.toString();
	}

}
