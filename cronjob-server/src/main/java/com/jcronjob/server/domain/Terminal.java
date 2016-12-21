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
 *
 *
 */
package com.jcronjob.server.domain;

import com.jcronjob.common.utils.RSAUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

/**
 * Created by benjobs on 16/5/21.
 */

@Entity
@Table(name = "T_TERM")
public class Terminal implements Serializable{

    @Id
    @GeneratedValue
    private Long id;
    private Long userId;
    private String host;
    private int port;
    private String userName;

    @Lob
    @Column(columnDefinition = "BLOB")
    private byte[] authorization;


    private String status = SUCCESS;
    private Date logintime;

    @Transient
    private Agent agent;


    @Transient
    public static final String INITIAL ="INITIAL";
    @Transient
    public static final String AUTH_FAIL ="AUTHFAIL";
    @Transient
    public static final String PUBLIC_KEY_FAIL ="KEYAUTHFAIL";
    @Transient
    public static final String GENERIC_FAIL ="GENERICFAIL";
    @Transient
    public static final String SUCCESS ="SUCCESS";
    @Transient
    public static final String HOST_FAIL ="HOSTFAIL";

    @Transient
    private User user;

    @Transient
    private String password;

    @Transient
    private static String publicKey = null;

    @Transient
    private static String privateKey = null;

    static {
        try {
            Map<String, Object> keyMap = RSAUtils.genKeyPair();
            publicKey = RSAUtils.getPublicKey(keyMap);
            privateKey = RSAUtils.getPrivateKey(keyMap);
            System.err.println("公钥: \n\r" + publicKey);
            System.err.println("私钥： \n\r" + privateKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() throws Exception {
        byte[] decodedData = RSAUtils.decryptByPrivateKey(this.authorization, privateKey);
        return new String(decodedData);
    }

    public void setPassword(String password) throws Exception {
        //对key进行非对称加密
        this.authorization = RSAUtils.encryptByPublicKey(password.getBytes(), publicKey);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getLogintime() {
        return logintime;
    }

    public void setLogintime(Date logintime) {
        this.logintime = logintime;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public byte[] getAuthorization() {
        return authorization;
    }

    public void setAuthorization(byte[] authorization) {
        this.authorization = authorization;
    }


    @Override
    public String toString() {
        return "Terminal{" +
                "id=" + id +
                ", userId=" + userId +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", userName='" + userName + '\'' +
                ", status='" + status + '\'' +
                ", logintime=" + logintime +
                ", agent=" + agent +
                ", password='" + password + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Terminal terminal = (Terminal) o;

        if (userName != null ? !userName.equals(terminal.userName) : terminal.userName != null) return false;
        if (agent != null ? !agent.equals(terminal.agent) : terminal.agent != null) return false;
        return user != null ? user.equals(terminal.user) : terminal.user == null;
    }

    @Override
    public int hashCode() {
        int result = userName != null ? userName.hashCode() : 0;
        result = 31 * result + (agent != null ? agent.hashCode() : 0);
        result = 31 * result + (user != null ? user.hashCode() : 0);
        return result;
    }
}
