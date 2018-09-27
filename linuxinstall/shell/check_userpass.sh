#!/usr/bin/expect

if {$argc!=3} {
	send_user "usage: check_userpass.sh ip name password\n"
	exit 1
}

set host [lindex $argv 0]
set user [lindex $argv 1]
set pass [lindex $argv 2]

set isSend 0
spawn ssh -t "${user}@${host}"

while 1 {
	expect {
		"* (yes/no)*" {send "yes\r"}
		"* ¿ÚÁî*" {
			send "${pass}\r"
			set isSend 1
		}
		"* password:*" {
			send "${pass}\r"
			set isSend 1
		}
		"Permission denied" {
			send_user "Not allowed\n"
			exit 1
		}
		"*~]" {
			send_user "OK\n"
			send "exit\r"
			exit 0
		}
		"Last login:*" {
			send_user "OK\n"
			send "exit\r"
			exit 0
		}
		default {
			send_user "error\n"
			exit 2
		}
	}
}


