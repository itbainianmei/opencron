#!/bin/bash

# Copyright 2016 benjobs
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied. See the License for the
# specific language governing permissions and limitations
# under the License.

case "$1" in
    disk)
        df -h
        exit $?
        ;;
    cpu)
        cpulog_1=$(cat /proc/stat | grep 'cpu ' | awk '{print $2" "$3" "$4" "$5" "$6" "$7" "$8}')
        sysidle1=$(echo $cpulog_1 | awk '{print $4}')
        total1=$(echo $cpulog_1 | awk '{print $1+$2+$3+$4+$5+$6+$7}')

        cpulog_2=$(cat /proc/stat | grep 'cpu ' | awk '{print $2" "$3" "$4" "$5" "$6" "$7" "$8}')
        sysidle2=$(echo $cpulog_2 | awk '{print $4}')
        total2=$(echo $cpulog_2 | awk '{print $1+$2+$3+$4+$5+$6+$7}')
        cpudetail=`top -b -n 1 | grep Cpu |sed -r 's/\s+//g'|awk -F ":" '{print $2}'`

        echo -e "{id2:\"$sysidle2\",id1:\"$sysidle1\",total2:\"$total2\",total1:\"$total1\",detail:\"$cpudetail\"}";
        exit $?
        ;;
    mem)
        loadmemory=$(cat /proc/meminfo | awk '{print $2}')
        total=$(echo $loadmemory | awk '{print $1}')
        free1=$(echo $loadmemory | awk '{print $2}')
        free2=$(echo $loadmemory | awk '{print $3}')
        free3=$(echo $loadmemory | awk '{print $4}')

        used=`expr $total - $free1 - $free2 - $free3`;
        echo -e "{total:$total,used:$used}"
        exit $?
        ;;
    swap)
        total=$(cat /proc/meminfo |grep SwapTotal |awk '{print $2}')
        free=$(cat /proc/meminfo |grep SwapFree |awk '{print $2}')
        echo -e "{total:$total,free:$free}"
        exit $?
        ;;
    load)
        cat /proc/loadavg |awk '{print $1","$2","$3}'
        exit $?
        ;;
    conf)
        #fix ubuntn osname have \n \l
        hostname=$(echo `hostname`|sed 's/\\.//g');
        os=$(echo `head -n 1 /etc/issue`|sed 's/\\.//g');
        kernel=`uname -r`;
        machine=`uname -m`;

        #get cpudata and trim...
        cpucount=`cat /proc/cpuinfo | grep name | wc -l`;
        cpuname=`cat /proc/cpuinfo | grep name|uniq -c |awk -F ":" '{print $2}'|awk -F "@" '{print $1}'|sed -r 's/^\\s|\\s$//g'`;
        cpuinfo=`cat /proc/cpuinfo | grep name|uniq -c |awk -F ":" '{print $2}'|awk -F "@" '{print $2}'|sed -r 's/^\\s|\\s$//g'`;
        cpuconf="cpuinfo:{\"count\":\"$cpucount\",\"name\":\"$cpuname\",\"info\":\"$cpuinfo\"}";

        #to json data...
        echo -e "{\\n"hostname":\"$hostname\",\\n"os":\"$os\",\\n"kernel":\"$kernel\",\\n"machine":\"$machine\",\\n$cpuconf\\n}";
        exit $?
        ;;
    net)
        netarr=$(cat /proc/net/dev | grep : |tr : " "|awk '{print $1}');
	netstr="";
        for net in ${netarr[@]}
        do
            rxpre=$(cat /proc/net/dev | grep $net | tr : " " | awk '{print $2}')
            txpre=$(cat /proc/net/dev | grep $net | tr : " " | awk '{print $10}')
            sleep 1
            rxnext=$(cat /proc/net/dev | grep $net | tr : " " | awk '{print $2}')
            txnext=$(cat /proc/net/dev | grep $net | tr : " " | awk '{print $10}')
            rx=$((${rxnext}-${rxpre}))
            tx=$((${txnext}-${txpre}))
            rx=$(echo $rx | awk '{print $1*8/1024}');
            tx=$(echo $tx | awk '{print $1*8/1024}');
            netstr=${netstr}"{name:\"$net\",read:$rx,write:$tx},";
        done
	netstr=`echo $netstr|sed 's/.$//'`
        echo $netstr
	exit $?
        ;;
    all)
        net=`bash +x $0 net`;
        disk=`bash +x $0 disk`;
        mem=`bash +x $0 mem`;
        swap=`bash +x $0 swap`;
        load=`bash +x $0 load`;
        conf=`bash +x $0 conf`;
        cpu=`bash +x $0 cpu`;
        echo -e "{\\ncpu:$cpu,\\ndisk:\"$disk\",\\nmem:$mem,\\nnet:[\\n$net\\n],\\nswap:$swap,\\nload:\"$load\",\\nconf:$conf\\n}"
        exit $?
        ;;
    *)
        echo "usage [cpu|disk|mem|net|swap|load|conf|all]"
        exit 1
        ;;
  esac
exit 0;
