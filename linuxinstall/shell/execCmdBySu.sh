#!/usr/bin/expect

if {$argc!=4&&$argc!=3} {
	send_user "usage: execCmdBySu.sh resultFlag command name \[password\]\n"
	exit 1
}
# 0是执行命令并取得结果，1是执行&后台程序
set resultFlag [lindex $argv 0]
set cmd [lindex $argv 1]
set user [lindex $argv 2]
set pass [lindex $argv 3]

set timeout 30
set flag 0

#关闭控制台输出信息
log_user 0
	
spawn whoami
expect "root" {set flag 1}
		
# 在执行命令${cmd}后追加"echo $?",为了取cmd命令运行的实际结果, 并屏蔽掉其他不相关的输出信息
if {$resultFlag==0} {
		spawn su - -c "${cmd};echo $?" ${user}
		
		#当前登录用户不是root，输入密码
		if {$flag==0} {
				expect {
						"口令*" {send ${pass}\r}
						"password*" {send ${pass}\r}
						default {
								send_error "error\n"
								exit 2
						}
				}
		}
		
		#打开控制台输出信息
	  log_user 1
	  
		expect {
		 		"* 密码不正确*" {exit 1}
		 		#----此处的目的是为了能读取到所有输出结果
				eof {exit 0}
		}
	
} else {	
		spawn su - -c "${cmd}" ${user}
		
		#当前登录用户不是root，输入密码
		if {$flag==0} {
				expect {
						"口令*" {send ${pass}\r}
						"password*" {send ${pass}\r}
						default {
								send_error "error\n"
								exit 2
						}
				}
		}
		expect {
			 "* 密码不正确*" {exit 1}
			 #----为了&后台启动命令能正常结束
			 eof {
			 		send_user "ok\n"
			 		exit 0
			 }
		}
}
interact
exit 0


#overlay chess
#overlay program args
#disconnect
#close -onexec 0 -i ${spawn_id}
#remove_nulls 1