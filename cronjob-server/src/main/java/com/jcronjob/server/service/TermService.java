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


package com.jcronjob.server.service;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcronjob.common.utils.*;
import com.jcronjob.server.domain.Term;
import com.jcronjob.server.dao.QueryDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Properties;

/**
 *
 * @author <a href="mailto:benjobs@qq.com">benjobs@qq.com</a>
 * @name:CommonUtil
 * @version: 1.0.0
 * @company: com.jcronjob
 * @description: webconsole核心类
 * @date: 2016-05-25 10:03<br/><br/>
 *
 * <b style="color:RED"></b><br/><br/>
 * 你快乐吗?<br/>
 * 风轻轻的问我<br/>
 * 曾经快乐过<br/>
 * 那时的湖面<br/>
 * 她踏着轻舟泛过<br/><br/>
 *
 * 你忧伤吗?<br/>
 * 雨悄悄的问我<br/>
 * 一直忧伤着<br/>
 * 此时的四季<br/>
 * 全是她的柳絮飘落<br/><br/>
 *
 * 你心痛吗?<br/>
 * 泪偷偷的问我<br/>
 * 心痛如刀割<br/>
 * 收到记忆的包裹<br/>
 * 都是她冰清玉洁还不曾雕琢<br/><br/>
 *
 * <hr style="color:RED"/>
 */

@Service
public class TermService {

    @Autowired
    private SSHService sshService;

    @Autowired
    private StatusService statusService;

    @Autowired
    private QueryDao queryDao;

    @Value("#{config['ssh.aes_name']}")
    private String aesKey;

    public static final int SERVER_ALIVE_INTERVAL = 60 * 1000;

    public static final int SESSION_TIMEOUT = 60000;

    private Logger logger = LoggerFactory.getLogger(getClass());

    public Term getTerm(Long userId, String host) throws Exception {
        Term term = queryDao.sqlUniqueQuery(Term.class,"SELECT * FROM T_TERM WHERE userId=? AND host=? And status=?",userId,host, Term.SUCCESS);
        if (term!=null) {
            queryDao.getSession().clear();
            term.desDecrypt();
        }
        return term;
    }

    public boolean saveOrUpdate(Term term) {
        Term dbTerm = queryDao.sqlUniqueQuery(Term.class,"SELECT * FROM T_TERM WHERE userId=? AND host=?",term.getUserId(),term.getHost());
        if (dbTerm!=null) {
            term.setId(dbTerm.getId());
        }
        try {
            //私钥.
            term.desEncrypt(CommonUtils.uuid());
            term.setLogintime(new Date());
            queryDao.save(term);
            return true;
        }catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String auth(Term term) {
        Session jschSession = null;
        try {
            jschSession = createJschSession(term);
            jschSession.connect(SESSION_TIMEOUT);
            return "success";
        } catch (JSchException e) {
            if (e.getLocalizedMessage().equals("Auth fail")) {
                return "authfail";
            }else if(e.getLocalizedMessage().contentEquals("timeout")){
                return "timeout";
            }
           return "error";
        }finally {
            if (jschSession!=null) {
                jschSession.disconnect();
            }
        }
    }

    public Session createJschSession(Term term) throws JSchException {
        JSch jsch = new JSch();
        Session jschSession = jsch.getSession(term.getUser(), term.getHost(), term.getPort());
        jschSession.setPassword(term.getPassword());

        Properties config = new Properties();
        //不记录本次登录的信息到$HOME/.ssh/known_hosts
        config.put("StrictHostKeyChecking", "no");
        //强制登陆认证
        config.put("PreferredAuthentications", "publickey,keyboard-interactive,password");
        jschSession.setServerAliveInterval(SERVER_ALIVE_INTERVAL);
        jschSession.setConfig(config);
        return jschSession;
    }

    public Term getNextPending(Long userId, Long termId) {
        String sql = "select t.* from T_SSH_STATUS s inner join T_TERM t on s.termId = t.id where (s.status like ? or s.status like ? or s.status like ?) and s.userId=? and s.termId=?";
        return queryDao.sqlUniqueQuery(Term.class,sql, Term.INITIAL, Term.AUTH_FAIL, Term.PUBLIC_KEY_FAIL,userId,termId);
    }

    public Term getCurrentTrem(Long termId, Long userId) {
        String sql = "select t.* from T_SSH_STATUS s inner join T_TERM t on s.termId = t.id and s.termId=? and s.userid=?";
        return queryDao.sqlUniqueQuery(Term.class,sql,termId,userId);
    }


}
