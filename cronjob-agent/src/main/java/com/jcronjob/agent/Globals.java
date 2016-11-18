package com.jcronjob.agent;


import java.io.File;

public final class Globals {

    /**
     * Name of the system property containing
     */
    public static final String CRONJOB_HOME = "cronjob.home";

    /**
     * port
     */
    public static String CRONJOB_PORT = System.getProperty("cronjob.port");

    /**
     * password
     */
    public static String CRONJOB_PASSWORD = System.getProperty("cronjob.password");

    /**
     * password file
     */

    public static File CRONJOB_PASSWORD_FILE = new File(System.getProperty(CRONJOB_HOME) + File.separator + ".password");

    /**
     *
     * conf file
     */

    public static File CRONJOB_CONF_FILE = new File(System.getProperty(CRONJOB_HOME) + File.separator + "/conf/cronjob.properties");

    /**
     * pid
     */
    public static File CRONJOB_PID_FILE = new File(System.getProperty(CRONJOB_HOME) + File.separator + "cronjob.pid");


    /**
     * monitor file
     */
    public static File CRONJOB_MONITOR_SHELL = new File(System.getProperty(CRONJOB_HOME) + "/bin/monitor.sh");

    /**
     * kill file
     */
    public static File CRONJOB_KILL_SHELL = new File(System.getProperty(CRONJOB_HOME) + "/bin/kill.sh");

}
