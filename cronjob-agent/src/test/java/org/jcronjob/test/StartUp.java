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
        int[] xx = new int[]{1,2,3,4};
        for(final int x : xx) {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    System.out.println("beg:==>"+x);
                    try {
                        Thread.sleep(10000);
                        System.out.println("after:===>"+x);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            t.start();
        }
    }



    private static void agent() throws Exception {
        AgentBootstrap bootstrap = new AgentBootstrap();
        bootstrap.start(12009,"cronjob123!@#");
    }


}
