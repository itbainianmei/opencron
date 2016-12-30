/**
 * Copyright 2016 benjobs
 * <p/>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.opencron.server.service;

import org.opencron.server.domain.Config;
import org.opencron.server.domain.Log;
import org.opencron.server.domain.User;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.mail.HtmlEmail;
import org.opencron.common.job.Opencron;
import org.opencron.common.utils.CommonUtils;
import org.opencron.common.utils.DateUtils;
import org.opencron.common.utils.HttpUtils;
import org.opencron.server.domain.Agent;
import org.opencron.server.vo.JobVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.*;

import static org.opencron.common.utils.CommonUtils.notEmpty;

/**
 * Created by benjobs on 16/3/18.
 */
@Service
public class NoticeService {


    @Autowired
    private ConfigService configService;

    @Autowired
    private HomeService homeService;

    @Autowired
    private UserService userService;

    private Template template;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @PostConstruct
    public void initConfig() throws Exception {
        Configuration configuration = new Configuration();
        File file = new File(getClass().getClassLoader().getResource("/").getPath().replace("classes","common"));
        configuration.setDirectoryForTemplateLoading(file);
        configuration.setDefaultEncoding("UTF-8");
        this.template = configuration.getTemplate("email.template");
    }

    public void notice(Agent agent) {
        if (!agent.getWarning()) return;
        String content = getMessage(agent, "通信失败,请速速处理!");
        logger.info(content);
        try {
            sendMessage(null,agent.getAgentId(), agent.getEmailAddress(), agent.getMobiles(), content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void notice(JobVo job,String msg) {
        if (!job.getWarning()) return;
        Agent agent = job.getAgent();
        String message = "执行任务:" + job.getCommand() + "(" + job.getCronExp() + ")失败,%s!";
        if (msg==null) {
            message = String.format(message,"");
        }else {
            message = String.format(message,"["+msg+"]");
        }
        String content = getMessage(agent,message);
        logger.info(content);
        try {
            sendMessage(job.getUserId(),agent.getAgentId(), job.getEmailAddress(), job.getMobiles(), content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getMessage(Agent agent, String message) {
        String msgFormat = "[opencron] 机器:%s(%s:%s)%s\n\r\t\t--%s";
        return String.format(msgFormat, agent.getName(), agent.getIp(), agent.getPort(), message, DateUtils.formatFullDate(new Date()));
    }

    public void sendMessage(Long receiverId,Long workId, String emailAddress, String mobiles, String content) {
        Log log = new Log();
        log.setIsread(false);
        log.setAgentId(workId);
        log.setMessage(content);
        //手机号和邮箱都为空则发送站内信
        if ( CommonUtils.isEmpty(emailAddress,mobiles) ) {
            log.setType(Opencron.MsgType.WEBSITE.getValue());
            log.setSendTime(new Date());
            homeService.saveLog(log);
            return;
        }

        /**
         * 发送邮件并且记录发送日志
         */
        boolean emailSuccess = false;
        boolean mobileSuccess = false;

        Config config = configService.getSysConfig();
        try {
            log.setType(Opencron.MsgType.EMAIL.getValue());
            HtmlEmail email = new HtmlEmail();
            email.setCharset("UTF-8");
            email.setHostName(config.getSmtpHost());
            email.setSslSmtpPort(config.getSmtpPort().toString());
            email.setAuthentication(config.getSenderEmail(), config.getPassword());
            email.setFrom(config.getSenderEmail());
            email.setSubject("opencron监控告警");
            email.setHtmlMsg(msgToHtml(receiverId, content));
            email.addTo(emailAddress.split(","));
            email.send();
            emailSuccess = true;
            /**
             * 记录邮件发送记录
             */
            log.setReceiver(emailAddress);
            log.setSendTime(new Date());
            homeService.saveLog(log);
        }catch (Exception e) {
            e.printStackTrace(System.err);
        }

        /**
         * 发送短信并且记录发送日志
         */
        try {

            for (String mobile : mobiles.split(",")) {
                //发送POST请求
                String sendUrl = String.format(config.getSendUrl(), mobile, String.format(config.getTemplate(), content));

                String url = sendUrl.substring(0, sendUrl.indexOf("?"));
                String postData = sendUrl.substring(sendUrl.indexOf("?") + 1);
                String message = HttpUtils.doPost(url, postData, "UTF-8");
                log.setResult(message);
                logger.info(message);
                mobileSuccess = true;
            }
            log.setReceiver(mobiles);
            log.setType(Opencron.MsgType.SMS.getValue());
            log.setSendTime(new Date());
            homeService.saveLog(log);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        /**
         * 短信和邮件都发送失败,则发送站内信
         */
        if( !mobileSuccess && !emailSuccess ){
            log.setType(Opencron.MsgType.WEBSITE.getValue());
            log.setSendTime(new Date());
            homeService.saveLog(log);
        }

    }

    private String msgToHtml(Long receiverId,String content) throws Exception {
        Map root = new HashMap();
        if (receiverId!=null) {
            User user = userService.getUserById(receiverId);
            root.put("receiver", notEmpty(user) ? user.getRealName() : "管理员");
        }else {
            root.put("receiver","管理员");
        }
        root.put("message", content);
        StringWriter writer = new StringWriter();
        template.process(root, writer);
        return writer.toString();
    }


}
