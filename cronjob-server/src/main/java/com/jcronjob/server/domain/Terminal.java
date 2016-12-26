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

import com.alibaba.fastjson.annotation.JSONField;
import com.jcronjob.common.utils.RSAUtils;
import com.jcronjob.server.job.CronjobAuth;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by benjobs on 16/5/21.
 */

@Entity
@Table(name = "T_TERMINAL")
public class Terminal implements Serializable{

    @Id
    @GeneratedValue
    private Long id;
    private String name;
    private Long userId;
    private String host;
    private int port;
    private String userName;

    @Lob
    @Column(columnDefinition = "BLOB")
    @JSONField(serialize=false)
    private byte[] authorization;

    private String status = SUCCESS;
    private Date logintime;

    @Transient
    @JSONField(serialize=false)
    private User user;

    @Transient
    @JSONField(serialize=false)
    private String password;

    @Transient
    @JSONField(serialize=false)
    public static final String INITIAL ="INITIAL";
    @Transient
    @JSONField(serialize=false)
    public static final String AUTH_FAIL ="AUTHFAIL";
    @Transient
    @JSONField(serialize=false)
    public static final String PUBLIC_KEY_FAIL ="KEYAUTHFAIL";
    @Transient
    @JSONField(serialize=false)
    public static final String GENERIC_FAIL ="GENERICFAIL";
    @Transient
    @JSONField(serialize=false)
    public static final String SUCCESS ="SUCCESS";
    @Transient
    @JSONField(serialize=false)
    public static final String HOST_FAIL ="HOSTFAIL";

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        byte[] decodedData = RSAUtils.decryptByPrivateKey(this.authorization, CronjobAuth.getPrivateKey());
        return new String(decodedData);
    }

    public void setPassword(String password) throws Exception {
        this.authorization = RSAUtils.encryptByPublicKey(password.getBytes(), CronjobAuth.getPublicKey());
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
                ", password='" + password + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Terminal terminal = (Terminal) o;

        if (userName != null ? !userName.equals(terminal.userName) : terminal.userName != null) return false;
        return user != null ? user.equals(terminal.user) : terminal.user == null;
    }

    @Override
    public int hashCode() {
        int result = userName != null ? userName.hashCode() : 0;
        result = 31 * result + (user != null ? user.hashCode() : 0);
        return result;
    }
}
