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
import com.jcronjob.common.utils.*;
import com.jcronjob.server.domain.Terminal;
import com.jcronjob.server.dao.QueryDao;
import com.jcronjob.server.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.jcronjob.common.utils.CommonUtils.notEmpty;


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

    @Autowired
    private QueryDao queryDao;

    public Terminal getTerm(Long userId, String host) throws Exception {
        return queryDao.sqlUniqueQuery(Terminal.class,"SELECT * FROM T_TERM WHERE userId=? AND host=? And status=?",userId,host, Terminal.SUCCESS);
    }

    public boolean saveOrUpdate(Terminal term) {
        Terminal dbTerm = queryDao.sqlUniqueQuery(Terminal.class,"SELECT * FROM T_TERM WHERE userId=? AND host=?",term.getUserId(),term.getHost());
        if (dbTerm!=null) {
            term.setId(dbTerm.getId());
        }
        try {
            term.setLogintime(new Date());
            queryDao.save(term);
            return true;
        }catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String auth(Terminal terminal) {
        Connection connection = null;
        try {
            connection = new Connection(terminal.getHost(), terminal.getPort());
            connection.connect();
            if (!connection.authenticateWithPassword(terminal.getUserName(),terminal.getPassword() )) {
                return "authfail";
            }
            return "success";
        } catch (Exception e) {
            if(e.getLocalizedMessage().replaceAll("\\s+","").contentEquals("Operationtimedout")){
                return "timeout";
            }
            return "error";
        }finally {
            if (connection!=null) {
                connection.close();
            }
        }
    }


    public static class TerminalClient {

        private WebSocketSession webSocketSession;
        private Connection connection;
        private Session session;
        private Terminal terminal;
        private InputStream inputStream;
        private OutputStream outputStream;
        private BufferedWriter writer;
        private boolean closed = false;

        public TerminalClient(WebSocketSession webSocketSession,Terminal terminal){
            this.webSocketSession = webSocketSession;
            this.terminal = terminal;
        }

        public void openTerminal(int cols,int rows,int width,int height) throws Exception {
            connection = new Connection(terminal.getHost(), terminal.getPort());
            connection.connect();
            connection.authenticateWithPassword(terminal.getUserName(),terminal.getPassword());

            session = connection.openSession();
            session.requestPTY("xterm",cols, rows, width, height,null);
            session.startShell();
            inputStream = session.getStdout();
            outputStream = session.getStdin();
            writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));

            new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] buffer = new byte[ 1024 ];
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
            }).start();

        }

        /**
         * 向ssh终端输入内容
         * @param text
         * @throws IOException
         */
        public void write(String text) throws IOException {
            if (writer != null) {
                writer.write(text);
                writer.flush();
            }
        }

        public void disconnect() throws IOException {
            if (session != null) {
                session.close();
                session = null;
            }
            if (connection!=null) {
                connection.close();
                connection = null;
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

        public static boolean isOpened(Terminal terminal) {
            for(Map.Entry<WebSocketSession,TerminalClient> entry:terminalSession.entrySet()){
                if (entry.getValue().getTerminal().equals(terminal)) {
                    return true;
                }
            }
            return false;
        }

        public static WebSocketSession findSession(Terminal terminal) {
            for(Map.Entry<WebSocketSession,TerminalClient> entry:terminalSession.entrySet()){
                TerminalClient client = entry.getValue();
                if(client.getTerminal().equals(terminal)){
                    return entry.getKey();
                }
            }
            return null;
        }

        public static void exit(User user) throws IOException {
            if (notEmpty(terminalSession)) {
                for(Map.Entry<WebSocketSession, TerminalClient> entry: terminalSession.entrySet()){
                    TerminalClient terminalClient = entry.getValue();
                    if (terminalClient.getTerminal().getUser().equals(user)) {
                        terminalClient.disconnect();
                        terminalClient.getWebSocketSession().sendMessage(new TextMessage("Sorry! logout, so Cronjob Terminal changed to closed. "));
                        terminalClient.getWebSocketSession().close();
                    }
                }
            }
        }

    }


}



