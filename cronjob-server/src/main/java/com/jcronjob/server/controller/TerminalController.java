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
import com.jcronjob.common.utils.WebUtils;
import com.jcronjob.server.domain.Terminal;
import com.jcronjob.server.job.Globals;
import com.jcronjob.server.domain.User;
import com.jcronjob.server.domain.Agent;
import com.jcronjob.server.service.AgentService;
import com.jcronjob.server.service.TerminalService;

import com.jcronjob.server.tag.PageBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static com.jcronjob.server.service.TerminalService.*;

/**
 * benjobs..
 */
@Controller
@RequestMapping("/terminal")
public class TerminalController {

    @Autowired
    private TerminalService termService;

    @Autowired
    private AgentService agentService;

    @RequestMapping("/ssh")
    public void ssh(HttpSession session,HttpServletResponse response, Agent agent) throws Exception {
        User user = (User) session.getAttribute(Globals.LOGIN_USER);

        String json = "{status:'%s',url:'%s'}";
        final Terminal terminal = termService.getTerm(user.getUserId(), agent.getIp());
        if (terminal == null) {
            WebUtils.writeJson(response, String.format(json,"null","null"));
            return;
        }

        String authStr = termService.auth(terminal);
        //登陆认证成功
        if (authStr.equalsIgnoreCase(Terminal.SUCCESS)) {
            agent = agentService.getByHost(agent.getIp());
            String token = CommonUtils.uuid();
            terminal.setAgent(agent);
            terminal.setUser(user);

            TerminalContext.put(token,terminal);
            session.setAttribute(Globals.SSH_SESSION_ID,token);
            WebUtils.writeJson(response, String.format(json,"success","/terminal/open?token="+token));
        }else {
            //重新输入密码进行认证...
            WebUtils.writeJson(response, String.format(json,authStr,"null"));
            return;
        }
    }

    @RequestMapping("/view")
    public String view(HttpSession session,Model model ) throws Exception {
        PageBean<Terminal> pageBean = termService.getTerminalByUser(Globals.getUserIdBySession(session));
        model.addAttribute("page",pageBean);
        return "/terminal/view";
    }

    @RequestMapping("/open")
    public String open(HttpServletRequest request,String token ) throws Exception {
        Terminal terminal = TerminalContext.get(token);
        if (terminal!=null) {
            request.setAttribute("name",terminal.getAgent().getName()+"("+terminal.getAgent().getIp()+")");
            return "/terminal/console";
        }
        return "/terminal/error";
    }

    @RequestMapping("/add")
    public void save(HttpSession session, HttpServletResponse response, Terminal term) throws Exception {
        String message = termService.auth(term);
        if ("success".equalsIgnoreCase(message)) {
            User user = (User)session.getAttribute(Globals.LOGIN_USER);
            term.setUserId(user.getUserId());
            termService.saveOrUpdate(term);
        }
        WebUtils.writeHtml(response,message);
    }


}