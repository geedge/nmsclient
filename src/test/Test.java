package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.nis.nmsclient.common.Contants;
import com.nis.nmsclient.util.DateUtil;
import com.nis.nmsclient.util.FileUtil;
import com.nis.nmsclient.util.FileWrUtil;

public class Test {
	public static void main(String[] args) {
		//method2();
		//method1();
		
		/*File file = new File("C:\\Documents and Settings\\zhenzhen\\桌面\\nmsdata\\test1");
		long now = System.currentTimeMillis();
		
		File[] files = file.listFiles();
		for(File file2 : files){
			System.out.println("========"+file2.getName());
		}
		System.out.println(System.currentTimeMillis()-now);
		
		now = System.currentTimeMillis();
		
		String[] fileNames = file.list();
		for(String file2 : fileNames){
			System.out.println("========"+file2);
		}
		System.out.println(System.currentTimeMillis()-now);*/
		/*try {
			long now = System.currentTimeMillis();
			ProcessUtil.getProcessId("javaw.exe");
			System.out.println(System.currentTimeMillis()-now);
		} catch (Exception e) {
			e.printStackTrace();
		}*/
		
		/*try {
			FileUtils.copyDirectoryToDirectory(new File("D:\\temp\\test"), new File("D:\\temp\\t1"));
			FileUtils.copyDirectory(new File("D:\\temp\\test"), new File("D:\\temp\\test2"));
			File file = new File("D:\\temp\\tt\\temp");
			if(file.getParentFile().canWrite()){
				System.out.println("Yes");
			}
			String[] except = new String[3];
			except[0]="D:\\temp\\test\\WebRoot";
			except[1]="D:\\temp\\test\\t1.tar.gz";
			except[2]="D:\\temp\\test\\src\\filemgr\\ZipUtil.java";
			//new AgentCommand().backup(new File("D:\\temp\\test"), new File("D:\\temp\\t2"), except);
		} catch (Exception e) {
			e.printStackTrace();
		}*/
		/*String filepath = "";
		if(filepath.startsWith("/") || (filepath.length()>3 && filepath.substring(1,3).equals(":\\"))){
			System.out.println("ff");
		}*/
		/*File file = new File("D:\\temp");
		File file2 = new File("D:\\temp\\test\\..");
		System.out.println(file.compareTo(file2));
		try{
			System.out.println(file.getCanonicalPath() + "--" + file2.getCanonicalPath());
		}catch (Exception e) {
			// TODO: handle exception
		}*/
		try {
			//ProcessUtil.runExec("D:/test.bat");
			/*ProcessUtil.runExec("D:\\Program Files\\PLSQL Developer\\plsqldev.exe");
			String cmd ="D:/Program Files/Navicat for MySQL/navicat.exe";
			Runtime.getRuntime().exec("cmd /c " + cmd.replace(" ", "\" \""));
			//ProcessUtil.runExec("D:/Program Files/feiq/飞秋FeiQ.exe");
			String tempString = "0";
			int length = tempString.split(":").length;
			System.out.println(length);*/
			
			/*Properties prop = System.getProperties();
			for(Map.Entry<Object,Object> entry : prop.entrySet()){
				System.out.println(entry.getKey()+"="+entry.getValue());
			}*/
			
			System.out.println(Contants.class.getClassLoader().getResource("myconfig.properties").getPath())	;		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void method1(){
		long curDate = System.currentTimeMillis();
		
		// 删除指定分钟之前的所有监测文件
		File file = new File("C:/Documents and Settings/zhenzhen/桌面/nmsdata/test");
		if (!file.exists()) {
			return;
		}
		File[] dirs = FileUtil.getDirectoryArray(file);
		if (dirs == null || dirs.length <= 0) {
			return;
		}
		for (File dir : dirs) {
			File[] files = FileUtil.getFilesEndWith(dir, ".csv");
			if (files == null || files.length <= 0) {
				return;
			}
			for (File f : files) {
				long diff = DateUtil.getMinutesFromBeginToEnd(f.lastModified(), curDate);
				
				if (f.exists() && diff > 1) {
					f.delete();
				}
			}
		}
		
		System.out.println("=========method1 : " + (System.currentTimeMillis()-curDate));
	}
	
	public static void method2(){
		try {
			long curDate = System.currentTimeMillis();
			
			List<String> arrayList = new ArrayList<String>();
			
			// 删除指定分钟之前的所有监测文件
			File file = new File("D:/test1");
			if (!file.exists()) {
				return;
			}
			File[] dirs = FileUtil.getDirectoryArray(file);
			if (dirs == null || dirs.length <= 0) {
				return;
			}
			for (File dir : dirs) {
				File[] files = FileUtil.getFilesEndWith(dir, ".csv");
				if (files == null || files.length <= 0) {
					return;
				}
				for (File f : files) {
					//arrayList.add(f.getAbsolutePath());
					FileWrUtil.cfgFileAppender(new File("C:/Documents and Settings/zhenzhen/桌面/nmsdata/test.txt"), "utf-8", new String[]{f.getAbsolutePath()});
				}
			}
			
			//String[] tmp = new String[arrayList.size()];
			//FileWrUtil.cfgFileAppender(new File("C:/Documents and Settings/zhenzhen/桌面/nmsdata/test.txt"), "utf-8", arrayList.toArray(tmp));
			
			System.out.println("=========method2 Write end : " + (System.currentTimeMillis()-curDate));
			
			curDate = System.currentTimeMillis();
			
			FileInputStream in = new FileInputStream(new File("C:/Documents and Settings/zhenzhen/桌面/nmsdata/test.txt"));
			BufferedReader bReader = new BufferedReader(new InputStreamReader(in, Contants.charset));
			String line = "";
			List<File> arrList = new ArrayList<File>();
			while ((line = bReader.readLine()) != null){
				/*File file2 = new File(line);
				if(file2.exists()){
					file2.delete();
				}*/
				arrList.add(new File(line));
			}
			bReader.close();
			in.close();
			System.out.println("=========method2 : " + (System.currentTimeMillis()-curDate));
			curDate = System.currentTimeMillis();
			for(File file3 : arrList){
				if(file3.exists()){
					file3.delete();
				}
			}
			
			System.out.println("=========method2 : " + (System.currentTimeMillis()-curDate));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
