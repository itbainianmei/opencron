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


package com.jredrain.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.jredrain.base.job.Monitor;
import com.jredrain.base.utils.ReflectUitls;
import com.jredrain.startup.Bootstrap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by benjobs on 16/3/4.
 */
public class StartUp {

    public static void main(String[] args) throws Exception {
       String str = " PID USER PR NI VIRT RES SHR S %CPU %MEM TIME+ COMMAND\n" +
               " 1 root 20 0 19336 240 84 S 0.0 0.0 2:45.89 init\n" +
               " 2 root 20 0 0 0 0 S 0.0 0.0 0:05.39 kthreadd\n" +
               " 3 root RT 0 0 0 0 S 0.0 0.0 7:25.88 migration/0\n" +
               " 4 root 20 0 0 0 0 S 0.0 0.0 14:07.25 ksoftirqd/0\n" +
               " 5 root RT 0 0 0 0 S 0.0 0.0 0:00.00 migration/0\n" +
               " 6 root RT 0 0 0 0 S 0.0 0.0 0:34.87 watchdog/0\n" +
               " 7 root RT 0 0 0 0 S 0.0 0.0 6:58.41 migration/1\n" +
               " 8 root RT 0 0 0 0 S 0.0 0.0 0:00.00 migration/1\n" +
               " 9 root 20 0 0 0 0 S 0.0 0.0 13:37.90 ksoftirqd/1\n" +
               " 10 root RT 0 0 0 0 S 0.0 0.0 0:29.13 watchdog/1\n" +
               " 11 root RT 0 0 0 0 S 0.0 0.0 5:45.59 migration/2\n" +
               " 12 root RT 0 0 0 0 S 0.0 0.0 0:00.00 migration/2\n" +
               " 13 root 20 0 0 0 0 S 0.0 0.0 18:51.35 ksoftirqd/2\n" +
               " 14 root RT 0 0 0 0 S 0.0 0.0 0:29.24 watchdog/2\n" +
               " 15 root RT 0 0 0 0 S 0.0 0.0 4:50.77 migration/3 ";

        Map<Integer, String> index = new HashMap<Integer, String>(0);

        List<Field> fields = Arrays.asList(Monitor.Top.class.getDeclaredFields());

        List<String> topList = new ArrayList<String>(0);

        String result = str;

        Scanner scan = new Scanner(result);
        boolean isFirst = true;
        while (scan.hasNextLine()) {
            String text = scan.nextLine().trim();
            String data[] = text.split("\\s+");
            if (isFirst) {
                for (int i = 0; i < data.length; i++) {
                    for (Field f : fields) {
                        if (f.getName().equalsIgnoreCase(data[i].replaceAll("%|\\+", ""))) {
                            index.put(i, f.getName());
                        }
                    }
                }
                isFirst = false;
            } else {
                Monitor.Top top = new Monitor.Top();
                for (Map.Entry<Integer, String> entry : index.entrySet()) {
                    Method setMethod = ReflectUitls.setter(Monitor.Top.class, entry.getValue());
                    setMethod.invoke(top, data[entry.getKey()]);
                }
                topList.add( JSON.toJSONString(top, SerializerFeature.SortField) );
            }

        }
        scan.close();

        System.out.println(JSON.toJSONString(topList));

    }



    private static void agent() throws Exception {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.start(12009,"cronjob123!@#");
    }


}
