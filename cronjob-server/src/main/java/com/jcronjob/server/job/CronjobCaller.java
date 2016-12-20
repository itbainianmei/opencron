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


package com.jcronjob.server.job;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import com.alibaba.fastjson.JSON;
import com.jcronjob.common.utils.CommonUtils;
import com.jcronjob.server.domain.Agent;
import com.jcronjob.server.domain.Terminal;
import com.jcronjob.server.service.AgentService;
import com.jcronjob.server.service.TerminalService;
import com.jcronjob.server.vo.JobVo;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import com.jcronjob.common.job.Action;
import com.jcronjob.common.job.Cronjob;
import com.jcronjob.common.job.Request;
import com.jcronjob.common.job.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * agent CronjobCaller
 *
 * @author  <a href="mailto:benjobs@qq.com">B e n</a>
 * @version 1.0
 * @date 2016-03-27
 */

@Component
public class CronjobCaller {

    @Autowired
    private AgentService agentService;

    @Autowired
    private TerminalService terminalService;

    public Response call(Request request,Agent agent) throws Exception {

        //代理...
        if (agent.getProxy() == Cronjob.ConnType.PROXY.getType()) {
            Map<String,String> proxyParams = new HashMap<String, String>(0);
            proxyParams.put("proxyHost",request.getHostName());
            proxyParams.put("proxyPort",request.getPort()+"");
            proxyParams.put("proxyAction",request.getAction().name());
            proxyParams.put("proxyPassword",request.getPassword());
            if (CommonUtils.notEmpty(request.getParams())) {
                proxyParams.put("proxyParams", JSON.toJSONString(request.getParams()));
            }

            Agent proxyAgent = agentService.getAgent(agent.getProxyAgent());
            request.setHostName(proxyAgent.getIp());
            request.setPort(proxyAgent.getPort());
            request.setAction(Action.PROXY);
            request.setPassword(proxyAgent.getPassword());
            request.setParams(proxyParams);
        }

        TTransport transport;
        /**
         * ping的超时设置为5毫秒,其他默认
         */
        if (request.getAction().equals(Action.PING)) {
            transport = new TSocket(request.getHostName(),request.getPort(),1000*5);
        }else {
            transport = new TSocket(request.getHostName(),request.getPort());
        }
        TProtocol protocol = new TBinaryProtocol(transport);
        Cronjob.Client client = new Cronjob.Client(protocol);
        transport.open();

        Response response = null;
        for(Method method:client.getClass().getMethods()){
            if (method.getName().equalsIgnoreCase(request.getAction().name())) {
                response = (Response) method.invoke(client, request);
                break;
            }
        }

       transport.flush();
       transport.close();
       return response;
   }


    /**
     *
     * 其实可以不用agent端,server直接通过ssh连接目标服务器执行任务...考虑到安全性,已经通过agent端实现的性能监控等等种种原因,还是用server调agent的方式.
     * 方法已经实现,暂时保留.不排除以后彻底移除cronjob-agent
     * @param job
     * @return
     * @throws Exception
     */
    public Response execute(JobVo job) throws Exception {
        Agent agent = job.getAgent();
        Terminal terminal = terminalService.getTerm(job.getOperateId(), agent.getIp());
        if (terminal==null) {
            return null;
        }

        Connection connection = new Connection(terminal.getHost());
        Session session = null;
        try {
            connection.connect();
            boolean isAuthenticated = connection.authenticateWithPassword(terminal.getUserName(), terminal.getPassword());
            if (isAuthenticated == false) {
                throw new IOException("Authentication failed.");
            }
            session = connection.openSession();
            Response response = new Response().start();
            session.execCommand(job.getCommand());

            InputStream stdout = new StreamGobbler(session.getStdout());
            BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));

            StringBuffer stringBuffer = new StringBuffer();
            while (true)  {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }else {
                    line+="\n";
                }
                stringBuffer.append(line);
            }

            int exitStatus = session.getExitStatus();
            response.setMessage(stringBuffer.toString()).setExitCode(exitStatus).setSuccess(exitStatus == 0).end();

            return response;
        } catch (IOException e) {
            e.printStackTrace(System.err);
        } finally {
            if (connection != null) {
                connection.close();
            }
            if (session != null) {
                session.close();
            }
        }
        return null;
    }

}
