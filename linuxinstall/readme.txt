NC--Linux下的安装文件结构：

|--NC_install
|  |--autoinstall.sh
|  |--install.sh
|  |--bin
|  |  |--cer
|  |  |  |--client_ks
|  |  |  |--client_ts
|  |  |--fileComment.jar
|  |  |--nmsclient.jar
|  |--conf
|  |  |--jvm.conf
|  |  |--log4j.properties
|  |  |--myconfig.properties
|  |  |--version.properties
|  |--jre_install
|  |  |--jre-6u45-linux-i586.bin
|  |  |--jre-6u45-linux-x64.bin
|  |--lib
|  |  |--ant.jar
|  |  |--commons-beanutils-1.7.jar
|  |  |--commons-collections.jar
|  |  |--commons-io.jar
|  |  |--commons-lang.jar
|  |  |--commons-logging.jar
|  |  |--ezmorph-1.0.4.jar
|  |  |--java-unrar-0.3.jar
|  |  |--javatar-2.5.jar
|  |  |--jreloader.jar
|  |  |--json-lib-2.2.2-jdk15.jar
|  |  |--junit.jar
|  |  |--log4j-1.2.15.jar
|  |  |--ostermillerutils_1_07_00.jar
|  |  |--sigar.jar
|  |  |--libsigar-amd64-linux.so
|  |  |--libsigar-x86-linux.so
|  |--shell
|  |  |--check_userpass.sh
|  |  |--execCmdBySu.sh
|  |  |--nmsagent.sh
|  |  |--nmsclient_shouhu.sh
|  |  |--restart.sh
|  |  |--shutdown.sh
|  |  |--startup.sh


脚本说明：

1、安装脚本install.sh：
   先检查JDK是否安装且Java版本是否适合，来选择是否安装自带的JRE。
   若需自带安装，则判断系统的位数，相应安装32位JDK或64位JDK。
   JDK安装目录为/home/nms/jre1.6.0_45, 并建软连接/home/nms/nmsjdk
   若系统已安装相应版本的JDK，则建连接/home/nms/nmsjdk
   
2、自动安装脚本autoinstall.sh
   JDK的检查逻辑与install.sh一样，只是所有参数都按默认值，不需人工交互，并安装完成后自动启动。
  
3、shell目录下，脚本nmsagent.sh和datacontroller.sh，不检查JDK版本，只检查nmsjdk是否存在且nmsjdk/bin/Java命令能否正常使用