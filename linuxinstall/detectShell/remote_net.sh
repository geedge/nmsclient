#!/bin/bash

python /root/zhong/monitor_net.py #py脚本的路径，需要手动填写
cat `ls|grep csv`
rm -f *.csv
