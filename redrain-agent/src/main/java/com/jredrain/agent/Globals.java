package com.jredrain.agent;


import java.io.File;

public final class Globals {

    /**
     * Default domain for MBeans if none can be determined
     */
    public static final String DEFAULT_MBEAN_DOMAIN = "Redrain";


    /**
     * Name of the system property containing
     */
    public static final String REDRAIN_BASE = "redrain.base";

    /**
     * Name of the system property containing
     */
    public static final String REDRAIN_HOME = "redrain.home";

    /**
     * password file
     */
    public static File REDRAIN_PASSWORD_FILE = new File(System.getProperty(REDRAIN_HOME) + File.separator + ".password");


    /**
     * monitor file
     */
    public static File REDRAIN_MONITOR_SHELL = new File(System.getProperty(REDRAIN_HOME) + "/bin/monitor.sh");

    /**
     * kill file
     */
    public static File REDRAIN_KILL_SHELL = new File(System.getProperty(REDRAIN_HOME) + "/bin/kill.sh");



}
