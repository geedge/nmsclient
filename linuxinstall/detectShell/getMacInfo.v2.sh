#!/bin/bash

basePath=$(cd `dirname $0`; pwd)
date=`date +%Y%m%d%H%M%S`
d=`date "+%Y-%m-%d %H:%M:%S"`  #temp column
t=`date -d "$d" +%s`  #temp column
t2=`date "+%N" | awk '{print int($0)}'`
dateStamp=$((t*1000+t2/1000000))  #current time stamp
sep=\$@\$

cd ${basePath}
while read line;do  # 读取config
        eval "$line"  
done < config

if [ ! -d macInfo ]
        then
                mkdir macInfo
fi

if [ ! -d result ]
        then
                mkdir result
fi

if [ ! -f firstTimeDate ]
        then
                touch firstTimeDate
                echo ${dateStamp} > firstTimeDate
                firstTimeDate=${dateStamp}
        else
                firstTimeDate=`cat firstTimeDate`
fi

# 读取任务配置文件
OLD_IFS="$IFS"
IFS=","

cpu_pubInfo=`cat ${configPath}/cpu_cpu.cfg | grep pubInfo | awk -F'=' '{print $2}'`
cpu_checkState=`cat ${configPath}/cpu_cpu.cfg | grep checkState | awk -F'=' '{print $2}'`
cpu_checkGap=`cat ${configPath}/cpu_cpu.cfg | grep checkGap | awk -F'=' '{print $2}'`
cpu_checkOutTime=`cat ${configPath}/cpu_cpu.cfg | grep checkOutTime | awk -F'=' '{print $2}'`
cpu_checkMaxTimes=`cat ${configPath}/cpu_cpu.cfg | grep checkMaxTimes | awk -F'=' '{print $2}'`
cpu_planCheckTime=`cat ${configPath}/cpu_cpu.cfg | grep planCheckTime | awk -F'=' '{print $2}'`
cpu_uploadGap=`cat ${configPath}/cpu_cpu.cfg | grep uploadGap | awk -F'=' '{print $2}'`
cpu_dataFileDir=`cat ${configPath}/cpu_cpu.cfg | grep dataFileDir | awk -F'=' '{print $2}'`
cpu_polices=`cat ${configPath}/cpu_cpu.cfg | grep police | awk -F'=' '{print $2}'`
cpu_polices_arr=($cpu_polices)
cpu_police_flag=0
cpu_nextDateStamp=$((cpu_checkGap*60*1000+dateStamp))
mem_pubInfo=`cat ${configPath}/memory_memory.cfg | grep pubInfo | awk -F'=' '{print $2}'`
mem_checkState=`cat ${configPath}/memory_memory.cfg | grep checkState | awk -F'=' '{print $2}'`
mem_checkGap=`cat ${configPath}/memory_memory.cfg | grep checkGap | awk -F'=' '{print $2}'`
mem_checkOutTime=`cat ${configPath}/memory_memory.cfg | grep checkOutTime | awk -F'=' '{print $2}'`
mem_checkMaxTimes=`cat ${configPath}/memory_memory.cfg | grep checkMaxTimes | awk -F'=' '{print $2}'`
mem_planCheckTime=`cat ${configPath}/memory_memory.cfg | grep planCheckTime | awk -F'=' '{print $2}'`
mem_uploadGap=`cat ${configPath}/memory_memory.cfg | grep uploadGap | awk -F'=' '{print $2}'`
mem_dataFileDir=`cat ${configPath}/memory_memory.cfg | grep dataFileDir | awk -F'=' '{print $2}'`
mem_polices=`cat ${configPath}/memory_memory.cfg | grep police | awk -F'=' '{print $2}'`
mem_polices_arr=($mem_polices)
mem_police_flag=0
mem_nextDateStamp=$((mem_checkGap*60*1000+dateStamp))
disk_pubInfo=`cat ${configPath}/disk_disk.cfg | grep pubInfo | awk -F'=' '{print $2}'`
disk_checkState=`cat ${configPath}/disk_disk.cfg | grep checkState | awk -F'=' '{print $2}'`
disk_checkGap=`cat ${configPath}/disk_disk.cfg | grep checkGap | awk -F'=' '{print $2}'`
disk_checkOutTime=`cat ${configPath}/disk_disk.cfg | grep checkOutTime | awk -F'=' '{print $2}'`
disk_checkMaxTimes=`cat ${configPath}/disk_disk.cfg | grep checkMaxTimes | awk -F'=' '{print $2}'`
disk_planCheckTime=`cat ${configPath}/disk_disk.cfg | grep planCheckTime | awk -F'=' '{print $2}'`
disk_uploadGap=`cat ${configPath}/disk_disk.cfg | grep uploadGap | awk -F'=' '{print $2}'`
disk_dataFileDir=`cat ${configPath}/disk_disk.cfg | grep dataFileDir | awk -F'=' '{print $2}'`
disk_polices=`cat ${configPath}/disk_disk.cfg | grep police | awk -F'=' '{print $2}'`
disk_polices_arr=($disk_polices)
disk_police_flag=0
disk_nextDateStamp=$((disk_checkGap*60*1000+dateStamp))
net_pubInfo=`cat ${configPath}/net_net.cfg | grep pubInfo | awk -F'=' '{print $2}'`
net_checkState=`cat ${configPath}/net_net.cfg | grep checkState | awk -F'=' '{print $2}'`
net_checkGap=`cat ${configPath}/net_net.cfg | grep checkGap | awk -F'=' '{print $2}'`
net_checkOutTime=`cat ${configPath}/net_net.cfg | grep checkOutTime | awk -F'=' '{print $2}'`
net_checkMaxTimes=`cat ${configPath}/net_net.cfg | grep checkMaxTimes | awk -F'=' '{print $2}'`
net_planCheckTime=`cat ${configPath}/net_net.cfg | grep planCheckTime | awk -F'=' '{print $2}'`
net_uploadGap=`cat ${configPath}/net_net.cfg | grep uploadGap | awk -F'=' '{print $2}'`
net_dataFileDir=`cat ${configPath}/net_net.cfg | grep dataFileDir | awk -F'=' '{print $2}'`
net_polices=`cat ${configPath}/net_net.cfg | grep police | awk -F'=' '{print $2}'`
net_polices_arr=($net_polices)
net_police_flag=0
net_nextDateStamp=$((net_checkGap*60*1000+dateStamp))
sys_pubInfo=`cat ${configPath}/systeminfo_servicessysinfo.cfg | grep pubInfo | awk -F'=' '{print $2}'`
sys_checkState=`cat ${configPath}/systeminfo_servicessysinfo.cfg | grep checkState | awk -F'=' '{print $2}'`
sys_checkGap=`cat ${configPath}/systeminfo_servicessysinfo.cfg | grep checkGap | awk -F'=' '{print $2}'`
sys_checkOutTime=`cat ${configPath}/systeminfo_servicessysinfo.cfg | grep checkOutTime | awk -F'=' '{print $2}'`
sys_checkMaxTimes=`cat ${configPath}/systeminfo_servicessysinfo.cfg | grep checkMaxTimes | awk -F'=' '{print $2}'`
sys_planCheckTime=`cat ${configPath}/systeminfo_servicessysinfo.cfg | grep planCheckTime | awk -F'=' '{print $2}'`
sys_uploadGap=`cat ${configPath}/systeminfo_servicessysinfo.cfg | grep uploadGap | awk -F'=' '{print $2}'`
sys_dataFileDir=`cat ${configPath}/systeminfo_servicessysinfo.cfg | grep dataFileDir | awk -F'=' '{print $2}'`
sys_polices=`cat ${configPath}/systeminfo_servicessysinfo.cfg | grep police | awk -F'=' '{print $2}'`
sys_polices_arr=($sys_polices)
sys_police_flag=0
sys_nextDateStamp=$((sys_checkGap*60*1000+dateStamp))

IFS="$OLD_IFS"

sysinfoSrc=`cat ${sysinfoPath}`

## begin content weaving ##
# task info
cpuResult_taskInfo=${cpu_pubInfo}","${firstTimeDate}","${cpu_checkOutTime}","${dateStamp}",1,"${cpu_nextDateStamp}","
memResult_taskInfo=${mem_pubInfo}","${firstTimeDate}","${mem_checkOutTime}","${dateStamp}",1,"${mem_nextDateStamp}","
diskResult_taskInfo=${disk_pubInfo}","${firstTimeDate}","${disk_checkOutTime}","${dateStamp}",1,"${disk_nextDateStamp}","
netResult_taskInfo=${net_pubInfo}","${firstTimeDate}","${net_checkOutTime}","${dateStamp}",1,"${net_nextDateStamp}","
sysResult_taskInfo=${sys_pubInfo}","${firstTimeDate}","${sys_checkOutTime}","${dateStamp}",1,"${sys_nextDateStamp}",1,"

cpuResult_coreNum=0
disk_total_num=0
net_total_num=0
sys_net_total_num=0
diskResult_baseInfo="\""
memResult_baseInfo="\""
netResult_baseInfo="\""
sysResult_baseInfo="\""
# result file

cpuResult=result/cpuResult_${date}
memResult=result/memResult_${date}
diskResult=result/diskResult_${date}
netResult=result/netResult_${date}
sysResult=result/sysResult_${date}

function getDetail {
	cpuInfo=macInfo/$1_cpuInfo_${date}
        memInfo=macInfo/$1_memInfo_${date}
        diskInfo=macInfo/$1_diskInfo_${date}
        netInfo=macInfo/$1_netInfo_${date}
	sysInfo=macInfo/$1_sysInfo_${date}

        ssh -tt $2 "sar -P ALL 1 1" > ${cpuInfo}
        ssh -tt $2 "cat /proc/cpuinfo | grep MHz" > ${cpuInfo}2
        ssh -tt $2 "free|awk 'NR>1{print}'" > ${memInfo}
        ssh -tt $2 "df -l|grep -v tmpfs|awk 'NR>1{print}'" > ${diskInfo}

	# 网卡信息
	special_net_result_t=`ssh -C $2 "/bin/bash" < remote_net.sh` # 获取特殊网卡信息
	special_net_result_base=${special_net_result_t%details,*}
	special_net_result_details_t=${special_net_result_t#*details,}
	special_net_result_details="\""${special_net_result_details_t#*\"}
	special_net_result_details_t_arr=($special_net_result_details_t)
	special_net_result_detailsNum=${special_net_result_details_t_arr[0]}
	special_net_result=${special_net_result_base}"details,"${special_net_result_detailsNum}$'\n'
	for((i=1;i<${#special_net_result_details_t_arr[@]};i++));do
		special_net_result=${special_net_result}${special_net_result_details_t_arr[$i]}$'\n'
	done
	echo "$special_net_result" > ${netInfo}3
	getNetInfo

        dos2unix ${cpuInfo} > /dev/null 2>&1
        dos2unix ${cpuInfo}2 > /dev/null 2>&1
        dos2unix ${memInfo} > /dev/null 2>&1
        dos2unix ${diskInfo} > /dev/null 2>&1
	#dos2unix ${diskInfo} > /dev/null 2>&1
	
	# cpu info
	cpu_t=`awk -F "(" '{print $3}' ${cpuInfo}` # temp column
	
        cpu_coreNum=`echo $cpu_t|awk -F " " '{print $1}'`
	cpuResult_coreNum=$((cpuResult_coreNum+cpu_coreNum))
        cpu_totalMHz=`awk '{total+=$4}END{print total}' ${cpuInfo}2`
        cpu_averMHz=`echo "scale=2;$cpu_totalMHz/$cpu_coreNum"|bc|awk '{printf "%.2f",$0}'`
        cpu_idle_total=`awk 'NR==4{print $9}' ${cpuInfo}`
        cpu_shiyonglv=`bc <<<100-$cpu_idle_total|awk '{printf "%.2f",$0}'`
	
	cpuResult_baseInfo=${cpuResult_baseInfo}${sep}"cpu${index} i18n_client.SystemInfo.mhz_n81i"${cpu_averMHz}"MHz，i18n_client.SystemInfo.shiyonglv_n81i "${cpu_shiyonglv}"%; "
	getCpuBase2 ${cpuInfo} ${cpuInfo}2
	getCpuDetail ${cpuInfo} ${cpuInfo}2

	# mem info
	swap_total_t=`cat ${memInfo}|grep '^S'|awk '{print $2}'`
	swap_total=`echo "scale=2;${swap_total_t}/1024/1024"|bc|awk '{printf "%.2f",$0}'`
        swap_free_t=`cat ${memInfo}|grep '^S'|awk '{print $4}'`
        swap_free=`echo "scale=2;${swap_free_t}/1024/1024"|bc|awk '{printf "%.2f",$0}'`
        mem_total_t=`awk 'NR==1{print $2}' ${memInfo}`
        mem_total=`echo "scale=2;${mem_total_t}/1024/1024"|bc|awk '{printf "%.2f",$0}'`
        mem_used_t=`awk 'NR==1{print $3}' ${memInfo}`
        mem_used=`echo "scale=2;${mem_used_t}/1024/1024"|bc|awk '{printf "%.2f",$0}'`
        mem_free=`echo "${mem_total}-${mem_used}"|bc|awk '{printf "%.2f",$0}'`
        mem_shiyonglv=`echo "scale=2;${mem_used}/${mem_total}*100.00"|bc|awk '{printf "%.2f",$0}'`
        memResult_baseInfo="${memResult_baseInfo}"${sep}"i18n_client.SystemInfo.memerySize_n81i: "${mem_total}"G, i18n_client.SystemInfo.currentUsed_n81i："${mem_used}"G, i18n_client.SystemInfo.spaceRemain_n81i："${mem_free}"G, i18n_client.SystemInfo.shiyonglv_n81i："${mem_shiyonglv}"% "
	mem_detail_nopolice=${swap_total}","${swap_free}","${mem_total}","${mem_used}","${mem_free}","${mem_shiyonglv}","$'\n'
	OLD_IFS="$IFS"
        IFS=","
        mem_detail_nopolice_arr=(${mem_detail_nopolice})
        IFS=$OLD_IFS
	# police
        if [ ! -z $mem_polices ]
	        then
        	        mem_police_most_level=0
                        mem_police_flag_t=0 #临时变量
                        mem_baseInfo_police=""
                        for((j=0;j<${#mem_polices_arr[@]};j++));do
                	        mem_detail_police_t=""
                                OLD_IFS="$IFS"
                                IFS="|"
                                mem_police_arr=(${mem_polices_arr[$j]})
                                IFS=$OLD_IFS

                                if (($(echo "${mem_detail_nopolice_arr[((${mem_police_arr[0]}-1))]} ${mem_police_arr[1]} ${mem_police_arr[2]}"|bc) == 1))
	                                then #触发告警
	                                        mem_detail_police_t=${mem_police_arr[0]}","${mem_police_arr[3]}","${mem_police_arr[2]}","
                                                mem_police_flag=1
                                                mem_police_flag_t=1
                                                if ((${mem_police_arr[3]} > ${mem_police_most_level}))
	                                                then
	                                                        mem_police_most_level=${mem_police_arr[3]}
                                                                mem_detail_police=$mem_detail_police_t
                                                                mem_baseInfo_police="【${mem_detail_nopolice_arr[0]}】"${mem_police_arr[4]}${mem_detail_nopolice_arr[((${mem_police_arr[0]}-1))]}"% i18n_client.GetInfoRun.abnormal_n81i</br>"
                                                fi
                                        elif [ $mem_police_flag_t -eq 0 ]&&[ -z "${mem_baseInfo_police}" ];then
                                                mem_detail_police="\"\",,,"
                                                mem_baseInfo_police="【${mem_detail_nopolice_arr[0]}】"${mem_police_arr[4]}${mem_detail_nopolice_arr[((${mem_police_arr[0]}-1))]}" i18n_client.GetInfoRun.normal_n81i</br>"
                                fi
                        done
                        memResult_baseInfo_police=${memResult_baseInfo_police}${mem_baseInfo_police}
                else
                        mem_detail_police="\"\",,,"
        fi
        memResult_detail=${memResult_detail}${mem_detail_police}${mem_detail_nopolice}

	
        #memResult_detail="${memResult_detail}""\"\",,,"${swap_total}","${swap_free}","${mem_total}","${mem_used}","${mem_free}","${mem_shiyonglv}","$'\n'

	# disk info
	disk_total_size=`awk '{total+=$2}END{print total}' ${diskInfo}` # Kb
	disk_total_used_size=`awk '{total+=$3}END{print total}' ${diskInfo}`
	disk_total_shiyonglv=`echo "scale=2;(${disk_total_used_size}*100)/${disk_total_size}"|bc|awk '{printf "%.2f",$0}'`
	diskResult_baseInfo=${diskResult_baseInfo}${sep}"disk-"${index}"i18n_client.SystemInfo.diskSize_n81i："`echo "scale=2;${disk_total_size}/1024/1024"|bc|awk '{printf "%.2f",$0}'`"G，i18n_client.SystemInfo.used2_n81i "${disk_total_shiyonglv}"% ;"
	getDiskBase ${diskInfo}
	getDiskDetail

	# net info
	#net_avai_if_arr=($(awk 'NR==1{print $0}' ${netInfo}))
	#net_all_if_arr=($(awk 'NR>1{print}' ${netInfo} | awk -F ':' '{print $1 NR}'))
	#getNetInfo
}

function getCpuBase2 {
	arr1=($(awk '/^[0-2]/{print $9}' $1|awk 'NR>2{print $0}'))
        arr2=($(awk '{print $4}' $2))

        for((i=0;i<${#arr1[@]};i++));do
                cpu_t2=`bc <<<100-${arr1[$i]} |awk '{printf "%.2f",$0}'`
                cpuResult_baseInfo=${cpuResult_baseInfo}${sep}"cpu"${index}"-$i i18n_client.SystemInfo.mhz_n81i"${arr2[$i]}"MHz，i18n_client.SystemInfo.shiyonglv_n81i "${cpu_t2}"%; "
        done
}

function getCpuDetail {
        cpu_t3=`awk '/^[0-2]/{print $0}' $1|awk 'NR>1{print $0}'`
	
        for((i=2;i<=`echo "${cpu_t3}"|awk 'END{print NR}'`;i++));do
                cpu_row=`echo "${cpu_t3}"|awk 'NR=="'$i'"{print $0}'`
                cpu_idle=`echo "${cpu_row}"|awk '{print $9}'`
                cpu_shiyonglv_t=`bc <<<100-${cpu_idle} |awk '{printf "%.2f",$0}'`

		cpu_detail_nopolice="cpu${index}-$((i-2)),"`echo "${cpu_row}"|awk '{print $4}'`","`echo "${cpu_row}"|awk '{print $6}'`","`echo "${cpu_row}"|awk '{print $7}'`","`echo "${cpu_row}"|awk '{print $5}'`","`echo "${cpu_row}"|awk '{print $9}'`","${cpu_shiyonglv_t}","`awk 'NR=="'$((i-1))'"{print $4}' $2`$'\n'
		
		# police
		if [ ! -z $cpu_polices ]
			then
				OLD_IFS="$IFS"
				IFS=","
				cpu_detail_nopolice_arr=(${cpu_detail_nopolice})
				IFS=$OLD_IFS
				cpu_police_most_level=0
				cpu_police_flag_t=0 #临时变量
				cpu_baseInfo_police=""
				for((j=0;j<${#cpu_polices_arr[@]};j++));do
					cpu_detail_police_t=""
					OLD_IFS="$IFS"
					IFS="|"
					cpu_police_arr=(${cpu_polices_arr[$j]})
					IFS=$OLD_IFS
					
					if (($(echo "${cpu_detail_nopolice_arr[((${cpu_police_arr[0]}-1))]} ${cpu_police_arr[1]} ${cpu_police_arr[2]}"|bc) == 1))
                                        	then #触发告警
                                                	cpu_detail_police_t=${cpu_police_arr[0]}","${cpu_police_arr[3]}","${cpu_police_arr[2]}","
	                                                cpu_police_flag=1
							cpu_police_flag_t=1
							if ((${cpu_police_arr[3]} > ${cpu_police_most_level}))
								then
									cpu_police_most_level=${cpu_police_arr[3]}
									cpu_detail_police=$cpu_detail_police_t
									cpu_baseInfo_police="【cpu${index}-$((i-2))】"${cpu_police_arr[4]}${cpu_detail_nopolice_arr[((${cpu_police_arr[0]}-1))]}"% i18n_client.GetInfoRun.abnormal_n81i</br>"
							fi
        	                                elif [ $cpu_police_flag_t -eq 0 ]&&[ -z "${cpu_baseInfo_police}" ];then
                	                                cpu_detail_police="\"\",,,"
							cpu_baseInfo_police="【cpu${index}-$((i-2))】"${cpu_police_arr[4]}${cpu_detail_nopolice_arr[((${cpu_police_arr[0]}-1))]}"% i18n_client.GetInfoRun.normal_n81i</br>"
                        	        fi
				done
				cpuResult_baseInfo_police=${cpuResult_baseInfo_police}${cpu_baseInfo_police}
			else
				cpu_detail_police="\"\",,,"
		fi
                cpuResult_detail=${cpuResult_detail}${cpu_detail_police}${cpu_detail_nopolice}
        done
        cpuResult_detail=${cpuResult_detail}"\"\",,,cpu${index},"`awk 'NR==4{print $4}' $1`","`awk 'NR==4{print $6}' $1`","`awk 'NR==4{print $7}' $1`","`awk 'NR==4{print $5}' $1`","`awk 'NR==4{print $9}' $1`","${cpu_shiyonglv}","${cpu_averMHz}$'\n'
}

function getDiskBase {
	disk_size_arr=($(awk '{print $2}' $1))
	disk_used_arr=($(awk '{print $3}' $1))
	disk_avai_arr=($(awk '{print $4}' $1)) # available
	disk_usep_arr=($(awk '{print $5}' $1))
	disk_moun_arr=($(awk '{print $6}' $1)) # mount on
	
	for((i=0;i<${#disk_size_arr[@]};i++));do
		disk_size=`echo "scale=2;${disk_size_arr[$i]}/1024/1024"|bc|awk '{printf "%.2f",$0}'` # Gb
		disk_used=`echo "scale=2;${disk_used_arr[$i]}/1024/1024"|bc|awk '{printf "%.2f",$0}'`
		disk_avai=`echo "scale=2;${disk_avai_arr[$i]}/1024/1024"|bc|awk '{printf "%.2f",$0}'`
		disk_usep=${disk_usep_arr[$i]}
		disk_moun=${disk_moun_arr[$i]}

		diskResult_baseInfo=${diskResult_baseInfo}${sep}"【disk-$index:"${disk_moun}"】i18n_client.SystemInfo.size_n81i "${disk_size}"G, i18n_client.SystemInfo.used1_n81i "${disk_used}"G, i18n_client.SystemInfo.spaceRemain_n81i "${disk_avai}"G, i18n_client.SystemInfo.shiyonglv_n81i "${disk_usep}" ; "
	done
}

function getDiskDetail {
	disk_total_num=`echo "${disk_total_num}+${#disk_size_arr[@]}"|bc`
	for((i=0;i<${#disk_size_arr[@]};i++));do
		disk_size=`echo "scale=2;${disk_size_arr[$i]}/1024/1024"|bc|awk '{printf "%.2f",$0}'` # Gb
                disk_avai=`echo "scale=2;${disk_avai_arr[$i]}/1024/1024"|bc|awk '{printf "%.2f",$0}'`
		disk_detail_nopolice="disk-$index:"${disk_moun_arr[$i]}","${disk_size}","${disk_avai}","${disk_usep_arr[$i]%\%*}",0,"$'\n'
		OLD_IFS=$IFS
		IFS=","
		disk_detail_nopolice_arr=(${disk_detail_nopolice})
		IFS=$OLD_IFS

		# police
		if [ ! -z $disk_polices ]
                        then
				disk_police_most_level=0
	        	        disk_police_flag_t=0 #临时变量
                		disk_baseInfo_police=""
				for((j=0;j<${#disk_polices_arr[@]};j++));do
                                        disk_detail_police_t=""
                                        OLD_IFS="$IFS"
                                        IFS="|"
                                        disk_police_arr=(${disk_polices_arr[$j]})
                                        IFS=$OLD_IFS

                                        if (($(echo "${disk_detail_nopolice_arr[((${disk_police_arr[0]}-1))]} ${disk_police_arr[1]} ${disk_police_arr[2]}"|bc) == 1))
                                                then #触发告警
							disk_detail_police_t=${disk_police_arr[0]}","${disk_police_arr[3]}","${disk_police_arr[2]}","
							disk_police_flag=1
                                                        disk_police_flag_t=1
							if ((${disk_police_arr[3]} > ${disk_police_most_level}))
                                                                then
                                                                        disk_police_most_level=${disk_police_arr[3]}
                                                                        disk_detail_police=$disk_detail_police_t
                                                                        disk_baseInfo_police="【${disk_detail_nopolice_arr[0]}】"${disk_police_arr[4]}${disk_detail_nopolice_arr[((${disk_police_arr[0]}-1))]}"% i18n_client.GetInfoRun.abnormal_n81i</br>"
                                                        fi
                                                elif [ $disk_police_flag_t -eq 0 ]&&[ -z "${disk_baseInfo_police}" ];then
                                                        disk_detail_police="\"\",,,"
                                                        disk_baseInfo_police="【${disk_detail_nopolice_arr[0]}】"${disk_police_arr[4]}${disk_detail_nopolice_arr[((${disk_police_arr[0]}-1))]}" i18n_client.GetInfoRun.normal_n81i</br>"
					fi
				done
				diskResult_baseInfo_police=${diskResult_baseInfo_police}${disk_baseInfo_police}
			else
                                disk_detail_police="\"\",,,"
		fi
		diskResult_detail=${diskResult_detail}${disk_detail_police}${disk_detail_nopolice}
	done
}

function getNetInfo {
	# 处理特殊网卡信息的内容
	special_base_row=`awk 'NR==1{print $0}' ${netInfo}3` # 第一行
	special_base_11=`echo ${special_base_row} | awk -F '"' '{print $2}'` # 11列
	special_base_11=${sep}${special_base_11#*$sep}
	special_base_12=`echo $special_base_row | awk -F '"' '{print $4}'` # 12列
	special_base_12=${sep}${special_base_12#*$sep}
	special_base_t=${special_base_11}${special_base_12}
	special_base_t=${special_base_t//$sep/\!}
	OLDIFS=$IFS
	IFS="\!"
	special_base_t_arr=(${special_base_t})
	IFS=$OLD_IFS

	special_base=""
	for((i=1;i<${#special_base_t_arr[@]};i++));do
		special_base=${special_base}"${sep}if${index}-"${special_base_t_arr[$i]}
	done
	special_detail_rows=`awk 'NR>2{print}' ${netInfo}3` # details行下方所有行
	special_detail_rows_arr=($special_detail_rows) # details下方所有行的数组，每个元素为一行内容

	#special_if_t=`echo ${special_detail_rows}|awk -F ',' '{print $4}'` # 取每行第三、四个逗号之间的网卡名
	special_if_arr=() # 网卡名称数组
	
	special_if_baseInfo="" # 初始化特殊网卡基础信息
	for ((i=0;i<${#special_detail_rows_arr[@]};i++));do
		special_detail_row=`echo ${special_detail_rows_arr[$i]} | awk -F ',' '{print gensub($4,"if"'$index'"-"$4,1)}'` # detail行网卡名替换为ifx-开头的名字
		netResult_detail=${netResult_detail}${special_detail_row}$'\n'

		special_if_baseInfo=${special_if_baseInfo}${special_base_12}${special_base_11}
		special_if_arr[$i]=`echo ${special_detail_rows_arr[$i]}| awk -F ',' '{print $4}'`
	done
	sys_net_total_num=`echo "${sys_net_total_num}+${#net_all_if_arr[@]}"|bc`
	net_total_num=`echo "${net_total_num}+${#net_avai_if_arr[@]}"|bc`
	net_avai_baseInfo=""
	net_disa_baseInfo=""

	for((i=0;i<${#net_all_if_arr[@]};i++));do
		ifName_t=${net_all_if_arr[$i]}
		ifName=${ifName_t%?}
		echo ${special_if_arr[0]}"--"${special_if_arr[1]}"--"${special_if_arr[2]}"--"${ifName}
		if echo "${special_if_arr[@]}"|grep -w "${ifName}" &>/dev/null;then
			let "net_total_num--"
			let "sys_net_total_num--"
        		continue
		fi

	        net_rowNum=$((${net_all_if_arr[$i]: -1}+1))
	        net_if_name_t=`awk 'NR=="'${net_rowNum}'"{print $1}' ${netInfo}`
	        net_if_name=${net_if_name_t%?}
	        is_avai=`echo "${net_avai_if_arr[@]}" | grep -wq "${net_all_if_arr[$i]%?}" && echo "1" || echo "0"`
	        if [ "1" -eq "$is_avai" ]
	                then    
	                        echo "#!/bin/bash"$'\n'"ethtool ${net_if_name}" > ethtool.sh
	                        net_if_ethtoolResult=`ssh -C $value "/bin/bash" < ethtool.sh`
	                        net_if_speed_t=`echo "${net_if_ethtoolResult}"|grep "Speed:"|awk '{print $2}'`
	                        if [ ${#net_if_speed_t} -lt 1 ]
	                                then
	                                        net_if_speed=0
	                                else
	                                        net_if_speed=${net_if_speed_t:0:${#net_if_speed_t}-4}
	                        fi
	                        net_if_rxByte1=`awk 'NR=="'${net_rowNum}'"{print $2}' ${netInfo}`
	                        net_if_rxByte2=`awk 'NR=="'${net_rowNum}'"{print $2}' ${netInfo}2`
	                        net_if_rxBps=`echo "scale=2;($net_if_rxByte2-$net_if_rxByte1)*8/5"|bc|awk '{printf "%.2f",$0}'`
	                        net_if_rxPackets1=`awk 'NR=="'${net_rowNum}'"{print $3}' ${netInfo}`
	                        net_if_rxPackets2=`awk 'NR=="'${net_rowNum}'"{print $3}' ${netInfo}2`
				net_if_rxPps_t=`echo "($net_if_rxPackets2-$net_if_rxPackets1)"|bc`
	                        net_if_rxPps=`echo "scale=2;($net_if_rxPackets2-$net_if_rxPackets1)*8/5"|bc|awk '{printf "%.2f",$0}'`
	                        net_if_txByte1=`awk 'NR=="'${net_rowNum}'"{print $10}' ${netInfo}`
	                        net_if_txByte2=`awk 'NR=="'${net_rowNum}'"{print $10}' ${netInfo}`
	                        net_if_txBps=`echo "scale=2;($net_if_txByte2-$net_if_txByte1)*8/5"|bc|awk '{printf "%.2f",$0}'`
	                        net_if_txPackets1=`awk 'NR=="'${net_rowNum}'"{print $11}' ${netInfo}`
	                        net_if_txPackets2=`awk 'NR=="'${net_rowNum}'"{print $11}' ${netInfo}2`
				net_if_txPps_t=`echo "($net_if_rxPackets2-$net_if_rxPackets1)"|bc`
	                        net_if_txPps=`echo "scale=2;($net_if_rxPackets2-$net_if_rxPackets1)*8/5"|bc|awk '{printf "%.2f",$0}'`
				net_if_rxError1=`awk 'NR=="'${net_rowNum}'"{print $4}' ${netInfo}`
	                        net_if_rxError2=`awk 'NR=="'${net_rowNum}'"{print $4}' ${netInfo}2`
        	                net_if_txError1=`awk 'NR=="'${net_rowNum}'"{print $12}' ${netInfo}`
                	        net_if_txError2=`awk 'NR=="'${net_rowNum}'"{print $12}' ${netInfo}2`
	                        net_if_rxDrop1=`awk 'NR=="'${net_rowNum}'"{print $5}' ${netInfo}`
        	                net_if_rxDrop2=`awk 'NR=="'${net_rowNum}'"{print $5}' ${netInfo}2`
                	        net_if_txDrop1=`awk 'NR=="'${net_rowNum}'"{print $13}' ${netInfo}`
	                        net_if_txDrop2=`awk 'NR=="'${net_rowNum}'"{print $13}' ${netInfo}2`

				if [ $net_if_rxPps_t -gt 0 ] 
	                                then
	                                        net_if_rxErrorPerc=`echo "scale=2;($net_if_rxError2-$net_if_rxError1)*100/($net_if_rxPackets2-$net_if_rxPackets1)"|bc|awk '{printf "%.2f",$0}'`
	                                        net_if_rxDropPerc=`echo "scale=2;($net_if_rxDrop2-$net_if_rxDrop1)*100/($net_if_rxPackets2-$net_if_rxPackets1)"|bc|awk '{printf "%.2f",$0}'`
	                                else
	                                        net_if_rxErrorPerc=0.00
	                                        net_if_rxDropPerc=0.00
	                        fi  
	                        if [ $net_if_txPps_t -gt 0 ]
	                                then
	                                        net_if_txErrorPerc=`echo "scale=2;($net_if_txError2-$net_if_txError1)*100/($net_if_txPackets2-$net_if_txPackets1)"|bc|awk '{printf "%.2f",$0}'`
	                                        net_if_txDropPerc=`echo "scale=2;($net_if_txDrop2-$net_if_txDrop1)*100/($net_if_txPackets2-$net_if_txPackets1)"|bc|awk '{printf "%.2f",$0}'`
	                                else
	                                        net_if_txErrorPerc=0.00
	                                        net_if_txDropPerc=0.00
	                        fi
	                        net_avai_baseInfo=${net_avai_baseInfo}${sep}"if"${index}"-"${net_if_name}" i18n_client.SystemInfo.netSpeed_n81i"${net_if_speed}"Mbps, i18n_client.SystemInfo.input_n81i"${net_if_rxBps}"bps、"${net_if_rxPps}"pps, Output"${net_if_txBps}"bps、"${net_if_txPps}"pps; "
				net_detail_nopolice="if${index}-"${net_if_name}","${net_if_rxPackets2}","${net_if_txPackets2}","${net_if_rxByte2}","${net_if_txByte2}","${net_if_rxError2}","${net_if_txError2}","${net_if_rxDrop2}","${net_if_txDrop2}","${net_if_speed}","${net_if_rxBps}","${net_if_txBps}","${net_if_rxPps}","${net_if_txPps}","${net_if_rxErrorPerc}","${net_if_txErrorPerc}","${net_if_rxDropPerc}","${net_if_txDropPerc}$'\n'
				#netResult_detail=${netResult_detail}"\"\",,,if${index}-"${net_if_name}","${net_if_rxPackets2}","${net_if_txPackets2}","${net_if_rxByte2}","${net_if_txByte2}","${net_if_rxError2}","${net_if_txError2}","${net_if_rxDrop2}","${net_if_txDrop2}","${net_if_speed}","${net_if_rxBps}","${net_if_txBps}","${net_if_rxPps}","${net_if_txPps}","${net_if_rxErrorPerc}","${net_if_txErrorPerc}","${net_if_rxDropPerc}","${net_if_txDropPerc}$'\n'
				# police
				if [ ! -z $net_polices ]
					then
						OLD_IFS="$IFS"
						IFS=","
						net_detail_nopolice_arr=(${net_detail_nopolice})
						IFS=$OLD_IFS
						net_police_most_level=0
						net_police_flag_t=0 #临时变量
						net_baseInfo_police=""
						for((j=0;j<${#net_polices_arr[@]};j++));do
							net_detail_police_t=""
							OLD_IFS="$IFS"
							IFS="|"
							net_police_arr=(${net_polices_arr[$j]})
							IFS=$OLD_IFS
					
							if (($(echo "${net_detail_nopolice_arr[((${net_police_arr[0]}-1))]} ${net_police_arr[1]} ${net_police_arr[2]}"|bc) == 1))
		                                        	then #触发告警
        		                                        	net_detail_police_t=${net_police_arr[0]}","${net_police_arr[3]}","${net_police_arr[2]}","
	        		                                        net_police_flag=1
									net_police_flag_t=1
									if ((${net_police_arr[3]} > ${net_police_most_level}))
										then
											net_police_most_level=${net_police_arr[3]}
											net_detail_police=$net_detail_police_t
											net_baseInfo_police="【${net_detail_nopolice_arr[0]}】"${net_police_arr[4]}${net_detail_nopolice_arr[((${net_police_arr[0]}-1))]}"% i18n_client.GetInfoRun.abnormal_n81i</br>"
									fi
        			                                elif [ $net_police_flag_t -eq 0 ]&&[ -z "${net_baseInfo_police}" ];then
                			                                net_detail_police="\"\",,,"
									net_baseInfo_police="【${net_detail_nopolice_arr[0]}】"${net_police_arr[4]}${net_detail_nopolice_arr[((${net_police_arr[0]}-1))]}"% i18n_client.GetInfoRun.normal_n81i</br>"
                        			        fi
						done
						netResult_baseInfo_police=${netResult_baseInfo_police}${net_baseInfo_police}
					else
						net_detail_police="\"\",,,"
				fi
				netResult_detail=${netResult_detail}${net_detail_police}${net_detail_nopolice}
	                else
	                        net_disa_baseInfo=${net_disa_baseInfo}${sep}"if"${index}"-"${net_if_name}"i18n_client.SystemInfo.disable_n81i; "
	        fi
	done
	
	sys_net_total_num=`echo "${sys_net_total_num}+${#special_detail_rows_arr[@]}"|bc`
	net_total_num=`echo "$net_total_num+${#special_detail_rows_arr[@]}"|bc`
	netResult_baseInfo=${netResult_baseInfo}"${sep}if"${index}"-"${#special_detail_rows_arr[@]}"i18n_client.SystemInfo.insert_n81i: "${special_base}${net_avai_baseInfo}${net_disa_baseInfo}$'\n'
}

# main
index=1
flag=1
while [ $flag -eq 1 ]; do
        key='ip'${index}
        value=`eval echo '$'${key}`

        if [ -z ${value} ]
                then
                        flag=0
                else
                        getDetail ${key} ${value}
			let "index++"
        fi  
done

## result
# cpu
cpuResult_baseInfo=${cpuResult_baseInfo}"\""
if ((${cpu_police_flag} == 0 ))
	then
		cpuResult_baseInfo_police="\""${cpuResult_baseInfo}
		cpu_police_flag=1
	else
		cpuResult_baseInfo_police="\""${cpuResult_baseInfo_police}"\""
		cpu_police_flag=0
fi
echo "${cpuResult_taskInfo}""${cpu_police_flag},""\"${cpuResult_coreNum}""i18n_client.SystemInfo.core_n81i: ""${cpuResult_baseInfo}"",""${cpuResult_baseInfo_police}"$'\n'"details,""$((cpuResult_coreNum+index-1))"$'\n'"${cpuResult_detail}" > ${cpu_dataFileDir}/${date}".csv"  #${cpuResult}

# mem
memResult_baseInfo=${memResult_baseInfo}"\""
if ((${mem_police_flag} == 0 ))
        then
                memResult_baseInfo_police=${memResult_baseInfo}
		mem_police_flag=1
	else
		memResult_baseInfo_police="\""${memResult_baseInfo_police}"\""
		mem_police_flag=0
fi
echo "${memResult_taskInfo}"${mem_police_flag}",${memResult_baseInfo}"",""${memResult_baseInfo_police}"$'\n'"details,""$((index-1))"$'\n'"${memResult_detail}" > ${mem_dataFileDir}/${date}".csv" #${memResult}

# disk
diskResult_baseInfo=${diskResult_baseInfo}"\""
if ((${disk_police_flag} == 0 ))
        then
                diskResult_baseInfo_police=${diskResult_baseInfo}
		disk_police_flag=1
	else
		diskResult_baseInfo_police="\""${diskResult_baseInfo_police}"\""
		disk_police_flag=0
fi
echo "${diskResult_taskInfo}"${disk_police_flag}",${diskResult_baseInfo}"",""${diskResult_baseInfo_police}"$'\n'"details,$disk_total_num"$'\n'"${diskResult_detail}" > ${disk_dataFileDir}/${date}".csv" #${diskResult}

# net
netResult_baseInfo=${netResult_baseInfo}"\""
if ((${net_police_flag} == 0 ))
        then
                netResult_baseInfo_police=${netResult_baseInfo}
		net_police_flag=1
	else
		netResult_baseInfo_police="\""${netResult_baseInfo_police}"\""
		net_police_flag=0
fi
echo "${netResult_taskInfo}"${net_police_flag}",${netResult_baseInfo}"",""${netResult_baseInfo_police}"$'\n'"details,$net_total_num"$'\n'"${netResult_detail}" > ${net_dataFileDir}/${date}".csv" #${netResult}

# sys
sysinfo=`cat ${sysinfoPath}`
echo "${sysResult_taskInfo}""${sysinfo}" > ${sys_dataFileDir}/${date}".csv" #${sysResult}
