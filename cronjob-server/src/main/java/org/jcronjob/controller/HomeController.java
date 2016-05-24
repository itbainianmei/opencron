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

import net.sf.json.JSONObject;
import org.jcronjob.base.job.CronJob;
import org.jcronjob.base.job.Response;
import org.jcronjob.base.utils.DateUtils;
import org.jcronjob.base.utils.JsonMapper;
import org.jcronjob.base.utils.PageIOUtils;
import org.jcronjob.domain.Job;
import org.jcronjob.domain.Worker;
import org.jcronjob.service.*;
import org.jcronjob.tag.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.jcronjob.base.utils.CommonUtils.isEmpty;
import static org.jcronjob.base.utils.CommonUtils.notEmpty;

/**
 * Created by ChenHui on 2016/2/17.
 */
@Controller
public class HomeController {

    @Autowired
    private HomeService homeService;

    @Autowired
    private RecordService recordService;

    @Autowired
    private WorkerService workerService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private ExecuteService executeService;

    @Autowired
    private MonitorService monitorService;

    @Autowired
    private JobService jobService;

    @RequestMapping("/home")
    public String index(Model model) {

        /**
         * agent...
         */
        List<Worker> success = workerService.getWorkerByStatus(1);
        List<Worker> failed = workerService.getWorkerByStatus(0);
        model.addAttribute("success",success.size());
        model.addAttribute("failed",failed.size());

        success.addAll(failed);
        model.addAttribute("workers",success);

        /**
         * job
         */
        List<Job> singleton = jobService.getJobsByCategory(CronJob.JobCategory.SINGLETON);
        List<Job> flow = jobService.getJobsByCategory(CronJob.JobCategory.FLOW);

        model.addAttribute("singleton",singleton.size());
        model.addAttribute("flow",flow.size());
        singleton.addAll(flow);
        model.addAttribute("job",singleton.size());

        /**
         * 成功作业,自动执行
         */
        Long successAutoRecord = recordService.getRecords(1,CronJob.ExecType.AUTO);
        Long successOperRecord = recordService.getRecords(1,CronJob.ExecType.OPERATOR);

        model.addAttribute("successAutoRecord",successAutoRecord);
        model.addAttribute("successOperRecord",successOperRecord);
        model.addAttribute("successRecord",successAutoRecord+successOperRecord);

        /**
         * 失败作业
         */
        Long failedAutoRecord = recordService.getRecords(0,CronJob.ExecType.AUTO);
        Long failedOperRecord = recordService.getRecords(0,CronJob.ExecType.OPERATOR);
        model.addAttribute("failedAutoRecord",failedAutoRecord);
        model.addAttribute("failedOperRecord",failedOperRecord);
        model.addAttribute("failedRecord",failedAutoRecord+failedOperRecord);


        model.addAttribute("startTime", DateUtils.getCurrDayPrevDay(7));
        model.addAttribute("endTime", DateUtils.formatSimpleDate(new Date()));

        return "/home/index";
    }

    @RequestMapping("/darwchart")
    public void refreshChart(HttpServletResponse response) {
        JsonMapper jsonMapper = new JsonMapper();
        JSONObject json = new JSONObject();
        //执行类型占比数据
        json.put("execType", jsonMapper.toJson(recordService.getExecTypePieData()));
        //成功失败占比数据
        json.put("status", jsonMapper.toJson(recordService.getStatusDonutData()));

        PageIOUtils.writeJson(response, json.toString());
    }

    @RequestMapping("/diffchart")
    public void diffChart(HttpServletResponse response,String startTime, String endTime) {
        if (isEmpty(startTime)) {
            startTime = DateUtils.getCurrDayPrevDay(7);
        }
        if (isEmpty(endTime)) {
            endTime = DateUtils.formatSimpleDate(new Date());
        }
          //成功失败折线图数据
        JsonMapper jsonMapper = new JsonMapper();
        List<ChartVo> voList = recordService.getDiffData(startTime, endTime);
        if (CommonUtils.isEmpty(voList)) {
            PageIOUtils.writeHtml(response,"null");
        }else {
            PageIOUtils.writeJson(response, jsonMapper.toJson(voList));
        }
    }
    
    

    @RequestMapping("/url")
    public void port(HttpServletResponse response, Long workerId) throws Exception {
        Worker worker = workerService.getWorker(workerId);
        Response resource = executeService.port(worker);
        String port = resource.getResult().get("port");
        String url = String.format("http://%s:%s",worker.getIp(),port);
        PageIOUtils.writeTxt(response,url);
    }

    @RequestMapping("/monitor")
    public void monitor(HttpServletResponse response, Long workerId) throws Exception {
        Worker worker = workerService.getWorker(workerId);
        Map<String, String> data = executeService.monitor(worker);
        JsonMapper jsonMapper = new JsonMapper();
        PageIOUtils.writeJson(response, jsonMapper.toJson(data));
    }

    @RequestMapping("/cpuchart")
    public void cpuChart(HttpServletResponse response, Model model, Long workerId) throws Exception {
        //CPU图表数据
        if (notEmpty(workerId)) {
            model.addAttribute("workerId", workerId);
            JsonMapper jsonMapper = new JsonMapper();
            PageIOUtils.writeJson(response, jsonMapper.toJson(monitorService.getCpuData(workerId)));
        }
    }

    @RequestMapping("/login")
    public void login(HttpServletResponse response, HttpSession httpSession, @RequestParam String username, @RequestParam String password) {
        //用户信息验证
        int status = homeService.checkLogin(httpSession, username, password);

        if (status == 500) {
            PageIOUtils.writeJson(response, "{\"msg\":\"用户名密码错误\"}");
            return;
        }
        if (status == 200) {
            PageIOUtils.writeJson(response, "{\"successUrl\":\"/home\"}");
            return;
        }
    }

    @RequestMapping("/logout")
    public String logout(HttpSession httpSession) {
        httpSession.removeAttribute("user");
        return "redirect:/";
    }

    @RequestMapping("/notice/view")
    public String log(HttpSession session, Model model, Page page, Long workerId, String sendTime) {
        model.addAttribute("workers", workerService.getAll());
        if (notEmpty(workerId)) {
            model.addAttribute("workerId", workerId);
        }
        if (notEmpty(sendTime)) {
            model.addAttribute("sendTime", sendTime);
        }
        homeService.getLog(session,page, workerId, sendTime);
        return "notice/view";
    }

    @RequestMapping("/notice/info")
    public String log(HttpSession session, Model model) {
        model.addAttribute("message",homeService.getMsg(session));
        return "notice/info";
    }

    @RequestMapping("/notice/detail")
    public String detail(Model model, Long logId) {
        model.addAttribute("sender", configService.getSysConfig().getSenderEmail());
        model.addAttribute("log", homeService.getLogDetail(logId));
        return "notice/detail";
    }
}

