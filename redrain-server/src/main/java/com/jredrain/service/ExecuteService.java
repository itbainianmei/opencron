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


package com.jredrain.service;

import com.jredrain.base.job.Action;
import com.jredrain.base.job.Request;
import com.jredrain.base.job.Response;
import com.jredrain.base.utils.ParamsMap;
import com.jredrain.domain.Record;
import com.jredrain.domain.Agent;
import com.jredrain.domain.User;
import com.jredrain.job.RedRainCaller;
import com.jredrain.vo.JobVo;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import static com.jredrain.base.job.RedRain.*;

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
    private RedRainCaller cronJobCaller;

    @Autowired
    private AgentService agentService;

    @Autowired
    private UserService userService;

    private Map<Long,Integer> reExecuteThreadMap = new HashMap<Long, Integer>(0);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String key = jobExecutionContext.getJobDetail().getKey().getName();
        JobVo jobVo = (JobVo) jobExecutionContext.getJobDetail().getJobDataMap().get(key);
        try {
            ExecuteService executeService = (ExecuteService) jobExecutionContext.getJobDetail().getJobDataMap().get("jobBean");
            boolean success = executeService.executeJob(jobVo);
            logger.info("[redrain] job:{} at {}:{},execute:{}", jobVo.getJobName(),jobVo.getAgent().getIp(),jobVo.getAgent().getPort(), success?"successful":"failed");
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
        }
    }

    /**
     *
     * @param job
     * @return
     */

    public boolean executeJob(final JobVo job) {

        if (!checkJobPermission(job.getAgentId(),job.getOperateId()))return false;
        //流程作业..

        JobType jobType = JobType.getJobType(job.getJobType());

        switch (jobType) {
            case FLOW:
                final long groupId = System.nanoTime()+Math.abs(new java.util.Random().nextInt());//分配一个流程组Id
                final Queue<JobVo> jobQueue = new LinkedBlockingQueue<JobVo>();
                jobQueue.add(job);
                jobQueue.addAll(job.getChildren());

                /**
                 * 并行任务
                 */

                RunModel runModel = RunModel.getRunModel(job.getRunModel());
                switch (runModel) {
                    case SAMETIME:
                        final List<Boolean> result = new ArrayList<Boolean>(0);
                        Thread jobThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                for (final JobVo jobVo : jobQueue) {
                                    //如果子任务是并行(则启动多线程,所有子任务同时执行)
                                    Thread thread = new Thread(new Runnable() {
                                        public void run() {
                                            result.add(executeFlowJob(jobVo, groupId));
                                        }
                                    });
                                    thread.start();
                                }
                            }
                        });
                        jobThread.start();
                        /**
                         * 确保所有的现场执行作业都全部执行完毕,拿到返回的执行结果。检查并行任务中有是否失败的...
                         */
                        try {
                            jobThread.join();
                        } catch (InterruptedException e) {
                            logger.error("[redrain] job rumModel with SAMETIME error:{}",e.getMessage());
                        }
                        return !result.contains(false);
                    case SEQUENCE:
                        for (JobVo jobVo : jobQueue) {
                            if (!executeFlowJob(jobVo, groupId)) {
                                return false;
                            }
                        }
                        return true;
                    default:
                        return false;
                }
            case SINGLETON:
                return executeSingletonJob(job,job.getOperateId());
            default:
                return false;
        }
    }


    private boolean executeFlowJob(JobVo job,long groupId) {
        Record record = new Record(job);
        record.setGroupId(groupId);//组Id
        record.setJobType(JobType.FLOW.getCode());//流程任务
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

            if (!result.isSuccess()) {
                recordService.update(record);
                //被kill,直接退出
                if ( StatusCode.KILL.getValue().equals(result.getExitCode()) ) {
                    recordService.flowJobDone(record);
                }else {
                    success = false;
                }
                return false;
            } else {
                //当前任务是流程任务的最后一个任务,则整个任务运行完毕
                if (job.getLastFlag()) {
                    recordService.update(record);
                    recordService.flowJobDone(record);
                } else {
                    //当前任务非流程任务最后一个子任务,全部流程任务为运行中...
                    record.setStatus(RunStatus.RUNNING.getStatus());
                    recordService.update(record);
                }
                return true;
            }
        } catch (Exception e) {
            String errorInfo = String.format("execute failed(flow job):jobName:%s,,jobId:%d,,ip:%s,port:%d,info:%s", job.getJobName(), job.getJobId(), job.getIp(), job.getPort(), e.getMessage());
            record.setSuccess(ResultStatus.FAILED.getStatus());//程序调用失败
            record.setReturnCode(StatusCode.ERROR_EXEC.getValue());
            record.setEndTime(new Date());
            record.setMessage(errorInfo);
            recordService.update(record);
            logger.error(errorInfo, e);
            success = false;
            return false;
        } finally {
            //流程任务的重跑靠自身维护...
            if (!success) {
                Record red = recordService.get(record.getRecordId());
                if (job.getRedo() == 1 && job.getRunCount() > 0) {
                    int index = 0;
                    boolean flag;
                    do {
                        flag = reExecuteJob(red, job, JobType.FLOW);
                        ++index;
                    } while (!flag && index < job.getRunCount());

                    //重跑到截止次数还是失败,则发送通知,记录最终运行结果
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

    private boolean executeSingletonJob(JobVo job, Long userId) {

        if (!checkJobPermission(job.getAgentId(),userId))return false;

        Record record = new Record(job);
        record.setJobType(JobType.SINGLETON.getCode());//单一任务
        //执行前先保存
        record = recordService.save(record);

        try {
            //执行前先检测一次通信是否正常
            checkPing(job, record);
            Response response = responseToRecord(job, record);
            recordService.update(record);
            if (!response.isSuccess()) {
                //当前的单一任务只运行一次未设置重跑.
                if (job.getRedo()==0 || job.getRunCount()==0) {
                    noticeService.notice(job);
                }
                logger.info("execute failed:jobName:{},jobId:{},ip:{},port:{},info:", job.getJobName(), job.getJobId(), job.getIp(), job.getPort(), record.getMessage());
                return false;
            }else {
                logger.info("execute successful:jobName:{},jobId:{},ip:{},port:", job.getJobName(), job.getJobId(), job.getIp(), job.getPort());
            }
        } catch (Exception e) {
            if (job.getRedo()==0 || job.getRunCount()==0) {
                noticeService.notice(job);
            }
            String errorInfo = String.format("execute failed:jobName:%s,jobId:%d,ip:%s,port:%d,info:%s", job.getJobName(), job.getJobId(), job.getIp(), job.getPort(), e.getMessage());
            logger.error(errorInfo, e);
        }

        return record.getSuccess().equals(ResultStatus.SUCCESSFUL.getStatus());
    }

    public boolean reExecuteJob(final Record parentRecord, JobVo job, JobType jobType) {

        if (parentRecord.getRedoCount().equals(reExecuteThreadMap.get(parentRecord.getRecordId()))){
            return false;
        }else {
            reExecuteThreadMap.put(parentRecord.getRecordId(),parentRecord.getRedoCount());
        }

        parentRecord.setStatus(RunStatus.RERUNNING.getStatus());

        recordService.update(parentRecord);
        /**
         * 当前重新执行的新纪录
         */
        job.setExecType(ExecType.RERUN.getStatus());
        Record record = new Record(job);
        record.setParentId(parentRecord.getRecordId());
        record.setGroupId(parentRecord.getGroupId());
        record.setJobType(jobType.getCode());
        parentRecord.setRedoCount(parentRecord.getRedoCount() + 1);//运行次数
        record.setRedoCount(parentRecord.getRedoCount());
        record = recordService.save(record);

        try {
            //执行前先检测一次通信是否正常
            checkPing(job, record);

            Response result = responseToRecord(job, record);

            //当前重跑任务成功,则父记录执行完毕
            if (result.isSuccess()) {
                parentRecord.setStatus(RunStatus.RERUNDONE.getStatus());
                //重跑的某一个子任务被Kill,则整个重跑计划结束
            } else if (StatusCode.KILL.getValue().equals(result.getExitCode())) {
                parentRecord.setStatus(RunStatus.RERUNDONE.getStatus());
            } else {
                //已经重跑到最后一次了,还是失败了,则认为整个重跑任务失败,发送通知
                if (job.getRunCount().equals(parentRecord.getRedoCount())) {
                    noticeService.notice(job);
                }
                parentRecord.setStatus(RunStatus.RERUNUNDONE.getStatus());
            }
            logger.info("execute successful:jobName:{},jobId:{},ip:{},port:{}", job.getJobName(), job.getJobId(), job.getIp(), job.getPort());
        } catch (Exception e) {
            noticeService.notice(job);
            String errorInfo = String.format("execute failed:jobName:%s,jobId:%d,ip:%s,port:%d,info:%s", job.getJobName(), job.getJobId(), job.getIp(), job.getPort(), e.getMessage());
            errorExec(record, errorInfo);
            logger.error(errorInfo, e);
        } finally {
            //如果已经到了任务重跑的截至次数直接更新为已重跑完成
            if (job.getRunCount().equals(parentRecord.getRedoCount())) {
                parentRecord.setStatus(RunStatus.RERUNDONE.getStatus());
            }
            recordService.save(record);
            recordService.update(parentRecord);
        }

        return record.getSuccess().equals(ResultStatus.SUCCESSFUL.getStatus());
    }

    public void killJob(Record record) {

        final Queue<Record> recordQueue = new LinkedBlockingQueue<Record>();

        //单一任务
        if (JobType.SINGLETON.getCode().equals(record.getJobType())) {
            recordQueue.add(record);
        } else if (JobType.FLOW.getCode().equals(record.getJobType())) {
            //流程任务
            recordQueue.addAll(recordService.getRunningFlowJob(record.getRecordId()));
        }

        Thread jobThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (final Record cord : recordQueue) {
                    //如果kill并行任务(则启动多线程,所有任务同时kill)
                    Thread thread = new Thread(new Runnable() {
                        public void run() {
                            /**
                             * 临时的改成停止中...
                             */
                            cord.setStatus(RunStatus.STOPPING.getStatus());//停止中
                            cord.setSuccess(ResultStatus.KILLED.getStatus());//被杀.
                            recordService.update(cord);

                            JobVo job = jobService.getJobVoById(cord.getJobId());
                            try {
                                /**
                                 * 向远程机器发送kill指令
                                 */
                                cronJobCaller.call(Request.request(job.getIp(), job.getPort(), Action.KILL, job.getPassword()).putParam("pid", cord.getPid()), job.getAgent());
                                cord.setStatus(RunStatus.STOPED.getStatus());
                                cord.setEndTime(new Date());
                                recordService.update(cord);
                                logger.info("killed successful :jobName:{},ip:{},port:{},pid:{}", job.getJobName(), job.getIp(), job.getPort(), cord.getPid());
                            } catch (Exception e) {
                                noticeService.notice(job);
                                String errorInfo = String.format("killed error:jobName:%s,ip:%d,port:%d,pid:%d,failed info:%s", job.getJobName(), job.getIp(), job.getPort(), cord.getPid(), e.getMessage());
                                logger.error(errorInfo, e);
                            }
                        }
                    });
                    thread.start();
                }
            }
        });
        jobThread.start();
    }

    public boolean ping(Agent agent) {
        boolean ping = false;
        try {
            ping = cronJobCaller.call(Request.request(agent.getIp(), agent.getPort(), Action.PING, agent.getPassword()),agent).isSuccess();
        } catch (Exception e) {
            logger.error("[redrain]ping failed,host:{},port:{}", agent.getIp(), agent.getPort());
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
    public boolean password(Agent agent, String ip, int port, final String password, final String newPassword) {
        boolean ping = false;
        try {
            Response response = cronJobCaller.call(Request.request(ip, port, Action.PASSWORD, password).putParam("newPassword", newPassword),agent);
            ping = response.isSuccess();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return ping;
        }
    }

    private Response responseToRecord(final JobVo job, final Record record) throws Exception {
        Response response = cronJobCaller.call(Request.request(job.getIp(), job.getPort(), Action.EXECUTE, job.getPassword()).putParam("command", job.getCommand()).putParam("pid", record.getPid()).putParam("timeout",job.getTimeout()+"") , job.getAgent());
        logger.info("[redrain]:execute response:{}", response.toString());
        record.setReturnCode(response.getExitCode());
        record.setMessage(response.getMessage());

        record.setSuccess(response.isSuccess() ? ResultStatus.SUCCESSFUL.getStatus() : ResultStatus.FAILED.getStatus());
        if (StatusCode.KILL.getValue().equals(response.getExitCode())) {
            record.setStatus(RunStatus.STOPED.getStatus());
            record.setSuccess(ResultStatus.KILLED.getStatus());//被kill任务失败
        }else if(StatusCode.TIME_OUT.getValue().equals(response.getExitCode())){
            record.setStatus(RunStatus.STOPED.getStatus());
            record.setSuccess(ResultStatus.TIMEOUT.getStatus());//超时...
        }else {
            record.setStatus(RunStatus.DONE.getStatus());
        }

        record.setStartTime(new Date(response.getStartTime()));
        record.setEndTime(new Date(response.getEndTime()));
        return response;
    }

    private void checkPing(JobVo job, Record record) throws Exception {
        if (!ping(job.getAgent())) {
            record.setStatus(RunStatus.DONE.getStatus());//已完成
            record.setReturnCode(StatusCode.ERROR_PING.getValue());

            String format = "can't to communicate with agent:%s(%s:%d),execute job:%s failed";
            String content = String.format(format, job.getAgentName(), job.getIp(), job.getPort(), job.getJobName());

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

    public Response monitor(Agent agent) throws Exception {
        return cronJobCaller.call(
                Request.request(agent.getIp(), agent.getPort(), Action.MONITOR, agent.getPassword()).setParams( ParamsMap.instance().fill("connType",ConnType.getByType(agent.getProxy()).getName()) ),
                agent
        );
    }

    public void batchExecuteJob(final Long operateId, String command, String agentIds) {
        final Queue<JobVo> jobQueue = new LinkedBlockingQueue<JobVo>();

        String[] arrayIds = agentIds.split(";");

        for (String agentId:arrayIds) {
            Agent agent = agentService.getAgent(Long.parseLong(agentId));
            JobVo jobVo = new JobVo();
            jobVo.setJobName(agent.getName()+"-batchJob");
            jobVo.setJobId(0L);
            jobVo.setOperateId(operateId);
            jobVo.setCommand(command);
            jobVo.setExecType(ExecType.BATCH.getStatus());
            jobVo.setAgent(agent);
            jobVo.setAgentId(agent.getAgentId());
            jobVo.setIp(agent.getIp());
            jobVo.setPort(agent.getPort());
            jobVo.setPassword(agent.getPassword());
            jobQueue.add(jobVo);
        }

        Thread jobThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (final JobVo jobVo : jobQueue) {
                    //如果批量现场执行(则启动多线程,所有任务同时执行)
                    Thread thread = new Thread(new Runnable() {
                        public void run() {
                            executeSingletonJob(jobVo,operateId);
                        }
                    });
                    thread.start();
                }
            }
        });
        jobThread.start();
    }

    private boolean checkJobPermission(Long jobAgentId, Long userId){
        if (userId==null) return false;
        User user = userService.getUserById(userId);
        //超级管理员拥有所有执行器的权限
        if (user!=null&&user.getRoleId()==999) return true;
        String agentIds = userService.getUserById(userId).getAgentIds();
        agentIds = ","+agentIds+",";
        String thisAgentId = ","+jobAgentId+",";
        return agentIds.contains(thisAgentId);
    }
}
