package org.opencron.server.job;


import org.opencron.server.domain.User;

import javax.servlet.http.HttpSession;

public final class Globals {

    public static final String CACHED_AGENT_ID = "cronjob_agent";

    public static final String CACHED_CRONTAB_JOB = "cronjob_cron_job";

    public static final String LOGIN_USER = "cronjob_user";

    public static final String PERMISSION = "permission";

    public static final String SSH_SESSION_ID = "ssh_session_id";

    public static boolean isPermission(HttpSession session){
        return (Boolean) session.getAttribute(PERMISSION);
    }

    public static User getUserBySession(HttpSession session){
        return (User)session.getAttribute(Globals.LOGIN_USER);
    }

    public static Long getUserIdBySession(HttpSession session){
        return getUserBySession(session).getUserId();
    }

}
