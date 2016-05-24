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

import java.util.Collections;
import java.util.List;

import org.jcronjob.base.job.CronJob;
import org.jcronjob.dao.QueryDao;
import org.jcronjob.session.MemcacheCache;
import org.jcronjob.tag.Page;

import static org.jcronjob.base.job.CronJob.*;

import org.jcronjob.base.utils.CommonUtils;
import org.jcronjob.domain.Job;
import org.jcronjob.vo.JobVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;

import static org.jcronjob.base.utils.CommonUtils.notEmpty;

@Service
public class JobService {

    @Autowired
    private QueryDao queryDao;

    @Autowired
    private WorkerService workerService;

    @Autowired
    private MemcacheCache memcacheCache;

    @Autowired
    private SchedulerService schedulerService;

    private final String CRONTAB_KEY = "CRONJOB_CRONTAB";

    /**
     * 获取将要执行的任务
     * @return
     */
    public List<JobVo> getJob(ExecType execType, CronType cronType) {
        String sql = "SELECT t.*,d.name AS workerName,d.port,d.ip,d.password,d.warning FROM job t LEFT JOIN worker d ON t.workerId = d.workerId WHERE IFNULL(t.flowNum,0)=0 AND cronType=? AND execType = ? AND t.status=1";
        List<JobVo> jobs = queryDao.sqlQuery(JobVo.class, sql, cronType.getType(), execType.getStatus());
        if (CommonUtils.notEmpty(jobs)) {
            for (JobVo job : jobs) {
                job.setWorker(workerService.getWorker(job.getWorkerId()));
                job.setChildren(queryChildren(job));
            }
        }
        return jobs;
    }

    public List<Job> getJobsByCategory(JobCategory category){
        String sql = "SELECT * FROM job WHERE status=1 AND category=?";
        if (JobCategory.FLOW.equals(category)) {
            sql +=" AND flownum=0";
        }
        return queryDao.sqlQuery(Job.class,sql,category.getCode());
    }

    public void cleanCrontabJob() {
        this.memcacheCache.evict(this.CRONTAB_KEY);
    }

    //任务修改后同步..
    public void syncCrontabJob() {
        this.cleanCrontabJob();
        memcacheCache.put(this.CRONTAB_KEY, getJob(CronJob.ExecType.AUTO, CronJob.CronType.CRONTAB));
    }

    public List<JobVo> getCrontabJob() {
        return memcacheCache.get(this.CRONTAB_KEY, List.class);
    }

    public Page<JobVo> getJobs(HttpSession session, Page page, JobVo job) {
        String sql = "SELECT t.*,d.name AS workerName,d.port,d.ip,d.password,u.userName AS operateUname " +
                " FROM job AS t LEFT JOIN worker AS d ON t.workerId = d.workerId LEFT JOIN user as u ON t.operateId = u.userId WHERE IFNULL(flowNum,0)=0 AND t.status=1 ";
        if (job != null) {
            if (notEmpty(job.getWorkerId())) {
                sql += " AND t.workerId=" + job.getWorkerId();
            }
            if (notEmpty(job.getExecType())) {
                sql += " AND t.execType=" + job.getExecType();
            }
            if (notEmpty(job.getRedo())) {
                sql += " AND t.redo=" + job.getRedo();
            }
            if (!(Boolean) session.getAttribute("permission")) {
                sql += " AND t.operateId = " + session.getAttribute("userId");
            }
        }
        page = queryDao.getPageBySql(page, JobVo.class, sql);
        List<JobVo> parentJobs = page.getResult();

        for (JobVo parentJob : parentJobs) {
            queryChildren(parentJob);
        }
        page.setResult(parentJobs);
        return page;
    }

    private List<JobVo> queryChildren(JobVo job) {
        if (job.getCategory() == 1) {
            String sql = "SELECT t.*,d.name AS workerName,d.port,d.ip,d.password,u.userName AS operateUname" +
                    " FROM job AS t LEFT JOIN worker AS d ON t.workerId = d.workerId LEFT JOIN user AS u " +
                    " ON t.operateId = u.userId WHERE t.flowId = ? AND t.flowNum>0 ORDER BY t.flowNum ASC";
            List<JobVo> childJobs = queryDao.sqlQuery(JobVo.class, sql, job.getFlowId());
            job.setChildren(childJobs);
            return childJobs;
        }
        return Collections.emptyList();
    }

    public Job addOrUpdate(Job job) {
        Job savedJob = (Job) queryDao.save(job);
        schedulerService.startCrontab();
        return savedJob;
    }

    public Job queryJobById(Long id) {
        return queryDao.get(Job.class, id);
    }

    public JobVo getJobById(Long id) {
        String sql = "SELECT t.*,d.name AS workerName,d.port,d.ip,d.password,u.userName AS operateUname " +
                " FROM job AS t LEFT JOIN worker AS d ON t.workerId = d.workerId LEFT JOIN user AS u ON t.operateId = u.userId WHERE t.jobId =?";
        JobVo job = queryDao.sqlUniqueQuery(JobVo.class, sql, id);
        if (job.getCategory() == 1) {
            sql = "SELECT t.*,d.name AS workerName,d.port,d.ip,d.password,u.userName AS operateUname" +
                    " FROM job AS t LEFT JOIN worker AS d ON t.workerId = d.workerId LEFT JOIN user AS u " +
                    " ON t.operateId = u.userId WHERE t.flowId = ? AND t.flowNum>0 ORDER BY t.flowNum ASC";
            List<JobVo> childJobs = queryDao.sqlQuery(JobVo.class, sql, job.getFlowId());
            job.setChildren(childJobs);
        }
        return job;
    }

    public List<Job> getAll() {
        return queryDao.getAll(Job.class);
    }

    public List<JobVo> getJobByWorkerId(Long workerId) {
        String sql = "SELECT t.*,d.name AS workerName,d.port,d.ip,d.password,u.userName AS operateUname " +
                " FROM job t LEFT JOIN user u ON t.operateId = u.userId LEFT JOIN worker d ON t.workerId = d.workerId WHERE t.workerId =?";
        return queryDao.sqlQuery(JobVo.class, sql, workerId);
    }

    public String checkName(Long id, String name) {
        String sql = "SELECT COUNT(1) FROM job WHERE status=1 AND jobName=? ";
        if (notEmpty(id)) {
            sql += " AND jobId != " + id + " AND flowId != " + id;
        }
        return (queryDao.getCountBySql(sql, name)) > 0L ? "no" : "yes";
    }

    @Transactional(readOnly = false)
    public void dealOldFlowJob(Long deleteId) {
        Boolean exist = queryDao.getCountBySql("SELECT COUNT(1) FROM record r LEFT JOIN job t ON r.jobid = t.jobid WHERE t.flowid = ?", deleteId) > 0L;
        if (exist) {
            queryDao.createSQLQuery("UPDATE job SET status = 0 WHERE flowid = " + deleteId).executeUpdate();
        } else {
            queryDao.createSQLQuery("DELETE FROM job WHERE flowid = " + deleteId).executeUpdate();
        }
        schedulerService.startCrontab();
    }


}
