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


    public static class TerminalClient {

        private Connection connection;
        private ch.ethz.ssh2.Session session;
        private Terminal terminal;
        private InputStream inputStream;
        private OutputStream outputStream;
        private BufferedWriter writer;

        public TerminalClient(Terminal terminal){
            this.terminal = terminal;
        }

        public boolean connect() {
            try {
                connection = new Connection(terminal.getHost(), terminal.getPort());
                connection.connect();
                if (!connection.authenticateWithPassword(terminal.getUser(),terminal.getPassword() )) {
                    return false;
                }
                session = connection.openSession();
                session.requestPTY("xterm", 90, 30, 0, 0, null);
                session.startShell();
                inputStream = session.getStdout();
                outputStream = session.getStdin();
                writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
            } catch (Exception e) {
                e.printStackTrace();
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

        public void sendMessage(WebSocketSession session) {
            new MessageSender(session, inputStream).start();
        }

        public void disconnect() {
            if (connection != null) {
                connection.close();
                connection = null;
            }
            if (session != null) {
                session.close();
                session = null;
            }
        }


        class MessageSender extends Thread {

            private final WebSocketSession session;
            private final InputStream out;

            public MessageSender(WebSocketSession session, InputStream out) {
                super();
                this.session = session;
                this.out = out;
            }

            @Override
            public void run() {
                super.run();
                byte[] buffer = new byte[8192];
                StringBuilder builder = new StringBuilder();
                try {
                    while (session != null && session.isOpen()) {
                        builder.setLength(0);
                        int len = out.read(buffer);
                        if (len == -1)
                            return;
                        for (int i = 0; i < len; i++) {
                            char c = (char) (buffer[i] & 0xff);
                            builder.append(c);
                        }
                        if (DigestUtils.getEncoding(builder.toString()).equals("ISO-8859-1"))
                            session.sendMessage(new TextMessage(new String(builder.toString().getBytes("ISO-8859-1"), "UTF-8")));
                        else
                            session.sendMessage(new TextMessage(new String(builder.toString().getBytes("gb2312"), "UTF-8")));
                    }
                } catch (Exception e) {
                }
            }


        }

    }

    public static class TerminalSession implements Serializable {

        private static Map<String, Terminal> session = new ConcurrentHashMap<String, Terminal>(0);

        public static Terminal get(String key) {
            return session.get(key);
        }

        public static void put(String key, Terminal terminal) {
            session.put(key, terminal);
        }

        public static Terminal remove(String key) {
            return session.remove(key);
        }
    }



}



