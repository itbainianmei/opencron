package org.opencron.server.service;

import org.opencron.server.domain.User;
import org.opencron.server.job.Globals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * Created by benjobs on 2017/1/12.
 */
public class SingletonLoginListener implements HttpSessionListener {

    // key为sessionId，value为HttpSession，使用static，定义静态变量，使之程序运行时，一直存在内存中。
    private static Map<String, HttpSession> singletonLoginSessionMap = new ConcurrentHashMap<String, HttpSession>(500);

    /**
     * HttpSessionListener中的方法，在创建session
     */
    public void sessionCreated(HttpSessionEvent event) {
    }

    /**
     * HttpSessionListener中的方法，回收session时,删除sessionMap中对应的session
     */
    public void sessionDestroyed(HttpSessionEvent event) {
        getSingletonLoginSessionMap().remove(event.getSession().getId());
    }

    /**
     * 得到在线用户会话集合
     */
    public static List<HttpSession> getUserSessions() {
        List<HttpSession> list = new ArrayList<HttpSession>();
        for(Map.Entry<String,HttpSession> entry: singletonLoginSessionMap.entrySet()) {
            HttpSession session = entry.getValue();
            list.add(session);
        }
        return list;
    }

    /**
     * 得到用户对应会话map，key为用户ID,value为会话ID
     */
    public static Map<Long, String> getSessionIds() {
        Map<Long, String> map = new HashMap<Long, String>();
        for(Map.Entry<String,HttpSession> entry: singletonLoginSessionMap.entrySet()){
            String sessionId = entry.getKey();
            HttpSession session = entry.getValue();
            User user = (User) session.getAttribute(Globals.LOGIN_USER);
            if (user != null) {
                map.put(user.getUserId(), sessionId);
            }
        }
        return map;
    }

    /**
     * 移除用户Session
     */
    public synchronized static void removeUserSession(Long userId) {
        Map<Long, String> userSessionMap = getSessionIds();
        if (userSessionMap.containsKey(userId)) {
            String sessionId = userSessionMap.get(userId);
            HttpSession httpSession = singletonLoginSessionMap.get(sessionId);
            if (!httpSession.isNew()) {
                httpSession.removeAttribute(Globals.LOGIN_USER);
                //httpSession.invalidate();
            }
            singletonLoginSessionMap.remove(sessionId);
        }
    }

    /**
     * 增加用户到session集合中
     */
    public static void addUserSession(HttpSession session) {
        getSingletonLoginSessionMap().put(session.getId(), session);
    }

    /**
     * 移除一个session
     */
    public static void removeSession(String sessionID) {
        getSingletonLoginSessionMap().remove(sessionID);
    }

    public static boolean containsKey(String key) {
        return getSingletonLoginSessionMap().containsKey(key);
    }

    public synchronized static boolean logined(User user) {
        for(Map.Entry<String,HttpSession> entry: singletonLoginSessionMap.entrySet()){
            HttpSession session = entry.getValue();
            User sessionuser = (User) session.getAttribute(Globals.LOGIN_USER);
            if (sessionuser != null) {
                if (sessionuser.getUserId().equals(user.getUserId())){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取在线的sessionMap
     */
    public static Map<String, HttpSession> getSingletonLoginSessionMap() {
        return singletonLoginSessionMap;
    }

    public static HttpSession getLoginedSession(Long userId) {
        String sessionId = getSessionIds().get(userId);
        if (sessionId!=null) {
           return getSingletonLoginSessionMap().get(sessionId);
        }
        return null;
    }
}