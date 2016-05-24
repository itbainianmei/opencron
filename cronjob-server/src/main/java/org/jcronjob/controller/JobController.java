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

import java.util.*;

import org.jcronjob.base.job.CronJob;
import org.jcronjob.base.job.CronJob.ExecType;
import org.jcronjob.tag.Page;
import org.jcronjob.base.utils.CommonUtils;
import org.jcronjob.base.utils.JsonMapper;
import org.jcronjob.base.utils.PageIOUtils;
import org.jcronjob.domain.Worker;
import org.jcronjob.domain.Job;
import org.jcronjob.service.*;
import org.jcronjob.vo.JobVo;
import org.quartz.SchedulerException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.jcronjob.base.utils.CommonUtils.notEmpty;

@Controller
@RequestMapping("/job")
public class JobController {

    @Autowired
    private ExecuteService executeService;

    @Autowired
    private JobService jobService;

    @Autowired
    private WorkerService workerService;

    @Autowired
    private RecordService recordService;

    @Autowired
    private SchedulerService schedulerService;

    @RequestMapping("/view")
    public String view(HttpServletRequest request, HttpSession session, Page page, JobVo job, Model model) {

        model.addAttribute("workers", workerService.getAll());

        model.addAttribute("jobs", jobService.getAll());
        if (notEmpty(job.getWorkerId())) {
            model.addAttribute("workerId", job.getWorkerId());
        }
        if (notEmpty(job.getExecType())) {
            model.addAttribute("execType", job.getExecType());
        }
        if (notEmpty(job.getRedo())) {
            model.addAttribute("redo", job.getRedo());
        }
        jobService.getJobs(session, page, job);
        if (request.getParameter("refresh") != null) {
            return "/job/refresh";
        }
        return "/job/view";
    }

    @RequestMapping("/checkname")
    public void checkName(HttpServletResponse response, Long id, String name) {
        String result = jobService.checkName(id, name);
        PageIOUtils.writeHtml(response, result);
    }

    @RequestMapping("/addpage")
    public String addpage(Model model, Long id) {
        if (notEmpty(id)) {
            Worker worker = workerService.getWorker(id);
            model.addAttribute("worker", worker);
            List<Worker> workers = workerService.getAll();
            model.addAttribute("workers", workers);
        } else {
            List<Worker> workers = workerService.getAll();
            model.addAttribute("workers", workers);
        }
        return "/job/add";
    }

    @RequestMapping(value = "/save")
    public String save(HttpSession session, Job job, HttpServletRequest request) throws SchedulerException {

        //流程任务
        if (job.getCategory() == CronJob.JobCategory.FLOW.getCode()) {
            Map<String, Object[]> map = request.getParameterMap();
            Object[] jobName = map.get("child.jobName");
            Object[] workerId = map.get("child.workerId");
            Object[] command = map.get("child.command");
            Object[] comment = map.get("child.comment");
            Object[] redo = map.get("child.redo");
            Object[] runCount = map.get("child.runCount");

            //流程任务必须有子任务,没有的话不保存
            if (CommonUtils.isEmpty(jobName)) {
                return "redirect:/job/view";
            }

            if (job.getJobId() != null) {
                jobService.dealOldFlowJob(job.getJobId());
            }

            if (job.getOperateId() == null) {
                job.setOperateId((Long) (session.getAttribute("userId")));
            }

            job.setLastFlag(false);
            job.setUpdateTime(new Date());
            job = jobService.addOrUpdate(job);
            job.setFlowNum(0);//顶层sort是0
            job.setFlowId(job.getJobId());//flowId

            job = jobService.addOrUpdate(job);
            syncJobTigger(job);

            for (int i = 0; i < jobName.length; i++) {
                Job chind = new Job();
                chind.setJobName((String) jobName[i]);
                chind.setWorkerId(Long.parseLong((String) workerId[i]));
                chind.setCommand((String) command[i]);
                //chind.setComment((String) comment[i]);

                chind.setRedo(Integer.parseInt((String) redo[i]));
                if (chind.getRedo() == 0) {
                    chind.setRunCount(null);
                } else {
                    chind.setRunCount(Long.parseLong((String) runCount[i]));
                }

                if (i == jobName.length - 1) {//最后一个子任务
                    chind.setLastFlag(true);
                } else {
                    chind.setLastFlag(false);
                }

                job = saveSubJob(job, chind);
            }
        } else {//单任务
            job.setOperateId((Long) (session.getAttribute("userId")));
            job.setUpdateTime(new Date());
            job = jobService.addOrUpdate(job);
            syncJobTigger(job);
        }
        return "redirect:/job/view";
    }

    private void syncJobTigger(Job job) throws SchedulerException {
        JobVo jobVo = new JobVo();
        Worker worker = workerService.getWorker(job.getWorkerId());
        BeanUtils.copyProperties(job, jobVo);
        jobVo.setWorker(worker);
        jobVo.setIp(worker.getIp());
        jobVo.setPort(worker.getPort());
        jobVo.setPassword(worker.getPassword());

        //quartz表达式
        if (job.getCronType().equals(CronJob.CronType.QUARTZ.getType())) {
            if (job.getExecType().equals(ExecType.AUTO.getStatus())) {//自动执行
                schedulerService.addOrModify(jobVo, executeService);
            } else {//手动执行
                schedulerService.remove(job.getJobId());
            }
        } else {//crontab表达式..
            schedulerService.remove(job.getJobId());
        }
    }

    private Job saveSubJob(Job parent, Job job) {
        job.setFlowId(parent.getFlowId());
        job.setOperateId(parent.getOperateId());
        job.setExecType(parent.getExecType());
        job.setUpdateTime(new Date());
        job.setCategory(1);
        job.setFlowNum(parent.getFlowNum() + 1);
        return jobService.addOrUpdate(job);
    }

    @RequestMapping("/editsingle")
    public void editSingleJob(HttpServletResponse response, Long id) {
        JobVo job = jobService.getJobById(id);
        JsonMapper json = new JsonMapper();
        PageIOUtils.writeJson(response, json.toJson(job));
    }

    @RequestMapping("/editflow")
    public String editFlowJob(Model model, Long id) {
        JobVo job = jobService.getJobById(id);
        model.addAttribute("job", job);
        List<Worker> workers = workerService.getAll();
        model.addAttribute("workers", workers);
        return "/job/edit";
    }


    @RequestMapping("/edit")
    public void edit(HttpServletResponse response, Job job) throws SchedulerException {
        Job befJob = jobService.queryJobById(job.getJobId());
        befJob.setExecType(job.getExecType());
        befJob.setCronType(job.getCronType());
        befJob.setCronExp(job.getCronExp());
        befJob.setCommand(job.getCommand());
        befJob.setJobName(job.getJobName());
        befJob.setRedo(job.getRedo());
        befJob.setRunCount(job.getRunCount());
        befJob.setUpdateTime(new Date());
        jobService.addOrUpdate(befJob);
        syncJobTigger(befJob);
        PageIOUtils.writeHtml(response, "success");
    }

    @RequestMapping("/canrun")
    public void canRun(Long id, HttpServletResponse response) {
        PageIOUtils.writeJson(response, recordService.isRunning(id).toString());
    }

    @RequestMapping("/execute")
    public void remoteExecute(Long id) {
        JobVo job = jobService.getJobById(id);//找到要执行的任务
        job.setWorker(workerService.getWorker(job.getWorkerId()));
        try {
            this.executeService.executeJob(job, ExecType.OPERATOR);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequestMapping("/detail")
    public String showDetail(Model model, Long id) {
        JobVo jobVo = jobService.getJobById(id);
        model.addAttribute("job", jobVo);
        return "/job/detail";
    }

}
