package org.opencron.server.job;


import org.opencron.common.utils.CommonUtils;
import org.opencron.server.domain.User;
import org.opencron.server.service.TerminalService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public final class OpencronTools {

    public static final String CACHED_AGENT_ID = "opencron_agent";

    public static final String CACHED_CRONTAB_JOB = "opencron_cron_job";

    public static final String LOGIN_USER = "opencron_user";

    public static final String PERMISSION = "permission";

    public static final String SSH_SESSION_ID = "ssh_session_id";

    public static final String HTTP_SESSION_ID = "http_session_id";

    public static final String CSRF_NAME = "_csrf";

    public static final String LOGIN_MSG = "loginMsg";

    public static boolean isPermission(HttpSession session){
        return (Boolean) session.getAttribute(PERMISSION);
    }

    public static User getUserBySession(HttpSession session){
        return (User)session.getAttribute(OpencronTools.LOGIN_USER);
    }

    public static Long getUserIdBySession(HttpSession session){
        return getUserBySession(session).getUserId();
    }

    public static void clearSession(HttpSession session) throws Exception {
        session.removeAttribute(LOGIN_USER);
        session.removeAttribute(PERMISSION);
        session.removeAttribute(SSH_SESSION_ID);
        session.removeAttribute(CSRF_NAME);
        TerminalService.TerminalSession.exit(session.getId());
        session.removeAttribute(LOGIN_MSG);
        session.invalidate();
    }

    public static String getCSRF(HttpSession session) {
        String token;
        synchronized (session) {
            token = (String) session.getAttribute(CSRF_NAME);
            if (null == token) {
                token = CommonUtils.uuid();
                session.setAttribute(CSRF_NAME, token);
            }
        }
        return token;
    }

    public static String getCSRF(HttpServletRequest request) {
        String csrf = request.getHeader(CSRF_NAME);
        if (csrf==null) {
            csrf = request.getParameter(CSRF_NAME);
        }
        return csrf;
    }

}
