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


package org.jcronjob.job;

import org.apache.log4j.Logger;
import org.jcronjob.base.job.CronJob;
import org.jcronjob.base.utils.CommonUtils;
import org.jcronjob.domain.Record;
import org.jcronjob.domain.Worker;
import org.jcronjob.service.*;
import org.jcronjob.vo.JobVo;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class CronJobInitializeBean implements Serializable,InitializingBean {

    private final Logger logger = Logger.getLogger(CronJobInitializeBean.class);

    @Autowired
    private WorkerService workerService;

    @Autowired
    private ExecuteService executeService;

    @Autowired
    private RecordService recordService;

    @Autowired
    private JobService jobService;

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    private MonitorService monitorService;

    @Override
    public void afterPropertiesSet() throws Exception {
        //quartz job
        List<JobVo> jobs = jobService.getJobVo(CronJob.ExecType.AUTO, CronJob.CronType.QUARTZ);
        for (JobVo job : jobs) {
            try {
                schedulerService.addOrModify(job,executeService);
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
        }

        schedulerService.startCrontab();

    }

    /**
     * 执行器通信监控,每10秒通信一次
     */

    @Scheduled(cron = "0/5 * * * * ?")
    public void ping() {
        logger.info("[cronjob]:checking Worker connection...");

        List<Worker> workers = workerService.getAll();
        for (final Worker worker : workers) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Boolean result;
                    int countLimit = 0;
                    do {
                        result = executeService.ping(worker.getIp(), worker.getPort(), worker.getPassword());
                        ++countLimit;
                    } while (!result && countLimit < 3);
                    if (!result) {
                        if (CommonUtils.isEmpty(worker.getFailTime()) || new Date().getTime() - worker.getFailTime().getTime() >= configService.getSysConfig().getSpaceTime() * 60 * 1000) {
                            noticeService.notice(worker);
                            //记录本次任务失败的时间
                            worker.setFailTime(new Date());
                            workerService.updateStatus(worker,0);
                        }

                        if (worker.getStatus()) {
                            workerService.updateStatus(worker,0);
                        }

                    } else {
                        if (!worker.getStatus()) {
                            workerService.updateStatus(worker,1);
                        }
                    }
                }
            };
            runnable.run();
        }
    }

    @Scheduled(cron = "0/5 * * * * ?")
    public void redoJob() {
        List<Record> records = recordService.getReExecuteRecord();
        for (final Record record : records) {
            JobVo jobVo = jobService.getJobVoById(record.getJobId());
            try {
                jobVo.setWorker(workerService.getWorker(jobVo.getWorkerId()));
                executeService.reRunJob(record, jobVo, CronJob.JobCategory.SINGLETON);
            } catch (Exception e) {
                //任务执行失败,发送通知警告
                noticeService.notice(jobVo);
                throw new RuntimeException("reexecute job is failed while executing job:" + jobVo.getJobId());
            }
        }
    }

    //@Scheduled(cron = "0 0/1 * * * ?")
  /*  public void monitor() throws Exception {
        List<Worker> workers = workerService.getAll();

        for (Worker worker : workers) {
            Map<String, String> systemData = executeService.monitor(worker);

            String cpuUsage = systemData.remove("cpuUsage");
            Float cpuUs = Float.parseFloat(cpuUsage.split(",")[0]);
            Float cpuSy = Float.parseFloat(cpuUsage.split(",")[1]);
            Float cpuId = Float.parseFloat(cpuUsage.split(",")[2]);

            String memUsage = systemData.remove("memUsage");
            Long memUsed = Long.parseLong(memUsage.split(",")[0]);
            Long memFree = Long.parseLong(memUsage.split(",")[1]);

            Monitor monitor = new Monitor(worker.getWorkerId(), cpuUs, cpuSy, cpuId, memUsed, memFree);
            monitorService.save(monitor);
        }
    }*/


}
