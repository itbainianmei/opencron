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
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * Created by benjobs on 2016/12/22.
 */

public class CronjobAuth {

    private static Logger logger = LoggerFactory.getLogger(CronjobAuth.class);
    public static String publicKey = null;
    public static String privateKey = null;
    private static final String charset = "UTF-8";

    public static String KEY_PATH = null;
    public static String PRIVATE_KEY_PATH = null;
    public static String PUBLIC_KEY_PATH = null;

    private static void generateKey() {
        if (CommonUtils.isEmpty(publicKey, privateKey)) {
            try {
                File keyPath = new File(KEY_PATH);
                if (!keyPath.exists()) {
                    keyPath.mkdirs();
                }
                Map<String, Object> keyMap = RSAUtils.genKeyPair();
                publicKey = RSAUtils.getPublicKey(keyMap);
                privateKey = RSAUtils.getPrivateKey(keyMap);
                File pubFile = new File(getPublicKeyPath());
                File prvFile = new File(getPrivateKeyPath());
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
        File file = new File(type.equals(KeyType.PUBLIC) ? getPublicKeyPath() : getPrivateKeyPath());
        if (file.exists()) {
            switch (type) {
                case PUBLIC:
                    publicKey = IOUtils.readText(file, charset);
                    if (CommonUtils.isEmpty(publicKey)) {
                        generateKey();
                    }
                    break;
                case PRIVATE:
                    privateKey = IOUtils.readText(file, charset);
                    if (CommonUtils.isEmpty(privateKey)) {
                        generateKey();
                    }
                    break;
            }
        } else {
            generateKey();
        }
        return type.equals(KeyType.PUBLIC) ? publicKey : privateKey;
    }

    private static String getKeyPath() {
        if (KEY_PATH == null) {
            KEY_PATH = System.getProperties().getProperty("user.home")+File.separator+".cronjob";
            // 从config.properties配置都读取用户手动设置的keypath的位置,配置文件里默认没有,不建议用户指定
            // 如果指定了位置可能会导致之前所有已可ssh登录的机器无法登陆,需要再次输入用户名密码
            InputStream inputStream = CronjobAuth.class.getClassLoader().getResourceAsStream("config.properties");
            Properties properties = new Properties();
            try {
                properties.load(inputStream);
                String path = properties.getProperty("cronjob.keypath");
                if (path!=null) {
                    KEY_PATH = path;
                }
            } catch (Exception e) {
            }
        }
        return KEY_PATH;
    }

    private static String getPrivateKeyPath() {
        PRIVATE_KEY_PATH = getKeyPath() + File.separator+ "id_rsa";
        return PRIVATE_KEY_PATH;
    }

    private static String getPublicKeyPath() {
        PUBLIC_KEY_PATH = getPrivateKeyPath() + ".pub";
        return PUBLIC_KEY_PATH;
    }

    enum KeyType {
        PUBLIC, PRIVATE
    }

}
