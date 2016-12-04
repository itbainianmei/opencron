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
    private String userName;
    private String password;
    private String status = SUCCESS;
    private Date logintime;
    private String token;


    @Transient
    private Agent agent;

    @Transient
    private User user;

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
            this.userName = DigestUtils.desDecrypt(this.token,userName);
            this.password = DigestUtils.desDecrypt(this.token,password);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void desEncrypt(String key){
        try {
            this.token = key;
            this.userName = DigestUtils.desEncrypt(key,userName);
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


    @Override
    public String toString() {
        return "Terminal{" +
                "id=" + id +
                ", userId=" + userId +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                ", status='" + status + '\'' +
                ", logintime=" + logintime +
                ", token='" + token + '\'' +
                ", agent=" + agent +
                ", user=" + user.toString() +
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
