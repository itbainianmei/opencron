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


package org.jcronjob.service;

import org.jcronjob.base.job.CronJob;
import org.jcronjob.job.CronJobCollector;
import org.jcronjob.vo.JobVo;
import org.quartz.*;
import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.TriggerBuilder.newTrigger;


@Service
public final class SchedulerService {

    private final Logger logger = LoggerFactory.getLogger(SchedulerService.class);

    @Autowired
    private JobService jobService;

    @Autowired
    private WorkerService workerService;

    @Autowired
    private CronJobCollector cronJobCollector;

    private Scheduler scheduler;

    private it.sauronsoftware.cron4j.Scheduler crontabScheduler;

    public SchedulerService() throws SchedulerException {
        this.scheduler = StdSchedulerFactory.getDefaultScheduler();
    }

    public boolean checkExists(Long jobId) throws SchedulerException {
        return scheduler.checkExists(JobKey.jobKey(jobId.toString()));
    }

    public void addOrModify(List<JobVo> jobs, Job jobBean) throws SchedulerException {
        for(JobVo jobVo:jobs){
            addOrModify(jobVo,jobBean);
        }
    }

    public void addOrModify(JobVo job, Job jobBean) throws SchedulerException {
        TriggerKey triggerKey = TriggerKey.triggerKey(job.getJobId().toString());

        if (checkExists(job.getJobId())) {
            this.remove(job.getJobId());
        }

        CronTrigger cronTrigger = newTrigger().withIdentity(triggerKey).withSchedule(cronSchedule(job.getCronExp())).build();
        JobDetail jobDetail = JobBuilder.newJob(jobBean.getClass()).withIdentity(JobKey.jobKey(job.getJobId().toString())).build();
        jobDetail.getJobDataMap().put(job.getJobId().toString(), job);
        jobDetail.getJobDataMap().put("jobBean", jobBean);
        Date date = scheduler.scheduleJob(jobDetail, cronTrigger);
        if (!scheduler.isStarted()) {
            scheduler.start();
        }
        logger.info("cronjob: add success,cronTrigger:{}", cronTrigger, date);

    }

    public void remove(Long jobId) throws SchedulerException {
        TriggerKey triggerKey = TriggerKey.triggerKey(jobId.toString());
        if (checkExists(jobId)) {
            boolean result = scheduler.unscheduleJob(triggerKey);
            logger.info("cronjob: removed, triggerKey:{}, result [{}]", triggerKey, result);
        }
    }

    public void pause(Long jobId) throws SchedulerException {
        TriggerKey triggerKey = TriggerKey.triggerKey(jobId.toString());
        if (checkExists(jobId)) {
            scheduler.pauseTrigger(triggerKey);
        }
    }

    public void resume(Long jobId) throws SchedulerException {
        TriggerKey triggerKey = TriggerKey.triggerKey(jobId.toString());
        if (checkExists(jobId)) {
            scheduler.resumeTrigger(triggerKey);
        } else {
        }
    }

    public void startCrontab() {
        if (this.crontabScheduler == null) {
            this.crontabScheduler = new it.sauronsoftware.cron4j.Scheduler();
            crontabScheduler.addTaskCollector(cronJobCollector);
        } else {
            this.crontabScheduler.stop();
        }
        this.crontabScheduler.start();
    }


    public void syncJobTigger(Long jobId,ExecuteService executeService) throws SchedulerException {
        JobVo job = jobService.getJobVoById(jobId);
        job.setWorker(workerService.getWorker(job.getWorkerId()));

        //quartz表达式
        if (job.getCronType().equals(CronJob.CronType.QUARTZ.getType())) {
            /**
             * 如果当前的job是之前crontab类型改为现在的quartz类型..
             * 将该任务从crontab任务计划里删除
             */
            cronJobCollector.removeTask(job.getJobId());

            if ( CronJob.ExecType.AUTO.getStatus().equals(job.getExecType()) ) {//自动执行
                addOrModify(job, executeService);
            } else {//手动执行
                remove(job.getJobId());
            }
        } else {
            //crontab表达式..
            /**
             * 不管之前在不在quartz任务计划里,统统删除.
             */
            remove(job.getJobId());
            /**
             * 将作业加到crontab任务计划
             */
            if ( CronJob.ExecType.AUTO.getStatus().equals(job.getExecType()) ) {//自动执行
                cronJobCollector.addTask(job); //手动执行
            }else {
                cronJobCollector.removeTask(job.getJobId());
            }
        }
    }

}