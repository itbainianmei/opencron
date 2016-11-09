/*
 * Copyright 2016 benjobs
 *
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by benjobs on 15/12/5.
 */
public class RegDemo {

    public static void main(String[] args) {


        String cpuDetail[] = "2.2%us,1.0%sy,0.0%ni,96.4id,0.5wa,0.0hi,0.0si,0.0st".split(",");
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
            System.out.println(key+"--->"+val);
        }



    }
}
