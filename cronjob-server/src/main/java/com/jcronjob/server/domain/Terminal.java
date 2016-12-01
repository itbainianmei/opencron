package com.jcronjob.server.domain;

import com.jcronjob.common.utils.DigestUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

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
    private String user;
    private String password;
    private String status = SUCCESS;
    private Date logintime;
    private String token;

    public static final String INITIAL ="INITIAL";

    @Transient
    private String instanceId;

    @Transient
    private Agent agent;

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

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
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

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getToken() {
        return token;
    }

    public void desDecrypt(){
        try {
            this.user = DigestUtils.desDecrypt(this.token,user);
            this.password = DigestUtils.desDecrypt(this.token,password);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void desEncrypt(String key){
        try {
            this.token = key;
            this.user = DigestUtils.desEncrypt(key,user);
            this.password = DigestUtils.desEncrypt(key,password);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setToken(String token) {
        this.token = token;
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
}
