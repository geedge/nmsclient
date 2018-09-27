#!/usr/bin/env python
# -*- coding: UTF-8 -*-

# created by zhongyoub 
'''
get all net card info and write to file with csv format 
time: ms
'''

import time,re,os,csv
import ConfigParser
import StringIO
from datetime import datetime

global cfg_path_str 
global nic_path_str
global marsio_cmd
cfg_path_str="/home/nms/nc_config/net_net/net_net.cfg"
nic_path_str="/proc/net/dev"
marsio_cmd="monit_device"

def round2(a):
    return ("%.2f"%a)

class monitor_net():
    def __init__(self):
        self.seqId=""                        # seqId
        self.id=""                           # 监测设置ID
        self.cattory=""                      # 监测类别
        self.process=""                      # 进程名称
        
        self.start_time=int(round(time.time()*1000))          # 监测服务启动时间
        self.delay=0                         # 检测时延
        self.checktime=int(round(time.time()*1000))           # 本次检测时间
        self.try_times=1                     # 尝试次数
        self.next_time=0                     # 下次监测时间  self.start_time + self.interval
        self.checkState=1                    # 检测状态
        
        self.checkGap=0
        self.config_path=cfg_path_str
        self.nic_path=nic_path_str
        
        '''
        nic info 
        '''
	self.nic_info={"eth_name":"", "rx_packets":0, "tx_packets":0, "rx_bytes":0, "tx_bytes":0,
                       "rx_errs":0, "tx_errs":0, "rx_drop":0, "tx_drops":0, "speed":10000,
                       "rx_bps":0, "tx_bps":0, "rx_pps":0, "tx_pps":0,
                       "rx_err_perc":0, "tx_err_perc":0, "rx_drop_perc":0, "tx_drop_perc":0 }
	self.marsio_info={"eth_name":"", "rx_packets":0, "tx_packets":0, "rx_bytes":0, "tx_bytes":0,
                       "rx_errs":0, "tx_errs":0, "rx_drop":0, "tx_drops":0, "speed":10000,
                       "rx_bps":0, "tx_bps":0, "rx_pps":0, "tx_pps":0,
                       "rx_err_perc":0, "tx_err_perc":0, "rx_drop_perc":0, "tx_drop_perc":0 }
     #   self.vf_dict={'0':'','1':'','2':''}
	self.vf_list=[]
        self.marsio_list=[]
        
        
    '''
    read config file
    '''    
    def read_config(self):
	'''
        with open(self.config_path, 'r') as f:
            section_string = '[dummy_section]\n' + f.read()
	'''
	
	config=StringIO.StringIO()
	config.write('[dummy_section]\n')
	config.write(open(self.config_path).read())
	config.seek(0,os.SEEK_SET)	

        cfg = ConfigParser.ConfigParser()
   #     cfg.readfp(section_string)
	cfg.readfp(config)
        pubInfo=cfg.get("dummy_section", "pubInfo")
        self.seqId=pubInfo.split(",",3)[0]
        self.id=pubInfo.split(",",3)[1]
        self.cattory=pubInfo.split(",",3)[2]
        self.process=pubInfo.split(",",3)[3]
        
        self.delay=cfg.get("dummy_section","checkOutTime")
        self.try_times=cfg.get("dummy_section","checkMaxTimes")
        self.checkGap=int(cfg.get("dummy_section","checkGap"))*60*1000       # ms  
        self.next_time=self.start_time+self.checkGap
        self.checkState=cfg.get("dummy_section","checkState")
          
            
    '''
    get net card info/home/nms/nc_config/net_net
    '''
    def get_vfnet_info(self):
        net_info=open("/proc/net/dev","r")
        info=net_info.readlines()
        net_info.close()
#	print self.marsio_list
        for line in info:           # find nic name like ens*f*
	    self.nic_info={"eth_name":"", "rx_packets":0, "tx_packets":0, "rx_bytes":0, "tx_bytes":0,
                       "rx_errs":0, "tx_errs":0, "rx_drop":0, "tx_drops":0, "speed":10000,
                       "rx_bps":0, "tx_bps":0, "rx_pps":0, "tx_pps":0,
                       "rx_err_perc":0, "tx_err_perc":0, "rx_drop_perc":0, "tx_drop_perc":0 }
            if re.search(r'ens.f.',line):
                field=line.split()
                self.nic_info["eth_name"]=field[0].split(":")[0]
                self.nic_info["rx_bytes"]=field[1]
                self.nic_info["rx_packets"]=field[2]
                self.nic_info["rx_errs"]=field[3]
                self.nic_info["rx_drop"]=field[4]
                self.nic_info["tx_bytes"]=field[9]
                self.nic_info["tx_packets"]=field[10]
                self.nic_info["tx_errs"]=field[11]
                self.nic_info["tx_drop"]=field[12]
    #        	self.vf_dict[str(i)]=self.nic_info
    #		print "nic_info"
   #		print self.nic_info
  		self.vf_list.append(self.nic_info)
   #		print self.vf_list
#		print self.vf_list[str(i)]
                
            '''      
                eth_name=field[0].split(":")[0]
                rx_bytes=field[1]
                rx_packets=field[2]
                rx_errs=field[3]
                rx_drop=field[4]
                tx_bytes=field[9]
                tx_packts=field[10]
                tx_errs=field[11]
                tx_drop=field[12]
            return(eth_name, int(rx_bytes),int(rx_packets),int(rx_errs),int(rx_drop),int(tx_bytes),
                   int(tx_packts),int(tx_errs),int(tx_drop))
            '''       
    
    '''
    get marsio net card info by open(monit_device)
    '''
    def get_marsionet_info(self):
        info=os.popen(marsio_cmd)
        for line in info:
            if re.search(r'ens.f*',line):
                self.marsio_info["eth_name"]=line.split(",")[1].split(":")[1].strip()
            if re.search(r'Accu',line):
                self.marsio_info["rx_packets"]=line.split()[1]    
                self.marsio_info["rx_bytes"]=int(line.split()[2])/8    # bit to byte(float)
                self.marsio_info["rx_drop"]=line.split()[9]
                self.marsio_info["rx_errs"]=line.split()[4]
                self.marsio_info["tx_packets"]=line.split()[6]
                self.marsio_info["tx_bytes"]=int(line.split()[7])/8    
                self.marsio_info["tx_errs"]=line.split()[8]
                self.marsio_info['tx_drop']=line.split()[10]  
                
            if re.search(r'Second',line):
                self.marsio_info["rx_bps"]=line.split()[3]
                rx_packets=self.marsio_info["rx_pps"]=line.split()[2]
                tx_packets=self.marsio_info["tx_pps"]=line.split()[7]
                self.marsio_info["tx_bps"]=line.split()[8]
                
                rx_err_per=line.split()[5]
                tx_err_per=line.split()[9]
		if int(rx_packets)!=0:
                    self.marsio_info["rx_err_perc"]=round2(float(rx_err_per)/int(rx_packets))
		else:
		    self.marsio_info["rx_err_perc"]=round2(0)
#		print self.marsio_info["rx_err_perc"]
		if int(tx_packets)!=0:
                    self.marsio_info["tx_err_perc"]=round2(float(tx_err_per)/int(tx_packets))
		else:
		    self.marsio_info["tx_err_perc"]=round2(0)
 #               print self.marsio_info["tx_err_perc"]
                rx_drop_per=line.split()[10]
                tx_drop_per=line.split()[11]
		if int(rx_packets)!=0:
                    self.marsio_info["rx_drop_perc"]=round2(float(rx_drop_per)/int(rx_packets))
		else:
	            self.marsio_info["rx_drop_perc"]=round2(0)
#		print self.marsio_info["rx_drop_perc"]
		if int(tx_packets)!=0:
                    self.marsio_info["tx_drop_perc"]=round2(float(tx_drop_per)/int(tx_packets))
		else:
	            self.marsio_info["tx_drop_perc"]=round2(0)
#		print self.marsio_info["tx_drop_perc"]
        self.marsio_list.append(self.marsio_info)
#	print "marsio_info"
#	print self.marsio_info

def list_append(list1, dict1):
    list1.append(dict1["eth_name"])
    list1.append(dict1["rx_packets"])
    list1.append(dict1["tx_packets"])
    list1.append(dict1["rx_bytes"])
    list1.append(dict1["tx_bytes"])
    list1.append(dict1["rx_errs"])
    list1.append(dict1["tx_errs"])
    list1.append(dict1["rx_drop"])
    list1.append(dict1["tx_drop"])
    list1.append(dict1["speed"])
    list1.append(dict1["rx_bps"])
    list1.append(dict1["tx_bps"])
    list1.append(dict1["rx_pps"])
    list1.append(dict1["tx_pps"])
    list1.append(dict1["rx_err_perc"])
    list1.append(dict1["tx_err_perc"])
    list1.append(dict1["rx_drop_perc"])
    list1.append(dict1["tx_drop_perc"])           

def main():
    monitor=monitor_net()
    monitor.read_config()
    monitor.chectime=int(round(time.time()*1000))
    monitor.get_marsionet_info()
    list3=monitor.marsio_list
    monitor.get_vfnet_info()	
    list1=monitor.vf_list
#    print "list1"
#    print list1
    monitor.vf_list=[]
#    print monitor.vf_list
 #   monitor.vf_list.clear()
    time.sleep(1)
    monitor.get_vfnet_info()
    list2=monitor.vf_list   
#    print "list2"
#    print list2    
    for x, y in zip(list1,list2):
        rx_bytes_pers=int(y["rx_bytes"])-int(x["rx_bytes"])
        tx_bytes_pers=int(y["tx_bytes"])-int(x["tx_bytes"])
        rx_packet_pers=float(y["rx_packets"])-int(x["rx_packets"])
        tx_packet_pers=float(y["tx_packets"])-int(x["tx_packets"])
        
        rx_bps=rx_bytes_pers*8
        rx_pps=rx_packet_pers
        tx_bps=tx_bytes_pers*8
        tx_pps=tx_packet_pers
	if rx_packet_pers >0:
            rx_err_rate=(int(y["rx_errs"])-int(x["rx_errs"]))/rx_packet_pers
        else:
            rx_err_rate=0
        if tx_packet_pers>0:
            tx_err_rate=(int(y["tx_errs"])-int(x["tx_errs"]))/tx_packet_pers
        else:
            tx_err_rate=0
        if rx_packet_pers>0:
            rx_drop_rate=(int(y["rx_drop"])-int(x["rx_drop"]))/rx_packet_pers
        else:
            rx_drop_rate=0
        if tx_packet_pers>0:
            tx_drop_rate=(int(y["tx_drop"])-int(x["tx_drop"]))/tx_packet_pers
        else:
            tx_drop_rate=0
        '''
        y["rx_bps"]=round2(rx_bps)
        y["rx_pps"]=round2(rx_pps)
        y["tx_bps"]=round2(tx_bps)
        y["tx_pps"]=round2(tx_pps)
	'''
        y["rx_err_perc"]=round2(rx_err_rate)
        y["tx_err_perc"]=round2(tx_err_rate)
        y["rx_drop_perc"]=round2(rx_drop_rate)
        y["tx_drop_perc"]=round2(tx_drop_rate)
        
#    print list2 
#    print list3  
    vf_num=len(monitor.vf_list)
 #   print monitor.vf_list
 #   print vf_num
    marsio_num=len(list3)
  
    csv_file=datetime.now().strftime("%Y%m%d%H%M%S")
    
    header=[monitor.seqId,monitor.id,monitor.cattory,monitor.process,monitor.start_time,monitor.delay,
            monitor.chectime,monitor.try_times,monitor.next_time,monitor.checkState]
    
    first_str=str(vf_num)+"i18n_client.SystemInfo.insert_n81i: "
    for each in list2:
        first_str=first_str+"$@$"+each["eth_name"]+ \
        " i18n_client.SystemInfo.netSpeed_n81i10000Mbps,"+"i18n_client.SystemInfo.input_n81i"+ \
        str(each["rx_bps"])+"bps、"+str(each["rx_pps"])+"pps,"+"Output"+str(each["tx_bps"])+"bps、"+str(each["tx_pps"])+ \
        "pps;"
    
    second_str=str(marsio_num)+"i18n_client.SystemInfo.insert_n81i: "
    for each in list3:
        second_str=second_str+"$@$"+each["eth_name"]+ \
        " i18n_client.SystemInfo.netSpeed_n81i10000Mbps,"+"i18n_client.SystemInfo.input_n81i"+ \
        str(each["rx_bps"])+"bps、 "+str(each["rx_pps"])+"pps,"+"Output"+str(each["tx_bps"])+"bps、"+str(each["tx_pps"])+ \
        "pps;"
        
    four_1=['\"','','']
    four_2=['\"','','']
    four_3=['\"','','']
    if len(list2)>1: 
    	first_dict=list2[0]
    	second_dict=list2[1]
	list_append(four_1,first_dict)
	list_append(four_2,second_dict)
    elif len(list2)==1:
	first_dict=list2[0]
	list_append(four_1,first_dict)
	four_2=[]
    else:
	four_1=[]
	four_2=[]
    if len(list3)>0:
    	three_dict=list3[0]
	'''
	three_dict["rx_bps"]=round2(int(three_dict["rx_bps"]))
	three_dict["rx_pps"]=round2(int(three_dict["rx_pps"]))
	three_dict["tx_bps"]=round2(int(three_dict["tx_bps"]))
	three_dict["tx_bps"]=round2(int(three_dict["tx_bps"]))
	three_dict["rx_err_perc"]=round2(int(three_dict["rx_err_perc"]))
	three_dict["tx_err_perc"]=round2(int(three_dict["tx_err_perc"]))
	three_dict["rx_drop_perc"]=round2(int(three_dict["rx_drop_perc"]))
	three_dict["tx_drop_perc"]=round2(int(three_dict["tx_drop_perc"]))
	'''
    	list_append(four_3,three_dict)
    else:
	four_3=[]
    '''
    four_1.append(first_list["eth_name"])
    four_1.append(first_list["rx_packets"])
    four_1.append(first_list["tx_packets"])
    four_1.append(first_list["rx_bytes"])
    four_1.append(first_list["tx_bytes"])
    four_1.append(first_list["rx_errs"])
    four_1.append(first_list["tx_errs"])
    four_1.append(first_list["rx_drop"])
    four_1.append(first_list["tx_drop"])
    four_1.append(first_list["speed"])
    four_1.append(first_list["rx_bps"])
    four_1.append(first_list["tx_bps"])
    four_1.append(first_list["rx_pps"])
    four_1.append(first_list["tx_pps"])
    four_1.append(first_list["rx_err_perc"])
    four_1.append(first_list["tx_err_perc"])
    four_1.append(first_list["rx_drop_perc"])
    '''
#    print four_1
#    print four_2
#    print four_3
    header.append(first_str)
    header.append(second_str)
    two_list=["details"]
    two_list.append((vf_num+marsio_num))
    
    with open(csv_file+".csv","w") as f:
        f_writer=csv.writer(f,lineterminator='\n')
        f_writer.writerow(header)
        f_writer.writerow(two_list)
	if len(four_1):
            f_writer.writerow(four_1)
	if len(four_2):
            f_writer.writerow(four_2)
	if len(four_3):
            f_writer.writerow(four_3)
        f.close()
    
if __name__ == '__main__':
    main()
    exit(0)
        
    
    
    
