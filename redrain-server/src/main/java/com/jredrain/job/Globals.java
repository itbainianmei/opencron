package com.jredrain.job;


import com.jredrain.domain.User;

import javax.servlet.http.HttpSession;

public final class Globals {

    public static final String CACHED_AGENT_ID = "redrain_agent";

    public static final String CACHED_CRONTAB_JOB = "redrain_cron_job";

    public static final String LOGIN_USER = "redrain_user";

    public static final String PERMISSION = "permission";

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
