package com.nis.nmsclient.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.Ostermiller.util.ExcelCSVParser;
import com.Ostermiller.util.ExcelCSVPrinter;
import com.nis.nmsclient.util.io.UnicodeReader;

public class FileWrUtil {
	static Logger logger = Logger.getLogger(FileWrUtil.class);
	
	/**
	 * 根据文件编码添加文件内容前缀
	 * @param file
	 * @param encoding
	 */
	protected static boolean addFileHeadByEncoding(File file, String encoding) throws Exception{
		FileOutputStream fos = null;
		boolean flag = false;
		try{
			byte[] head = null;
			if("UTF-8".equalsIgnoreCase(encoding)){
	    		head = new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF};
	    	}else if("UTF-16BE".equalsIgnoreCase(encoding)){
	    		head = new byte[]{(byte)0xFE,(byte)0xFF};
	    	}else if("UTF-16LE".equalsIgnoreCase(encoding)){
	    		head = new byte[]{(byte)0xFF,(byte)0xFE};
	    	}else if("UTF-32BE".equalsIgnoreCase(encoding)){
	    		head = new byte[]{(byte)0x00,(byte)0x00,(byte)0xFE,(byte)0xFF};
	    	}else if("UTF-32LE".equalsIgnoreCase(encoding)){
	    		head = new byte[]{(byte)0xFF,(byte)0xFF,(byte)0x00,(byte)0x00};
	    	}
			
			if(head!=null){
				fos = new FileOutputStream(file);
		    	fos.write(head);
		    	fos.flush();
		    	
		    	flag = true;
			}
		} catch (IOException e) {
			throw e;
		}finally {
			if(fos!=null){
				try {
					fos.close();
				} catch (IOException e) {
				}
			}
		}
		
		return flag;
	}
	
	/**
	 * 写入文件, .cfg\.ini\.txt
	 *
	 */
	public static boolean cfgFilePrinter(File file, String charset, String[] values) throws Exception{
		OutputStreamWriter fos = null;
        try {
        	if(addFileHeadByEncoding(file, charset)){
	        	fos = new OutputStreamWriter(
	    				new FileOutputStream(file,true), charset);
        	}else{
        		fos = new OutputStreamWriter(
        				new FileOutputStream(file), charset);
        	}
        	for(String val : values){
        		fos.write(val);
        		fos.write("\n");
        	}
			fos.flush();
		} catch (IOException e) {
			throw e;
		}finally {
			if(fos!=null){
				try {
					fos.close();
				} catch (IOException e) {
				}
			}
		}
		
		return true;
	}
	
	/**
	 * 追加内容到文件, .cfg\.ini\.txt
	 *
	 */
	public static boolean cfgFileAppender(File file, String charset, String[] values) throws Exception{
		OutputStreamWriter fos = null;
        try {
        	fos = new OutputStreamWriter(
    				new FileOutputStream(file, true), charset);
        	for(String val : values){
        		fos.write(val);
        		fos.write("\n");
        	}
			fos.flush();
		} catch (IOException e) {
			throw e;
		}finally {
			if(fos!=null){
				try {
					fos.close();
				} catch (IOException e) {
				}
			}
		}
		
		return true;
	}
	
	/**
	 * 根据文件的编码读取文件内容, .cfg\.ini\.txt
	 *
	 */
	public static String[] cfgFileReader(File file) throws Exception{
		BufferedReader br = null;
		ArrayList<String> list = new ArrayList<String>();
        try {
        	
        	br = new BufferedReader(new UnicodeReader(new FileInputStream(
					file), Charset.defaultCharset().name()));
        	String str = null;
    		while((str=br.readLine())!=null){
    			list.add(str);
    		}
        } catch (IOException e) {
        	throw e;
		}finally {
			if(br!=null){
				try {
					br.close();
				} catch (IOException e) {
				}
			}
		}
		
		String[] values = new String[list.size()];
		return list.toArray(values);
	}
	
	/**
	 * 写CSV文件，只写入一行
	 *
	 */
	public static boolean csvFilePrinter(File file, String charset, String[] values) throws Exception{
		OutputStreamWriter fos = null;
        try {
        	fos = new OutputStreamWriter(
    				new FileOutputStream(file), charset);
    		
    		
    		ExcelCSVPrinter ecsvp = new ExcelCSVPrinter(fos);
            ecsvp.changeDelimiter(',');
            ecsvp.setAutoFlush(true);
			ecsvp.writeln(values);
			
			fos.flush();
		} catch (IOException e) {
			throw e;
		}finally {
			if(fos!=null){
				try {
					fos.close();
				} catch (IOException e) {
				}
			}
		}
		
		return true;
	}
	
	/**
	 * 写CSV文件，写入多行
	 * 
	 */
	public static boolean csvFilePrinter(File file, String charset, List<String[]> values) throws Exception{
		OutputStreamWriter fos = null;
        try {
        	fos = new OutputStreamWriter(
    				new FileOutputStream(file), charset);
    		
    		
    		ExcelCSVPrinter ecsvp = new ExcelCSVPrinter(fos);
            ecsvp.changeDelimiter(',');
            ecsvp.setAutoFlush(true);
            for(String[] val : values){
            	ecsvp.writeln(val);
            }
			
			fos.flush();
		} catch (IOException e) {
			throw e;
		}finally {
			if(fos!=null){
				try {
					fos.close();
				} catch (IOException e) {
				}
			}
		}
		
		return true;
	}
	
	/**
	 * 解析CSV文件中的指定行
	 * 
	 */
	public static String[] csvFileParser(File file, String charset, int rowNum) throws Exception{
		Reader in = null;
		String[] currLine = null;
        try {
        	 //创建解析信息流
            in = new InputStreamReader(new FileInputStream(file),charset); 
    		
            ExcelCSVParser csvParser = new ExcelCSVParser(in);
            while((currLine=csvParser.getLine())!=null){
            	if(rowNum != csvParser.getLastLineNumber()){
            		currLine = null;
            	}
            }
			
		} catch (IOException e) {
			throw e;
		}finally {
			if(in!=null){
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}
		
		return currLine;
	}
	
	/**
	 * 解析CSV文件的所有行
	 * 
	 */
	public static List<String[]> csvFileParser(File file, String charset) throws Exception{
		Reader in = null;
		List<String[]> allLineList = new ArrayList<String[]>();
        try {
        	 //创建解析信息流
            in = new InputStreamReader(new FileInputStream(file),charset);
    		
            ExcelCSVParser csvParser = new ExcelCSVParser(in);
            String[] currLine = null;
            while((currLine=csvParser.getLine())!=null){
            	allLineList.add(currLine);
            }
			
		} catch (IOException e) {
			throw e;
		}finally {
			if(in!=null){
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}
		
		return allLineList;
	}
	
}
