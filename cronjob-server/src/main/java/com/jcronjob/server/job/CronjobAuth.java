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

package com.jcronjob.server.job;

import com.jcronjob.common.utils.CommonUtils;
import com.jcronjob.common.utils.IOUtils;
import com.jcronjob.common.utils.RSAUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

/**
 * Created by benjobs on 2016/12/22.
 */

public class CronjobAuth {

    private static Logger logger = LoggerFactory.getLogger(CronjobAuth.class);
    public static String publicKey = null;
    public static String privateKey = null;
    private static final String charset = "UTF-8";

    public static final String KEY_PATH = CronjobAuth.class.getClassLoader().getResource(".").getPath() + "cronjob";
    public static final String PRIVATE_KEY_PATH = KEY_PATH + "/id_rsa";
    public static final String PUBLIC_KEY_PATH = PRIVATE_KEY_PATH + ".pub";

    private static void load() {
        if (CommonUtils.isEmpty(publicKey, privateKey)) {
            try {
                File keyPath = new File(KEY_PATH);
                if (!keyPath.exists()) {
                    keyPath.mkdirs();
                }
                Map<String, Object> keyMap = RSAUtils.genKeyPair();
                publicKey = RSAUtils.getPublicKey(keyMap);
                privateKey = RSAUtils.getPrivateKey(keyMap);
                File pubFile = new File(PUBLIC_KEY_PATH);
                File prvFile = new File(PRIVATE_KEY_PATH);
                IOUtils.writeText(pubFile, publicKey, charset);
                IOUtils.writeText(prvFile, privateKey, charset);
            } catch (Exception e) {
                logger.error("[cronjob] error:{}" + e.getMessage());
                throw new RuntimeException("init RSA'publicKey and privateKey error!");
            }
        }
    }

    public static String getPublicKey() {
        return getKey(KeyType.PUBLIC);
    }

    public static String getPrivateKey() {
        return getKey(KeyType.PRIVATE);
    }

    private static String getKey(KeyType type) {
        File file = new File(type.equals(KeyType.PUBLIC) ? PUBLIC_KEY_PATH : PRIVATE_KEY_PATH);
        if (file.exists()) {
            switch (type) {
                case PUBLIC:
                    publicKey = IOUtils.readText(file, charset);
                    if (CommonUtils.isEmpty(publicKey)) {
                        load();
                    }
                    break;
                case PRIVATE:
                    privateKey = IOUtils.readText(file, charset);
                    if (CommonUtils.isEmpty(privateKey)) {
                        load();
                    }
                    break;
            }
        } else {
            load();
        }
        return type.equals(KeyType.PUBLIC)? publicKey : privateKey;
    }

    enum KeyType{
        PUBLIC,PRIVATE;
    }

}
