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


package com.jcronjob.server.service;

import java.util.Collections;
import java.util.List;

import com.jcronjob.common.utils.CommonUtils;
import com.jcronjob.server.dao.QueryDao;
import com.jcronjob.server.domain.User;
import com.jcronjob.server.job.CronjobContext;
import com.jcronjob.server.job.Globals;
import com.jcronjob.server.tag.Page;
import org.apache.commons.codec.digest.DigestUtils;
import com.jcronjob.common.job.Cronjob;
import com.jcronjob.server.domain.Agent;
import com.jcronjob.server.vo.JobVo;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

import static com.jcronjob.common.utils.CommonUtils.notEmpty;

@Service
public class AgentService {

    @Autowired
    private QueryDao queryDao;

    @Autowired
    private ExecuteService executeService;

    @Autowired
    private JobService jobService;

    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    private UserService userService;

    public List<Agent> getAgentByConnType(Cronjob.ConnType connType) {
        return queryDao.sqlQuery(Agent.class,"SELECT * FROM T_AGENT WHERE status = 1 AND proxy = "+connType.getType());
    }

    public List<Agent> getAll() {
        List<Agent> agents = CronjobContext.get(Globals.CACHED_AGENT_ID,List.class);
        if (CommonUtils.isEmpty(agents)) {
            flushAgent();
        }
       return CronjobContext.get(Globals.CACHED_AGENT_ID,List.class);
    }

    private void flushAgent(){
        CronjobContext.put(Globals.CACHED_AGENT_ID,queryDao.getAll(Agent.class));
    }

    public List<Agent> getAgentByStatus(int status, HttpSession session){
        String sql = "SELECT * FROM T_AGENT WHERE status=?";
        if (!Globals.isPermission(session)) {
            User user = userService.getUserBySession(session);
            sql += " AND agentId in ("+user.getAgentIds()+")";
        }
        return queryDao.sqlQuery(Agent.class,sql,status);
    }

    public Page getAgent(HttpSession session, Page page) {
        String sql = "SELECT * FROM T_AGENT";
        if (!Globals.isPermission(session)) {
            User user = userService.getUserBySession(session);
            sql += " WHERE agentId IN ("+user.getAgentIds()+")";
        }
        queryDao.getPageBySql(page, Agent.class, sql);
        return page;
    }

    public Agent getAgent(Long id) {
        return queryDao.get(Agent.class, id);
    }

    public void addOrUpdate(Agent agent) {
        /**
         * 修改过agent
         */
        boolean update = false;
        if (agent.getAgentId()!=null) {
            update = true;
        }

        /**
         * fix bug.....
         * 修改了agent要刷新所有在任务队列里对应的作业,
         * 否则一段端口改变了,任务队列里的还是更改前的连接端口,
         * 当作业执行的时候就会连接失败...
         *
         */
        if (update) {
            queryDao.save(agent);
            /**
             * 获取该执行器下所有的自动执行,并且是quartz类型的作业
             */
            List<JobVo> jobVos = jobService.getJobVoByAgentId(agent, Cronjob.ExecType.AUTO, Cronjob.CronType.QUARTZ);
            try {
                schedulerService.put(jobVos,this.executeService);
            } catch (SchedulerException e) {
                /**
                 * 创新任务列表失败,抛出异常,整个事务回滚...
                 */
                throw new RuntimeException(e.getCause());
            }
        }else {
            queryDao.save(agent);
        }

        /**
         * 同步缓存...
         */
        flushAgent();

    }

    public String checkName(Long id, String name) {
        String sql = "SELECT COUNT(1) FROM T_AGENT WHERE name=? ";
        if (notEmpty(id)) {
            sql += " AND agentId != " + id;
        }
        return (queryDao.getCountBySql(sql, name)) > 0L ? "no" : "yes";
    }


    public String checkhost(Long id, String host) {
        String sql = "SELECT COUNT(1) FROM T_AGENT WHERE ip=? ";
        if (notEmpty(id)) {
            sql += " AND agentId != " + id;
        }
        return (queryDao.getCountBySql(sql, host)) > 0L ? "no" : "yes";
    }


    public String editPwd(Long id, String pwd0, String pwd1, String pwd2) {
        Agent agent = this.getAgent(id);
        String password = DigestUtils.md5Hex(pwd0);
        if (password.equals(agent.getPassword())) {
            if (pwd1.equals(pwd2)) {
                pwd1 = DigestUtils.md5Hex(pwd1);
                agent.setPassword(pwd1);
                Boolean flag = executeService.password(agent, pwd1);
                if (flag) {
                    this.addOrUpdate(agent);
                    return "success";
                } else {
                    return "failure";
                }
            } else {
                return "two";
            }
        } else {
            return "one";
        }
    }


    public List<Agent> getAgentsBySession(HttpSession session) {
        String sql = "SELECT * FROM T_AGENT ";
        if (!Globals.isPermission(session)) {
            User user = userService.getUserBySession(session);
            sql += " WHERE agentId IN ("+user.getAgentIds()+")";
        }
        return queryDao.sqlQuery(Agent.class,sql);
    }

    public Agent getByHost(String host) {
        String sql = "SELECT * FROM T_AGENT WHERE ip=?";
        return queryDao.sqlUniqueQuery(Agent.class,sql,host);
    }
}
