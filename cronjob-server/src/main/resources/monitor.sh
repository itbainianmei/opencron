disk=$(df -h|sed -r 's/\s+/ /g'|sed -r 's/Mounted\s+on/Mounted/g'|sed -r 's/%//g');

load=$(cat /proc/loadavg |awk '{print $1","$2","$3}');

total=$(cat /proc/meminfo |grep SwapTotal |awk '{print $2}');
free=$(cat /proc/meminfo |grep SwapFree |awk '{print $2}');
swap=$(echo  "{total:$total,free:$free}");

cpulog_1=$(cat /proc/stat | grep 'cpu ' | awk '{print $2" "$3" "$4" "$5" "$6" "$7" "$8}');
sysidle1=$(echo $cpulog_1 | awk '{print $4}');
total1=$(echo $cpulog_1 | awk '{print $1+$2+$3+$4+$5+$6+$7}');
cpulog_2=$(cat /proc/stat | grep 'cpu ' | awk '{print $2" "$3" "$4" "$5" "$6" "$7" "$8}');
sysidle2=$(echo $cpulog_2 | awk '{print $4}');
total2=$(echo $cpulog_2 | awk '{print $1+$2+$3+$4+$5+$6+$7}');
cpudetail=$(top -b -n 1 | grep Cpu |sed -r 's/\s+//g'|awk -F ":" '{print $2}');
cpu=$(echo  "{id2:\"$sysidle2\",id1:\"$sysidle1\",total2:\"$total2\",total1:\"$total1\",detail:\"$cpudetail\"}");

loadmemory=$(cat /proc/meminfo | awk '{print $2}');
total=$(echo $loadmemory | awk '{print $1}');
free1=$(echo $loadmemory | awk '{print $2}');
free2=$(echo $loadmemory | awk '{print $3}');
free3=$(echo $loadmemory | awk '{print $4}');
used=$(($total - $free1 - $free2 - $free3));
mem=$(echo  "{total:$total,used:$used}");

hostname=$(echo `hostname`|sed 's/\\.//g');
os=$(echo `head -n 1 /etc/issue`|sed 's/\\.//g');
if [ -z "$os" ];then
 os=$(echo `cat /etc/redhat-release`|sed 's/\\.//g');
fi
kernel=$(uname -r);
machine=$(uname -m);

top=$(echo "P"|top -b -n 1| head -18|sed -r 's/\s+/ /g'| sed  '1,6d');

cpucount=$(cat /proc/cpuinfo | grep name | wc -l);
cpuname=$(cat /proc/cpuinfo | grep name|uniq -c |awk -F ":" '{print $2}'|awk -F "@" '{print $1}'|sed -r 's/^\\s|\\s$//g');
cpuinfo=$(cat /proc/cpuinfo | grep name|uniq -c |awk -F ":" '{print $2}'|awk -F "@" '{print $2}'|sed -r 's/^\\s|\\s$//g');
cpuconf="cpuinfo:{\"count\":\"$cpucount\",\"name\":\"$cpuname\",\"info\":\"$cpuinfo\"}";

conf=$(echo  "{"hostname":\"$hostname\","os":\"$os\","kernel":\"$kernel\","machine":\"$machine\",$cpuconf}");

echo  "{top:\"$top\",cpu:$cpu,disk:\"$disk\",mem:$mem,swap:$swap,load:\"$load\",conf:$conf}";
