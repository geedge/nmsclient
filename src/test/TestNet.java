package test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Scanner;

import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;


public class TestNet {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Sigar sigar = new Sigar();
			
			FileSystem fslist[] = sigar.getFileSystemList();
			for (int i = 0; i < fslist.length; i++) {
				FileSystem fs = fslist[i];
				try {
					FileSystemUsage usage = sigar.getFileSystemUsage(fs.getDirName());
					if (fs.getType() == 2) {// 先取每个盘符的大小和剩余空间
						OutputStreamWriter fos = null;
						File file = new File(fs.getDirName() + File.separator + "writetest.temp");
						System.out.println("-----------file path--" + file.getAbsolutePath());
				        try {
				        	String testStr = "测试硬盘是否可写，现在开始写入文件";
				        	fos = new OutputStreamWriter(
				        				new FileOutputStream(file), "utf-8");
				        	fos.write(testStr);
							fos.flush();
						} catch (IOException e) {
							System.out.println("测试硬盘是否可写：" + e.getMessage());
						}finally {
							if(fos!=null){
								try {
									fos.close();
								} catch (IOException e) {
								}
							}
						}
						if(file.exists()){
							file.delete();
							System.out.println("---------file delete=" + file.getAbsolutePath());
						}
					}
				} catch (SigarException e) {
					if (fs.getType() == 2)
						throw e;
					continue;
				}
			}
			
			String ifNames[] = sigar.getNetInterfaceList();
			if(ifNames.length >0){
				System.out.println("========= all list ==========");
				for (int i = 0; i < ifNames.length; i++) {
					String name = ifNames[i];
					try {
						NetInterfaceConfig ifconfig = sigar.getNetInterfaceConfig(name);
						System.out
								.println(name
										+ "\t"
										+ ifconfig.getType()
										+ "\t"
										+ ifconfig.getFlags()
										+ "---((ifconfig.getFlags()&NetFlags.IFF_POINTOPOINT)>0)="
										+ ((ifconfig.getFlags() & NetFlags.IFF_POINTOPOINT) > 0)
										+ "---((ifconfig.getFlags()&NetFlags.IFF_LOOPBACK)>0)="
										+ ((ifconfig.getFlags() & NetFlags.IFF_LOOPBACK) > 0)
										+ "---((ifconfig.getFlags()&NetFlags.IFF_BROADCAST)<=0)="
										+ ((ifconfig.getFlags() & NetFlags.IFF_BROADCAST) <= 0)
										+ "---((ifconfig.getFlags()&NetFlags.IFF_MULTICAST)<=0)="
										+ ((ifconfig.getFlags() & NetFlags.IFF_MULTICAST) <= 0)
										+ "\t---((ifconfig.getFlags()&NetFlags.IFF_UP)<=0)="
										+ ((ifconfig.getFlags() & NetFlags.IFF_UP) <= 0));
					} catch (SigarException e) {
						System.out.println(name);
						continue;
					}
				}
				
				System.out.println("========= net list ==========");
				for (int i = 0; i < ifNames.length; i++) {
					String name = ifNames[i];
					try {
						NetInterfaceConfig ifconfig = sigar.getNetInterfaceConfig(name);
						// *** 网卡类型：ethernet, Local Loopback(本地环回网卡)
						/*if (!"ethernet".equalsIgnoreCase(ifconfig.getType())) {
							continue;
						}*/
						// *** 这里只取 ethernet 网卡类型
						
						if ((ifconfig.getFlags() & NetFlags.IFF_POINTOPOINT) > 0) {
							continue;
						}
						if ((ifconfig.getFlags() & NetFlags.IFF_LOOPBACK) > 0) {
							continue;
						}
						if ((ifconfig.getFlags() & NetFlags.IFF_BROADCAST) <= 0) {// broadcast地址无效
							continue;
						}
						if ((ifconfig.getFlags() & NetFlags.IFF_MULTICAST) <= 0) {// 不支持multicast
							continue;
						}
						if ((ifconfig.getFlags() & NetFlags.IFF_UP) <= 0) {
							continue;
						}
						System.out.println(name + "--" + ifconfig.getAddress());
					} catch (SigarException e) {
						continue;
					}
				}
				
				Scanner sc = new Scanner(System.in);
				while(true){
					System.out.print("\nplease enter net name:");
					String name = sc.nextLine();
					if("quit".equalsIgnoreCase(name)){
						break;
					}
					if(name==null || "".equals(name)){
						continue;
					}
					System.out.println("-----------" + name + "------------");
					try{
						NetInterfaceStat ifstat = sigar.getNetInterfaceStat(name);
						System.out.println("speed: " + ifstat.getSpeed() + "b/s\nRxPackets: " + ifstat.getRxPackets() + "\nTxPackets: " + ifstat.getTxPackets());
					} catch (SigarException e) {
						e.printStackTrace();
					}
				}
			}
			
		} catch (SigarException e) {
			System.err.println("获取网络信息异常");
			e.printStackTrace();
		}
	}

}
