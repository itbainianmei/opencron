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

import com.alibaba.fastjson.JSON;
import com.jredrain.base.utils.IOUtils;
import com.jredrain.domain.User;
import net.sf.json.JSONObject;
import com.jredrain.base.job.RedRain;
import com.jredrain.base.job.Response;
import com.jredrain.base.utils.DateUtils;
import com.jredrain.base.utils.JsonMapper;
import com.jredrain.base.utils.PageIOUtils;
import com.jredrain.domain.Job;
import com.jredrain.domain.Worker;
import com.jredrain.service.*;
import com.jredrain.tag.Page;
import com.jredrain.vo.ChartVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static com.jredrain.base.utils.CommonUtils.isEmpty;
import static com.jredrain.base.utils.CommonUtils.notEmpty;

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

    @Autowired
    private UserService userService;

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
        List<Job> singleton = jobService.getJobsByCategory(RedRain.JobCategory.SINGLETON);
        List<Job> flow = jobService.getJobsByCategory(RedRain.JobCategory.FLOW);

        model.addAttribute("singleton",singleton.size());
        model.addAttribute("flow",flow.size());
        singleton.addAll(flow);
        model.addAttribute("job",singleton.size());

        /**
         * 成功作业,自动执行
         */
        Long successAutoRecord = recordService.getRecords(1, RedRain.ExecType.AUTO);
        Long successOperRecord = recordService.getRecords(1, RedRain.ExecType.OPERATOR);

        model.addAttribute("successAutoRecord",successAutoRecord);
        model.addAttribute("successOperRecord",successOperRecord);
        model.addAttribute("successRecord",successAutoRecord+successOperRecord);

        /**
         * 失败作业
         */
        Long failedAutoRecord = recordService.getRecords(0, RedRain.ExecType.AUTO);
        Long failedOperRecord = recordService.getRecords(0, RedRain.ExecType.OPERATOR);
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
        if (isEmpty(voList)) {
            PageIOUtils.writeJson(response, "null");
        }else {
            PageIOUtils.writeJson(response, jsonMapper.toJson(voList));
        }
    }

    @RequestMapping("/monitor")
    public void port(HttpServletResponse response, Long workerId) throws Exception {
        Worker worker = workerService.getWorker(workerId);
        Response req = executeService.monitor(worker);
        /**
         * 直联
         */

        String format = "{\"conn\":" + worker.getProxy() + "},\"data\":%s";

        if (worker.getProxy().equals(RedRain.ConnType.CONN.getType())) {
            String port = req.getResult().get("port");
            String url = String.format("http://%s:%s", worker.getIp(), port);
            PageIOUtils.writeJson(response, String.format(format, url));
        } else {//代理
            PageIOUtils.writeJson(response, String.format(format, JSON.toJSONString(req.getResult())));
        }
    }




   /* @RequestMapping("/monitor")
    public void monitor(HttpServletResponse response, Long workerId) throws Exception {
        Worker worker = workerService.getWorker(workerId);
        Map<String, String> data = executeService.monitor(worker);
        JsonMapper jsonMapper = new JsonMapper();
        PageIOUtils.writeJson(response, jsonMapper.toJson(data));
    }*/

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
    public void login(HttpServletResponse response, HttpSession httpSession, @RequestParam String username, @RequestParam String password) throws Exception {
        //用户信息验证
        int status = homeService.checkLogin(httpSession, username, password);

        if (status == 500) {
            PageIOUtils.writeJson(response, "{\"msg\":\"用户名密码错误\"}");
            return;
        }
        if (status == 200) {
            User user = (User) httpSession.getAttribute("user");
            if (user.getHeaderpic()!=null) {
                String path = httpSession.getServletContext().getRealPath(File.separator) + "temp" + File.separator + user.getUserId() + "_pic.png";
                IOUtils.writeFile(new File(path), user.getHeaderpic().getBinaryStream());
                user.setHreaderPath(path);
            }
            PageIOUtils.writeJson(response, "{\"successUrl\":\"/home\"}");
            return;
        }
    }

    @RequestMapping("/uploadimg")
    public void uploadimg(@RequestParam(value = "file", required = false) MultipartFile file, HttpSession httpSession, HttpServletResponse response) throws Exception {
        User user = (User) httpSession.getAttribute("user");
        user = userService.uploadimg(file,user);
        String path = httpSession.getServletContext().getRealPath(File.separator) + "temp" + File.separator + user.getUserId()+"_pic.png";
        IOUtils.writeFile(new File(path),user.getHeaderpic().getBinaryStream());
        PageIOUtils.writeJson(response,String.format("{\"fileUrl\":\"%s\"}",path));
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


    @RequestMapping("/notice/uncount")
    public void uncount(HttpSession session, HttpServletResponse response) {
         Long count = homeService.getUnReadCount(session);
         PageIOUtils.writeHtml(response,count.toString());
    }

    /**
     * 未读取的站类信
     * @param session
     * @param model
     * @return
     */
    @RequestMapping("/notice/unread")
    public String nuread(HttpSession session, Model model) {
        model.addAttribute("message",homeService.getUnReadMessage(session));
        return "notice/info";
    }

    @RequestMapping("/notice/detail")
    public String detail(Model model, Long logId) {
        model.addAttribute("sender", configService.getSysConfig().getSenderEmail());
        model.addAttribute("log", homeService.getLogDetail(logId));
        return "notice/detail";
    }
}

