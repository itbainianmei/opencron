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

package org.opencron.server.controller;

import com.alibaba.fastjson.JSON;
import org.opencron.common.utils.CommonUtils;
import org.opencron.common.utils.WebUtils;
import org.opencron.server.domain.Terminal;
import org.opencron.server.job.Globals;
import org.opencron.server.domain.User;
import org.opencron.server.job.OpencronContext;
import org.opencron.server.service.TerminalService;

import org.opencron.server.tag.PageBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.List;

import static org.opencron.server.service.TerminalService.*;

/**
 * benjobs..
 */
@Controller
@RequestMapping("/terminal")
public class TerminalController {

    @Autowired
    private TerminalService termService;

    @RequestMapping("/ssh")
    public void ssh(HttpSession session,HttpServletResponse response, Terminal terminal) throws Exception {
        User user = (User) session.getAttribute(Globals.LOGIN_USER);

        String json = "{status:'%s',url:'%s'}";

        terminal = termService.getById(terminal.getId());
        String authStr = termService.auth(terminal);
        //登陆认证成功
        if (authStr.equalsIgnoreCase(Terminal.SUCCESS)) {
            String token = CommonUtils.uuid();
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

    @RequestMapping("/ssh2")
    public String ssh2(HttpSession session,Terminal terminal) throws Exception {
        User user = (User) session.getAttribute(Globals.LOGIN_USER);

        terminal = termService.getById(terminal.getId());
        String authStr = termService.auth(terminal);
        //登陆认证成功
        if (authStr.equalsIgnoreCase(Terminal.SUCCESS)) {
            String token = CommonUtils.uuid();
            terminal.setUser(user);
            TerminalContext.put(token,terminal);
            session.setAttribute(Globals.SSH_SESSION_ID,token);
            return "redirect:/terminal/open?token="+token;
        }else {
            //重新输入密码进行认证...
            return "redirect:/terminal/open?id="+terminal.getId();
        }

    }

    @RequestMapping("/detail")
    public void detail(HttpServletResponse response,Terminal terminal) throws Exception {
        terminal = termService.getById(terminal.getId());
        WebUtils.writeJson(response, JSON.toJSONString(terminal));
    }

    @RequestMapping("/exists")
    public void exists(HttpServletResponse response,HttpSession session,  Terminal terminal) throws Exception {
        User user = Globals.getUserBySession(session);
        boolean exists = termService.exists( user.getUserId(),terminal.getHost());
        WebUtils.writeHtml(response,exists?"true":"false");
    }

    @RequestMapping("/view")
    public String view(HttpSession session,PageBean pageBean,Model model ) throws Exception {
        pageBean = termService.getPageBeanByUser(pageBean,Globals.getUserIdBySession(session));
        model.addAttribute("pageBean",pageBean);
        return "/terminal/view";
    }

    @RequestMapping("/open")
    public String open(HttpServletRequest request,String token,Long id) throws Exception {
        //登陆失败
        if (token==null && id!=null) {
            Terminal terminal = termService.getById(id);
            request.setAttribute("terminal",terminal);
            return "/terminal/error";
        }
        Terminal terminal = TerminalContext.get(token);
        if (terminal!=null) {
            request.setAttribute("name",terminal.getName()+"("+terminal.getHost()+")");
            request.setAttribute("token",token);
            List<Terminal> terminas = termService.getListByUser(terminal.getUser());
            request.setAttribute("terms",terminas);
            return "/terminal/console";
        }
        return "/terminal/error";
    }

    @RequestMapping("/reopen")
    public String reopen(HttpSession session,String token ) throws Exception {
        Terminal terminal = (Terminal) OpencronContext.get(token);
        if (terminal!=null) {
            token = CommonUtils.uuid();
            TerminalContext.put(token,terminal);
            session.setAttribute(Globals.SSH_SESSION_ID,token);
            return "redirect:/terminal/open?token="+token;
        }
        return "/terminal/error";
    }

    @RequestMapping("/resize")
    public void resize(String token,Integer cols,Integer rows) throws Exception {
        TerminalClient terminalClient = TerminalSession.get(token);
        if (terminalClient!=null) {
            terminalClient.resize(cols,rows);
        }
    }

    @RequestMapping("/save")
    public void save(HttpSession session, HttpServletResponse response, Terminal term) throws Exception {
        String message = termService.auth(term);
        if ("success".equalsIgnoreCase(message)) {
            User user = (User)session.getAttribute(Globals.LOGIN_USER);
            term.setUserId(user.getUserId());
            termService.saveOrUpdate(term);
        }
        WebUtils.writeHtml(response,message);
    }


    @RequestMapping("/del")
    public void del(HttpSession session, HttpServletResponse response, Terminal term) throws Exception {
        String message = termService.delete(session,term.getId());
        WebUtils.writeHtml(response,message);
    }

}