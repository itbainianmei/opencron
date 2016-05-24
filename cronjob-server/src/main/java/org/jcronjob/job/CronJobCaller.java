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

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.jcronjob.base.job.CronJob;
import org.jcronjob.base.job.Request;
import org.jcronjob.base.job.Response;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;


/**
 *
 * agent CronJobCaller
 *
 * @author  <a href="mailto:benjobs@qq.com">B e n</a>
 * @version 1.0
 * @date 2016-03-27
 */

@Component
public class CronJobCaller {

   public Response call(Request request) throws Exception {
       TTransport transport = new TSocket(request.getHostName(),request.getPort());
       TProtocol protocol = new TBinaryProtocol(transport);
       CronJob.Client client = new CronJob.Client(protocol);
       transport.open();

       Response response = null;
       Method[] methods= client.getClass().getMethods();
       for(Method method:methods){
           if (method.getName().equals(request.getAction())) {
               response = (Response) method.invoke(client,request);
           }
       }
       transport.flush();
       transport.close();
       return response;
   }

}
