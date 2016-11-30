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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

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


    public Terminal openTerminal(Terminal terminal, String sessionId) {

        JSch jsch = new JSch();

        SchSession schSession = null;

        String retVal = "SUCCESS";
        try {
            //create session
            Session session = jsch.getSession(terminal.getUser(), terminal.getHost(), terminal.getPort());
            session.setPassword(terminal.getPassword());
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
            Message message = new Message(sessionId);

            Runnable run = new MessageReceiver(message, inputStream);
            Thread thread = new Thread(run);
            thread.start();

            OutputStream inputToChannel = channel.getOutputStream();
            PrintStream commander = new PrintStream(inputToChannel, true);

            channel.connect();

            schSession = new SchSession();
            schSession.setTerminal(terminal);
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

        terminal.setStatus(retVal);

        saveOrUpdate(terminal);

        //add session to map
        if (retVal.equals(Terminal.SUCCESS)) {
            TerminalSession.put(sessionId, schSession);
        }

        return terminal;
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
        TerminalMessage.remove(sessionId);
    }

    /**
     * removes session output for host system
     *
     * @param sessionId    session id
     */
    public static void removeOutput(String sessionId) {

        Message message = TerminalMessage.get(sessionId);
        if (message != null) {
            TerminalMessage.remove(sessionId);
        }
    }

    /**
     * adds a new output
     *
     * @param message session output object
     */
    public static void addOutput(Message message) {
        TerminalMessage.remove(message.getSessionId());
        TerminalMessage.put(message.getSessionId(), message);
    }


    /**
     * adds a new output
     *
     * @param sessionId    session id
     * @param value        Array that is the source of characters
     * @param offset       The initial offset
     * @param count        The length
     */
    public static void addToOutput(String sessionId,char value[], int offset, int count) {

        Message message = TerminalMessage.get(sessionId);
        if (message != null) {
            message.getOutput().append(value, offset, count);
        }
    }

    /**
     * returns list of output lines
     *
     * @param sessionId session id object
     * @return session output list
     */
    public static List<Message> getOutput(String sessionId) {
        List<Message> outputList = new ArrayList<Message>();
        Message message = TerminalMessage.get(sessionId);
        if (message !=null) {
            TerminalMessage.put(sessionId,new Message(sessionId));
        }
        outputList.add(message);
        return outputList;
    }

    public static class SchSession {

        private Terminal terminal;
        private Session session;
        private Channel channel;
        private PrintStream commander;
        private InputStream outFromChannel;
        private OutputStream inputToChannel;

        public Terminal getTerminal() {
            return terminal;
        }

        public void setTerminal(Terminal terminal) {
            this.terminal = terminal;
        }

        public Session getSession() {
            return session;
        }

        public void setSession(Session session) {
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


    public static class MessageReceiver implements Runnable {

        private InputStream outFromChannel;
        private Message message;

        public MessageReceiver(Message message, InputStream outFromChannel) {
            this.message = message;
            this.outFromChannel = outFromChannel;
        }

        public void run() {
            InputStreamReader isr = new InputStreamReader(outFromChannel);
            BufferedReader br = new BufferedReader(isr);
            try {
                addOutput(message);
                char[] buff = new char[1024];
                int read;
                while((read = br.read(buff)) != -1) {
                    addToOutput(message.getSessionId(), buff,0,read);
                    Thread.sleep(50);
                }
                removeOutput(message.getSessionId());
            } catch (Exception ex) {
                logger.error(ex.toString(), ex);
            }
        }

    }

    public static class MessageSender implements Runnable {

        private WebSocketSession session;
        private String sessionId;

        public MessageSender(String sessionId, WebSocketSession session) {
            this.sessionId = sessionId;
            this.session = session;
        }

        public void run() {
            while (session!=null && session.isOpen()) {
                List<Message> message = getOutput(sessionId);
                try {
                    if (CommonUtils.notEmpty(message)) {
                        String json =  JSON.toJSONString(message);
                        //必须返回一个集合,不然写给前端解析失败
                        this.session.sendMessage(new TextMessage(json));
                    }
                    Thread.sleep(25);
                } catch (Exception ex) {
                    logger.error(ex.toString(), ex);
                }
            }
        }
    }


    public static class Message {

        private String sessionId;
        private StringBuilder output = new StringBuilder();

        public Message(String sessionId) {
            this.sessionId=sessionId;

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

    public static class TerminalSession {

        private static Map<String, SchSession> session = new ConcurrentHashMap<String, SchSession>(0);

        public static SchSession get(String key){
            return session.get(key);
        }

        public static void put(String key,SchSession schSession){
            session.put(key,schSession);
        }

        public static SchSession remove(String key) {
            return session.remove(key);
        }
    }

    public static class TerminalMessage {

        private static Map<String, Message> message = new ConcurrentHashMap<String, Message>(0);

        public static Message get(String key){
            return message.get(key);
        }

        public static void put(String key,Message schSession){
            message.put(key,schSession);
        }

        public static Message remove(String key) {
           return message.remove(key);
        }
    }


    public static class TerminalKeyMap {
        private static Map<Integer, byte[]> keyMap = new HashMap<Integer, byte[]>();
        static {
            //ESC
            keyMap.put(27, new byte[]{(byte) 0x1b});
            //ENTER
            keyMap.put(13, new byte[]{(byte) 0x0d});
            //LEFT
            keyMap.put(37, new byte[]{(byte) 0x1b, (byte) 0x4f, (byte) 0x44});
            //UP
            keyMap.put(38, new byte[]{(byte) 0x1b, (byte) 0x4f, (byte) 0x41});
            //RIGHT
            keyMap.put(39, new byte[]{(byte) 0x1b, (byte) 0x4f, (byte) 0x43});
            //DOWN
            keyMap.put(40, new byte[]{(byte) 0x1b, (byte) 0x4f, (byte) 0x42});
            //BS
            keyMap.put(8, new byte[]{(byte) 0x7f});
            //TAB
            keyMap.put(9, new byte[]{(byte) 0x09});
            //CTR
            keyMap.put(17, new byte[]{});
            //DEL
            keyMap.put(46, "\033[3~".getBytes());
            //CTR-A
            keyMap.put(65, new byte[]{(byte) 0x01});
            //CTR-B
            keyMap.put(66, new byte[]{(byte) 0x02});
            //CTR-C
            keyMap.put(67, new byte[]{(byte) 0x03});
            //CTR-D
            keyMap.put(68, new byte[]{(byte) 0x04});
            //CTR-E
            keyMap.put(69, new byte[]{(byte) 0x05});
            //CTR-F
            keyMap.put(70, new byte[]{(byte) 0x06});
            //CTR-G
            keyMap.put(71, new byte[]{(byte) 0x07});
            //CTR-H
            keyMap.put(72, new byte[]{(byte) 0x08});
            //CTR-I
            keyMap.put(73, new byte[]{(byte) 0x09});
            //CTR-J
            keyMap.put(74, new byte[]{(byte) 0x0A});
            //CTR-K
            keyMap.put(75, new byte[]{(byte) 0x0B});
            //CTR-L
            keyMap.put(76, new byte[]{(byte) 0x0C});
            //CTR-M
            keyMap.put(77, new byte[]{(byte) 0x0D});
            //CTR-N
            keyMap.put(78, new byte[]{(byte) 0x0E});
            //CTR-O
            keyMap.put(79, new byte[]{(byte) 0x0F});
            //CTR-P
            keyMap.put(80, new byte[]{(byte) 0x10});
            //CTR-Q
            keyMap.put(81, new byte[]{(byte) 0x11});
            //CTR-R
            keyMap.put(82, new byte[]{(byte) 0x12});
            //CTR-S
            keyMap.put(83, new byte[]{(byte) 0x13});
            //CTR-T
            keyMap.put(84, new byte[]{(byte) 0x14});
            //CTR-U
            keyMap.put(85, new byte[]{(byte) 0x15});
            //CTR-V
            keyMap.put(86, new byte[]{(byte) 0x16});
            //CTR-W
            keyMap.put(87, new byte[]{(byte) 0x17});
            //CTR-X
            keyMap.put(88, new byte[]{(byte) 0x18});
            //CTR-Y
            keyMap.put(89, new byte[]{(byte) 0x19});
            //CTR-Z
            keyMap.put(90, new byte[]{(byte) 0x1A});
            //CTR-[
            keyMap.put(219, new byte[]{(byte) 0x1B});
            //CTR-]
            keyMap.put(221, new byte[]{(byte) 0x1D});
            //INSERT
            keyMap.put(45, "\033[2~".getBytes());
            //PG UP
            keyMap.put(33, "\033[5~".getBytes());
            //PG DOWN
            keyMap.put(34, "\033[6~".getBytes());
            //END
            keyMap.put(35, "\033[4~".getBytes());
            //HOME
            keyMap.put(36, "\033[1~".getBytes());
        }

        public static byte[] get(Integer key){
            return keyMap.get(key);
        }

        public static boolean containsKey(Integer keyCode) {
            return keyMap.containsKey(keyCode);
        }
    }
}
