/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.jredrain.startup;

/**
 * Created by benjobs on 16/3/3.
 */

import org.apache.commons.cli.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import com.jredrain.base.job.RedRain;
import com.jredrain.base.utils.IOUtils;
import com.jredrain.base.utils.LoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.InvocationTargetException;

import static com.jredrain.base.utils.CommonUtils.isEmpty;
import static com.jredrain.base.utils.CommonUtils.notEmpty;

public class Bootstrap implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static Bootstrap daemon ;

    private TServer server;

    /**
     * agent port
     */
    private int port;

    /**
     * agent password
     */
    private String password;

    private final String CHARSET = "UTF-8";

    private static Logger logger = LoggerFactory.getLogger(Bootstrap.class);


    public static void main(String[] args) {
        if (daemon == null) {
            daemon = new Bootstrap();
        }
        try {
            if (isEmpty(args)) {
                logger.warn("Bootstrap: error,usage start|stop");
            }else {
                String command = args[0];
                if ("start".equals(command)) {
                    daemon.init();
                    daemon.start();
                }else if("stop".equals(command)){
                    daemon.stop();
                }else {
                    logger.warn("Bootstrap: command \"" + command + "\" does not exist.");
                }
            }
        } catch (Throwable t) {
            if (t instanceof InvocationTargetException && t.getCause() != null) {
                t = t.getCause();
            }
            handleThrowable(t);
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * init start........
     * @throws Exception
     */
    public void init() throws Exception {

        port = Integer.valueOf(Integer.parseInt( Globals.REDRAIN_PORT ));
        String inputPwd = Globals.REDRAIN_PASSWORD;

        File passfile = new File(System.getProperty(Globals.REDRAIN_HOME));
        if (!passfile.exists()) {
            passfile.mkdirs();
        }

        passfile = Globals.REDRAIN_PASSWORD_FILE;

        if (notEmpty(inputPwd)) {
            this.password = DigestUtils.md5Hex(inputPwd).toLowerCase();
            passfile.delete();
            IOUtils.writeFile(passfile, this.password, CHARSET);
        } else {
            if (!passfile.exists()) {
                this.password = DigestUtils.md5Hex(this.password).toLowerCase();
                IOUtils.writeFile(passfile, this.password, CHARSET);
            } else {
                password = IOUtils.readFile(passfile, CHARSET).trim().toLowerCase();
            }
        }
    }

    public void start(int port, String password) throws Exception {
        this.port = port;
        this.password = DigestUtils.md5Hex(password).toLowerCase();
        start();
    }

    public void start() throws Exception {
        try {
            TServerSocket serverTransport = new TServerSocket(port);
            AgentProcessor agentProcessor = new AgentProcessor(password,port);
            RedRain.Processor processor = new RedRain.Processor(agentProcessor);
            TBinaryProtocol.Factory protFactory = new TBinaryProtocol.Factory(true, true);
            TThreadPoolServer.Args arg = new TThreadPoolServer.Args(serverTransport);
            arg.protocolFactory(protFactory);
            arg.processor(processor);
            this.server = new TThreadPoolServer(arg);
            logger.info("[redrain]agent started @ port:{},pid:{}",port,getPid());
            /**
             * 写入pid文件
             */
            IOUtils.writeFile(Globals.REDRAIN_PID_FILE,getPid()+"",CHARSET);

            this.server.serve();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopServer() {
        if (this.server != null && this.server.isServing()) {
            this.server.stop();
            /**
             * 删除Pid文件
             */
            Globals.REDRAIN_PID_FILE.deleteOnExit();
        }
    }

    public void destroy() {
        stopServer();
    }

    public void stop() throws Exception {
        stopServer();
    }

    private static void handleThrowable(Throwable t) {
        if (t instanceof ThreadDeath) {
            throw (ThreadDeath) t;
        }
        if (t instanceof VirtualMachineError) {
            throw (VirtualMachineError) t;
        }
    }

    private static int getPid() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String name = runtime.getName();
        try {
            return Integer.parseInt(name.substring(0, name.indexOf('@')));
        } catch (Exception e) {
        }
        return -1;
    }

}

