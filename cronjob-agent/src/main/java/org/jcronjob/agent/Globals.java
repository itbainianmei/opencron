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
    public static final String CRONJOB_BASE = "cronjob.base";

    /**
     * Name of the system property containing
     */
    public static final String CRONJOB_HOME = "cronjob.home";

    /**
     * password file
     */
    public static File CRONJOB_PASSWORD_FILE = new File(System.getProperty(CRONJOB_HOME) + File.separator + ".password");


    /**
     * monitor file
     */
    public static File CRONJOB_MONITOR_SHELL = new File(System.getProperty(CRONJOB_HOME) + "/bin/monitor.sh");

    /**
     * kill file
     */
    public static File CRONJOB_KILL_SHELL = new File(System.getProperty(CRONJOB_HOME) + "/bin/kill.sh");



}
