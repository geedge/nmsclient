package com.nis.nmsclient.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class Utils {
	static Logger logger = Logger.getLogger(Utils.class);
	
	/**
	 * 获得本机IP
	 * 
	 * @return
	 */
	public static String getLocalIp() {
		String nativeip = "";
		try {
			//根据操作系统确定获取进程ID的方式
			String os = System.getProperty("os.name");
			if (os.startsWith("Windows")) {
				InetAddress ipv4 = InetAddress.getLocalHost();
				nativeip = ipv4.getHostAddress().toString();
				logger.debug("------getLocalIp--nativeip=" + nativeip);
			}else if (os.startsWith("Linux")){
				InetAddress ip = null;
				boolean findIp = false;
				// 根据网卡取本机配置的IP
				Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
				while (netInterfaces.hasMoreElements()) {
					if (findIp) {
						break;
					}
					NetworkInterface ni =  netInterfaces.nextElement();
					logger.debug("------getLocalIp--NetWorkName=" + ni.getName());
					Enumeration<InetAddress> ips = ni.getInetAddresses();
					while (ips.hasMoreElements())
						ip = ips.nextElement();
					logger.debug("------getLocalIp--ip.isSiteLocalAddress()="
							+ ip.isSiteLocalAddress());
					logger.debug("------getLocalIp--ip.isLoopbackAddress()="
							+ ip.isLoopbackAddress());
					logger.debug("------getLocalIp--HostAddress="
							+ ip.getHostAddress());
					if (ip.isSiteLocalAddress() && !ip.isLoopbackAddress()
							&& ip.getHostAddress().indexOf(":") == -1) {
						findIp = true;
						logger.debug("------findIp--" + ip.getHostAddress());
						break;
					}
				}
				if (ip != null) {
					nativeip = ip.getHostAddress();
				}
			} else {
			     throw new IOException("unknown operating system: " + os);
			}
			
		} catch (Exception e) {
			logger.error(Utils.printExceptionStack(e));
		}

		return nativeip;
	}
	
	public static String getOperateSystem() {
		BufferedReader bReader = null;
		BufferedReader errorReader = null;
		Process process = null;
		try {
			String os = System.getProperty("os.name");//根据操作系统确定运行方式
			String[] cmdArr = null;
			if (os.startsWith("Windows")) {
				cmdArr = new String[] { "cmd.exe", "/C", "ver"};
			} else if (os.startsWith("Linux")) {
				cmdArr = new String[] { "/bin/bash", "-c", "uname -r;uname -i;lsb_release -d| cut -d: -f2| cut -f2" };
			} else {
				throw new IOException("unknown operating system: " + os);
			}
			process = Runtime.getRuntime().exec(cmdArr);
			process.getOutputStream().close();
			bReader = new BufferedReader(new InputStreamReader(process
					.getInputStream()));
			errorReader = new BufferedReader(new InputStreamReader(process
					.getErrorStream()));
			String line = null;
			StringBuffer sb = new StringBuffer();
			while ((line = bReader.readLine()) != null) {
				if(line.trim().length()>0){
					sb.append(line.trim() + ",");
					logger.debug("getOperateSystem right-->" + line);
				}
			}
			while ((line = errorReader.readLine()) != null) {
				logger.debug("getOperateSystem error-->" + line);
			}
			if(sb.length() > 0) {
				if (os.startsWith("Windows")) {
					String osInfo = System.getProperty("sun.arch.data.model");//32位 or 64 位
					sb.append(osInfo+"i18n_client.Utils.bit_n81i");
				}else {
					sb.deleteCharAt(sb.length()-1);//去掉最后一个逗号
				}
			}
			
			
			return sb.toString();
		} catch (Exception e) {
			logger.error("Get the exception of the operating system and the release version", e);
		} finally {
			 try {
				if (bReader != null) {
					bReader.close();
				}
				if (errorReader != null) {
					errorReader.close();
				}
				if (process != null) {
					process.destroy();
				}
			} catch (Exception e1) {
			}
		}
		
		return null;
	}
	
	public static boolean checkIP(String checkStr) {
		try {
			String number = checkStr.substring(0, checkStr.indexOf('.'));
			if (Integer.parseInt(number) > 255 || Integer.parseInt(number) < 0) {
				return false;
			}
			checkStr = checkStr.substring(checkStr.indexOf('.') + 1);
			number = checkStr.substring(0, checkStr.indexOf('.'));
			if (Integer.parseInt(number) > 255 || Integer.parseInt(number) < 0) {
				return false;
			}

			checkStr = checkStr.substring(checkStr.indexOf('.') + 1);
			number = checkStr.substring(0, checkStr.indexOf('.'));
			if (Integer.parseInt(number) > 255 || Integer.parseInt(number) < 0) {
				return false;
			}
			number = checkStr.substring(checkStr.indexOf('.') + 1);
			if (Integer.parseInt(number) > 255 || Integer.parseInt(number) < 0) {
				return false;
			} else {
				return true;
			}
		} catch (Exception e) {
			return false;
		}
	}
	
	   //测试IP地址是否合法 
    public static boolean isIp(String ipAddress){ 
//        String test = "([1-9]|[1-9]\\d|1\\d{2}|2[0-1]\\d|22[0-3])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}"; 
//        Pattern pattern = Pattern.compile(test); 
//        Matcher matcher = pattern.matcher(ipAddress); 
//        return matcher.matches();          
        String regex0 = "(2[0-4]\\d)" + "|(25[0-5])";
		 String regex1 = "1\\d{2}";
		 String regex2 = "[1-9]\\d";
		 String regex3 = "\\d";
		 String regex = "(" + regex0 + ")|(" + regex1 + ")|(" + regex2 + ")|(" + regex3 + ")";
		 regex = "(" + regex + ").(" + regex + ").(" + regex + ").(" + regex + ")";
		 String[] str = ipAddress.split("\\.");//根据@拆分IP地址
		 if (!Pattern.matches(regex, ipAddress))
			return false;
		 else if(str!=null && str.length!=4){//如果IP地址拆分后的数组长度不为4则不是正确的IP地址
			 return false;
		 }else{
			return true; 
		 }
    }
    public static long ipToLong(String strIP)
    //将127.0.0.1 形式的IP地址转换成10进制整数，这里没有进行任何错误处理
    {
         int j=0;
         int i=0;
         long [] ip=new long[4];
         int position1=strIP.indexOf(".");
         int position2=strIP.indexOf(".",position1+1);
         int position3=strIP.indexOf(".",position2+1);  
         ip[0]=Long.parseLong(strIP.substring(0,position1));
         ip[1]=Long.parseLong(strIP.substring(position1+1,position2));
         ip[2]=Long.parseLong(strIP.substring(position2+1,position3));
         ip[3]=Long.parseLong(strIP.substring(position3+1));
         return (ip[0]<<24)+(ip[1]<<16)+(ip[2]<<8)+ip[3]; 
    }
    public static String longToIP(long longIP)
    //将10进制整数形式转换成127.0.0.1形式的IP地址，按主机序
    {
         StringBuffer sb=new StringBuffer("");
         sb.append(String.valueOf(longIP>>>24&0xFF));//直接右移24位
         sb.append(".");          //将高8位置0，然后右移16位
         sb.append(String.valueOf((longIP&0x00FFFFFF)>>>16)); 
         sb.append(".");
         sb.append(String.valueOf((longIP&0x0000FFFF)>>>8));
         sb.append(".");
         sb.append(String.valueOf(longIP&0x000000FF));
         //sb.append(".");
         return sb.toString(); 
    } 
    //将10进制整数形式转换成127.0.0.1形式的IP地址，按网络序
    public static String longToNetIp(long longIP){
   	 StringBuffer sb=new StringBuffer("");
   	 sb.append(String.valueOf(longIP&0x000000FF));         
        sb.append(".");          
        sb.append(String.valueOf((longIP&0x0000FFFF)>>>8));         
        sb.append(".");//将高8位置0，然后右移16位
        sb.append(String.valueOf((longIP&0x00FFFFFF)>>>16));
        sb.append(".");
        sb.append(String.valueOf(longIP>>>24&0xFF));//直接右移24位
        //sb.append(".");
        return sb.toString(); 
    }
    public static long netIpToLong(String strIP)
    //将127.0.0.1 形式的IP地址转换成10进制整数，这里没有进行任何错误处理
    {
         int j=0;
         int i=0;
         long [] ip=new long[4];
         int position1=strIP.indexOf(".");
         int position2=strIP.indexOf(".",position1+1);
         int position3=strIP.indexOf(".",position2+1);  
         ip[0]=Long.parseLong(strIP.substring(0,position1));
         ip[1]=Long.parseLong(strIP.substring(position1+1,position2));
         ip[2]=Long.parseLong(strIP.substring(position2+1,position3));
         ip[3]=Long.parseLong(strIP.substring(position3+1));
         return (ip[0])+(ip[1]<<8)+(ip[2]<<16)+(ip[3]<<24); 
    }
    public static void main(String argus[]){
    	System.out.println(Utils.checkIP("10.a.1.1"));
    	System.out.println(Utils.isIp("10.1.1.1"));
    }
	
    
    public static String printExceptionStack(Exception e){
    	StackTraceElement[] ste = e.getStackTrace();
    	StringBuffer sb = new StringBuffer();
    	sb.append("\n\t" + e.toString() + "\n");
    	for(StackTraceElement element : ste){
    		sb.append("\t" + element.toString() + "\n");
    	}
    	
    	return sb.toString();
    }
    

    /**
     * 根据网口名称获取此网口的Ip地址
     * @param ethName
     * @return
     */
    public static String getIpAddressByEthName(String ethName) {
     String ip = null;
     try {
      //根据端口名称获取网卡信息
      NetworkInterface netWork = NetworkInterface.getByName(ethName);
      //获取此网卡的ip地址，可能包含ipv4和ipv6
      Enumeration<InetAddress> adds = netWork.getInetAddresses();
      while (adds.hasMoreElements()) {
       InetAddress add = adds.nextElement();
       String ipStirng = add.getHostAddress();
       // 如果Ip地址长度大于16，说明是ipv6格式地址
       if (ipStirng.length() > 16) {
        continue;
       }
       ip = ipStirng;
      }
     } catch (Exception e) {
      logger.error("Getting IP address failure based on port name",e);
     }
     return ip;
    }

    /**
     * 根据ip地址查找端口的名字
     * 
     * @param ip
     * @return
     */
    public static String getNetInterfaceNameByIp(String ip) {
     byte[] ipArr = Utils.ipStringToByte(ip);
     String name = null;
     try {
      NetworkInterface local = NetworkInterface
        .getByInetAddress(InetAddress.getByAddress(ipArr));
      name = local.getName();
     } catch (Exception e) {
      logger.error("Getting port name failure based on IP address", e);
     }
     return name;
    }

    /**
     * 将string类型的ip地址，转换成数组类型，只支持ipv4
     * 
     * @param ip
     * @return
     */
    public static byte[] ipStringToByte(String ip) {
     if (!Utils.isIp(ip)) {
      logger.error("IP is not legal, can not be converted!");
      return null;
     }
     String[] ipArr = ip.split("\\.");
     byte[] result = new byte[4];
     for (int i = 0; i < ipArr.length; i++) {
      int tem = Integer.parseInt(ipArr[i]);
      result[i] = (byte) tem;
     }
     return result;
    }

}
