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


package com.jcronjob.agent;

import com.alibaba.fastjson.JSON;
import com.jcronjob.common.job.*;
import org.apache.commons.exec.*;
import org.apache.thrift.TException;
import com.jcronjob.common.utils.*;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.reflect.Method;
import java.util.*;

import static com.jcronjob.common.utils.CommonUtils.*;

/**
 * Created by benjo on 2016/3/25.
 */
public class AgentProcessor implements Cronjob.Iface {

    private Logger logger = LoggerFactory.getLogger(AgentProcessor.class);

    private String password;

    private Integer agentPort;

    private Integer socketPort;

    private final String EXITCODE_KEY = "exitCode";

    private final String EXITCODE_SCRIPT = String.format(" \n echo %s:$?", EXITCODE_KEY);

    private final String REPLACE_REX = "%s:\\sline\\s[0-9]+:";

    private AgentMonitor agentMonitor;

    public AgentProcessor(String password, Integer agentPort) {
        this.password = password;
        this.agentPort = agentPort;
    }

    @Override
    public Response ping(Request request) throws TException {
        if (!this.password.equalsIgnoreCase(request.getPassword())) {
            return errorPasswordResponse(request);
        }
        return Response.response(request).setSuccess(true).setExitCode(Cronjob.StatusCode.SUCCESS_EXIT.getValue()).end();
    }

    @Override
    public Response monitor(Request request) throws TException {
        Cronjob.ConnType connType = Cronjob.ConnType.getByName(request.getParams().get("connType"));
        Response response = Response.response(request);
        Map<String,String> map = new HashMap<String, String>(0);

        if (agentMonitor==null) {
            agentMonitor = new AgentMonitor();
        }

        switch (connType) {
            case CONN:
                if (  CommonUtils.isEmpty(agentMonitor,socketPort) || agentMonitor.stoped() ) {
                    //选举一个空闲可用的port
                    this.socketPort = HttpUtils.freePort();
                    try {
                        agentMonitor.start(socketPort);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                logger.debug("[cronjob]:getMonitorPort @:{}", socketPort);
                map.put("port",this.socketPort.toString());
                response.setResult(map);
                return response;
            case PROXY:
                Monitor monitor = agentMonitor.monitor();
                map = serializableToMap(monitor);
                response.setResult(map);
                return response;
            default:
                return null;
        }
    }

    @Override
    public Response proxy(Request request) throws TException {
        String proxyHost = request.getParams().get("proxyHost");
        String proxyPort = request.getParams().get("proxyPort");
        String proxyAction = request.getParams().get("proxyAction");
        String proxyPassword = request.getParams().get("proxyPassword");

        //其他参数....
        String proxyParams = request.getParams().get("proxyParams");
        Map<String,String> params = new HashMap<String, String>(0);
        if (CommonUtils.notEmpty(proxyParams)) {
            params = (Map<String, String>) JSON.parse(proxyParams);
        }

        Request proxyReq = Request.request(proxyHost,toInt(proxyPort), Action.findByName(proxyAction),proxyPassword).setParams(params);

        logger.info("[cronjob]proxy params:{}",proxyReq.toString());

        TTransport transport = null;
        /**
         * ping的超时设置为5毫秒,其他默认
         */
        if (proxyReq.getAction().equals(Action.PING)) {
            transport = new TSocket(proxyReq.getHostName(),proxyReq.getPort(),1000*5);
        }else {
            transport = new TSocket(proxyReq.getHostName(),proxyReq.getPort());
        }
        TProtocol protocol = new TBinaryProtocol(transport);
        Cronjob.Client client = new Cronjob.Client(protocol);
        transport.open();

        Response response = null;
        for(Method method:client.getClass().getMethods()){
            if (method.getName().equalsIgnoreCase(proxyReq.getAction().name())) {
                try {
                    response = (Response) method.invoke(client, proxyReq);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;
            }
        }
        transport.flush();
        transport.close();
        return response;
    }

    @Override
    public Response execute(final Request request) throws TException {
        if (!this.password.equalsIgnoreCase(request.getPassword())) {
            return errorPasswordResponse(request);
        }

        String command = request.getParams().get("command") + EXITCODE_SCRIPT;

        String pid = request.getParams().get("pid");
        //以分钟为单位
        Long timeout = CommonUtils.toLong(request.getParams().get("timeout"),0L);

        boolean timeoutFlag = timeout>0;

        logger.info("[cronjob]:execute:{},pid:{}", command, pid);

        File shellFile = CommandUtils.createShellFile(command, pid);

        Integer exitValue;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        final Response response = Response.response(request);

        final ExecuteWatchdog watchdog = new ExecuteWatchdog(Integer.MAX_VALUE);

        final Timer timer = new Timer();

        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

        try {
            CommandLine commandLine = CommandLine.parse("/bin/bash +x " + shellFile.getAbsolutePath());
            final DefaultExecutor executor = new DefaultExecutor();

            ExecuteStreamHandler stream = new PumpStreamHandler(outputStream, outputStream);
            executor.setStreamHandler(stream);
            response.setStartTime(new Date().getTime());
            //成功执行完毕时退出值为0,shell标准的退出
            executor.setExitValue(0);

            if (timeoutFlag) {
                //设置监控狗...
                executor.setWatchdog(watchdog);
                //监控超时的计时器
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        //超时,kill...
                        if (watchdog.isWatching()) {
                            /**
                             * 调用watchdog的destroyProcess无法真正kill进程...
                             * watchdog.destroyProcess();
                             */
                            timer.cancel();
                            watchdog.stop();
                            //call  kill...
                            request.setAction(Action.KILL);
                            try {
                                kill(request);
                                response.setExitCode(Cronjob.StatusCode.TIME_OUT.getValue());
                            } catch (TException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                },timeout*60*1000);

                //正常执行完毕则清除计时器
                resultHandler = new DefaultExecuteResultHandler(){
                    @Override
                    public void onProcessComplete(int exitValue) {
                        super.onProcessComplete(exitValue);
                        timer.cancel();
                    }

                    @Override
                    public void onProcessFailed(ExecuteException e) {
                        super.onProcessFailed(e);
                        timer.cancel();
                    }
                };
            }

            executor.execute(commandLine, resultHandler);

            resultHandler.waitFor();

        } catch (Exception e) {
            if (e instanceof ExecuteException) {
                exitValue = ((ExecuteException) e).getExitValue();
            } else {
                exitValue = Cronjob.StatusCode.ERROR_EXEC.getValue();
            }
            if (Cronjob.StatusCode.KILL.getValue().equals(exitValue)) {
                if(timeoutFlag) {
                    timer.cancel();
                    watchdog.stop();
                }
                logger.info("[cronjob]:job has be killed!at pid :{}", request.getParams().get("pid"));
            } else {
                logger.info("[cronjob]:job execute error:{}", e.getCause().getMessage());
            }
        } finally {

            exitValue = resultHandler.getExitValue();

            if ( CommonUtils.notEmpty(outputStream.toByteArray()) ) {
                try {
                    outputStream.flush();
                    String text = outputStream.toString();
                    if (notEmpty(text)) {
                        try {
                            text = text.replaceAll(String.format(REPLACE_REX,shellFile.getAbsolutePath()),"");
                            response.setMessage(text.substring(0, text.lastIndexOf(EXITCODE_KEY)));
                            exitValue = Integer.parseInt(text.substring(text.lastIndexOf(EXITCODE_KEY) + EXITCODE_KEY.length() + 1).trim());
                        } catch (IndexOutOfBoundsException e) {
                            response.setMessage(text);
                        }
                    }
                    outputStream.close();
                } catch (Exception e) {
                    logger.error("[cronjob]:error:{}", e);
                }
            }

            if ( Cronjob.StatusCode.TIME_OUT.getValue() == response.getExitCode() ) {
                response.setSuccess(false).end();
            }else {
                response.setExitCode(exitValue).setSuccess(response.getExitCode() == Cronjob.StatusCode.SUCCESS_EXIT.getValue()).end();
            }

            if (shellFile != null) {
                shellFile.delete();//删除文件
            }
        }
        logger.info("[cronjob]:execute result:{}", response.toString());
        watchdog.stop();

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
            return response.setSuccess(false).setExitCode(Cronjob.StatusCode.SUCCESS_EXIT.getValue()).setMessage("密码不能为空").end();
        }
        this.password = newPassword.toLowerCase().trim();

        IOUtils.writeText(Globals.CRONJOB_PASSWORD_FILE, this.password, "UTF-8");

        return response.setSuccess(true).setExitCode(Cronjob.StatusCode.SUCCESS_EXIT.getValue()).end();
    }

    @Override
    public Response kill(Request request) throws TException {

        if (!this.password.equalsIgnoreCase(request.getPassword())) {
            return errorPasswordResponse(request);
        }

        String pid = request.getParams().get("pid");
        logger.info("[cronjob]:kill pid:{}", pid);

        Response response = Response.response(request);
        String text = CommandUtils.executeShell(Globals.CRONJOB_KILL_SHELL, pid, EXITCODE_SCRIPT);
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

        response.setExitCode(Cronjob.StatusCode.ERROR_EXIT.getValue().equals(exitVal) ? Cronjob.StatusCode.ERROR_EXIT.getValue() : Cronjob.StatusCode.SUCCESS_EXIT.getValue())
                .setMessage(message)
                .end();

        logger.info("[cronjob]:kill result:{}" + response);
        return response;
    }

    private Response errorPasswordResponse(Request request) {
        return Response.response(request)
                .setSuccess(false)
                .setExitCode(Cronjob.StatusCode.ERROR_PASSWORD.getValue())
                .setMessage(Cronjob.StatusCode.ERROR_PASSWORD.getDescription())
                .end();
    }

    private Map<String, String> serializableToMap(Object obj) {
        if (isEmpty(obj)) {
            return Collections.EMPTY_MAP;
        }

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
