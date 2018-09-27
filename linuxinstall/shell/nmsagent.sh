#!/bin/sh

# -----------------------------------------------------------------------------
# Start/Stop Script for the NMS Client
#
# Environment Variable Prequisites
#
#   NMSCLEINT_HOME   May point at your Catalina "build" directory.
#
#   NMSCLIENT_TASKDIR (Optional) Directory path location of taskresult directory
#                   Defaults to %NMSCLIENT_HOME%/task.
#
#   NMSCLIENT_TMPDIR (Optional) Directory path location of temporary directory
#                   the JVM should use (java.io.tmpdir).  Defaults to
#                   $NMSCLIENT_HOME/temp.
#
#   JAVA_HOME       Must point at your Java Development Kit installation.
#                   Required to run the with the "debug" argument.
#
# -----------------------------------------------------------------------------
# resolve links - $0 may be a softlink
PRG="$0"

#OS_TYPE=$( lsb_release -d| cut -d: -f2| cut -f2 )
#echo $OS_TYPE
while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
   PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

# Only set NMSCLEINT_HOME if not already set
[ -z "$NMSCLEINT_HOME" ] && NMSCLEINT_HOME=`cd "$PRGDIR/.." ; pwd`
cd "$NMSCLEINT_HOME"/shell

if [ -z "$NMSCLIENT_TMPDIR" ] ; then
  NMSCLIENT_TMPDIR="$NMSCLEINT_HOME"/temp
fi
if [ ! -d $NMSCLIENT_TMPDIR ]
   then
      mkdir $NMSCLIENT_TMPDIR
fi  

if [ -n "$4" ]; then
  NMSCLIENT_TASKDIR=`dirname "$4"`
fi 
if [ -z "$NMSCLIENT_TASKDIR" ] ; then
  NMSCLIENT_TASKDIR="$NMSCLEINT_HOME"/task
fi


# -------- check jdk
# check nmsjdk
NMS_HOME=`cd "$NMSCLEINT_HOME/.." ; pwd`
cd "$NMSCLEINT_HOME"/shell
NMS_JDK="$NMS_HOME"/nmsjdk
if [ ! -e "$NMS_JDK" ]
  then 
    echo "$NMS_JDK not exist"
    exit 0
fi
# check java -version
javaversion=`$NMS_JDK/bin/java -version 2>&1|grep "java version"`
if [ ! -n "$javaversion" ]
  then
    echo "$NMS_JDK cannot use, please install"
    exit 0
fi

# -------- set jdk path
export JAVA_HOME=$NMS_JDK
export CLASSPATH=$JAVA_HOME/lib/tools.jar:$JAVA_HOME/lib/dt.jar
export PATH=$JAVA_HOME/bin:$PATH

temp=$CLASSPATH
#setting libs path
libs="$NMSCLEINT_HOME"/lib/*
append(){
    temp=$temp":"$1
}
for file in $libs;    do
    append $file                
done
jars="$NMSCLEINT_HOME"/bin/*
for file in $jars;    do
    append $file                
done
export NC_CLASSPATH=$temp:.:$NMSCLEINT_HOME/conf
export LD_LIBRARY_PATH=$NMSCLEINT_HOME/lib
export LANG=zh_CN.UTF-8

# Bugzilla 37848: When no TTY is available, don't output to console
have_tty=0
if [ "`tty`" != "not a tty" ]; then
    have_tty=1
fi

# ----- Execute The Requested Command -----------------------------------------

# Bugzilla 37848: only output this if we have a TTY
if [ $have_tty -eq 1 ]; then
  echo "Using NMSCLEINT_HOME:   $NMSCLEINT_HOME"
  echo "Using NMSCLIENT_TMPDIR:  $NMSCLIENT_TMPDIR"
  echo "Using JAVA_HOME:       $JAVA_HOME"
  echo "Using CLASSPATH:       $NC_CLASSPATH"
fi

# ---- get jvm param
jvmconf_file=$NMSCLEINT_HOME"/conf/jvm.conf"
if [ -f $jvmconf_file ]
	then
		#sed -n '/Xmx=/p' $jvmconf_file | sed 's/Xmx=//g'
		XmsOpt=`sed -n '/-Xms/p' $jvmconf_file`
		XmxOpt=`sed -n '/-Xmx/p' $jvmconf_file`
		JAVA_OPTS="$XmsOpt $XmxOpt -XX:+UseParNewGC"
else
    JAVA_OPTS="-Xms64m -Xmx128m -XX:+UseParNewGC"
fi

program="com.nis.nmsclient.NmsClient"
name="NmsClient"

proc_id=

# handler agent upgrade result
handleTask()
{
   if [ -d "$NMSCLIENT_TASKDIR" ]
      then
        cd $NMSCLIENT_TASKDIR
        for i in *.upgrade;do mv "$i" "${i%.upgrade}.result";done >/dev/null 2>&1
        cd "$NMSCLEINT_HOME"/shell
   fi
}
writePid()
{
	 ps aux|grep java|grep $program|grep -v grep|awk '{print $2}' > $NMSCLIENT_TMPDIR/agentPid.temp
}
getPid()
{
	 unset proc_id
   proc_id=`ps aux|grep java|grep $program|grep -v grep|awk '{print $2}'`
}
shouhu_proc="$NMSCLEINT_HOME"/shell/nmsclient_shouhu.sh
stopShouhuProc()
{
   shouhu_proc_id=`ps aux|grep $shouhu_proc|grep -v grep|awk '{print $2}'`
   if [ -n "$shouhu_proc_id" ]
       then
       		 echo "kill shouhu process ....."
           kill -9  $shouhu_proc_id
   fi
}
startShouhuProc()
{
   shouhu_proc_id=`ps aux|grep $shouhu_proc|grep -v grep|awk '{print $2}'`
   if [ ! -n "$shouhu_proc_id" ]
       then
       		 echo "start shouhu process ....."
       		 nohup $shouhu_proc $NMSCLIENT_TASKDIR >/dev/null &
   fi
}
if [ "$1" = "start" ] ; then
	getPid
	if [ -n "$proc_id" ]
     then
        echo "$name already running......"
	else   
				nohup java $JAVA_OPTS -classpath $NC_CLASSPATH  $program >/dev/null &   
        sleep 3
        getPid
        if [ -n "$proc_id" ]
             then
                 echo "$name start success!!!!!"
                 writePid
        else
             echo "$name start error!!!!!"
        fi
        handleTask
  fi
elif [ "$1" = "stop" ]; then
   getPid
   if [ -n "$proc_id" ]
       then
		   stopShouhuProc
           sleep 1
           echo "$name is start, now kill......"
           kill -9  $proc_id
           writePid
           echo "$name kill ok !!!!!!!!!!!!!"    
   else
       echo "$name is not start!!!!!!!!!!!"
   fi 
elif [ "$1" = "restart" ] ; then
	 getPid
   if [ -n "$proc_id" ]
       then
           echo "$name is start, now restart......"
           stopShouhuProc
           sleep 1
           startShouhuProc
           kill -9  $proc_id
           getPid
				   if [ -n "$proc_id" ]
				      then
				      	echo "$name stop error!!!!!!!!!!"
				      	handleTask
				        exit 1
				   fi  
   else
       echo "$name is not start, now start......"
   fi
   copyError=
   if [ -n "$2" ] ; then
      srcFile=$2
      updateFile=/dev/null
      if [ -d "$2" ]; then
      	 srcFile="$2"/*
      fi
      if [ -n "$4" ]; then
         updateFile=$4
      fi 
   		if [ -n "$3" ]; then
   			 unalias cp >/dev/null 2>&1
   			 cp -rvf $srcFile $3 >>$updateFile 2>&1 ||copyError=1
   		fi
   		rm -rf $2 >/dev/null 2>&1
   fi
   if [ -n "$copyError" ]
   		then
   		#----copy error
      handleTask
   else
     #--------copy right, start proc
     nohup java $JAVA_OPTS -classpath $NC_CLASSPATH  $program >/dev/null &   
	   sleep 3
	   getPid
	   if [ -n "$proc_id" ]
	      then
	      writePid
	   else
	     echo "$name restart error!!!!!!!!!!"
	     handleTask
	   fi
   fi
   
else
   echo "Usage: nmsagent.sh ( commands ... )"
   echo "commands:"
   echo "  start             Start $name in a separate window"
   echo "  restart   				 ReStart $name in a separate window"
   echo "  stop              Stop $name"
fi
