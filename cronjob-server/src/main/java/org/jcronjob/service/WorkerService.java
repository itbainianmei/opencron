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

import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.jcronjob.dao.QueryDao;
import org.jcronjob.tag.Page;
import org.jcronjob.domain.Worker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.jcronjob.base.utils.CommonUtils.notEmpty;

@Service
public class WorkerService {

    @Autowired
    private QueryDao queryDao;

    @Autowired
    private ExecuteService executeService;

    public List<Worker> getAll() {
       return queryDao.getAll(Worker.class);
    }

    public List<Worker> getWorkerByStatus(int status){
        String sql = "SELECT * FROM worker WHERE status=?";
        return queryDao.sqlQuery(Worker.class,sql,status);
    }

    public Page getWorker(Page page) {
        String sql = "SELECT * FROM worker";
        queryDao.getPageBySql(page, Worker.class, sql);
        return page;
    }

    public Worker getWorker(Long id) {
        return queryDao.get(Worker.class, id);
    }

    public void addOrUpdate(Worker worker) {
        queryDao.save(worker);
    }

    public String checkName(Long id, String name) {
        String sql = "SELECT COUNT(1) FROM worker WHERE name=? ";
        if (notEmpty(id)) {
            sql += " AND workerId != " + id;
        }
        return (queryDao.getCountBySql(sql, name)) > 0L ? "no" : "yes";
    }

    public String editPwd(Long id, String pwd0, String pwd1, String pwd2) {
        Worker work = this.getWorker(id);
        String password = DigestUtils.md5Hex(pwd0);
        if (password.equals(work.getPassword())) {
            if (pwd1.equals(pwd2)) {
                pwd1 = DigestUtils.md5Hex(pwd1);
                work.setPassword(pwd1);
                Boolean flag = executeService.password(work.getIp(), work.getPort(), password, pwd1);
                if (flag) {
                    this.addOrUpdate(work);
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


}
