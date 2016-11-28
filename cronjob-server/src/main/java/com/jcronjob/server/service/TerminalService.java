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

import com.alibaba.fastjson.JSON;
import com.jcraft.jsch.*;
import com.jcronjob.common.utils.*;
import com.jcronjob.server.domain.Terminal;
import com.jcronjob.server.dao.QueryDao;
import com.jcronjob.server.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
public class TerminalService {

    public static final int SERVER_ALIVE_INTERVAL = 60 * 1000;

    public static final int SESSION_TIMEOUT = 60000;

    public static final boolean agentForwarding = false;

    @Autowired
    private QueryDao queryDao;

    private static Map<String,UserSessionsOutput> userSessionsOutputMap = new ConcurrentHashMap<String, UserSessionsOutput>();

    private static Logger logger = LoggerFactory.getLogger(TerminalService.class);

    public Terminal getTerm(Long userId, String host) throws Exception {
        Terminal term = queryDao.sqlUniqueQuery(Terminal.class,"SELECT * FROM T_TERM WHERE userId=? AND host=? And status=?",userId,host, Terminal.SUCCESS);
        if (term!=null) {
            queryDao.getSession().clear();
            term.desDecrypt();
        }
        return term;
    }

    public boolean saveOrUpdate(Terminal term) {
        Terminal dbTerm = queryDao.sqlUniqueQuery(Terminal.class,"SELECT * FROM T_TERM WHERE userId=? AND host=?",term.getUserId(),term.getHost());
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

    public String auth(Terminal term) {
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


    public Terminal openTerminal(Terminal term, Long userId, String sessionId, Map<String, UserSchSessions> userSessionMap) {

        String instanceId = CommonUtils.uuid();
        term.setInstanceId(instanceId);

        JSch jsch = new JSch();

        SchSession schSession = null;

        String retVal = "SUCCESS";
        try {
            //create session
            Session session = jsch.getSession(term.getUser(),term.getHost(), term.getPort());
            session.setPassword(term.getPassword());
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
            session.setServerAliveInterval(SERVER_ALIVE_INTERVAL);
            session.connect(SESSION_TIMEOUT);
            ChannelShell channel = (ChannelShell) session.openChannel("shell");
            if (agentForwarding) {
                channel.setAgentForwarding(true);
            }
            channel.setPtyType("xterm");

            InputStream inputStream = channel.getInputStream();

            //new session output
            SessionOutput sessionOutput = new SessionOutput(sessionId,term);

            Runnable run = new SecureShellTask(sessionOutput, inputStream);
            Thread thread = new Thread(run);
            thread.start();

            OutputStream inputToChannel = channel.getOutputStream();
            PrintStream commander = new PrintStream(inputToChannel, true);

            channel.connect();

            schSession = new SchSession();
            schSession.setUserId(userId);
            schSession.setTerm(term);
            schSession.setSession(session);
            schSession.setChannel(channel);
            schSession.setCommander(commander);
            schSession.setInputToChannel(inputToChannel);
            schSession.setOutFromChannel(inputStream);
        } catch (Exception e) {
            logger.info(e.toString(), e);
            if (e.getMessage().toLowerCase().contains("userauth fail")) {
                retVal = Terminal.PUBLIC_KEY_FAIL;
            } else if (e.getMessage().toLowerCase().contains("auth fail") || e.getMessage().toLowerCase().contains("auth cancel")) {
                retVal = Terminal.AUTH_FAIL;
            } else if (e.getMessage().toLowerCase().contains("unknownhostexception")) {
                retVal = Terminal.HOST_FAIL;
            } else {
                retVal = Terminal.GENERIC_FAIL;
            }
        }

        term.setStatus(retVal);

        saveOrUpdate(term);

        //add session to map
        if (retVal.equals(Terminal.SUCCESS)) {
            //get the server maps for user
            UserSchSessions userSchSessions = userSessionMap.get(sessionId);
            //if no user session create a new one
            if (userSchSessions == null) {
                userSchSessions = new UserSchSessions();
            }
            Map<String, SchSession> schSessionMap = userSchSessions.getSchSessionMap();

            //add server information
            schSessionMap.put(instanceId, schSession);
            userSchSessions.setSchSessionMap(schSessionMap);
            //add back to map
            userSessionMap.put(sessionId, userSchSessions);

        }

        return term;
    }


    public Session createJschSession(Terminal term) throws JSchException {
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

    /**
     * removes session for user session
     *
     * @param sessionId session id
     */
    public static void removeUserSession(String sessionId) {
        UserSessionsOutput userSessionsOutput = userSessionsOutputMap.get(sessionId);
        if (userSessionsOutput != null) {
            userSessionsOutput.getSessionOutputMap().clear();
        }
        userSessionsOutputMap.remove(sessionId);
    }

    /**
     * removes session output for host system
     *
     * @param sessionId    session id
     * @param instanceId id of host system instance
     */
    public static void removeOutput(String sessionId, Long instanceId) {

        UserSessionsOutput userSessionsOutput = userSessionsOutputMap.get(sessionId);
        if (userSessionsOutput != null) {
            userSessionsOutput.getSessionOutputMap().remove(instanceId);
        }
    }

    /**
     * adds a new output
     *
     * @param sessionOutput session output object
     */
    public static void addOutput(SessionOutput sessionOutput) {

        UserSessionsOutput userSessionsOutput = userSessionsOutputMap.get(sessionOutput.getSessionId());
        if (userSessionsOutput == null) {
            userSessionsOutputMap.put(sessionOutput.getSessionId(), new UserSessionsOutput());
            userSessionsOutput = userSessionsOutputMap.get(sessionOutput.getSessionId());
        }
        userSessionsOutput.getSessionOutputMap().put(sessionOutput.getId(), sessionOutput);


    }


    /**
     * adds a new output
     *
     * @param sessionId    session id
     * @param instanceId id of host system instance
     * @param value        Array that is the source of characters
     * @param offset       The initial offset
     * @param count        The length
     */
    public static void addToOutput(String sessionId, Long instanceId, char value[], int offset, int count) {

        UserSessionsOutput userSessionsOutput = userSessionsOutputMap.get(sessionId);
        if (userSessionsOutput != null) {
            userSessionsOutput.getSessionOutputMap().get(instanceId).getOutput().append(value, offset, count);
        }
    }

    /**
     * returns list of output lines
     *
     * @param sessionId session id object
     * @param user user auth object
     * @return session output list
     */
    public static List<SessionOutput> getOutput(String sessionId, User user) {
        List<SessionOutput> outputList = new ArrayList<SessionOutput>();
        UserSessionsOutput userSessionsOutput = userSessionsOutputMap.get(sessionId);
        if (userSessionsOutput != null) {
            for (Long key : userSessionsOutput.getSessionOutputMap().keySet()) {
                try {
                    SessionOutput sessionOutput = userSessionsOutput.getSessionOutputMap().get(key);
                    if (sessionOutput!=null && CommonUtils.notEmpty(sessionOutput.getOutput())) {
                        outputList.add(sessionOutput);
                        userSessionsOutput.getSessionOutputMap().put(key, new SessionOutput(sessionId, sessionOutput));
                    }
                } catch (Exception ex) {
                    logger.error(ex.toString(), ex);
                }

            }
        }
        return outputList;
    }

    public static class SchSession {

        private Long userId;
        private Terminal term;
        private com.jcraft.jsch.Session session;
        private Channel channel;
        private PrintStream commander;
        private InputStream outFromChannel;
        private OutputStream inputToChannel;


        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Terminal getTerm() {
            return term;
        }

        public void setTerm(Terminal term) {
            this.term = term;
        }

        public com.jcraft.jsch.Session getSession() {
            return session;
        }

        public void setSession(com.jcraft.jsch.Session session) {
            this.session = session;
        }

        public Channel getChannel() {
            return channel;
        }

        public void setChannel(Channel channel) {
            this.channel = channel;
        }

        public PrintStream getCommander() {
            return commander;
        }

        public void setCommander(PrintStream commander) {
            this.commander = commander;
        }

        public InputStream getOutFromChannel() {
            return outFromChannel;
        }

        public void setOutFromChannel(InputStream outFromChannel) {
            this.outFromChannel = outFromChannel;
        }

        public OutputStream getInputToChannel() {
            return inputToChannel;
        }

        public void setInputToChannel(OutputStream inputToChannel) {
            this.inputToChannel = inputToChannel;
        }
    }


    public static class SecureShellTask implements Runnable {

        private InputStream outFromChannel;
        private SessionOutput sessionOutput;

        public SecureShellTask(SessionOutput sessionOutput, InputStream outFromChannel) {
            this.sessionOutput = sessionOutput;
            this.outFromChannel = outFromChannel;
        }

        public void run() {
            InputStreamReader isr = new InputStreamReader(outFromChannel);
            BufferedReader br = new BufferedReader(isr);
            try {
                addOutput(sessionOutput);
                char[] buff = new char[1024];
                int read;
                while((read = br.read(buff)) != -1) {
                    addToOutput(sessionOutput.getSessionId(), sessionOutput.getId(), buff,0,read);
                    Thread.sleep(50);
                }
                removeOutput(sessionOutput.getSessionId(), sessionOutput.getId());
            } catch (Exception ex) {
                logger.error(ex.toString(), ex);
            }
        }

    }

    public static class SentOutputTask implements Runnable {

        private javax.websocket.Session session;
        private String sessionId;
        private User user;

        public SentOutputTask(String sessionId, javax.websocket.Session session, User user) {
            this.sessionId = sessionId;
            this.session = session;
            this.user = user;
        }

        public void run() {
            while (session.isOpen()) {
                List<SessionOutput> outputList = getOutput(sessionId, user);
                try {
                    if (outputList != null && !outputList.isEmpty()) {
                        String json = JSON.toJSONString(outputList);
                        //send json to session
                        this.session.getBasicRemote().sendText(json);
                    }
                    Thread.sleep(25);
                } catch (Exception ex) {
                    logger.error(ex.toString(), ex);
                }
            }
        }
    }


    public static class SessionOutput extends Terminal {

        private String sessionId;
        private StringBuilder output = new StringBuilder();

        public SessionOutput(String sessionId, Terminal hostSystem) {
            this.sessionId=sessionId;
            this.setId(hostSystem.getId());
            this.setInstanceId(hostSystem.getInstanceId());
            this.setUser(hostSystem.getUser());
            this.setHost(hostSystem.getHost());
            this.setPort(hostSystem.getPort());

        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public StringBuilder getOutput() {
            return output;
        }

        public void setOutput(StringBuilder output) {
            this.output = output;
        }
    }


    public static class UserSchSessions {

        Map<String, SchSession> schSessionMap = new ConcurrentHashMap<String, SchSession>();

        public Map<String, SchSession> getSchSessionMap() {
            return schSessionMap;
        }

        public void setSchSessionMap(Map<String, SchSession> schSessionMap) {
            this.schSessionMap = schSessionMap;
        }
    }


    public static class UserSessionsOutput {

        //instance id, host output
        Map<Long, SessionOutput> sessionOutputMap = new ConcurrentHashMap<Long,SessionOutput>();

        public Map<Long, SessionOutput> getSessionOutputMap() {
            return sessionOutputMap;
        }

        public void setSessionOutputMap(Map<Long, SessionOutput> sessionOutputMap) {
            this.sessionOutputMap = sessionOutputMap;
        }

    }

}
