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

import org.jcronjob.job.CronJobCollector;
import org.jcronjob.vo.JobVo;
import org.quartz.*;
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

    private Scheduler scheduler;

    private it.sauronsoftware.cron4j.Scheduler crontabScheduler;

    @Autowired
    private JobService jobService;

    @Autowired
    private CronJobCollector cronJobCollector;

    public SchedulerService() throws SchedulerException {
        this.scheduler = StdSchedulerFactory.getDefaultScheduler();
    }


    public boolean checkExists(Long jobId) throws SchedulerException {
        return scheduler.checkExists(JobKey.jobKey(jobId.toString()));
    }

    public boolean addOrModify(JobVo job, Job jobBean) throws SchedulerException {
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

        return true;
    }


    public boolean remove(Long jobId) throws SchedulerException {
        TriggerKey triggerKey = TriggerKey.triggerKey(jobId.toString());
        if (checkExists(jobId)) {
            boolean result = scheduler.unscheduleJob(triggerKey);
            logger.info("cronjob: removed, triggerKey:{}, result [{}]", triggerKey, result);
        }
        return true;
    }

    public boolean pause(Long jobId) throws SchedulerException {
        TriggerKey triggerKey = TriggerKey.triggerKey(jobId.toString());

        boolean result = false;
        if (checkExists(jobId)) {
            scheduler.pauseTrigger(triggerKey);
            result = true;
        } else {
        }
        return result;
    }

    public boolean resume(Long jobId) throws SchedulerException {
        TriggerKey triggerKey = TriggerKey.triggerKey(jobId.toString());

        boolean result = false;
        if (checkExists(jobId)) {
            scheduler.resumeTrigger(triggerKey);
            result = true;
        } else {
        }
        return result;
    }

    public void startCrontab() {
        jobService.syncCrontabJob();
        if (this.crontabScheduler == null) {
            this.crontabScheduler = new it.sauronsoftware.cron4j.Scheduler();
            crontabScheduler.addTaskCollector(cronJobCollector);
        } else {
            this.crontabScheduler.stop();
        }
        this.crontabScheduler.start();
    }

}