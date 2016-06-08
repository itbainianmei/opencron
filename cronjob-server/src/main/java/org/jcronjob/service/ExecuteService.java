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

import org.jcronjob.base.job.Action;
import org.jcronjob.base.job.Request;
import org.jcronjob.base.job.Response;
import org.jcronjob.domain.Record;
import org.jcronjob.domain.Worker;
import org.jcronjob.job.CronJobCaller;
import org.jcronjob.vo.JobVo;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.jcronjob.base.job.CronJob.*;

@Service
public class ExecuteService implements Job {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private RecordService recordService;

    @Autowired
    private JobService jobService;

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private CronJobCaller cronJobCaller;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String key = jobExecutionContext.getJobDetail().getKey().getName();
        JobVo jobVo = (JobVo) jobExecutionContext.getJobDetail().getJobDataMap().get(key);
        try {
            ExecuteService executeService = (ExecuteService) jobExecutionContext.getJobDetail().getJobDataMap().get("jobBean");
            executeService.executeJob(jobVo, ExecType.getByStatus(jobVo.getExecType()));
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
        }
    }

    public boolean executeJob(final JobVo job, final ExecType execType) {
        if (job.getCategory() == JobCategory.FLOW.getCode()) {//流程任务..
            final long flowGroup = System.currentTimeMillis();//分配一个流程组Id
            /**
             * 一个指定大小的job队列
             */
            job.getChildren().add(0, job);
            Queue<JobVo> jobQueue = new LinkedBlockingQueue<JobVo>(job.getChildren());

            final List<Boolean> result = new ArrayList<Boolean>(0);
            for (final JobVo jobVo : jobQueue) {
                //如果子任务是并行(则启动多线程,所有子任务同时执行)
                if (job.getRunModel().equals(RunModel.SAMETIME.getValue())) {
                    new Thread(new Runnable() {
                        public void run() {
                            result.add(executeFlowJob(jobVo, execType, flowGroup));
                        }
                    }).start();
                } else {//串行,按顺序执行
                    if (!executeFlowJob(jobVo, execType, flowGroup)) {
                        return false;
                    }
                }
            }

            /**
             * 并行任务中有失败的...
             */
            return !result.contains(false);

        }

        Record record = new Record(job, execType);
        record.setCategory(JobCategory.SINGLETON.getCode());//单一任务
        //执行前先保存
        record = recordService.save(record);

        try {
            //执行前先检测一次通信是否正常
            checkPing(job, record);

            Response response = responseToRecord(job, record);
            /**
             * 被kill
             */
            if (response.getExitCode() == StatusCode.KILL.getValue()) {
                record.setStatus(RunStatus.STOPED.getStatus());
                record.setSuccess(ResultStatus.KILLED.getStatus());//被kill任务失败
            } else {//非kill
                record.setStatus(RunStatus.DONE.getStatus());
            }
            recordService.update(record);

            if (record.getSuccess().equals(ResultStatus.SUCCESSFUL.getStatus())) {
                logger.info("execute successful:jobName:{},jobId:{},ip:{},port:", job.getJobName(), job.getJobId(), job.getIp(), job.getPort());
            } else {
                noticeService.notice(job);
                logger.info("execute failed:jobName:{},jobId:{},ip:{},port:{},info:", job.getJobName(), job.getJobId(), job.getIp(), job.getPort(), record.getMessage());
            }
        } catch (Exception e) {
            noticeService.notice(job);
            String errorInfo = String.format("execute failed:jobName:%s,jobId:%d,ip:%s,port:%d,info:%s", job.getJobName(), job.getJobId(), job.getIp(), job.getPort(), e.getMessage());
            logger.error(errorInfo, e);
        }
        return record.getSuccess().equals(ResultStatus.SUCCESSFUL.getStatus());
    }


    private boolean executeFlowJob(JobVo job, ExecType execType, long flowGroup) {
        Record record = new Record(job, execType);
        record.setRedoCount(0L);
        record.setFlowGroup(flowGroup);//组Id
        record.setCategory(JobCategory.FLOW.getCode());//流程任务
        record.setFlowNum(job.getFlowNum());

        //执行前先保存
        record = recordService.save(record);

        //执行前先检测一次通信是否正常
        try {
            checkPing(job, record);
        } catch (Exception e) {
            recordService.flowJobDone(record);//通信失败,流程任务挂起.
            return false;
        }

        boolean success = true;

        try {

            Response result = responseToRecord(job, record);
            /**
             * 被kill,直接退出
             */
            if (result.getExitCode() == StatusCode.KILL.getValue()) {
                record.setStatus(RunStatus.STOPED.getStatus());
                record.setSuccess(ResultStatus.KILLED.getStatus());
                recordService.update(record);
                recordService.flowJobDone(record);
                return false;
            }

            if (!result.isSuccess()) {
                success = false;
                recordService.update(record);
            } else {
                if (job.getLastFlag()) {
                    recordService.update(record);
                    recordService.flowJobDone(record);
                } else {
                    record.setStatus(RunStatus.RUNNING.getStatus());
                    recordService.update(record);
                }
            }
            return result.isSuccess();
        } catch (Exception e) {
            noticeService.notice(job);
            String errorInfo = String.format("execute failed(flow job):jobName:%s,,jobId:%d,,ip:%s,port:%d,info:%s", job.getJobName(), job.getJobId(), job.getIp(), job.getPort(), e.getMessage());
            record.setSuccess(ResultStatus.FAILED.getStatus());//程序调用失败
            record.setReturnCode(StatusCode.ERROR_EXEC.getValue());
            record.setEndTime(new Date());
            record.setMessage(errorInfo);
            recordService.update(record);
            success = false;
            logger.error(errorInfo, e);
            return false;
        } finally {
            if (!success) {
                Record red = recordService.get(record.getRecordId());
                if (job.getRedo() == 1 && job.getRunCount() > 0) {
                    int index = 0;
                    boolean flag;
                    do {
                        flag = reRunJob(red, job, JobCategory.FLOW);
                        ++index;
                    } while (!flag && index < job.getRunCount());

                    if (!flag) {
                        noticeService.notice(job);
                        recordService.flowJobDone(record);
                    }
                } else {
                    noticeService.notice(job);
                    recordService.flowJobDone(record);
                }
            }
        }

    }

    public boolean reRunJob(final Record parentRecord, JobVo job, JobCategory category) {
        /**
         * 当前重新执行的新纪录
         */
        Record record = new Record(job, ExecType.RERUN);
        record.setParentId(parentRecord.getRecordId());
        record.setFlowGroup(parentRecord.getFlowGroup());
        record.setCategory(category.getCode());

        record = recordService.save(record);
        /**
         * 父记录
         */
        parentRecord.setRedoCount(parentRecord.getRedoCount() + 1L);//运行次数
        //如果已经到了任务重跑的截至次数直接更新为已重跑完成
        if (job.getRunCount() == parentRecord.getRedoCount()) {
            parentRecord.setExecType(ExecType.RERUN_DONE.getStatus());
        }
        recordService.update(parentRecord);

        try {
            //执行前先检测一次通信是否正常
            checkPing(job, record);

            Response result = responseToRecord(job, record);
            /**
             * 被kill
             */
            if (result.getExitCode() == StatusCode.KILL.getValue()) {
                record.setStatus(RunStatus.STOPED.getStatus());
                record.setSuccess(ResultStatus.KILLED.getStatus());//被kill任务失败
            } else {
                record.setStatus(RunStatus.DONE.getStatus());
            }

            //本次重跑的执行成功,则父记录执行完毕
            if (parentRecord.getExecType() == ExecType.RERUN_DONE.getStatus()) {
                parentRecord.setExecType(ExecType.RERUN_DONE.getStatus());
                recordService.update(parentRecord);
            }
            recordService.update(record);
            logger.info("execute successful:jobName:{},jobId:{},ip:{},port:{}", job.getJobName(), job.getJobId(), job.getIp(), job.getPort());
        } catch (Exception e) {
            noticeService.notice(job);
            String errorInfo = String.format("execute failed:jobName:%s,jobId:%d,ip:%s,port:%d,info:%s", job.getJobName(), job.getJobId(), job.getIp(), job.getPort(), e.getMessage());
            errorExec(record, errorInfo);
            logger.error(errorInfo, e);
        }

        return record.getSuccess().equals(ResultStatus.SUCCESSFUL.getStatus());
    }

    public boolean killJob(Record record) {
        Long recordId = record.getRecordId();
        List<Record> records = new ArrayList<Record>(0);
        //单一任务
        if (record.getCategory() == 0) {
            records.add(record);
        } else if (record.getCategory() == 1) {
            records = recordService.getRunningFlowJob(recordId);
        }

        /**
         * 零时的改成停止中...
         */
        for (Record cord : records) {
            cord.setStatus(RunStatus.STOPPING.getStatus());//停止中
            cord.setSuccess(ResultStatus.KILLED.getStatus());//被杀.
            recordService.update(cord);
        }

        /**
         * 向远程机器发送kill指令
         */
        for (Record cord : records) {
            JobVo job = jobService.getJobVoById(cord.getJobId());
            try {
                cronJobCaller.call(Request.request(job.getIp(), job.getPort(), Action.KILL, job.getPassword()).putParam("pid", cord.getPid()));
                cord.setStatus(RunStatus.STOPED.getStatus());
                cord.setEndTime(new Date());
                recordService.update(cord);
                logger.info("killed successful :jobName:{},ip:{},port:{},pid:{}", job.getJobName(), job.getIp(), job.getPort(), cord.getPid());
            } catch (Exception e) {
                noticeService.notice(job);
                String errorInfo = String.format("killed error:jobName:%s,ip:%d,port:%d,pid:%d,failed info:%s", job.getJobName(), job.getIp(), job.getPort(), cord.getPid(), e.getMessage());
                logger.error(errorInfo, e);
                return false;
            }
        }
        return true;
    }

    public boolean ping(String ip, Integer port, final String password) {
        boolean ping = false;
        try {
            ping = cronJobCaller.call(Request.request(ip, port, Action.PING, password)).isSuccess();
        } catch (Exception e) {
            logger.error("[cronjob]ping failed,host:{},port:{}", ip, port);
        } finally {
            return ping;
        }
    }

    /**
     * 修改密码
     *
     * @param ip
     * @param port
     * @param password
     * @param newPassword
     * @return
     */
    public boolean password(String ip, int port, final String password, final String newPassword) {
        boolean ping = false;
        try {
            Response response = cronJobCaller.call(Request.request(ip, port, Action.PASSWORD, password).putParam("newPassword", newPassword));
            ping = response.isSuccess();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return ping;
        }
    }

    private Response responseToRecord(final JobVo job, final Record record) throws Exception {
        Response response = cronJobCaller.call(Request.request(job.getIp(), job.getPort(), Action.EXECUTE, job.getPassword()).putParam("command", job.getCommand()).putParam("pid", record.getPid()));
        logger.info("[cronjob]:execute response:{}", response.toString());
        record.setReturnCode(response.getExitCode());
        record.setMessage(response.getMessage());
        record.setSuccess(response.isSuccess() ? ResultStatus.SUCCESSFUL.getStatus() : ResultStatus.FAILED.getStatus());
        record.setStartTime(new Date(response.getStartTime()));
        record.setEndTime(new Date(response.getEndTime()));
        return response;
    }

    private void checkPing(JobVo job, Record record) throws Exception {
        if (!ping(job.getIp(), job.getPort(), job.getPassword())) {
            record.setStatus(RunStatus.DONE.getStatus());//已完成
            record.setReturnCode(StatusCode.ERROR_PING.getValue());

            String format = "can't to communicate with worker:%s(%s:%d),execute job:%s failed";
            String content = String.format(format, job.getWorkerName(), job.getIp(), job.getPort(), job.getJobName());

            record.setMessage(content);
            record.setSuccess(ResultStatus.FAILED.getStatus());
            record.setEndTime(new Date());
            recordService.update(record);
            throw new Exception(content);
        }
    }

    private void errorExec(Record record, String errorInfo) {
        record.setSuccess(ResultStatus.FAILED.getStatus());//程序调用失败
        record.setStatus(RunStatus.DONE.getStatus());//已完成
        record.setReturnCode(StatusCode.ERROR_EXEC.getValue());
        record.setEndTime(new Date());
        record.setMessage(errorInfo);
        recordService.update(record);

    }

    public Response port(Worker worker) throws Exception {
        return cronJobCaller.call(Request.request(worker.getIp(), worker.getPort(), Action.PORT, worker.getPassword()));
    }
}
