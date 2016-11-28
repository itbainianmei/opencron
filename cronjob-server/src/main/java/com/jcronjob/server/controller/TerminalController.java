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

import com.jcronjob.common.utils.CommonUtils;
import com.jcronjob.common.utils.DigestUtils;
import com.jcronjob.common.utils.WebUtils;
import com.jcronjob.server.domain.Terminal;
import com.jcronjob.server.job.Globals;
import com.jcronjob.server.domain.User;
import com.jcronjob.server.domain.Agent;
import com.jcronjob.server.service.AgentService;
import com.jcronjob.server.service.TerminalService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.jcronjob.server.service.TerminalService.*;

/**
 * benjobs..
 */
@Controller
public class TerminalController {

    @Autowired
    private TerminalService termService;

    @Autowired
    private AgentService agentService;

    public static Map<String, UserSchSessions> userSchSessionMap = new ConcurrentHashMap<String, UserSchSessions>();

    @RequestMapping("/ssh")
    public void ssh(HttpSession session,HttpServletResponse response, final Agent agent) throws Exception {
        User user = (User) session.getAttribute(Globals.LOGIN_USER);

        String json = "{status:'%s',url:'%s'}";
        final Terminal term = termService.getTerm(user.getUserId(), agent.getIp());
        if (term == null) {
            WebUtils.writeJson(response, String.format(json,"null","null"));
            return;
        }

        String authStr = termService.auth(term);
        //登陆认证成功
        if (authStr.equalsIgnoreCase(Terminal.SUCCESS)) {
            String uuid = CommonUtils.uuid();

            session.setAttribute(Globals.SSH_SESSION_ID, uuid);
            termService.openTerminal(term,user.getUserId(), uuid,userSchSessionMap);

            WebUtils.writeJson(response, String.format(json,"success","/term?id="+term.getInstanceId()));
        }else {
            //重新输入密码进行认证...
            WebUtils.writeJson(response, String.format(json,authStr,"null"));
            return;
        }
    }

    @RequestMapping("/term")
    public String open(HttpServletRequest request,HttpSession session,String id ) throws Exception {
        String sessionId = (String) session.getAttribute(Globals.SSH_SESSION_ID);
        if (sessionId != null && !sessionId.trim().equals("")) {
            UserSchSessions userSchSessions = userSchSessionMap.get(sessionId);
            SchSession schSession = userSchSessions.getSchSessionMap().get(id);
            Agent agent =agentService.getByHost(schSession.getTerm().getHost());
            request.setAttribute("hostName",agent.getName());
            request.setAttribute("ip",agent.getIp());

        }
        request.setAttribute("id",id);
        return "/term/console";
    }

    @RequestMapping("/addterm")
    public void save(HttpSession session, HttpServletResponse response, Terminal term) throws Exception {
        String message = termService.auth(term);
        if ("success".equals(message)) {
            User user = (User)session.getAttribute(Globals.LOGIN_USER);
            term.setUserId(user.getUserId());
            termService.saveOrUpdate(term);
        }
        WebUtils.writeHtml(response,message);
    }


}