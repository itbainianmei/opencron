/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.jcronjob.test;

import com.alibaba.fastjson.JSON;
import org.jcronjob.agent.AgentBootstrap;
import org.jcronjob.agent.AgentMonitor;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jcronjob.base.utils.CommandUtils.executeShell;
import static org.jcronjob.base.utils.CommonUtils.toLong;

/**
 * Created by benjobs on 16/3/4.
 */
public class StartUp {

    public static void main(String[] args) throws Exception {

        String str = "{\n" +
                "cpu:{id2:9253930185,id1:9253930177,total2:10041030251,total1:10041030242,detail:\"2.7%us,5.0%sy,0.1%ni,92.2%id,0.1%wa,0.0%hi,0.0%si,0.0%st\"},\n" +
                "disk:\"Filesystem            Size  Used Avail Use% Mounted on\n" +
                "/dev/mapper/vg_bluetest190-lv_root\n" +
                "                       50G  3.5G   44G   8% /\n" +
                "tmpfs                 7.8G     0  7.8G   0% /dev/shm\n" +
                "/dev/sda1             477M   73M  379M  17% /boot\n" +
                "/dev/mapper/vg_bluetest190-lv_home\n" +
                "                      408G  371G   16G  96% /home\",\n" +
                "mem:{total:16281408,used:14181444},\n" +
                "net:[\n" +
                "{name:\"lo\",read:27.1094,write:27.1094},{name:\"eth0\",read:11.5469,write:2.94531}\n" +
                "],\n" +
                "swap:{total:8208380,free:6716268},\n" +
                "load:\"0.14,0.39,0.38\",\n" +
                "conf:{\n" +
                "hostname:\"bluetest190\",\n" +
                "os:\"CentOS release 6.7 (Final)\",\n" +
                "kernel:\"2.6.32-573.3.1.el6.x86_64\",\n" +
                "machine:\"x86_64\",\n" +
                "cpuinfo:{\"count\":\"12\",\"name\":\"Intel(R) Core(TM) i7-3960X CPU\",\"info\":\"3.30GHz\"}\n" +
                "}\n" +
                "}";


        AgentMonitor.Info info = JSON.parseObject(str,AgentMonitor.Info.class);
        System.out.println(info.getNet());

    }



    private static void agent() throws Exception {
        AgentBootstrap bootstrap = new AgentBootstrap();
        bootstrap.start(12009,"cronjob123!@#");
    }


}
