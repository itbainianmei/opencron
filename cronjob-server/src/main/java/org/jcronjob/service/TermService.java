package org.jcronjob.service;

import com.alibaba.fastjson.JSON;
import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.jcronjob.base.utils.DigestUtils;
import org.jcronjob.base.utils.HttpUtils;
import org.jcronjob.dao.QueryDao;
import org.jcronjob.domain.Term;
import org.jcronjob.domain.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by benjobs on 16/5/21.
 */

@Service
public class TermService {

    @Autowired
    private QueryDao queryDao;

    @Autowired
    private ConfigService configService;

    private Map<UUID, SocketIOClient> clients = new HashMap<UUID, SocketIOClient>(0);

    private Map<Long, Integer> sessionMap = new HashMap<Long, Integer>(0);

    private Logger logger = LoggerFactory.getLogger(getClass());

    public Term getTerm(Long userId, String host) {
        return queryDao.sqlUniqueQuery(Term.class,"SELECT * FROM term WHERE userId=? AND host=? And status=1",userId,host);
    }

    public boolean saveOrUpdate(Term term) {
        Term dbTerm = queryDao.sqlUniqueQuery(Term.class,"SELECT * FROM term WHERE userId=? AND host=?",term.getUserId(),term.getHost());
        if (dbTerm!=null) {
            term.setTermId(dbTerm.getTermId());
        }

        try {
            queryDao.save(term);
            return true;
        }catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getTermUrl(HttpServletRequest request, Worker worker) {
        /**
         * 创建一个用户连接websocket的空闲端口,存起来
         */
        final Long workerId = worker.getWorkerId();

        if (sessionMap.get(workerId) == null) {
            final int port = HttpUtils.generagePort();
            this.sessionMap.put(workerId, port);

            /**
             * websocket server start......
             */
            Configuration configuration = new Configuration();
            configuration.setPort(port);
            final SocketIOServer server = new SocketIOServer(configuration);

            server.addEventListener("login", String.class, new DataListener<String>() {
                @Override
                public void onData(final SocketIOClient client, String str, AckRequest ackRequest) throws Exception {
                    /**
                     * 确保安全性,从数据库获取解密的私key,进行AES解密
                     */
                    String key = configService.getSysConfig().getAeskey();
                    String jsonTerm = DigestUtils.aesDecrypt(key,str);
                    Term term = JSON.parseObject(jsonTerm,Term.class);
                    Session jschSession = createJschSession(term);
                    logger.info("[cronjob]:SSHServer connected:SessionId @ {},port @ {}", client.getSessionId(), port);
                    clients.put(client.getSessionId(), client);
                    try {
                        jschSession.connect();
                        //登录成功,进入console
                        client.sendEvent("console", new VoidAckCallback() {
                            @Override
                            protected void onSuccess() {
                                System.out.println("ack from client: " + client.getSessionId());
                            }
                        }, "successful....");

                    } catch (JSchException e) {
                        /**
                         * ssh 登录失败
                         */
                        term.setStatus(0);
                        saveOrUpdate(term);
                        ackRequest.sendAckData(termFailCause(e));
                        logger.info("[cronjob]:SSHServer connect error:", e.getLocalizedMessage());
                        server.stop();
                        sessionMap.remove(workerId);
                    }

                }
            });

            server.addDisconnectListener(new DisconnectListener() {
                @Override
                public void onDisconnect(SocketIOClient client) {
                    clients.remove(client.getSessionId());
                    if (clients.isEmpty()) {
                        sessionMap.remove(workerId);
                        logger.info("[cronjob]:SSHServer disconnect:SessionId @ {},port @ {} ", client.getSessionId(), port);
                        server.stop();
                    }
                }
            });
            server.start();
            logger.debug("[cronjob] SSHServer started @ {}", port);
        }

        return String.format("http://%s:%s",request.getServerName(),sessionMap.get(workerId));
    }

    public Session createJschSession(Term term) throws JSchException {
        JSch jsch = new JSch();
        Session jschSession = jsch.getSession(term.getUser(), term.getHost(), term.getPort());
        jschSession.setPassword(term.getPassword());

        java.util.Properties config = new java.util.Properties();
        //不记录本次登录的信息到$HOME/.ssh/known_hosts
        config.put("StrictHostKeyChecking", "no");
        jschSession.setConfig(config);
        return jschSession;
    }

    public String termFailCause(JSchException e) {
        if (e.getLocalizedMessage().equals("Auth fail")) {
            return "authfail";
        }else if(e.getLocalizedMessage().contentEquals("timeout")){
            return "timeout";
        }

        return e.getLocalizedMessage();
    }

}
