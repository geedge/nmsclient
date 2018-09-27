package com.nis.nmsclient.thread.alarm;

public enum ErrorCode {
	ProcessNotExist, //进程不存在
	MultipleProcessExist,//多个进程存在
	ThreadRuntimeError, //执行异常
	HandShakeError, //通讯握手失败
	ProtListenerError, //端口监听失败
	DeamonNotExist, //守护进程不存在
	SocketError//通讯失败
}
