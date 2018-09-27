#!/bin/bash

# --- set default value
DEFAULT_DATA_DIR="/home/nms/nmsdata"
DEFAULT_INSTALL_DIR="/home/nms/nmsclient"

# --- set install dir
PRG="$0"
PRGDIR=`dirname "$PRG"`
CUR_PRGDIR=`cd "$PRGDIR"; pwd`

INSTALL_DIR="$DEFAULT_INSTALL_DIR"

if [ ! -d $INSTALL_DIR ]
	then
		mkdir -p $INSTALL_DIR
fi 

INSTALL_DIR=`cd "$INSTALL_DIR"; pwd`

# --- check jdk and jdk-version
install_jdk=0
javaversion=`java -version 2>&1|grep "java version"`
if [ -n "$javaversion" ]
	then
	#	javaversion=${javaversion:14:3}
		javavmajor=`echo $javaversion | cut -c15`
		javavminor=`echo $javaversion | cut -c17`
#		OS_TYPE=$( lsb_release -d| cut -d: -f2| cut -f2 )
#		if [ "`echo $OS_TYPE | cut -c1-6`" = "Ubuntu" ]
#			then
#			if [ 2 -gt $javavmajor ]; then
#				if [ 6 -gt $javavminor ]; then
#				install_jdk=1
#				fi
#			fi
#		else
			if [[ 2 -gt $javavmajor && 6 -gt $javavminor ]]; then
			install_jdk=1
			fi
#		fi 
	else   
		install_jdk=1
fi

# --- install jdk
cd "$INSTALL_DIR"/..
NMS_JDK="$(pwd)/nmsjdk"
JDK_DIR="$(pwd)/jdk1.7.0_80"
JDK_BIN_FILE=$CUR_PRGDIR"/jre_install/jdk-7u80-linux-i586.tar.gz"
if [ "`uname -i`" = "x86_64" ];then
	JDK_BIN_FILE=$CUR_PRGDIR"/jre_install/jdk-7u80-linux-x64.tar.gz"
fi
if [ $install_jdk = 1 ]
  then
	if [ ! -e "$NMS_JDK" ]
		then
			echo "JDK bin file: $JDK_BIN_FILE" 
		  echo "now, install jdk: $NMS_JDK"
		  sleep 3

		  if [ ! -e "$JDK_DIR" ];then 
		   mkdir -p $JDK_DIR
		   fi
		   tar -xzf $JDK_BIN_FILE -C $JDK_DIR --strip-components=1 || installJdk=1
		  if [ -n "$installJdk" ];then
		  	echo "install jdk failure, exit program"
		  	exit 1
		  fi
		  ln -s $JDK_DIR $NMS_JDK
      echo "install jdk done"
	fi
elif [ -n "$JAVA_HOME" ];then
  ln -s $JAVA_HOME $NMS_JDK
else
  echo "JDK bin file: $JDK_BIN_FILE" 
  echo "now, install jdk: $NMS_JDK"
	sleep 3
	if [ ! -e "$JDK_DIR" ];then 
		   mkdir -p $JDK_DIR
	fi
	tar -xzf $JDK_BIN_FILE -C $JDK_DIR --strip-components=1 || installJdk=1
	if [ -n "$installJdk" ];then
		  echo "install jdk failure, exit program"
		  exit 1
	fi
	ln -s $JDK_DIR $NMS_JDK
  echo "install jdk done"
fi
cd "$CUR_PRGDIR"

echo "==========================================="
echo "NMS_JDK: $NMS_JDK"
echo "INSTALL_DIR: $INSTALL_DIR"
echo "==========================================="

# --- copy file to install_dir
if [ "$INSTALL_DIR" != "$CUR_PRGDIR" ];then
   CP_DIR=(
	    bin
	    lib
	    conf
	    shell
	)
	#cp 
	for CP_NAME in ${CP_DIR[@]}
	do
	     cp -a $CUR_PRGDIR"/$CP_NAME" $INSTALL_DIR
	done
fi


function modify_file(){
	if [ $# != 2 ]
		 then
		 	echo "usage: modify_file [prop_name] [prop_value]"
		 	exit 0 
	fi
	prop_name="$1"
	prop_value="$2"
	#echo "modify_file $prop_name $prop_value"
	if [ -z "$(cat $PROP_FILE |grep $prop_name)" ]
		then
			echo "" >> $PROP_FILE
	    echo "$prop_name=$prop_value" >> $PROP_FILE
	else
			sed -i "s@^$prop_name.*@$prop_name=$prop_value@" $PROP_FILE
	fi
}

# --- modify property
#-------------file path
path="$DEFAULT_DATA_DIR"
#-------------include path
include_path="$INSTALL_DIR,$path"
#-------------exclude path
exclude_path="$INSTALL_DIR/bin,$INSTALL_DIR/lib,$INSTALL_DIR/shell,$INSTALL_DIR/conf"
#-------------log4j dir
logs_path="$path/nc_logs"

PROP_FILE=$INSTALL_DIR"/conf/myconfig.properties"
#echo "PROP_FILE: $PROP_FILE"
modify_file "local.data.path" $path
modify_file "common.del.path.include" $include_path
modify_file "common.del.path.exclude" $exclude_path

# modify log4j
PROP_FILE=$INSTALL_DIR"/conf/log4j.properties"
encoding=${LANG#*.}
modify_file "log4j.appender.stdout.encoding" $encoding
modify_file "log4j.appender.debugAppender.encoding" $encoding
modify_file "log4j.appender.infoAppender.encoding" $encoding
modify_file "log4j.appender.debugAppender.File" "$logs_path/nmsclient_debug.log"
modify_file "log4j.appender.infoAppender.File" "$logs_path/nmsclient_info.log"

#permit
cd $INSTALL_DIR"/shell"
chmod 755 *.sh
cd $CUR_PRGDIR

if [ -z "$(cat /etc/rc.local|grep $INSTALL_DIR"/shell/startup.sh")" ]
then
   echo $INSTALL_DIR"/shell/startup.sh" >> /etc/rc.local 
fi

echo ""
echo "install successed..."
echo "please use [$INSTALL_DIR/shell/startup.sh] to run the program..."

$INSTALL_DIR/shell/startup.sh
