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


package org.jcronjob.agent;

import com.alibaba.fastjson.JSON;
import org.apache.commons.exec.*;
import org.apache.thrift.TException;
import org.jcronjob.base.job.CronJob;
import org.jcronjob.base.job.Request;
import org.jcronjob.base.job.Response;
import org.jcronjob.base.utils.*;
import org.slf4j.Logger;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.util.*;

import static org.jcronjob.base.utils.CommonUtils.isEmpty;
import static org.jcronjob.base.utils.CommonUtils.isPrototype;
import static org.jcronjob.base.utils.CommonUtils.notEmpty;

/**
 * Created by benjo on 2016/3/25.
 */
public class AgentProcessor implements CronJob.Iface {

    private Logger logger = LoggerFactory.getLogger(AgentProcessor.class);

    private String password;

    private Integer agentPort;

    private Integer socketPort;

    private final String EXITCODE_KEY = "exitCode";

    private final String EXITCODE_SCRIPT = String.format(" || echo %s:$?", EXITCODE_KEY);

    private AgentMonitor agentMonitor;

    public AgentProcessor(String password,Integer agentPort) {
        this.password = password;
        this.agentPort = agentPort;
    }

    @Override
    public Response ping(Request request) throws TException {
        if (!this.password.equalsIgnoreCase(request.getPassword())) {
            return errorPasswordResponse(request);
        }
        return Response.response(request).setSuccess(true).setExitCode(CronJob.StatusCode.SUCCESS_EXIT.getValue()).end();
    }

    @Override
    public Response port(Request request) throws TException {
        if (  CommonUtils.isEmpty(agentMonitor,socketPort) || agentMonitor.stoped() ) {
            agentMonitor = new AgentMonitor();
            //选举一个空闲可用的port
            do {
                this.socketPort = HttpUtils.generagePort();
            }while (this.socketPort == this.agentPort);
            try {
                agentMonitor.start(socketPort);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        logger.debug("[cronjob]:getMonitorPort @:{}", socketPort);

        Response response = Response.response(request);
        Map<String,String> map = new HashMap<String, String>(0);
        map.put("port",this.socketPort.toString());
        response.setResult(map);
        return response;
    }

    @Override
    public Response execute(Request request) throws TException {
        if (!this.password.equalsIgnoreCase(request.getPassword())) {
            return errorPasswordResponse(request);
        }

        String command = request.getParams().get("command") + EXITCODE_SCRIPT;

        String pid = request.getParams().get("pid");

        logger.info("[cronjob]:execute:{},pid:{}", command, pid);

        File shellFile = CommandUtils.createShellFile(command, pid);

        Integer exitValue = 1;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Response response = Response.response(request);
        try {
            CommandLine commandLine = CommandLine.parse("/bin/bash +x " + shellFile.getAbsolutePath());
            DefaultExecutor executor = new DefaultExecutor();

            ExecuteStreamHandler stream = new PumpStreamHandler(outputStream, outputStream);
            executor.setStreamHandler(stream);
            response.setStartTime(new Date().getTime());
            exitValue = executor.execute(commandLine);
            exitValue = exitValue == null ? 0 : exitValue;
        } catch (Exception e) {
            if (e instanceof ExecuteException) {
                exitValue = ((ExecuteException) e).getExitValue();
            } else {
                exitValue = CronJob.StatusCode.ERROR_EXEC.getValue();
            }
            if (exitValue == CronJob.StatusCode.KILL.getValue()) {
                logger.info("[cronjob]:job has be killed!at pid :{}", request.getParams().get("pid"));
            } else {
                logger.info("[cronjob]:job execute error:{}", e.getCause().getMessage());
            }
        } finally {
            if (outputStream != null) {
                String text = outputStream.toString();
                if (notEmpty(text)) {
                    try {
                        response.setMessage(text.substring(0, text.lastIndexOf(EXITCODE_KEY)));
                        response.setExitCode(Integer.parseInt(text.substring(text.lastIndexOf(EXITCODE_KEY) + EXITCODE_KEY.length() + 1).trim()));
                    } catch (IndexOutOfBoundsException e) {
                        response.setMessage(text);
                        response.setExitCode(exitValue);
                    } catch (NumberFormatException e) {
                        response.setExitCode(exitValue);
                    }
                } else {
                    response.setExitCode(exitValue);
                }
                try {
                    outputStream.close();
                } catch (Exception e) {
                    logger.error("[cronjob]:error:{}", e);
                }
            } else {
                response.setExitCode(exitValue);
            }
            response.setSuccess(response.getExitCode() == CronJob.StatusCode.SUCCESS_EXIT.getValue()).end();
            if (shellFile != null) {
                shellFile.delete();//删除文件
            }
        }
        logger.info("[cronjob]:execute result:{}", response.toString());
        return response;
    }

    @Override
    public Response password(Request request) throws TException {
        if (!this.password.equalsIgnoreCase(request.getPassword())) {
            return errorPasswordResponse(request);
        }

        String newPassword = request.getParams().get("newPassword");
        Response response = Response.response(request);

        if (isEmpty(newPassword)) {
            return response.setSuccess(false).setExitCode(CronJob.StatusCode.SUCCESS_EXIT.getValue()).setMessage("密码不能为空").end();
        }
        this.password = newPassword.toLowerCase().trim();

        IOUtils.writeFile(Globals.CRONJOB_PASSWORD_FILE, this.password, "UTF-8");

        return response.setSuccess(true).setExitCode(CronJob.StatusCode.SUCCESS_EXIT.getValue()).end();
    }

    @Override
    public Response kill(Request request) throws TException {

        if (!this.password.equalsIgnoreCase(request.getPassword())) {
            return errorPasswordResponse(request);
        }

        String pid = request.getParams().get("pid");
        logger.info("[cronjob]:kill pid:{}", pid);

        Response response = Response.response(request);
        String text = CommandUtils.executeShell(Globals.CRONJOB_KILL_SHELL, request.getParams().get("pid"), EXITCODE_SCRIPT);
        String message = "";
        Integer exitVal = 0;

        if (notEmpty(text)) {
            try {
                message = text.substring(0, text.lastIndexOf(EXITCODE_KEY));
                exitVal = Integer.parseInt(text.substring(text.lastIndexOf(EXITCODE_KEY) + EXITCODE_KEY.length() + 1).trim());
            } catch (StringIndexOutOfBoundsException e) {
                message = text;
            }
        }

        response.setExitCode(CronJob.StatusCode.ERROR_EXIT.getValue() == exitVal ? CronJob.StatusCode.ERROR_EXIT.getValue() : CronJob.StatusCode.SUCCESS_EXIT.getValue())
                .setMessage(message)
                .end();

        logger.info("[cronjob]:kill result:{}" + response);
        return response;
    }

    private Response errorPasswordResponse(Request request) {
        return Response.response(request)
                .setSuccess(false)
                .setExitCode(CronJob.StatusCode.ERROR_PASSWORD.getValue())
                .setMessage(CronJob.StatusCode.ERROR_PASSWORD.getDescription())
                .end();
    }

    private Map<String, String> serializableToMap(Object obj) {
        if (isEmpty(obj))
            return Collections.EMPTY_MAP;
        Map<String, String> resultMap = new HashMap<String, String>(0);
        // 拿到属性器数组
        try {
            PropertyDescriptor[] pds = Introspector.getBeanInfo(obj.getClass()).getPropertyDescriptors();
            for (int index = 0; pds.length > 1 && index < pds.length; index++) {
                if (Class.class == pds[index].getPropertyType() || pds[index].getReadMethod() == null) {

                    continue;
                }
                Object value = pds[index].getReadMethod().invoke(obj);
                if (notEmpty(value)) {
                    if (isPrototype(pds[index].getPropertyType())//java里的原始类型(去除自己定义类型)
                            || pds[index].getPropertyType().isPrimitive()//基本类型
                            || ReflectUitls.isPrimitivePackageType(pds[index].getPropertyType())
                            || pds[index].getPropertyType() == String.class) {

                        resultMap.put(pds[index].getName(), value.toString());
                    }else {
                        resultMap.put(pds[index].getName(), JSON.toJSONString(value));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultMap;
    }


}
