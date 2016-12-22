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
        return getKey(true);
    }

    public static String getPrivateKey() {
        return getKey(false);
    }

    private static String getKey(boolean type) {
        File file = new File(type ? PUBLIC_KEY_PATH : PRIVATE_KEY_PATH);
        if (file.exists()) {
            try {
                if (type) {
                    publicKey = IOUtils.readText(file, charset);
                    if (CommonUtils.isEmpty(publicKey)) {
                        load();
                    }
                } else {
                    privateKey = IOUtils.readText(file, charset);
                    if (CommonUtils.isEmpty(privateKey)) {
                        load();
                    }
                }
            } catch (Exception ex) {
                logger.error(ex.toString(), ex);
            }
        } else {
            load();
        }
        return type ? publicKey : privateKey;
    }

}
