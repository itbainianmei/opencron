package com.jcronjob.server.job;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import com.alibaba.fastjson.JSON;
import com.jcronjob.common.job.Monitor;
import com.jcronjob.common.utils.DateUtils;
import com.jcronjob.common.utils.DigestUtils;
import com.jcronjob.common.utils.ReflectUitls;
import com.jcronjob.server.domain.Terminal;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.*;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jcronjob.common.utils.CommonUtils.toLong;

/**
 * Created by benjobs on 2016/12/21.
 */
public class MornitorClient {

    private WebSocketSession webSocketSession;
    private Connection connection;
    private Session session;
    private Terminal terminal;
    private Format format = new DecimalFormat("##0.00");

    private String shell = "disk=$(df -h|sed -r 's/\\s+/ /g'|sed -r 's/Mounted\\s+on/Mounted/g'|sed -r 's/%//g');\n" +
            "\n" +
            "load=$(cat /proc/loadavg |awk '{print $1\",\"$2\",\"$3}');\n" +
            "\n" +
            "total=$(cat /proc/meminfo |grep SwapTotal |awk '{print $2}');\n" +
            "free=$(cat /proc/meminfo |grep SwapFree |awk '{print $2}');\n" +
            "swap=$(echo  \"{total:$total,free:$free}\");\n" +
            "\n" +
            "cpulog_1=$(cat /proc/stat | grep 'cpu ' | awk '{print $2\" \"$3\" \"$4\" \"$5\" \"$6\" \"$7\" \"$8}');\n" +
            "sysidle1=$(echo $cpulog_1 | awk '{print $4}');\n" +
            "total1=$(echo $cpulog_1 | awk '{print $1+$2+$3+$4+$5+$6+$7}');\n" +
            "cpulog_2=$(cat /proc/stat | grep 'cpu ' | awk '{print $2\" \"$3\" \"$4\" \"$5\" \"$6\" \"$7\" \"$8}');\n" +
            "sysidle2=$(echo $cpulog_2 | awk '{print $4}');\n" +
            "total2=$(echo $cpulog_2 | awk '{print $1+$2+$3+$4+$5+$6+$7}');\n" +
            "cpudetail=$(top -b -n 1 | grep Cpu |sed -r 's/\\s+//g'|awk -F \":\" '{print $2}');\n" +
            "cpu=$(echo  \"{id2:\\\"$sysidle2\\\",id1:\\\"$sysidle1\\\",total2:\\\"$total2\\\",total1:\\\"$total1\\\",detail:\\\"$cpudetail\\\"}\");\n" +
            "\n" +
            "loadmemory=$(cat /proc/meminfo | awk '{print $2}');\n" +
            "total=$(echo $loadmemory | awk '{print $1}');\n" +
            "free1=$(echo $loadmemory | awk '{print $2}');\n" +
            "free2=$(echo $loadmemory | awk '{print $3}');\n" +
            "free3=$(echo $loadmemory | awk '{print $4}');\n" +
            "used=$(($total - $free1 - $free2 - $free3));\n" +
            "mem=$(echo  \"{total:$total,used:$used}\");\n" +
            "\n" +
            "hostname=$(echo `hostname`|sed 's/\\\\.//g');\n" +
            "os=$(echo `head -n 1 /etc/issue`|sed 's/\\\\.//g');\n" +
            "if [ -z \"$os\" ];then\n" +
            " os=$(echo `cat /etc/redhat-release`|sed 's/\\\\.//g');\n" +
            "fi\n" +
            "kernel=$(uname -r);\n" +
            "machine=$(uname -m);\n" +
            "\n" +
            "top=$(echo \"P\"|top -b -n 1| head -18|sed -r 's/\\s+/ /g'| sed  '1,6d');\n" +
            "\n" +
            "cpucount=$(cat /proc/cpuinfo | grep name | wc -l);\n" +
            "cpuname=$(cat /proc/cpuinfo | grep name|uniq -c |awk -F \":\" '{print $2}'|awk -F \"@\" '{print $1}'|sed -r 's/^\\\\s|\\\\s$//g');\n" +
            "cpuinfo=$(cat /proc/cpuinfo | grep name|uniq -c |awk -F \":\" '{print $2}'|awk -F \"@\" '{print $2}'|sed -r 's/^\\\\s|\\\\s$//g');\n" +
            "cpuconf=\"cpuinfo:{\\\"count\\\":\\\"$cpucount\\\",\\\"name\\\":\\\"$cpuname\\\",\\\"info\\\":\\\"$cpuinfo\\\"}\";\n" +
            "\n" +
            "conf=$(echo  \"{\"hostname\":\\\"$hostname\\\",\"os\":\\\"$os\\\",\"kernel\":\\\"$kernel\\\",\"machine\":\\\"$machine\\\",$cpuconf}\");\n" +
            "\n" +
            "echo  \"{top:\\\"$top\\\",cpu:$cpu,disk:\\\"$disk\\\",mem:$mem,swap:$swap,load:\\\"$load\\\",conf:$conf}\";";

    public MornitorClient(WebSocketSession webSocketSession, Terminal terminal) {
        this.webSocketSession = webSocketSession;
        this.terminal = terminal;
    }

    public boolean connect() throws Exception {
        connection = new Connection(terminal.getHost(), terminal.getPort());
        connection.connect();
        if (!connection.authenticateWithPassword(terminal.getUserName(), terminal.getPassword())) {
            return false;
        }
        session = connection.openSession();
        return true;
    }

    public void sendMessage() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                StringBuilder builder = new StringBuilder();
                try {
                    session.execCommand(shell);
                    InputStream stdout = new StreamGobbler(session.getStdout());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));

                    StringBuffer buffer = new StringBuffer();
                    while (true)  {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        }else {
                            line+="\n";
                        }
                        buffer.append(line);
                    }

                    Monitor monitor = monitor(buffer.toString());

                    String json = JSON.toJSONString(monitor);

                    if( webSocketSession != null && webSocketSession.isOpen() ) {
                        if (DigestUtils.getEncoding(builder.toString()).equals("ISO-8859-1")) {
                            webSocketSession.sendMessage(new TextMessage(new String(json.getBytes("ISO-8859-1"), "UTF-8")));
                        } else {
                            webSocketSession.sendMessage(new TextMessage(new String(json.getBytes("gb2312"), "UTF-8")));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 5 * 1000);
    }

    public void disconnect() throws IOException {
        if (connection != null) {
            connection.close();
            connection = null;
        }
        if (session != null) {
            session.close();
            session = null;
        }
    }


    public Monitor monitor(String monitorString) {

        try {

            Monitor monitor = new Monitor();

            Monitor.Info info = JSON.parseObject(monitorString,Monitor.Info.class);

            monitor.setTop( getTop(info.getTop()) );

            //config...
            monitor.setConfig( info.getConf() );

            //cpu
            monitor.setCpuData(getCpuData(info));

            //内存
            monitor.setMemUsage(setMemUsage(info));

            //磁盘
            monitor.setDiskUsage(getDiskUsage(info));

            //swap
            monitor.setSwap(getSwap(info));

            //load(负载)
            monitor.setLoad(getLoad(info));

            //time
            monitor.setTime(DateUtils.parseStringFromDate(new Date(), "HH:mm:ss"));

            return monitor;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getTop(String str) throws Exception {
        Map<Integer, String> index = new HashMap<Integer, String>(0);
        List<Field> fields = Arrays.asList(Monitor.Top.class.getDeclaredFields());
        List<String> topList = new ArrayList<String>(0);

        Scanner scan = new Scanner(str);
        boolean isFirst = true;
        while (scan.hasNextLine()) {
            String text = scan.nextLine().trim();
            String data[] = text.split("\\s+");
            if (isFirst) {
                for (int i = 0; i < data.length; i++) {
                    for (Field f : fields) {
                        if (f.getName().equalsIgnoreCase(data[i].replaceAll("%|\\+",""))) {
                            index.put(i, f.getName());
                        }
                    }
                }
                isFirst = false;
            } else {
                Monitor.Top top = new Monitor.Top();
                for (Map.Entry<Integer, String> entry : index.entrySet()) {
                    ReflectUitls.setter(Monitor.Top.class, entry.getValue()).invoke(top, data[entry.getKey()]);
                }
                topList.add( JSON.toJSONString(top) );
            }

        }
        scan.close();

        return  JSON.toJSONString(topList);
    }


    public  String getCpuData(Monitor.Info info) {
        //cpu usage report..
        Long sysIdle = toLong(info.getCpu().getId2()) - toLong(info.getCpu().getId1());
        Long total = toLong(info.getCpu().getTotal2())- toLong(info.getCpu().getTotal1());

        float sysUsage = (sysIdle.floatValue()/ total.floatValue()) * 100;
        float sysRate = 100 - sysUsage;

        Map<String, String> cpuData = new HashMap<String, String>(0);

        cpuData.put("usage", new DecimalFormat("##0.00").format(sysRate));

        //cpu detail...
        String cpuDetail[] = info.getCpu().getDetail().split(",");
        for (String detail : cpuDetail) {
            String key=null,val=null;
            Matcher valMatcher = Pattern.compile("^\\d+(\\.\\d+)?").matcher(detail);
            if (valMatcher.find()) {
                val = valMatcher.group();
            }

            Matcher keyMatcher = Pattern.compile("[a-zA-Z]+$").matcher(detail);
            if (keyMatcher.find()) {
                key = keyMatcher.group();
            }
            cpuData.put(key, val);
        }
        return  JSON.toJSONString(cpuData);
    }


    private  String getDiskUsage(Monitor.Info info) throws Exception {

        /**
         * get info...
         */
        Map<String, String> map = new HashMap<String, String>(0);

        Scanner scanner = new Scanner(info.getDisk());
        List<String> tmpArray = new ArrayList<String>(0);

        int usedIndex = 0, availIndex = 0, mountedIndex = 0, len;
        /**
         * title index....
         */
        String title = scanner.nextLine();
        List<String> strArray = Arrays.asList(title.split("\\s+"));
        len = strArray.size();//注意shell脚本中已经删除了Mounted on中的空格.
        /**
         * Size Used Avail Use% Mounted
         */
        for (int i = 0; i < strArray.size(); i++) {
            String key = strArray.get(i);
            if (key.equals("Used")) {
                usedIndex = i;
            }
            if (key.equals("Avail")) {
                availIndex = i;
            }
            if (key.equals("Mounted")) {
                mountedIndex = i;
            }
        }

        /**
         * data.....
         *
         * 注意:当Filesystem的值太长,导致本来一行的数据换行问题.
         *
         */
        while (scanner.hasNextLine()) {
            String content = scanner.nextLine();
            strArray = Arrays.asList(content.split("\\s+"));
            if (strArray.size() == len) {
                map.put(strArray.get(mountedIndex), strArray.get(usedIndex) + "," + strArray.get(availIndex));
            } else if (strArray.size() < len) {//某个字段对应的值太长,导致本来一行的数据换行问题.
                if (tmpArray.isEmpty()) {
                    tmpArray = new ArrayList<String>(strArray);
                } else {
                    tmpArray.addAll(strArray);
                    if (tmpArray.size() == len) {
                        /**
                         * 合并后的一行数据
                         */
                        map.put(tmpArray.get(mountedIndex), tmpArray.get(usedIndex) + "," + tmpArray.get(availIndex));
                        tmpArray = Collections.emptyList();
                    }
                }
            }

        }
        scanner.close();

        //set detail....
        List<Map<String, String>> disks = new ArrayList<Map<String, String>>();
        Double usedTotal = 0D;
        Double freeTotal = 0D;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            Map<String, String> disk = new HashMap<String, String>();

            Double used = generateDiskSpace(entry.getValue().split(",")[0]);
            Double free = generateDiskSpace(entry.getValue().split(",")[1]);

            usedTotal += used;
            freeTotal += free;
            disk.put("disk", entry.getKey());
            disk.put("used", format.format(used));
            disk.put("free", format.format(free));
            disks.add(disk);
        }

        Map<String, String> disk = new HashMap<String, String>();
        disk.put("disk", "usage");
        disk.put("used", format.format(usedTotal));
        disk.put("free", format.format(freeTotal));
        disks.add(disk);

        return JSON.toJSONString(disks);
    }

    public  String getNetwork(Monitor.Info info) {
        Map<String,Float> newWork = new HashMap<String, Float>();
        float read = 0;
        float write = 0;
        for(Monitor.Net net:info.getNet()){
            read += net.getRead();
            write += net.getWrite();
        }
        newWork.put("read", read);
        newWork.put("write", write);
        return JSON.toJSONString(newWork);
    }

    public  String getSwap(Monitor.Info info) {
        return format.format( ((info.getSwap().getTotal() - info.getSwap().getFree() ) / info.getSwap().getTotal() ) * 100);
    }

    public  List<String> getLoad(Monitor.Info info) {
        return Arrays.asList(info.getLoad().split(","));
    }

    private  String setMemUsage( Monitor.Info info) {
        return format.format( (info.getMem().getUsed() / info.getMem().getTotal() ) * 100);
    }

    private   Double generateDiskSpace(String value) {
        String fix = value.substring(value.length() - 1);
        String space = value.substring(0, value.length() - 1);

        if (fix.equalsIgnoreCase("D")) {
            return Double.parseDouble(space) * Math.pow(1024, 8);
        }
        if (fix.equalsIgnoreCase("N")) {
            return Double.parseDouble(space) * Math.pow(1024, 7);
        }

        if (fix.equalsIgnoreCase("B")) {
            return Double.parseDouble(space) * Math.pow(1024, 6);
        }

        if (fix.equalsIgnoreCase("Y")) {
            return Double.parseDouble(space) * Math.pow(1024, 5);
        }

        if (fix.equalsIgnoreCase("Z")) {
            return Double.parseDouble(space) * Math.pow(1024, 4);
        }

        if (fix.equalsIgnoreCase("E")) {
            return Double.parseDouble(space) * Math.pow(1024, 3);
        }

        if (fix.equalsIgnoreCase("P")) {
            return Double.parseDouble(space) * Math.pow(1024, 2);
        }

        if (fix.equalsIgnoreCase("T")) {
            return Double.parseDouble(space) * Math.pow(1024, 1);
        }

        if (fix.equalsIgnoreCase("G")) {
            return Double.parseDouble(space);
        }

        if (fix.equalsIgnoreCase("M")) {
            return Double.parseDouble(space) / 1024;
        }
        return 0D;
    }

}
