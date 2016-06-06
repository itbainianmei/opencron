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

package org.jcronjob.controller;

import com.alibaba.fastjson.JSON;
import com.jcraft.jsch.*;
import org.jcronjob.base.utils.DigestUtils;
import org.jcronjob.base.utils.PageIOUtils;
import org.jcronjob.domain.Term;
import org.jcronjob.domain.Worker;
import org.jcronjob.service.ConfigService;
import org.jcronjob.service.TermService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * benjobs..
 */
@Controller
@RequestMapping("term")
public class TermController {

    @Autowired
    private TermService termService;

    @Autowired
    private ConfigService configService;

    @RequestMapping("/ssh")
    public void ssh(HttpServletRequest request, HttpSession session, HttpServletResponse response, final Worker worker) throws Exception {

        Long userId = (Long) session.getAttribute("userId");
        final Term term = termService.getTerm(userId, worker.getIp());

        if (term == null) {
            PageIOUtils.writeHtml(response, "null");
            return;
        }
        String termUrl = termService.getTermUrl(request,worker);
        String json = JSON.toJSONString(term);
        String data = DigestUtils.aesEncrypt(configService.getAeskey(),json);
        PageIOUtils.writeHtml(response, termUrl+"?"+data);
    }

    @RequestMapping("/save")
    public void save(HttpSession session, HttpServletResponse response, Term term) throws Exception {
        Session connect = termService.createJschSession(term);
        try {
            connect.connect();
            Long userId = (Long) session.getAttribute("userId");
            term.setUserId(userId);
            term.setStatus(1);
            termService.saveOrUpdate(term);
            PageIOUtils.writeHtml(response,"success");
        }catch (JSchException e) {
            PageIOUtils.writeHtml(response,termService.termFailCause(e));
        }
    }


}