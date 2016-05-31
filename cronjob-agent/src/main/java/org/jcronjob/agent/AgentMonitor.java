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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jcronjob.base.utils.CommandUtils.executeShell;
import static org.jcronjob.base.utils.CommonUtils.toFloat;
import static org.jcronjob.base.utils.CommonUtils.toLong;

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

            //cpu
            monitor.setCpuData(getCpuData());

            //内存
            monitor.setMemUsage(setMemUsage());

            //磁盘
            monitor.setDiskUsage(getDiskUsage());

            //网卡
            monitor.setNetwork(getNetwork());

            //swap
            monitor.setSwap(getSwap());

            //load(负载)
            monitor.setLoad(getLoad());

            //config...
            monitor.setConfig( getConfig() );

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


    public  String getCpuData() {
        String cpuInfo[] = executeShell(Globals.CRONJOB_MONITOR_SHELL, "cpu").split("\\t");
        //cpu usage report..
        Long sysIdle = toLong(cpuInfo[0]) - toLong(cpuInfo[1]);
        Long total = toLong(cpuInfo[2]) - toLong(cpuInfo[3]);

        float sysUsage = (sysIdle.floatValue() / total.floatValue()) * 100;
        float sysRate = 100 - sysUsage;

        Map<String, String> cpuData = new HashMap<String, String>(0);

        cpuData.put("usage", new DecimalFormat("##0.00").format(sysRate));

        //cpu detail...
        String cpuDetail[] = cpuInfo[cpuInfo.length - 1].split(",");
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


    private  String getDiskUsage() throws Exception {

        /**
         * get info...
         */
        Map<String, String> map = new HashMap<String, String>(0);

        Scanner scanner = new Scanner(CommandUtils.executeShell(Globals.CRONJOB_MONITOR_SHELL, "disk"));
        List<String> tmpArray = new ArrayList<String>(0);

        int usedIndex = 0, availIndex = 0, mountedIndex = 0, len;
        /**
         * title index....
         */
        String title = scanner.nextLine();
        List<String> strArray = Arrays.asList(title.split("\\s+"));
        len = strArray.size() - 1;//Mounted on...
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
         */
        while (scanner.hasNextLine()) {
            String content = scanner.nextLine();
            strArray = Arrays.asList(content.split("\\s+"));
            if (strArray.size() == len) {
                map.put(strArray.get(mountedIndex), strArray.get(usedIndex) + "," + strArray.get(availIndex));
            } else if (strArray.size() < len) {
                if (tmpArray.isEmpty()) {
                    tmpArray = new ArrayList<String>(strArray);
                } else {
                    tmpArray.addAll(strArray);
                    if (tmpArray.size() == len) {
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

    public  String getNetwork() {
        Map<String, List<String>> newWork = new HashMap<String, List<String>>(0);
        String network = executeShell(Globals.CRONJOB_MONITOR_SHELL, "net");
        Scanner scan = new Scanner(network);
        while (scan.hasNextLine()) {
            String[] dataArr = scan.nextLine().split("\\t");
            String key = dataArr[0];
            List<String> data = Arrays.asList( CommonUtils.arrayRemoveIndex(dataArr,0) );
            newWork.put(key, data );
        }
        scan.close();
        return JSON.toJSONString(newWork);
    }

    public  String getSwap() {
        String swap[] = executeShell(Globals.CRONJOB_MONITOR_SHELL, "swap").split("\\t");
        return format.format(((toFloat(swap[0]) - toFloat(swap[1])) / toFloat(swap[0])) * 100);
    }

    public  List<String> getLoad() {
        return Arrays.asList( executeShell(Globals.CRONJOB_MONITOR_SHELL, "load").split(",") );
    }

    private  String setMemUsage() {
        String memUsage[] = CommandUtils.executeShell(Globals.CRONJOB_MONITOR_SHELL, "mem").split("\\s+");
        return format.format((toFloat(memUsage[1]) / toFloat(memUsage[0])) * 100);
    }

    private  String getConfig() {
        return CommandUtils.executeShell(Globals.CRONJOB_MONITOR_SHELL, "conf");
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



}
