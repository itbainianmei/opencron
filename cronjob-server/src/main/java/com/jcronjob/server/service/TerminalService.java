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

import ch.ethz.ssh2.*;
import com.jcraft.jsch.*;
import com.jcraft.jsch.Session;
import com.jcronjob.common.utils.*;
import com.jcronjob.server.domain.Terminal;
import com.jcronjob.server.dao.QueryDao;
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

    @Autowired
    private QueryDao queryDao;

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


    public Session createJschSession(Terminal term) throws JSchException {
        JSch jsch = new JSch();
        Session jschSession = jsch.getSession(term.getUserName(), term.getHost(), term.getPort());
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


    public static class TerminalClient {

        private WebSocketSession webSocketSession;
        private Connection connection;
        private ch.ethz.ssh2.Session sshSession;
        private Terminal terminal;
        private InputStream inputStream;
        private OutputStream outputStream;
        private BufferedWriter writer;
        private boolean closed = false;

        public TerminalClient(WebSocketSession webSocketSession,Terminal terminal){
            this.webSocketSession = webSocketSession;
            this.terminal = terminal;
        }

        public boolean connect() {
            try {
                connection = new Connection(terminal.getHost(), terminal.getPort());
                connection.connect();
                if (!connection.authenticateWithPassword(terminal.getUserName(),terminal.getPassword() )) {
                    return false;
                }
                sshSession = connection.openSession();
                sshSession.requestPTY("xterm", 90, 30, 0, 0, null);
                sshSession.startShell();
                inputStream = sshSession.getStdout();
                outputStream = sshSession.getStdin();
                writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
            } catch (Exception e) {
                e.printStackTrace();
                closed = true;
                return false;
            }
            return true;
        }

        public void write(String text) throws IOException {
            if (writer != null) {
                writer.write(text);
                writer.flush();
            }
        }

        public void sendMessage() {

            class MessageSender extends Thread {
                private final InputStream inputStream;

                public MessageSender(InputStream inputStream) {
                    super();
                    this.inputStream = inputStream;
                }

                @Override
                public void run() {
                    super.run();
                    byte[] buffer = new byte[ 1024*8 ];
                    StringBuilder builder = new StringBuilder();
                    try {
                        while (webSocketSession != null && webSocketSession.isOpen()) {
                            builder.setLength(0);
                            int bufferSize = inputStream.read(buffer);
                            if (bufferSize == -1) {
                                return;
                            }
                            for (int i = 0; i < bufferSize; i++) {
                                char chr = (char) (buffer[i] & 0xff);
                                builder.append(chr);
                            }

                            if (DigestUtils.getEncoding(builder.toString()).equals("ISO-8859-1")) {
                                webSocketSession.sendMessage(new TextMessage(new String(builder.toString().getBytes("ISO-8859-1"), "UTF-8")));
                            } else {
                                webSocketSession.sendMessage(new TextMessage(new String(builder.toString().getBytes("gb2312"), "UTF-8")));
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }
            new MessageSender(inputStream).start();
        }

        public void disconnect() throws IOException {
            if (connection != null) {
                connection.close();
                connection = null;
            }
            if (sshSession != null) {
                sshSession.close();
                sshSession = null;
            }
            closed = true;
        }

        public Terminal getTerminal() {
            return terminal;
        }

        public void setTerminal(Terminal terminal) {
            this.terminal = terminal;
        }

        public boolean isClosed() {
            return closed;
        }

        public WebSocketSession getWebSocketSession() {
            return webSocketSession;
        }
    }

    public static class TerminalContext implements Serializable {

        public static Map<String, Terminal> terminalContext = new ConcurrentHashMap<String, Terminal>(0);

        public static Terminal get(String key) {
            return terminalContext.get(key);
        }

        public static void put(String key, Terminal terminal) {
            terminalContext.put(key, terminal);
        }

        public static Terminal remove(String key) {
            return terminalContext.remove(key);
        }
    }


    public static class TerminalSession implements Serializable {

        public static Map<WebSocketSession, TerminalClient> terminalSession = new ConcurrentHashMap<WebSocketSession, TerminalClient>(0);

        public static TerminalClient get(WebSocketSession key) {
            return terminalSession.get(key);
        }

        public static void put(WebSocketSession key, TerminalClient terminalClient) {
            terminalSession.put(key, terminalClient);
        }

        public static TerminalClient remove(WebSocketSession key) {
            return terminalSession.remove(key);
        }
    }


}



