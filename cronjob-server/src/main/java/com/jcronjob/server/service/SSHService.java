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

package com.jcronjob.server.service;

import com.jcraft.jsch.Session;
import com.jcronjob.server.dao.QueryDao;
import com.jcronjob.server.domain.Term;
import com.jcronjob.server.domain.TermSession;
import com.jcraft.jsch.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import  static com.jcronjob.server.service.TerminalObject.*;

import java.io.*;
import java.util.Date;
import java.util.Map;

@Service
public class SSHService {

    private static Logger log = LoggerFactory.getLogger(SSHService.class);

    public static final int SERVER_ALIVE_INTERVAL = 60 * 1000;

    public static final int SESSION_TIMEOUT = 60000;

    public static final boolean agentForwarding = false;

    @Autowired
    private TermService termService;

    @Autowired
    private QueryDao queryDao;

    @Autowired
    private StatusService statusService;

    public Term openSSHTerm(Term term, Long userId, Long sessionId, Map<Long, UserSchSessions> userSessionMap) {

        Long instanceId = getNextInstanceId(sessionId, userSessionMap);
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
            log.info(e.toString(), e);
            if (e.getMessage().toLowerCase().contains("userauth fail")) {
                retVal = Term.PUBLIC_KEY_FAIL;
            } else if (e.getMessage().toLowerCase().contains("auth fail") || e.getMessage().toLowerCase().contains("auth cancel")) {
                retVal = Term.AUTH_FAIL;
            } else if (e.getMessage().toLowerCase().contains("unknownhostexception")) {
                retVal = Term.HOST_FAIL;
            } else {
                retVal = Term.GENERIC_FAIL;
            }
        }

        term.setStatus(retVal);

        termService.saveOrUpdate(term);

        //add session to map
        if (retVal.equals(Term.SUCCESS)) {
            //get the server maps for user
            UserSchSessions userSchSessions = userSessionMap.get(sessionId);
            //if no user session create a new one
            if (userSchSessions == null) {
                userSchSessions = new UserSchSessions();
            }
            Map<Long, SchSession> schSessionMap = userSchSessions.getSchSessionMap();

            //add server information
            schSessionMap.put(instanceId, schSession);
            userSchSessions.setSchSessionMap(schSessionMap);
            //add back to map
            userSessionMap.put(sessionId, userSchSessions);

        }

        statusService.update(retVal,term,userId);

        return term;
    }

    private Long getNextInstanceId(Long sessionId, Map<Long, UserSchSessions> userSessionMap) {
        Long instanceId = 1L;
        if (userSessionMap.get(sessionId) != null) {
            for (Long id : userSessionMap.get(sessionId).getSchSessionMap().keySet()) {
                if (!id.equals(instanceId) && userSessionMap.get(sessionId).getSchSessionMap().get(instanceId) == null) {
                    return instanceId;
                }
                instanceId = instanceId + 1;
            }
        }
        return instanceId;
    }


    public static String getFingerprint(String publicKey) {
        String fingerprint = null;
        if (StringUtils.isNotEmpty(publicKey)) {
            try {
                KeyPair keyPair = KeyPair.load(new JSch(), null, publicKey.getBytes());
                if (keyPair != null) {
                    fingerprint = keyPair.getFingerPrint();
                }
            } catch (JSchException ex) {
                log.error(ex.toString(), ex);
            }

        }
        return fingerprint;

    }


    public TermSession createSession(Long userId) {
        TermSession session = new TermSession();
        session.setUserId(userId);
        session.setSessionTime(new Date());
        return (TermSession) queryDao.save(session);
    }

    public String getKeyType(String publicKey) {
        String keyType = null;
        if (StringUtils.isNotEmpty(publicKey)) {
            try {
                KeyPair keyPair = KeyPair.load(new JSch(), null, publicKey.getBytes());
                if (keyPair != null) {
                    int type = keyPair.getKeyType();
                    if (KeyPair.DSA == type) {
                        keyType = "DSA";
                    } else if (KeyPair.RSA == type) {
                        keyType = "RSA";
                    } else if (KeyPair.ECDSA == type) {
                        keyType = "ECDSA";
                    } else if (KeyPair.UNKNOWN == type) {
                        keyType = "UNKNOWN";
                    } else if (KeyPair.ERROR == type) {
                        keyType = "ERROR";
                    }
                }

            } catch (JSchException ex) {
                log.error(ex.toString(), ex);
            }
        }
        return keyType;

    }

}