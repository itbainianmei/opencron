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

import java.io.*;

/**
 * Created by benjobs on 15/6/24.
 */
public class IOUtils implements Serializable {

    public static String readFile(File file, String charset) {
        InputStream inputStream = null;
        InputStreamReader inputReader = null;
        BufferedReader bufferReader = null;

        if (CommonUtils.notEmpty(file)) {
            try {
                inputStream = new FileInputStream(file);
                inputReader = new InputStreamReader(new FileInputStream(file), charset);
                bufferReader = new BufferedReader(inputReader);

                StringBuffer strBuffer = new StringBuffer();
                // 读取一行
                String line;
                while ((line = bufferReader.readLine()) != null) {
                    strBuffer.append(line).append("\n\r");
                }
                return strBuffer.toString();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (bufferReader != null) {
                        bufferReader.close();
                    }

                    if (inputReader != null) {
                        inputReader.close();
                    }

                    if (inputStream != null) {
                        inputStream.close();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static boolean writeFile(File file, String text, String charset) {
        try {
            PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), charset));
            out.write(text);
            out.flush();
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static final synchronized String getTempFolderPath() {
        return System.getProperty("java.io.tmpdir");
    }

    public static final synchronized String getProjectFolderPath() {
        String path = null;
        try {
            File directory = new File("");
            path = directory.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

    public static void main(String[] args) {
        System.out.println(getProjectFolderPath());
    }
}
