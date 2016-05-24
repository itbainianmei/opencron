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

package org.jcronjob.agent;


import java.io.File;

public final class Globals {

    /**
     * Default domain for MBeans if none can be determined
     */
    public static final String DEFAULT_MBEAN_DOMAIN = "Cronjob";


    /**
     * Name of the system property containing
     */
    public static final String CRONJOB_HOME_PROP = "cronjob.home";


    /**
     * Name of the system property containing
     */
    public static final String CRONJOB_BASE_PROP = "cronjob.base";

    /**
     * password file
     */
    public static File CRONJOB_PASSWORD_FILE = new File(System.getProperty("cronjob.home") + File.separator + ".password");


    /**
     * monitor file
     */
    public static File CRONJOB_MONITOR_SHELL = new File(System.getProperty("cronjob.home") + "/bin/monitor.sh");

    /**
     * kill file
     */
    public static File CRONJOB_KILL_SHELL = new File(System.getProperty("cronjob.home") + "/bin/kill.sh");



}
