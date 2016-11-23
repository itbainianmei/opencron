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
 */

package com.jcronjob.server.model;

import com.google.gson.Gson;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.Session;
import com.jcronjob.server.domain.Term;
import com.jcronjob.server.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by benjobs on 2016/11/23.
 */
public class SShTermObject {

    private static Logger logger = LoggerFactory.getLogger(SShTermObject.class);

    public static class SchSession implements Serializable {

        private Long userId;
        private Term term;
        private Session session;
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

        public Term getTerm() {
            return term;
        }

        public void setTerm(Term term) {
            this.term = term;
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
                SessionOutputUtil.addOutput(sessionOutput);
                char[] buff = new char[1024];
                int read;
                while((read = br.read(buff)) != -1) {
                    SessionOutputUtil.addToOutput(sessionOutput.getSessionId(), sessionOutput.getId(), buff,0,read);
                    Thread.sleep(50);
                }
                SessionOutputUtil.removeOutput(sessionOutput.getSessionId(), sessionOutput.getId());
            } catch (Exception ex) {
                logger.error(ex.toString(), ex);
            }
        }

    }



    public static class SentOutputTask implements Runnable {

        private javax.websocket.Session session;
        private Long sessionId;
        private User user;

        public SentOutputTask(Long sessionId, javax.websocket.Session session, User user) {
            this.sessionId = sessionId;
            this.session = session;
            this.user = user;
        }

        public void run() {
            Gson gson = new Gson();
            while (session.isOpen()) {
                List<SessionOutput> outputList = SessionOutputUtil.getOutput(sessionId, user);
                try {
                    if (outputList != null && !outputList.isEmpty()) {
                        String json = gson.toJson(outputList);
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


    public static class SessionOutput extends Term {

        private Long sessionId;
        private StringBuilder output = new StringBuilder();

        public SessionOutput() {}

        public SessionOutput(Long sessionId, Term hostSystem) {
            this.sessionId=sessionId;
            this.setId(hostSystem.getId());
            this.setInstanceId(hostSystem.getInstanceId());
            this.setUser(hostSystem.getUser());
            this.setHost(hostSystem.getHost());
            this.setPort(hostSystem.getPort());

        }

        public Long getSessionId() {
            return sessionId;
        }

        public void setSessionId(Long sessionId) {
            this.sessionId = sessionId;
        }

        public StringBuilder getOutput() {
            return output;
        }

        public void setOutput(StringBuilder output) {
            this.output = output;
        }
    }


    public static class UserSchSessions implements Serializable{

        Map<Long, SchSession> schSessionMap = new ConcurrentHashMap<Long, SchSession>();

        public Map<Long, SchSession> getSchSessionMap() {
            return schSessionMap;
        }

        public void setSchSessionMap(Map<Long, SchSession> schSessionMap) {
            this.schSessionMap = schSessionMap;
        }
    }


    public static class UserSessionsOutput implements Serializable {

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
