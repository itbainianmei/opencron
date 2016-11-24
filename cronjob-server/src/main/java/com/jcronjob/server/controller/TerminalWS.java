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

package com.jcronjob.server.controller;

import com.google.gson.Gson;
import com.jcronjob.common.utils.DigestUtils;
import com.jcronjob.server.domain.User;
import com.jcronjob.server.job.Globals;
import com.jcronjob.server.session.HttpSessionConfigurator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static com.jcronjob.server.service.TerminalService.*;


/**
 *
 * Created by benjobs on 2016/11/23.
 *
 */


@ServerEndpoint(value = "/terms.ws", configurator = HttpSessionConfigurator.class)
@SuppressWarnings("unchecked")
public class TerminalWS {

    private static Logger log = LoggerFactory.getLogger(TerminalWS.class);

    private HttpSession httpSession;
    private Session session;
    private Long sessionId = null;

    public static final String TIMEOUT = "timeout";

    private Map<Long, UserSchSessions> userSchSessionMap = new ConcurrentHashMap<Long, UserSchSessions>(0);

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) throws Exception {
        //set websocket timeout
        session.setMaxIdleTimeout(0);

        this.httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        this.sessionId = getSessionId(httpSession);
        this.session = session;
        this.userSchSessionMap = TerminalController.userSchSessionMap;
        User user = getUser(this.httpSession);
        Runnable run = new SentOutputTask(sessionId, session, user);
        Thread thread = new Thread(run);
        thread.start();
    }


    public static Long getSessionId(HttpSession session) throws Exception {
        Long sessionId = null;
        String sessionIdStr = DigestUtils.aesDecrypt(Globals.AES_KEY, (String) session.getAttribute(Globals.SSH_SESSION_ID));
        if (sessionIdStr != null && !sessionIdStr.trim().equals("")) {
            sessionId = Long.parseLong(sessionIdStr);
        }
        return sessionId;
    }

    public User getUser(HttpSession session) {
        return (User) session.getAttribute(Globals.LOGIN_USER);
    }

    public void setTimeout(HttpSession session) {
        //set session timeout
        SimpleDateFormat sdf = new SimpleDateFormat("MMddyyyyHHmmss");
        Calendar timeout = Calendar.getInstance();
        timeout.add(Calendar.MINUTE, 15);
        session.setAttribute(TIMEOUT, sdf.format(timeout.getTime()));
    }


    @OnMessage
    public void onMessage(String message) {
        if (session.isOpen() && StringUtils.isNotEmpty(message)) {

            Map jsonRoot = new Gson().fromJson(message, Map.class);

            String command = (String) jsonRoot.get("command");

            Integer keyCode = null;
            Double keyCodeDbl = (Double) jsonRoot.get("keyCode");
            if (keyCodeDbl != null) {
                keyCode = keyCodeDbl.intValue();
            }

            for (String idStr : (ArrayList<String>) jsonRoot.get("id")) {
                Long id = Long.parseLong(idStr);

                //get servletRequest.getSession() for user
                UserSchSessions userSchSessions = userSchSessionMap.get(sessionId);
                if (userSchSessions != null) {
                    SchSession schSession = userSchSessions.getSchSessionMap().get(id);
                    if (keyCode != null) {
                        if (keyMap.containsKey(keyCode)) {
                            try {
                                schSession.getCommander().write(keyMap.get(keyCode));
                            } catch (IOException ex) {
                                log.error(ex.toString(), ex);
                            }
                        }
                    } else {
                        schSession.getCommander().print(command);
                    }
                }
            }

            setTimeout(httpSession);
        }
    }

    @OnClose
    public void onClose() {

        if (userSchSessionMap != null) {
            UserSchSessions userSchSessions = userSchSessionMap.get(sessionId);
            if (userSchSessions != null) {
                Map<Long, SchSession> schSessionMap = userSchSessions.getSchSessionMap();

                for (Long sessionKey : schSessionMap.keySet()) {
                    SchSession schSession = schSessionMap.get(sessionKey);
                    schSession.getChannel().disconnect();
                    schSession.getSession().disconnect();
                    schSession.setChannel(null);
                    schSession.setSession(null);
                    schSession.setInputToChannel(null);
                    schSession.setCommander(null);
                    schSession.setOutFromChannel(null);
                    schSession = null;
                    //remove from map
                    schSessionMap.remove(sessionKey);
                }
                //clear and remove session map for user
                schSessionMap.clear();
                schSessionMap.remove(sessionId);
                removeUserSession(sessionId);
            }
        }

    }


    /**
     * Maps key press events to the ascii values
     */
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

}


