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

import it.sauronsoftware.cron4j.*;
import org.jcronjob.base.job.CronJob;
import org.jcronjob.service.ExecuteService;
import org.jcronjob.service.JobService;
import org.jcronjob.vo.JobVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by benjobs on 16/3/28.
 */
@Component
public class CronJobCollector implements TaskCollector {

    @Autowired
    private JobService jobService;

    @Autowired
    private ExecuteService executeService;

    @Override
    public TaskTable getTasks() {
        TaskTable table = new TaskTable();
        List<JobVo> jobs = jobService.getCrontabJob();
        for (final JobVo job : jobs) {
            table.add(new SchedulingPattern(job.getCronExp()), new Task() {
                @Override
                public void execute(TaskExecutionContext context) throws RuntimeException {
                    executeService.executeJob(job, CronJob.ExecType.AUTO);
                }
            });
        }
        return table;
    }
}
