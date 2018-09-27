WEB布署说明：

1、Windows下： 在Tomcat目录下的bin中相应的添加sigar-x86-winnt.dll或sigar-amd64-winnt.dll

2、Linux下：加载动态库(.so)，在配置文件~/.bash_profile或者/etc/profile中添加变量 
	LD_LIBRARY_PATH = (相应配置libsigar-x86-linux.so或libsigar-amd64-linux.so的存放目录)，执行source ./bash_profile或/etc/profile



JAVA应用程序布署说明：
1、打nmsclient.jar,并使用MANIFEST.MF配置主函数入口（Main-Class）,和需要用到的jar包（Class-Path），jar包路径是相对nmsclient.jar的位置。
2、Windows下： 相应放sigar-x86-winnt.dll或sigar-amd64-winnt.dll在jdk/bin下。