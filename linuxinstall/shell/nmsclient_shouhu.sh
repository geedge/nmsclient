#!/bin/sh

# resolve links - $0 may be a softlink
PRG="$0"

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

NC_TASKDIR="$1"
log_file="$NMSCLEINT_HOME"/temp/job.log
echo "NC_TASKDIR:$NC_TASKDIR" >> $log_file

#count=1
while [ 1 -eq 1 ]
do
	sleep 50
	if [ `ps -ef | grep NmsClient | grep -v grep | wc -l` -lt 1 ]
		then
			echo -n "Down at:" >> $log_file
			date >> $log_file
			# ---------- handler agent upgrade result
	    if [ -d "$NC_TASKDIR" ]
	      then
	        cd $NC_TASKDIR
	        for i in *.upgrade;do mv "$i" "${i%.upgrade}.result";done >>$log_file 2>&1
	        cd "$NMSCLEINT_HOME"/shell
	    fi
      # ---------  start NC
			"$NMSCLEINT_HOME"/shell/startup.sh
			echo "NmsClient start...." >>$log_file
	fi
	sleep 1
	#count=$count+1
	
	#if [ count -eq 900 ]
	#then
	# jpid=`ps -ef | grep java | grep -v grep | cut -c0-5`
	# kill -9 $jpid
	# count=1
	#fi
done