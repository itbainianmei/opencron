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

package com.jredrain.agent;

/**
 * Created by benjobs on 16/3/3.
 */

import org.apache.commons.cli.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
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

import static com.jredrain.base.utils.CommonUtils.notEmpty;

public class AgentBootstrap implements Daemon,Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	

	private TServer server;

    private int port;

    private String password = "123456";

    private final String CHARSET = "UTF-8";

    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * init start........
     *
     * @param context
     * @throws Exception
     */

    @Override
    public void init(DaemonContext context) throws Exception {
        String[] args = context.getArguments();

        Options options = new Options();
        options.addOption("port", "port", true, "start port");
        options.addOption("pass", "password", true, "input password!");

        CommandLineParser parser = new BasicParser();
        CommandLine cmd = parser.parse(options, args);

        port = Integer.valueOf(Integer.parseInt(cmd.getOptionValue("port").toString()));
        String inputPwd = cmd.getOptionValue("pass");

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

    @Override
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
            this.server.serve();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopServer() {
        if (this.server != null && this.server.isServing()) {
            this.server.stop();
        }
    }

    public void destroy() {
        stopServer();
    }

    public void stop() throws Exception {
        stopServer();
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

