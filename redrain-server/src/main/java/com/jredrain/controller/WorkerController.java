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

package com.jredrain.controller;

import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import com.jredrain.tag.Page;
import com.jredrain.base.utils.JsonMapper;
import com.jredrain.base.utils.PageIOUtils;
import com.jredrain.domain.Worker;
import com.jredrain.service.WorkerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/worker")
public class WorkerController {

    @Autowired
    private WorkerService workerService;

    @RequestMapping("/view")
    public String queryDone(HttpServletRequest request, Page page) {
        workerService.getWorker(page);
        if (request.getParameter("refresh") != null) {
            return "/worker/refresh";
        }
        return "/worker/view";
    }

    @RequestMapping("/checkname")
    public void checkName(HttpServletResponse response, Long id, String name) {
        String result = workerService.checkName(id, name);
        PageIOUtils.writeHtml(response, result);
    }

    @RequestMapping("/addpage")
    public String addPage() {
        return "/worker/add";
    }

    @RequestMapping("/add")
    public String add(Worker worker) {
        if (!worker.getWarning()) {
            worker.setMobiles(null);
            worker.setEmailAddress(null);
        }

        //直联
        if (worker.getProxy()==0) {
            worker.setProxyWorker(null);
        }

        worker.setPassword(DigestUtils.md5Hex(worker.getPassword()));
        worker.setStatus(true);
        worker.setUpdateTime(new Date());
        workerService.addOrUpdate(worker);
        return "redirect:/worker/view";
    }

    @RequestMapping("/editpage")
    public void editPage(HttpServletResponse response, Long id) {
        Worker worker = workerService.getWorker(id);
        JsonMapper json = new JsonMapper();
        PageIOUtils.writeJson(response, json.toJson(worker));
    }

    @RequestMapping("/edit")
    public void edit(HttpServletResponse response, Worker worker) {
        Worker worker1 = workerService.getWorker(worker.getWorkerId());
        worker1.setName(worker.getName());
        worker1.setPort(worker.getPort());
        worker1.setWarning(worker.getWarning());
        if (worker.getWarning()) {
            worker1.setMobiles(worker.getMobiles());
            worker1.setEmailAddress(worker.getEmailAddress());
        }
        worker1.setUpdateTime(new Date());
        workerService.addOrUpdate(worker1);
        PageIOUtils.writeHtml(response, "success");
    }

    @RequestMapping("/pwdpage")
    public void pwdPage(HttpServletResponse response, Long id) {
        Worker worker = workerService.getWorker(id);
        JsonMapper json = new JsonMapper();
        PageIOUtils.writeJson(response, json.toJson(worker));
    }

    @RequestMapping("/editpwd")
    public void editPwd(HttpServletResponse response, Long id, String pwd0, String pwd1, String pwd2) {
        String result = workerService.editPwd(id, pwd0, pwd1, pwd2);
        PageIOUtils.writeHtml(response, result);
    }

    @RequestMapping("/detail")
    public String showDetail(Model model, Long id) {
        Worker worker = workerService.getWorker(id);
        model.addAttribute("worker", worker);
        return "/worker/detail";
    }
}
