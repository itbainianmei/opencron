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

package org.jcronjob.base.utils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;


public class PageIOUtils {

    public static void writeXml(HttpServletResponse response, String xml) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/xml");
        try {
            byte[] data = String.valueOf(xml).getBytes("UTF-8");
            response.setHeader("Content-Length", "" + data.length);
            write(response, xml);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static void writeTxt(HttpServletResponse response, String txt) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/plain");
        write(response, txt);
    }

    public static void writeHtml(HttpServletResponse response, String ajax) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html");
        write(response, ajax);
    }

    public static void writeJson(HttpServletResponse response, String json) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        write(response, json);
    }

    private static void write(HttpServletResponse response, String content) {
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        }
    }


}
