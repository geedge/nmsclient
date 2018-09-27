package com.nis.systeminfo.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.Logger;
import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.Swap;
import org.hyperic.sigar.Uptime;
import org.hyperic.sigar.cmd.Ps;
import com.nis.nmsclient.common.Common;
import com.nis.nmsclient.common.Contants;
import com.nis.nmsclient.thread.socket.CommonSocket;
import com.nis.nmsclient.thread.socket.SSLClient;
import com.nis.nmsclient.util.DateUtil;
import com.nis.nmsclient.util.ProcessUtil;
import com.nis.nmsclient.util.Utils;

public class SystemInfo
{
	
	static Logger logger = Logger.getLogger(SystemInfo.class);
	
	public static String SEPARATOR = "@#@";
	
	private Thread thread = null;// 这个属性是取进程信息中启动进程时使用
	
	public DetectInfo getDetectInfo(String checkType, String pidFile, String processPath,
			String procSearchKey, boolean isStart)
	{
		DetectInfo detectInfo = null;
		if (checkType != null)
		{
			if (checkType.equalsIgnoreCase(Contants.SYS_CHECK_TYPE_CPU))
			{// CPU
				detectInfo = testCpuPerc();
			} else if (checkType.equalsIgnoreCase(Contants.SYS_CHECK_TYPE_MEMORY))
			{// 内存
				detectInfo = testPhysicalMemory();
			} else if (checkType.equalsIgnoreCase(Contants.SYS_CHECK_TYPE_DISK))
			{// 硬盘
				detectInfo = testFileSystemInfo();
			} else if (checkType.equalsIgnoreCase(Contants.SYS_CHECK_TYPE_NET))
			{// 网络流量
				detectInfo = testNetDataList();
			} else if (checkType.equalsIgnoreCase(Contants.SYS_CHECK_TYPE_SYSDATE))
			{// 系统时间
				detectInfo = testSystemDate();
			} else if (checkType.equalsIgnoreCase(Contants.SYS_CHECK_TYPE_PROCESS))
			{// 进程信息
				detectInfo = testProcessInfo(pidFile, processPath, procSearchKey, isStart);
			} else if (checkType.equalsIgnoreCase(Contants.SYS_CHECK_TYPE_SYSTEMINFO))
			{// 系统信息
				detectInfo = testSystemInfo();
			}
		}
		
		return detectInfo;
	}
	
	// CPU的用户使用量、系统使用剩余量、总的剩余量、总的使用占用量等（单位：100%）
	public DetectInfo testCpuPerc()
	{
		DetectInfo detectInfo = new DetectInfo();
		try
		{
			Sigar sigar = new Sigar();
			// 不管是单块CPU还是多CPU都适用
			CpuPerc cpuList[] = sigar.getCpuPercList();
			CpuInfo[] cpuInfos = sigar.getCpuInfoList();
//			StringBuffer sb2 = new StringBuffer(cpuList.length + "核: ");
			StringBuffer sb2 = new StringBuffer();
			double userCpuPerc = 0.0;
			double sysCpuPerc = 0.0;
			double waitCpuPerc = 0.0;
			double niceCpuPerc = 0.0;
			double idleCpuPerc = 0.0;
			double totalCpuPerc = 0.0;
			long mhzCpuPerc = 0;
			for (int i = 0; i < cpuList.length; i++)
			{
				StringBuffer sb = new StringBuffer();
				sb.append("cpu" + i);// 标识
				sb.append(SEPARATOR);
				sb.append(CpuPerc.format(cpuList[i].getUser()));// 用户使用率：CPU耗费在不正常的用户进程的使用率
				sb.append(SEPARATOR);
				sb.append(CpuPerc.format(cpuList[i].getSys()));// 系统使用率
				sb.append(SEPARATOR);
				sb.append(CpuPerc.format(cpuList[i].getWait()));// 当前等待率
				sb.append(SEPARATOR);
				sb.append(CpuPerc.format(cpuList[i].getNice()));// 用户使用率：正常的用户进程的CPU使用率
				sb.append(SEPARATOR);
				sb.append(CpuPerc.format(cpuList[i].getIdle()));// 当前空闲率
				sb.append(SEPARATOR);
				sb.append(CpuPerc.format(cpuList[i].getCombined()));// 总的使用率
				sb.append(SEPARATOR);
				sb.append(cpuInfos[i].getMhz());// 主频
				detectInfo.getDetailDatas().add(sb.toString().replace("%", ""));
//				sb2.append(Contants.DETEC_STATE_INFO_FORMATE_POINT+"cpu" + i + " 主频" + cpuInfos[i].getMhz() + "MHz，使用率 "
				sb2.append(Contants.DETEC_STATE_INFO_FORMATE_POINT+"cpu" + i + " i18n_client.SystemInfo.mhz_n81i" + cpuInfos[i].getMhz() + "MHz，i18n_client.SystemInfo.shiyonglv_n81i "
						+ CpuPerc.format(cpuList[i].getCombined()) + "; ");
				
				// CPU总体信息情况
				userCpuPerc += cpuList[i].getUser();// 用户使用率累加
				sysCpuPerc += cpuList[i].getSys();// 系统使用率
				waitCpuPerc += cpuList[i].getWait();// 当前等待率
				niceCpuPerc += cpuList[i].getNice();// 用户使用率：正常的用户进程的CPU使用率
				idleCpuPerc += cpuList[i].getIdle();// 当前空闲率
				totalCpuPerc += cpuList[i].getCombined();// 总的使用率
				mhzCpuPerc = cpuInfos[i].getMhz();// 主频
			}
			
			StringBuffer sb = new StringBuffer();
			sb.append("cpu");// 标识
			sb.append(SEPARATOR);
			sb.append(CpuPerc.format(userCpuPerc/cpuList.length));// 用户使用率：CPU耗费在不正常的用户进程的使用率
			sb.append(SEPARATOR);
			sb.append(CpuPerc.format(sysCpuPerc/cpuList.length));// 系统使用率
			sb.append(SEPARATOR);
			sb.append(CpuPerc.format(waitCpuPerc/cpuList.length));// 当前等待率
			sb.append(SEPARATOR);
			sb.append(CpuPerc.format(niceCpuPerc/cpuList.length));// 用户使用率：正常的用户进程的CPU使用率
			sb.append(SEPARATOR);
			sb.append(CpuPerc.format(idleCpuPerc/cpuList.length));// 当前空闲率
			sb.append(SEPARATOR);
			sb.append(CpuPerc.format(totalCpuPerc/cpuList.length));// 总的使用率
			sb.append(SEPARATOR);
			sb.append(mhzCpuPerc);// 主频
			detectInfo.getDetailDatas().add(sb.toString().replace("%", ""));
//			sb2.append(Contants.DETEC_STATE_INFO_FORMATE_POINT+"cpu" + " 主频" +mhzCpuPerc + "MHz，使用率 " + CpuPerc.format(totalCpuPerc/cpuList.length) + "; ");
//			sb2.insert(0,cpuList.length + "核: "+Contants.DETEC_STATE_INFO_FORMATE_POINT+"cpu" + " 主频" +mhzCpuPerc + "MHz，使用率 " + CpuPerc.format(totalCpuPerc/cpuList.length) + "; ");
			sb2.insert(0,cpuList.length + "i18n_client.SystemInfo.core_n81i: "+Contants.DETEC_STATE_INFO_FORMATE_POINT+"cpu" + "i18n_client.SystemInfo.mhz_n81i" +mhzCpuPerc + "MHz，i18n_client.SystemInfo.shiyonglv_n81i " + CpuPerc.format(totalCpuPerc/cpuList.length) + "; ");
			
			detectInfo.setDescInfo(sb2.toString());
			logger.debug("testCpuPerc---" + sb2.toString());
		} catch (Exception e)
		{
			logger.error("Getting CPU information exception", e);
			return null;
		}
		
		return detectInfo;
	}
	
	// 内存资源信息
	public DetectInfo testPhysicalMemory()
	{
		DetectInfo detectInfo = new DetectInfo();
		try
		{
			Sigar sigar = new Sigar();
			
			StringBuffer sb = new StringBuffer();
			// b)系统页面文件交换区信息
			Swap swap = sigar.getSwap();
			Long swapTotal = swap.getTotal(); // 交换区总量
			Long swapFree = swap.getFree();// 当前交换区剩余量
			sb.append(doubleFormat(swapTotal / (1024 * 1024 * 1024.0), 2));// 交换区总量,单位："G"
			sb.append(SEPARATOR);
			sb.append(doubleFormat(swapFree / (1024 * 1024 * 1024.0), 2));// 当前交换区剩余量,单位："G"
			sb.append(SEPARATOR);
			// sb.append(SEPARATOR);
			// sb.append(swap.getUsed() / 1024L);// 当前交换区使用量,单位："K"
			// a)物理内存信息
			Mem mem = sigar.getMem();
			Long memTotal = mem.getTotal();// 内存总量
			Long memUsed = mem.getUsed();// 当前内存使用量
			Long memFree = mem.getFree();// 当前内存剩余量
			sb.append(doubleFormat(memTotal / (1024 * 1024 * 1024.0), 2));// 内存总量,单位："G"
			sb.append(SEPARATOR);
			sb.append(doubleFormat(memUsed / (1024 * 1024 * 1024.0), 2));// 当前内存使用量,单位："G"
			sb.append(SEPARATOR);
			sb.append(doubleFormat(memFree / (1024 * 1024 * 1024.0), 2));// 当前内存剩余量,单位："G"
			sb.append(SEPARATOR);
			sb.append(doubleFormat(((memTotal - memFree) * 100.0) / memTotal, 1));// 内存使用率
			
			StringBuffer sb2 = new StringBuffer();
//			sb2.append("内存总大小: " + doubleFormat(memTotal / (1024 * 1024 * 1024.0), 2) + "G, 现使用："
//					+ doubleFormat(memUsed / (1024 * 1024 * 1024.0), 2) + "G, 剩余：" 
//					+ doubleFormat(memFree / (1024 * 1024 * 1024.0), 2) + "G, 使用率："
//					+ doubleFormat(((memTotal - memFree) * 100.0) / memTotal, 1) + "%");
			sb2.append("i18n_client.SystemInfo.memerySize_n81i: " + doubleFormat(memTotal / (1024 * 1024 * 1024.0), 2) + "G, i18n_client.SystemInfo.currentUsed_n81i："
					+ doubleFormat(memUsed / (1024 * 1024 * 1024.0), 2) + "G, i18n_client.SystemInfo.spaceRemain_n81i：" 
					+ doubleFormat(memFree / (1024 * 1024 * 1024.0), 2) + "G, i18n_client.SystemInfo.shiyonglv_n81i："
					+ doubleFormat(((memTotal - memFree) * 100.0) / memTotal, 1) + "%");
			
			detectInfo.getDetailDatas().add(sb.toString());
			detectInfo.setDescInfo(sb2.toString());
			
			logger.debug("testPhysicalMemory--sb2=[ " + sb2.toString() + " ] -- tt="
					+ doubleFormat(mem.getUsedPercent(), 1));
		} catch (SigarException e)
		{
			logger.error("Getting the exception of memory information", e);
			return null;
		}
		
		return detectInfo;
	}
	
	// 资源信息（主要是硬盘已有的分区及其详细信息）
	public DetectInfo testFileSystemInfo()
	{
		int num=0;
		DetectInfo detectInfo = new DetectInfo();
		try
		{
			Sigar sigar = new Sigar();
			long allTotal = 0l;
			long allUse = 0l;
			// long allFree = 0l;
			StringBuffer sb2 = new StringBuffer();
			String dirName = "";
			FileSystem fslist[] = sigar.getFileSystemList();
			for (int i = 0; i < fslist.length; i++)
			{
				FileSystem fs = fslist[i];
				try
				{
					FileSystemUsage usage = sigar.getFileSystemUsage(fs.getDirName());
					if (fs.getType() == 2)
					{// 先取每个盘符的大小和剩余空间
						long total = usage.getTotal();// 文件系统总大小, 单位："KB"
						long free = usage.getFree();// 文件系统剩余大小, 单位："KB"
						String usePerc = doubleFormat(usage.getUsePercent() * 100, 1); // 文件系统资源的利用率
						
						allTotal += total;// 文件系统总大小, 单位："KB"
						// allFree += free;// 文件系统剩余大小, 单位："KB"
						allUse += usage.getUsed();//文件系统使用大小, 单位："KB"
						
						StringBuffer sb = new StringBuffer();
						sb.append(fs.getDirName());
						dirName +=fs.getDirName()+",";
						sb.append(SEPARATOR);
						sb.append(doubleFormat(total / (1024 * 1024.0), 2));// 文件系统总大小,
						// 单位："GB"
						sb.append(SEPARATOR);
						sb.append(doubleFormat(free / (1024 * 1024.0), 2));// 文件系统剩余大小,
						// 单位："GB"
						sb.append(SEPARATOR);
						sb.append(usePerc);// 文件系统资源的利用率
						sb.append(SEPARATOR);
						if (checkFileSystemIsWrite(fs.getDirName()))
						{// 文件系统是否可写
							sb.append("0");// 可写：0
						} else
						{
							sb.append("1");// 不可写：1
							num ++;
						}
						
						detectInfo.getDetailDatas().add(sb.toString());
						
//						sb2.append(Contants.DETEC_STATE_INFO_FORMATE_POINT+"【" + fs.getDirName() + "】大小 "
//								+ doubleFormat(total / (1024 * 1024.0), 2) + "G, 已使用 "  
//								+ doubleFormat((total-free) / (1024 * 1024.0), 2) + "G, 剩余 "
//								+ doubleFormat(free / (1024 * 1024.0), 2) + "G, Usage 使用率 " 
//								+ usePerc +	"% ; ");
						sb2.append(Contants.DETEC_STATE_INFO_FORMATE_POINT+"【" + fs.getDirName() + "】i18n_client.SystemInfo.size_n81i "
								+ doubleFormat(total / (1024 * 1024.0), 2) + "G, i18n_client.SystemInfo.used1_n81i "  
								+ doubleFormat((total-free) / (1024 * 1024.0), 2) + "G, i18n_client.SystemInfo.spaceRemain_n81i "
								+ doubleFormat(free / (1024 * 1024.0), 2) + "G, i18n_client.SystemInfo.shiyonglv_n81i " 
								+ usePerc +	"% ; ");
					}
				} catch (SigarException e)
				{
					if (fs.getType() == 2)
						throw e;
					continue;
				}
			}
			// 文件系统资源的总利用率
			// String allUsePerc = doubleFormat((allUse * 1.0 / allTotal) * 100,
			// 1);
			
			if(num==detectInfo.getDetailDatas().size()){//所有硬盘不可写 主动告警
//				String msg = "磁盘"+dirName.substring(0,dirName.length()-1)+"只读";
				String msg = "i18n_client.SystemInfo.disk_n81i"+dirName.substring(0,dirName.length()-1)+"i18n_client.SystemInfo.readOnly_n81i";
				detectInfo.setDiskMsg(msg);
				return detectInfo;
			}
			if(allUse==allTotal){//当磁盘满时候主动告警   
//				String msg = "磁盘总大小"+doubleFormat(allTotal / (1024 * 1024.0), 2)+"G，总的使用率 100%，分区 "+dirName.substring(0,dirName.length()-1)+"剩余 0%";
				String msg = "i18n_client.SystemInfo.diskSize_n81i"+doubleFormat(allTotal / (1024 * 1024.0), 2)+"G，i18n_client.SystemInfo.zongShiYongLv_n81i，i18n_client.SystemInfo.zone_n81i "+dirName.substring(0,dirName.length()-1)+"i18n_client.SystemInfo.spaceRemain_n81i 0%";
				detectInfo.setDiskMsg(msg);
				return detectInfo;
			}
			
			DecimalFormat decimalFormat = new DecimalFormat("0.00");
//			sb2.insert(0, "磁盘总大小：" + doubleFormat(allTotal / (1024 * 1024.0), 2) + "G，已使用 "+decimalFormat.format(((double)allUse/(double)allTotal)*100)+"% ; ");
			sb2.insert(0, "i18n_client.SystemInfo.diskSize_n81i：" + doubleFormat(allTotal / (1024 * 1024.0), 2) + "G，i18n_client.SystemInfo.used2_n81i "+decimalFormat.format(((double)allUse/(double)allTotal)*100)+"% ; ");
			detectInfo.setDescInfo(sb2.toString());
			logger.debug("testFileSystemInfo--sb2=[ " + sb2.toString() + " ]");
			
		} catch (SigarException e)
		{
			logger.error("Getting hard disk information abnormity", e);
			return null;
		}
		
		return detectInfo;
	}
	
	// 获取网络流量等信息
	public DetectInfo testNetDataList()
	{
		DetectInfo detectInfo = new DetectInfo();
		try
		{
			Sigar sigar = new Sigar();
			String ifNames[] = sigar.getNetInterfaceList();
			if (ifNames.length > 0)
			{
				StringBuffer sb2 = new StringBuffer();
				int ethernetCnt = 0;
				
				// 2013-02-26 设备名的处理并去重，这个是针对一块网卡有多个逻辑配置 如 eth0和eth0:1，只取eth0即可
				List<String> nameList = new LinkedList<String>();
				for (int i = 0; i < ifNames.length; i++)
				{
					String name = ifNames[i];
					
					if (name.indexOf(":") != -1)
					{
						name = name.substring(0, name.indexOf(":"));
					}
					
					if (!nameList.contains(name))
					{
						nameList.add(name);
					}
				}
				
				for (int i = 0; i < nameList.size(); i++)
				{
					String name = nameList.get(i);
					
					NetInterfaceConfig ifconfig = sigar.getNetInterfaceConfig(name);
					logger.debug("testNetDataList----type= " + ifconfig.getType());
					
					// *** 网卡类型：ethernet, Local Loopback(本地环回网卡)
					// *** 这里只取 ethernet 网卡类型
					if ((ifconfig.getFlags() & NetFlags.IFF_LOOPBACK) > 0)
					{
						continue;
					}
					/*
					 * if ((ifconfig.getFlags() & NetFlags.IFF_POINTOPOINT) > 0) {
					 * continue; }
					 */
					// 2013-4-22 针对LInux，按名称过滤掉sit0，实际命令查看类型为：IPv6-in-IPv4
					if (name.equals("sit0"))
					{
						continue;
					}
					// 2013-5-23 针对Win7取到的网卡数量太多问题
					if ((ifconfig.getFlags() & NetFlags.IFF_BROADCAST) <= 0)
					{// broadcast地址无效
						continue;
					}
					if ((ifconfig.getFlags() & NetFlags.IFF_MULTICAST) <= 0)
					{// 不支持multicast
						continue;
					}
					ethernetCnt++;
					
					if ((ifconfig.getFlags() & NetFlags.IFF_UP) <= 0)
					{
						// print("!IFF_UP...skipping getNetInterfaceStat");
//						sb2.append(Contants.DETEC_STATE_INFO_FORMATE_POINT+name + " 不可用; ");
						sb2.append(Contants.DETEC_STATE_INFO_FORMATE_POINT+name + " i18n_client.SystemInfo.disable_n81i; ");
						continue;
					}
					
					NetInterfaceStat ifstat;
					try
					{
						ifstat = sigar.getNetInterfaceStat(name);
					} catch (SigarException e)
					{
						continue;
					}
					Long rxPackets1 = ifstat.getRxPackets();// 接收的总包裹数
					Long txPackets1 = ifstat.getTxPackets();// 发送的总包裹数
					Long rxBytes1 = ifstat.getRxBytes();// 接收到的总字节数
					Long txBytes1 = ifstat.getTxBytes();// 发送的总字节数
					Long rxErrors1 = ifstat.getRxErrors();// 接收到的错误包数
					Long txErrors1 = ifstat.getTxErrors();// 发送数据包时的错误数
					Long rxDropped1 = ifstat.getRxDropped();// 接收时丢弃的包数
					Long txDropped1 = ifstat.getTxDropped();// 发送时丢弃的包数
					
					int seconds = 5;
					try
					{
						Thread.sleep(seconds * 1000);// 等待5秒
					} catch (InterruptedException e)
					{
						e.printStackTrace();
					}
					
					ifstat = sigar.getNetInterfaceStat(name);
					Long rxPackets2 = ifstat.getRxPackets();// 接收的总包裹数
					Long txPackets2 = ifstat.getTxPackets();// 发送的总包裹数
					Long rxBytes2 = ifstat.getRxBytes();// 接收到的总字节数
					Long txBytes2 = ifstat.getTxBytes();// 发送的总字节数
					Long rxErrors2 = ifstat.getRxErrors();// 接收到的错误包数
					Long txErrors2 = ifstat.getTxErrors();// 发送数据包时的错误数
					Long rxDropped2 = ifstat.getRxDropped();// 接收时丢弃的包数
					Long txDropped2 = ifstat.getTxDropped();// 发送时丢弃的包数
					
					
					// 添加监测日志，用于检查网络流量出现负值的原因
					logger.debug(" bps 输入: rxBytes1:" + rxBytes1 + ", rxBytes2:" + rxBytes2);
					logger.debug(" bps 输出: txBytes1:" + txBytes1 + ", txBytes2:" + txBytes2);
					logger.debug(" pps 输入: rxPackets1:" + rxPackets1 + ", rxPackets2:" + rxPackets2);
					logger.debug(" pps 输出: txPackets1:" + txPackets1 + ", txPackets2:" + txPackets2);
					
					Long speed = ifstat.getSpeed() / (1000 * 1000);// 带宽：Mbps
					// bps 输入：((第二次接收的总字节数-第一次)*8)/(时间秒*1000*1000)Mbps
					// String rx_bps = doubleFormat(((rxBytes2 - rxBytes1) * 8)
					// / (seconds * 1000 * 1000), 2);
					// bps 输入：((第二次接收的总字节数-第一次)*8)/(时间秒)bps
					String rx_bps = doubleFormat(((rxBytes2 - rxBytes1) * 8) / (seconds), 2);
					// bps 输出：((第二次发送的总字节数-第一次)*8)/(时间秒)bps
					String tx_bps = doubleFormat(((txBytes2 - txBytes1) * 8) / (seconds), 2);
					// pps 输入：(第二次接收的总包裹数-第一次)/时间秒
					Long rx_pps = (rxPackets2 - rxPackets1) / seconds;
					// pps 输出：(第二次发送的总包裹数-第一次)/时间秒
					Long tx_pps = (txPackets2 - txPackets1) / seconds;
					Long rx_errorPerc = 0l;// 接收错包率
					Long tx_errorPerc = 0l;// 发送错包率
					Long rx_droppedPerc = 0l;// 接收丢包率
					Long tx_droppedPerc = 0l;// 发送丢包率
					if (rxPackets2 - rxPackets1 > 0)
					{
						rx_errorPerc = (rxErrors2 - rxErrors1) / (rxPackets2 - rxPackets1) * 100;// 接收错包率
						rx_droppedPerc = (rxDropped2 - rxDropped1) / (rxPackets2 - rxPackets1)
								* 100;// 接收丢包率
					}
					if (txPackets2 - txPackets1 > 0)
					{
						tx_errorPerc = (txErrors2 - txErrors1) / (txPackets2 - txPackets1) * 100;// 发送错包率
						tx_droppedPerc = (txDropped2 - txDropped1) / (txPackets2 - txPackets1)
								* 100;// 发送丢包率
					}
					
					StringBuffer sb = new StringBuffer();
					sb.append(name);// 网卡名称
					sb.append(SEPARATOR);
					sb.append(rxPackets2);// 接收的总包裹数
					sb.append(SEPARATOR);
					sb.append(txPackets2);// 发送的总包裹数
					sb.append(SEPARATOR);
					sb.append(rxBytes2);// 接收到的总字节数
					sb.append(SEPARATOR);
					sb.append(txBytes2);// 发送到的总字节数
					sb.append(SEPARATOR);
					sb.append(rxErrors2);// 接收到的错误包数
					sb.append(SEPARATOR);
					sb.append(txErrors2);// 发送到的错误包数
					sb.append(SEPARATOR);
					sb.append(rxDropped2);// 接收时丢弃的包数
					sb.append(SEPARATOR);
					sb.append(txDropped2);// 发送时丢弃的包数
					sb.append(SEPARATOR);
					sb.append(speed);// 带宽：Mbps
					sb.append(SEPARATOR);
					sb.append(rx_bps);// bps 输入：Mbps
					sb.append(SEPARATOR);
					sb.append(tx_bps);// bps 输出：Mbps
					sb.append(SEPARATOR);
					sb.append(rx_pps);// pps 输入：每秒接收包数
					sb.append(SEPARATOR);
					sb.append(tx_pps);// pps 输出：每秒发送包数
					sb.append(SEPARATOR);
					sb.append(rx_errorPerc);// 接收错包率
					sb.append(SEPARATOR);
					sb.append(tx_errorPerc);// 发送错包率
					sb.append(SEPARATOR);
					sb.append(rx_droppedPerc);// 接收丢包率
					sb.append(SEPARATOR);
					sb.append(tx_droppedPerc);// 发送丢包率
					detectInfo.getDetailDatas().add(sb.toString());
					
//					sb2.append(Contants.DETEC_STATE_INFO_FORMATE_POINT+name + " 带宽" + speed + "Mbps, 输入" + rx_bps + "bps、" + rx_pps
//							+ "pps, 输出" + tx_bps + "bps、" + tx_pps + "pps; ");
					sb2.append(Contants.DETEC_STATE_INFO_FORMATE_POINT+name + " i18n_client.SystemInfo.netSpeed_n81i" + speed + "Mbps, i18n_client.SystemInfo.input_n81i" + rx_bps + "bps、" + rx_pps
							+ "pps, Output" + tx_bps + "bps、" + tx_pps + "pps; ");
				}
//				sb2.insert(0, ethernetCnt + "个逻辑网卡: ");
				sb2.insert(0, ethernetCnt + "i18n_client.SystemInfo.insert_n81i: ");
				detectInfo.setDescInfo(sb2.toString());
				logger.debug("testNetDataList----" + sb2);
			}
			
		} catch (SigarException e)
		{
			logger.error("Obtaining network information abnormity", e);
			return null;
		}
		return detectInfo;
	}
	
	public DetectInfo testSystemDate()
	{
		DetectInfo detectInfo = new DetectInfo();
		try
		{
			String sysDateStr = getServerSystemDate();
			if (sysDateStr != null)
			{
				long sysDate = Long.parseLong(sysDateStr);// 服务器系统时间
				long localDate = System.currentTimeMillis();// 客户机系统时间
				long timeDelay = Math.abs(DateUtil.getMinutesFromBeginToEnd(sysDate, localDate));
				
				detectInfo.getDetailDatas().add(sysDate + SEPARATOR + localDate + SEPARATOR
						+ timeDelay);
				detectInfo.setDescInfo("server:"
						+ DateUtil.getStingDate(DateUtil.YYYY_MM_DD_HH24_MM_SS, new Date(sysDate))
						+ " agent:"
						+ DateUtil
								.getStingDate(DateUtil.YYYY_MM_DD_HH24_MM_SS, new Date(localDate)));
			} else
			{
//				detectInfo.setDescInfo("获取DataController系统时间失败");
				detectInfo.setDescInfo("i18n_client.SystemInfo.getSysTimeErr_n81i");
			}
			
		} catch (Exception e)
		{
			logger.error("Getting the time anomaly of the system", e);
			return null;
		}
		return detectInfo;
	}
	
	/*
	 * 获取服务端系统时间
	 */
	private static String getServerSystemDate()
	{
		String date = null;
		try
		{
			Future<?> future = Common.service.submit(new SSLClient(
					Thread.currentThread().getName(), CommonSocket.REQ_SERVER_SYSTEMDATE, null));
			String msg = (String) future.get();
			if (Contants.isSucessByResult(msg))
			{
				date = Contants.getDescByResult(msg);
			}
		} catch (Exception e)
		{
			logger.error("Getting the time exception of the server system：" + Utils.printExceptionStack(e));
			date = null;
		}
		
		return date;
	}
	
	public DetectInfo testProcessInfo(String pidFile, final String procPath, String procSearchKey,
			boolean isStart)
	{
		DetectInfo detectInfo = new DetectInfo();
		try
		{
			Object[] objArr = ProcessUtil.checkPidAndGetPid(pidFile, procSearchKey);
			int isExistFlag = Integer.parseInt(objArr[0].toString());
			String pidInfo = objArr[1].toString();
			
			if (isStart && isExistFlag == 0)
			{// Agent启动且进程不存在，启动
				logger.info("进程“" + procPath + "”不存在，正在启动……");
				// -------------------启动进程开始
				final String threadName = Thread.currentThread().getName();
				Future future = Common.service.submit(new Runnable()
				{
					
					@Override
					public void run()
					{
						thread = Thread.currentThread();
						Thread.currentThread().setName(threadName);
						try
						{
							ProcessUtil.runExec(procPath, null, null, null);
						} catch (IOException e)
						{
							logger.error(e);
						}
					}
					
				});
				// ----获取最大等待时间（单位秒）
				long delay = 3 * 60;// 3分钟
				// ----超过一定时间，终止执行线程，返回结果超时
				try
				{
					future.get(delay, TimeUnit.SECONDS);
				} catch (TimeoutException e)
				{
					if (thread != null && thread.isAlive())
					{
						thread.stop();
						logger.debug(procPath + "---testProcessInfo Timeout stop thread--"
								+ thread.isAlive());
					}
					future.cancel(true);
					logger.info("执行“" + procPath + "”， 超时");
				}
				// -------------------启动进程结束
				// -------------------检查PID
				objArr = ProcessUtil.checkPidAndGetPid(pidFile, procSearchKey);
				isExistFlag = Integer.parseInt(objArr[0].toString());
				pidInfo = objArr[1].toString();
				if (isExistFlag != 0)
				{
					logger.info("进程“" + procPath + "”启动成功");
				} else
				{
					logger.info("进程“" + procPath + "”启动失败");
				}
			}
			logger.debug("testProcessInfo pidInfo --> " + pidInfo);
			if(isExistFlag == 0){
				StringBuffer sb = new StringBuffer();
				//sb.append(CpuPerc.format(procCpu.getTotal() * 1.0 / cpuTotal));// 进程的CPU使用率
				sb.append(SEPARATOR);
				//sb.append(doubleFormat((memUse * 1.0 / sigar.getMem().getTotal()) * 100, 1) + "%");// 进程的内存使用率
				sb.append(SEPARATOR);
				//sb.append(procCpu.getStartTime());// 进程的启动时间
				sb.append(SEPARATOR);
				sb.append("NO");// 进程状态
				sb.append(SEPARATOR);
				//sb.append(state.getPriority());// 进程优先级
				
				int index = 0;
				String[] tmp = sb.toString().split(SEPARATOR);
				
				detectInfo.getDetailDatas().add(sb.toString().replace("%", ""));
				detectInfo.setDescInfo(pidInfo);
				return detectInfo;
			}
			if (isExistFlag != 1)
			{// 进程不存在或找到多个进程
				detectInfo.setDescInfo(pidInfo);
				return detectInfo;
			} else
			{
				long pid = Long.parseLong(pidInfo);
				
				Sigar sigar = new Sigar();
				// 获取进程的CPU信息
				ProcCpu procCpu = sigar.getProcCpu(pid);
				long cpuTotal = 0;
				Cpu[] cpuArr = sigar.getCpuList();
				for (Cpu cpu2: cpuArr)
				{
					cpuTotal += cpu2.getTotal();
				}
				logger.debug("testProcessInfo---->cpuTotal=" + cpuTotal);
				logger.debug("testProcessInfo---->cpuUse=" + procCpu.getTotal());
				
				ProcState state = sigar.getProcState(pid);
				
				Ps ps = new Ps();
				List<String> list = ps.getInfo(sigar, pid);
				String memUseStr = "";
				for (int i = 0; i < list.size(); i++)
				{
					switch (i)
					{
						// case 0 : System.out.print("--Pid=" + list.get(0));
						// break;
						// case 1 : System.out.print("\tUser=" + list.get(1));
						// break;
						// case 2 : System.out.print("\tStartTimes=" +
						// list.get(2)); break;
						// case 3 : memSizeStr= list.get(3); break;
						case 4:
							memUseStr = list.get(4);
							break;
						// case 5 : logger.debug("testProcessInfo---->MemHare="
						// + list.get(5)); break;
						// case 6 : System.out.print("\tState=" + list.get(6));
						// break;
						// case 7 : System.out.print("\tCpuTime=" +
						// list.get(7)); break;
						// case 8 : System.out.println("\tName=" + list.get(8));
						// break;
						default:
							break;
					}
				}
				double memUse = Double.parseDouble(memUseStr.substring(0, memUseStr.length() - 1));
				char useUnit = memUseStr.toUpperCase().charAt(memUseStr.length() - 1);
				switch (useUnit)
				{
					case 'K':
						memUse = memUse * 1024;
						break;
					case 'M':
						memUse = memUse * 1024 * 1024;
						break;
					case 'G':
						memUse = memUse * 1024 * 1024 * 1024;
						break;
					case 'T':
						memUse = memUse * 1024 * 1024 * 1024 * 1024;
						break;
					case 'P':
						memUse = memUse * 1024 * 1024 * 1024 * 1024 * 1024;
						break;
					default:
						break;
				}
				
				logger.debug("testProcessInfo---->memTotal=" + sigar.getMem().getTotal() / 1024
						+ "KB");
				logger.debug("testProcessInfo---->memUseStr=" + memUseStr);
				logger.debug("testProcessInfo---->memUse=" + memUse);
				
				StringBuffer sb = new StringBuffer();
				sb.append(CpuPerc.format(procCpu.getTotal() * 1.0 / cpuTotal));// 进程的CPU使用率
				sb.append(SEPARATOR);
				sb.append(doubleFormat((memUse * 1.0 / sigar.getMem().getTotal()) * 100, 1) + "%");// 进程的内存使用率
				sb.append(SEPARATOR);
				sb.append(procCpu.getStartTime());// 进程的启动时间
				sb.append(SEPARATOR);
				sb.append(state.getState());// 进程状态
				sb.append(SEPARATOR);
				sb.append(state.getPriority());// 进程优先级
				
				int index = 0;
				String[] tmp = sb.toString().split(SEPARATOR);
				StringBuffer sb2 = new StringBuffer();
				sb2.append("cpuUsage:" + tmp[index++]);
				sb2.append(" memUsage:" + tmp[index++]);
				sb2.append(" startTime:"
						+ DateUtil.getStingDate(DateUtil.YYYY_MM_DD_HH24_MM_SS, new Date(Long
								.parseLong(tmp[index++]))));
				sb2.append(" state:" + tmp[index++]);
				sb2.append(" priority:" + tmp[index++]);
				
				detectInfo.getDetailDatas().add(sb.toString().replace("%", ""));
				detectInfo.setDescInfo(sb2.toString());
				
				logger.debug("testProcessInfo---->" + sb2.toString());
				
			}
		} catch (Exception e)
		{
			logger.error("Getting process information abnormity", e);
			return null;
		}
		
		return detectInfo;
	}
	
	public DetectInfo testSystemInfo()
	{
		DetectInfo detectInfo = new DetectInfo();
		try
		{
			Sigar sigar = new Sigar();
			
			StringBuffer sb = new StringBuffer();
			sb.append(sigar.getNetInfo().getHostName());// 主机名称
			sb.append(SEPARATOR);
			// 操作系统和发行版本 (os.getVendorName() + " " + os.getVersion())
			// Linux: uname -r;uname -i;lsb_release -d| cut -d: -f2| cut -f2
			// Windows: ver
			String operateSystem = Utils.getOperateSystem();
			sb.append(operateSystem);// 操作系统和发行版本
			sb.append(SEPARATOR);
			sb.append(sigar.getCpuInfoList().length);// CPU核数
			sb.append(SEPARATOR);
			sb.append(sigar.getCpuInfoList()[0].getMhz());// CPU主频
			sb.append(SEPARATOR);
			String totalMem = doubleFormat(sigar.getMem().getTotal() / (1024 * 1024 * 1024.0), 2);
			sb.append(totalMem);// 内存大小，单位：G
			sb.append(SEPARATOR);
			sb.append(doubleFormat(sigar.getSwap().getTotal() / (1024 * 1024 * 1024.0), 2));// SWAP大小，单位：G
			sb.append(SEPARATOR);
			
			long allTotal = 0;
			// **************** 以下是各分区大小 ******************* //
			List<String[]> diskDatas = new ArrayList<String[]>();
			FileSystem fslist[] = sigar.getFileSystemList();
			for (int i = 0; i < fslist.length; i++)
			{
				FileSystem fs = fslist[i];
				try
				{
					FileSystemUsage usage = sigar.getFileSystemUsage(fs.getDirName());
					if (fs.getType() == 2)
					{// 先取每个盘符的大小和剩余空间
						long total = usage.getTotal();// 文件系统大小, 单位："KB"
						allTotal += total;// 文件系统总大小, 单位："KB"
						// 各分区名称和大小(单位G)
						diskDatas.add(new String[] { fs.getDirName(),
								doubleFormat(total / (1024 * 1024.0), 2) });
					}
				} catch (SigarException e)
				{
					if (fs.getType() == 2)
						throw e;
					continue;
				}
			}
			// ---------------- 以上是各分区大小 ------------------- //
			sb.append(doubleFormat(allTotal / (1024 * 1024.0), 2));// 硬盘总大小，单位：G
			sb.append(SEPARATOR);
			
			int ethernetCnt = 0;
			// **************** 以下是各网卡状态 ******************* //
			List<String[]> netDatas = new ArrayList<String[]>();
			String ifNames[] = sigar.getNetInterfaceList();
			if (ifNames.length > 0)
			{
				String defaultGateWay = sigar.getNetInfo().getDefaultGateway();
				for (int i = 0; i < ifNames.length; i++)
				{
					String name = ifNames[i];
					
					NetInterfaceConfig ifconfig = sigar.getNetInterfaceConfig(name);
					
					// *** 网卡类型：ethernet, Local Loopback(本地环回网卡)
					// *** 这里只取 ethernet 网卡类型
					if ((ifconfig.getFlags() & NetFlags.IFF_LOOPBACK) > 0)
					{
						continue;
					}
					/*
					 * if ((ifconfig.getFlags() & NetFlags.IFF_POINTOPOINT) > 0) {
					 * continue; }
					 */
					// 2013-4-22 针对LInux，按名称过滤掉sit0，实际命令查看类型为：IPv6-in-IPv4
					if (name.equals("sit0"))
					{
						continue;
					}
					// 2013-5-23 针对Win7取到的网卡数量太多问题
					if ((ifconfig.getFlags() & NetFlags.IFF_BROADCAST) <= 0)
					{// broadcast地址无效
						continue;
					}
					if ((ifconfig.getFlags() & NetFlags.IFF_MULTICAST) <= 0)
					{// 不支持multicast
						continue;
					}
					ethernetCnt++;
					
//					String state = "可用";
					String state = "Available";
					if ((ifconfig.getFlags() & NetFlags.IFF_UP) <= 0)
					{// 网卡状态: 2050 网络电缆被拔出 2115 已连接上
//						state = "不可用";
						state = "Unavailable";
					}
					String speed = null;
					try
					{
						NetInterfaceStat ifstat = sigar.getNetInterfaceStat(name);
						speed = ifstat.getSpeed() / (1000 * 1000) + "";// 带宽：Mbps
					} catch (SigarException e)
					{
						speed = "";
					}
					
					netDatas.add(new String[] { name, // 网卡名称
							state, // 网卡状态
							speed, // 带宽
							ifconfig.getAddress(), // IP
							ifconfig.getNetmask(), // 子网掩码
							defaultGateWay, // 网关
							ifconfig.getHwaddr() // Mac地址
							});
				}
			}
			// System.out.println(sigar.getNetInfo().getDefaultGateway());
			// ---------------- 以上是各网卡状态 ------------------- //
			sb.append(ethernetCnt);// 网卡数量
			
			detectInfo.getDetailDatas().add(sb.toString());
			
			StringBuffer sb2 = new StringBuffer();
//			sb2.append(sigar.getNetInfo().getHostName() + "：" + operateSystem + ", "
//					+ sigar.getCpuInfoList().length + "核CPU, " + sigar.getCpuInfoList()[0].getMhz()
//					+ "MHz, " + totalMem + "GB的内存, " + doubleFormat(allTotal / (1024 * 1024.0), 2)
//					+ "GB的硬盘, " + ethernetCnt + "个逻辑网卡");
			sb2.append(sigar.getNetInfo().getHostName() + "：" + operateSystem + ", "
					+ sigar.getCpuInfoList().length + "i18n_client.SystemInfo.message.core_n81i CPU, " + sigar.getCpuInfoList()[0].getMhz()
					+ "MHz, " + totalMem + "i18n_client.SystemInfo.message.memery_n81i, " + doubleFormat(allTotal / (1024 * 1024.0), 2)
					+ "i18n_client.SystemInfo.message.disk_n81i, " + ethernetCnt + "i18n_client.SystemInfo.message.netcard_n81i");
			detectInfo.setDescInfo(sb2.toString());
			logger.debug("testSystemInfo--sb2--" + sb2.toString());
			
			Map<String, List<String[]>> relatedDataMap = new HashMap<String, List<String[]>>();
			relatedDataMap.put("disk", diskDatas);
			relatedDataMap.put("net", netDatas);
			detectInfo.setRelatedDatas(relatedDataMap);
			
		} catch (Exception e)
		{
			logger.error("Getting the information anomaly of the system", e);
			return null;
		}
		
		return detectInfo;
	}
	
	public static String getStartComputerTime()
	{
		String value = "";
		Sigar sigar = new Sigar();
		try
		{
			Uptime uptime = sigar.getUptime();
			Double d = new Double(uptime.getUptime());
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.SECOND, -d.intValue());
			
			value = cal.getTimeInMillis() + "";
		} catch (SigarException e)
		{
			logger.error("Getting the boot time anomaly", e);
			return value;
		}
		
		return value;
	}
	
	private String doubleFormat(double val, int xsws)
	{
//		String pattern = "#.";
		String pattern = "0.";
		if (xsws < 1)
		{
			xsws = 1;
		}
		for (int i = 1; i <= xsws; i++)
		{
			pattern += "0";
		}
		
		String temp = new DecimalFormat(pattern).format(val);
//		if (val < 1)
//		{
//			return "0" + temp;
//		} else
//		{
//			return temp;
//		}
		return temp;
	}
	
	/**
	 * 检查硬盘是否可写：在指定盘符目录下写指定字节大小的文件，写异常-不可写，正常-可写
	 * 
	 * @param fsDirName
	 *            盘符
	 * @return
	 */
	private boolean checkFileSystemIsWrite(String fsDirName)
	{
		boolean isWrite = true;
		OutputStreamWriter fos = null;
		File file = new File(fsDirName + File.separator + "writetest.temp");
		logger.debug("checkFileSystemIsWrite---file path = " + file.getAbsolutePath());
		try
		{
			fos = new OutputStreamWriter(new FileOutputStream(file), "utf-8");
//			fos.write("测试硬盘是否可写，现在开始写入文件\n");
			fos.write("i18n_client.SystemInfo.outputTest_n81i");
			fos.flush();
		} catch (IOException e)
		{
			logger.error("Test whether the hard disk can be written：" + e.getMessage());
			isWrite = false;
		} finally
		{
			if (fos != null)
			{
				try
				{
					fos.close();
				} catch (IOException e)
				{
				}
			}
		}
		if (file.exists())
		{// 不管是否写异常都将删除文件
			file.delete();
			logger.debug("checkFileSystemIsWrite---delete file = " + file.getAbsolutePath());
		}
		return isWrite;
	}
	
	public SystemInfo()
	{
	}
	
}
