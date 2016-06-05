/**
 * Copyright 2016 benjobs
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jcronjob.agent;

import com.alibaba.fastjson.JSON;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import org.jcronjob.base.job.Monitor;
import org.jcronjob.base.utils.*;
import org.slf4j.Logger;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.jcronjob.base.utils.CommonUtils.*;

import static org.jcronjob.base.utils.CommandUtils.executeShell;

/**
 * Created by benjobs on 16/4/7.
 */
public class AgentMonitor {

    private  Logger logger = LoggerFactory.getLogger(AgentMonitor.class);

    private  Format format = new DecimalFormat("##0.00");

    private boolean stop = false;

    private Map<UUID, SocketIOClient> clients = new HashMap<UUID, SocketIOClient>(0);

    public void start(final int port) throws Exception {
        Configuration configuration = new Configuration();
        configuration.setPort(port);

        final SocketIOServer server = new SocketIOServer(configuration);

        server.addConnectListener(new ConnectListener() {//添加客户端连接监听器
            @Override
            public void onConnect(final SocketIOClient client) {
                logger.info("[cronjob]:monitor connected:SessionId @ {},port @ {}", client.getSessionId(), port);
                clients.put(client.getSessionId(), client);
                for(;;) {
                    client.sendEvent("monitor", monitor());
                    try {
                        TimeUnit.MICROSECONDS.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        server.addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient client) {
                clients.remove(client.getSessionId());
                logger.info("[cronjob]:monitor disconnect:SessionId @ {},port @ {} ", client.getSessionId(), port);
                if (clients.isEmpty()) {
                    stop = true;
                    logger.info("[cronjob]:monitor closed:SessionId @ {},port @ {} ", client.getSessionId(), port);
                    server.stop();
                }
            }
        });

        server.start();
        stop = false;
        logger.debug("[cronjob] server started @ {}", port);
    }


    public  Monitor monitor() {

        try {

            Monitor monitor = new Monitor();

            String monitorString = executeShell(Globals.CRONJOB_MONITOR_SHELL, "all");

            Info info = JSON.parseObject(monitorString,Info.class);

            //config...
            monitor.setConfig( info.getConf() );

            //cpu
            monitor.setCpuData(getCpuData(info));

            //内存
            monitor.setMemUsage(setMemUsage(info));

            //磁盘
            monitor.setDiskUsage(getDiskUsage(info));

            //网卡
            monitor.setNetwork(getNetwork(info));

            //swap
            monitor.setSwap(getSwap(info));

            //load(负载)
            monitor.setLoad(getLoad(info));

            //iostat...
            //monitor.setIostat(  getIostat() );

            //time
            monitor.setTime(DateUtils.parseStringFromDate(new Date(), "HH:mm:ss"));

            return monitor;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public  String getCpuData(Info info) {
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

            Matcher keyMatcher = Pattern.compile("\\w+$").matcher(detail);
            if (keyMatcher.find()) {
                key = keyMatcher.group();
            }
            cpuData.put(key, val);
        }
        return  JSON.toJSONString(cpuData);
    }


    private  String getDiskUsage(Info info) throws Exception {

        /**
         * get info...
         */
        Scanner scanner = new Scanner(info.getDisk());

        int usedIndex = 0,availIndex = 0, mountedIndex = 0;
        /**
         * title index....
         */
        String title = scanner.nextLine();
        List<String> strArray = Arrays.asList(title.split("\\s"));
        /**
         * Used Avail Use Mounted
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
         */

        List<Map<String, String>> disks = new ArrayList<Map<String, String>>();
        Double usedTotal = 0D;
        Double freeTotal = 0D;

        //set detail....
        while (scanner.hasNextLine()) {
            String content = scanner.nextLine();
            strArray = Arrays.asList(content.split("\\s+"));

            Double used = generateDiskSpace(strArray.get(usedIndex));
            Double free = generateDiskSpace(strArray.get(availIndex));

            usedTotal += used;
            freeTotal += free;

            Map<String, String> disk = new HashMap<String, String>();
            disk.put("disk",strArray.get(mountedIndex));
            disk.put("used", format.format(used));
            disk.put("free", format.format(free));
            disks.add(disk);
        }
        scanner.close();
        //set total....

        Map<String, String> disk = new HashMap<String, String>();
        disk.put("disk", "usage");
        disk.put("used", format.format(usedTotal));
        disk.put("free", format.format(freeTotal));
        disks.add(disk);

        return JSON.toJSONString(disks);
    }

    public  String getNetwork(Info info) {
        Map<String,Float> newWork = new HashMap<String, Float>();
        float read = 0;
        float write = 0;
        for(Net net:info.getNet()){
            read += net.getRead();
            write += net.getWrite();
        }
        newWork.put("read", read);
        newWork.put("write", write);
        return JSON.toJSONString(newWork);
    }

    public  String getSwap(Info info) {
        return format.format( ((info.getSwap().getTotal() - info.getSwap().getFree() ) / info.getSwap().getTotal() ) * 100);
    }

    public  List<String> getLoad(Info info) {
        return Arrays.asList(info.getLoad().split(","));
    }

    private  String setMemUsage( Info info) {
        return format.format( (info.getMem().getUsed() / info.getMem().getTotal() ) * 100);
    }

    public  List<String> getIostat() throws Exception {
        /**
         *
         rrqm/s:  每秒进行 merge 的读操作数目。即 rmerge/s
         wrqm/s:  每秒进行 merge 的写操作数目。即 wmerge/s
         r/s:  每秒完成的读 I/O 设备次数。即 rio/s
         w/s:  每秒完成的写 I/O 设备次数。即 wio/s

         rkB/s:  每秒读K字节数。是 rsect/s 的一半，因为每扇区大小为512字节。
         wkB/s:  每秒写K字节数。是 wsect/s 的一半。
         avgrq-sz:  平均每次设备I/O操作的数据大小 (扇区)。
         avgqu-sz:  平均I/O队列长度。
         await:  平均每次设备I/O操作的等待时间 (毫秒)。
         svctm: 平均每次设备I/O操作的服务时间 (毫秒)。
         %util:  一秒中有百分之多少的时间用于 I/O 操作，即被io消耗的cpu百分比

         Device:         rrqm/s   wrqm/s     r/s     w/s    rkB/s    wkB/s avgrq-sz avgqu-sz   await  svctm  util
         sda               0.31    35.13    0.53   14.80     8.93   191.44    26.16     0.03    2.09   0.60   0.92
         dm-0              0.00     0.00    0.07    2.58     0.93    10.33     8.47     0.01    1.91   0.39   0.10
         dm-1              0.00     0.00    0.27    0.46     1.07     1.85     8.00     0.02   21.16   0.04   0.00
         dm-2              0.00     0.00    0.50   44.82     6.93   179.27     8.22     0.57   12.63   0.18   0.83
         *
         **/

        Map<Integer, String> index = new HashMap<Integer, String>(0);

        List<Field> fields = Arrays.asList(Monitor.Iostat.class.getDeclaredFields());

        List<String> ioList = new ArrayList<String>(0);

        String result = CommandUtils.executeShell(Globals.CRONJOB_MONITOR_SHELL, "io");

        Scanner scan = new Scanner(result);
        boolean isFirst = true;
        while (scan.hasNextLine()) {
            String text = scan.nextLine();
            String data[] = text.split("\\s+");
            if (isFirst) {
                for (int i = 0; i < data.length; i++) {
                    for (Field f : fields) {
                        if (f.getName().equalsIgnoreCase(data[i].replaceAll("/s|:|-.+", ""))) {
                            index.put(i, f.getName());
                        }
                    }
                }
                isFirst = false;
            } else {
                Monitor.Iostat iostat = new Monitor.Iostat();
                for (Map.Entry<Integer, String> entry : index.entrySet()) {
                    Method setMethod = ReflectUitls.setter(Monitor.Iostat.class, entry.getValue());
                    setMethod.invoke(iostat, data[entry.getKey()]);
                }
                ioList.add( JSON.toJSONString(iostat) );
            }

        }
        scan.close();
        return ioList;

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


    public boolean stoped() {
        return stop;
    }

    public static class Info implements Serializable {
        private List<Net> net = new ArrayList<Net>();
        private String disk;
        private Mem mem;
        private Swap swap;
        private Cpu cpu;
        private String load;
        private String conf;

        public List<Net> getNet() {
            return net;
        }

        public void setNet(List<Net> net) {
            this.net = net;
        }

        public String getDisk() {
            return disk;
        }

        public void setDisk(String disk) {
            this.disk = disk;
        }

        public Mem getMem() {
            return mem;
        }

        public void setMem(Mem mem) {
            this.mem = mem;
        }

        public Swap getSwap() {
            return swap;
        }

        public void setSwap(Swap swap) {
            this.swap = swap;
        }

        public Cpu getCpu() {
            return cpu;
        }

        public void setCpu(Cpu cpu) {
            this.cpu = cpu;
        }

        public String getLoad() {
            return load;
        }

        public void setLoad(String load) {
            this.load = load;
        }

        public String getConf() {
            return conf;
        }

        public void setConf(String conf) {
            this.conf = conf;
        }
    }

    public static class Conf implements Serializable {
        private String hostname;
        private String os;
        private String kernel;
        private String machine;
        private String cpuinfo;

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getOs() {
            return os;
        }

        public void setOs(String os) {
            this.os = os;
        }

        public String getKernel() {
            return kernel;
        }

        public void setKernel(String kernel) {
            this.kernel = kernel;
        }

        public String getMachine() {
            return machine;
        }

        public void setMachine(String machine) {
            this.machine = machine;
        }

        public String getCpuinfo() {
            return cpuinfo;
        }

        public void setCpuinfo(String cpuinfo) {
            this.cpuinfo = cpuinfo;
        }
    }

    public static class Cpu implements Serializable {
        private String id2;
        private String id1;
        private String total2;
        private String total1;
        private String detail;


        public String getId2() {
            return id2;
        }

        public void setId2(String id2) {
            this.id2 = id2;
        }

        public String getId1() {
            return id1;
        }

        public void setId1(String id1) {
            this.id1 = id1;
        }

        public String getTotal2() {
            return total2;
        }

        public void setTotal2(String total2) {
            this.total2 = total2;
        }

        public String getTotal1() {
            return total1;
        }

        public void setTotal1(String total1) {
            this.total1 = total1;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }
    }

    public static class Mem implements Serializable {
        private float total;
        private float used;

        public float getTotal() {
            return total;
        }

        public void setTotal(float total) {
            this.total = total;
        }

        public float getUsed() {
            return used;
        }

        public void setUsed(float used) {
            this.used = used;
        }
    }

    public static class Net implements Serializable {
        private String name;
        private float read;
        private float write;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public float getRead() {
            return read;
        }

        public void setRead(float read) {
            this.read = read;
        }

        public float getWrite() {
            return write;
        }

        public void setWrite(float write) {
            this.write = write;
        }
    }

    public static class Swap implements Serializable {
        private float total;
        private float free;

        public float getTotal() {
            return total;
        }

        public void setTotal(float total) {
            this.total = total;
        }

        public float getFree() {
            return free;
        }

        public void setFree(float free) {
            this.free = free;
        }
    }
}
