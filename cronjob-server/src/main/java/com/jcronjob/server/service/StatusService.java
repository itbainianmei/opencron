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

import com.jcronjob.server.domain.Terminal;
import com.jcronjob.server.dao.QueryDao;
import com.jcronjob.server.domain.TerminalStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by benjobs on 2016/11/22.
 */

@Service
public class StatusService {

    @Autowired
    private QueryDao queryDao;

    @Transactional(readOnly = false)
    public void delete(Long termId,Long userId){
        String sql = "DELETE FROM T_SSH_STATUS WHERE termId=? AND userId=?";
        queryDao.createSQLQuery(sql,termId,userId).executeUpdate();
    }

    public TerminalStatus query(Long termId, Long userId){
        String sql = "SELECT * FROM T_SSH_STATUS WHERE termId=? AND userId=?";
        return (TerminalStatus) queryDao.createSQLQuery(sql,termId,userId).addEntity(TerminalStatus.class).uniqueResult();
    }

    @Transactional(readOnly = false)
    public void flush(Long termId, Long userId) {
        queryDao.getSession().flush();
        TerminalStatus status = query(termId,userId);
        if (status==null) {
            status = new TerminalStatus();
            status.setTermId(termId);
            status.setUserId(userId);
        }
        status.setStatus(Terminal.INITIAL);
        queryDao.save(status);
    }

    public void update(String status, Terminal term, Long userId) {
        String sql = "UPDATE T_SSH_STATUS SET status=? WHERE termId=? and userId=?";
        queryDao.createSQLQuery(sql,status,term.getId(),userId).executeUpdate();
    }
}
