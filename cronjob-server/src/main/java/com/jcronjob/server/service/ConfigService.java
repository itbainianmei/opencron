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

import com.jcronjob.server.dao.QueryDao;
import com.jcronjob.server.domain.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by ChenHui on 2016/2/17.
 */
@Service
public class ConfigService {

    @Autowired
    private QueryDao queryDao;

    public Config getSysConfig() {
        return queryDao.sqlUniqueQuery(Config.class, "SELECT * FROM T_CONFIG WHERE configId = 1");
    }

    public void update(Config config) {
        queryDao.save(config);
    }

}
