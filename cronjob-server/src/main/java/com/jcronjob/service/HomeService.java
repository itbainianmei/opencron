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

package com.jcronjob.service;

import com.jcronjob.dao.QueryDao;
import com.jcronjob.common.utils.Digests;
import com.jcronjob.common.utils.Encodes;
import com.jcronjob.domain.Log;
import com.jcronjob.domain.User;
import com.jcronjob.job.Globals;
import com.jcronjob.tag.Page;
import com.jcronjob.vo.LogVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;

import java.util.List;

import static com.jcronjob.common.utils.CommonUtils.notEmpty;

/**
 * Created by ChenHui on 2016/2/17.
 */
@Service
public class HomeService {

    @Autowired
    private QueryDao queryDao;

    public int checkLogin(HttpSession httpSession, String username, String password) {
        //将session置为无效
        if (!httpSession.isNew()) {
            httpSession.invalidate();
            httpSession.removeAttribute(Globals.LOGIN_USER);
        }

        User user = queryDao.hqlUniqueQuery("FROM User WHERE userName = ?", username);
        if (user == null) return 500;

        byte[] salt = Encodes.decodeHex(user.getSalt());
        byte[] hashPassword = Digests.sha1(password.getBytes(), salt, 1024);
        password = Encodes.encodeHex(hashPassword);

        String sql = "SELECT COUNT(1) FROM user WHERE userName=? AND password=?";
        Long count = queryDao.getCountBySql(sql, username, password);

        if (count == 1L) {
            httpSession.setAttribute(Globals.LOGIN_USER, user);
            if (user.getRoleId() == 999L) {
                httpSession.setAttribute(Globals.PERMISSION, true);
            } else {
                httpSession.setAttribute(Globals.PERMISSION, false);
            }
            return 200;
        } else {
            return 500;
        }
    }

    public Page<LogVo> getLog(HttpSession session, Page page, Long agentId, String sendTime) {
        String sql = "SELECT L.*,w.name AS agentName FROM log L LEFT JOIN agent w ON L.agentId = w.agentId WHERE 1=1 ";
        if (notEmpty(agentId)) {
            sql += " AND L.agentId = " + agentId;
        }
        if (notEmpty(sendTime)) {
            sql += " AND L.sendTime like '" + sendTime + "%' ";
        }
        if (!Globals.isPermission(session)) {
            sql += " AND L.receiverId = " + Globals.getUserIdBySession(session);
        }
        sql += " ORDER BY L.sendTime DESC";
        queryDao.getPageBySql(page, LogVo.class, sql);
        return page;
    }

    public List<LogVo> getUnReadMessage(HttpSession session) {
        String sql = "SELECT * FROM log L WHERE isread=0 and type=2 ";
        if (!Globals.isPermission(session)) {
            sql += " and L.receiverId = " + Globals.getUserIdBySession(session);
        }
        sql += " ORDER BY L.sendTime DESC LIMIT 5";
        return queryDao.sqlQuery(LogVo.class,sql);
    }

    public Long getUnReadCount(HttpSession session) {
        String sql = "SELECT count(1) FROM log L WHERE isread=0 and type=2 ";
        if (!Globals.isPermission(session)) {
            sql += " and L.receiverId = " + Globals.getUserIdBySession(session);
        }
        return queryDao.getCountBySql(sql);
    }


    public void saveLog(Log log) {
        queryDao.save(log);
    }

    public Log getLogDetail(Long logId) {
        return queryDao.get(Log.class,logId);
    }

    @Transactional(readOnly = false)
    public void updateAfterRead(Long logId) {
        String sql = "update log set isread = 1 where logId = ?";
        queryDao.createSQLQuery(sql,logId).executeUpdate();
    }



}
