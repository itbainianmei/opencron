package com.jcronjob.server.job;

import com.jcronjob.common.utils.CommonUtils;
import com.jcronjob.common.utils.HttpUtils;
import com.jcronjob.server.domain.Agent;
import com.jcronjob.server.service.AgentService;
import com.jcronjob.server.service.ConfigService;
import com.jcronjob.server.service.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by benjobs on 2016/12/15.
 */

@Component
public class CronjobHeartBeat {

    public static int port;

    private volatile boolean running = false;

    private ConcurrentHashMap<Class, ObjectAction> actionMapping = new ConcurrentHashMap<Class, ObjectAction>();

    private Map<Agent, Long> connStatus = new ConcurrentHashMap<Agent, Long>(0);

    private Map<String,Agent> agentMap = new ConcurrentHashMap<String, Agent>(0);

    private Thread connWatchDog;

    private long keepAliveDelay = 5000;

    @Autowired
    private AgentService agentService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private NoticeService noticeService;

    /**
     * 要处理客户端发来的对象，并返回一个对象，可实现该接口。
     */
    public interface ObjectAction {
        void doAction(Object rev);
    }

    public final class DefaultObjectAction implements ObjectAction {
        public void doAction(Object rev) {
            String ip = rev.toString();
            if (agentMap.get(ip)==null) {
                Agent agent = agentService.getByHost(rev.toString());
                agentMap.put(ip,agent);
            }
            connStatus.put(agentMap.get(ip), System.currentTimeMillis());
        }
    }

    public void start() {
        this.port = HttpUtils.freePort();
        if (running) return;
        running = true;
        connWatchDog = new Thread(new ConnWatchDog());
        connWatchDog.start();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if(connStatus.isEmpty()){
                   return;
                }
                Iterator<Map.Entry<Agent, Long>> iterator = connStatus.entrySet().iterator();
                while(iterator.hasNext()){
                    long lastAliveTime = iterator.next().getValue();
                    Agent agent = iterator.next().getKey();
                    if (System.currentTimeMillis() - lastAliveTime > CronjobHeartBeat.this.keepAliveDelay) {
                        if (CommonUtils.isEmpty(agent.getFailTime()) || new Date().getTime() - agent.getFailTime().getTime() >= configService.getSysConfig().getSpaceTime() * 60 * 1000) {
                            noticeService.notice(agent);
                            //记录本次任务失败的时间
                            agent.setFailTime(new Date());
                            agent.setStatus(false);
                            agentService.addOrUpdate(agent);
                        }
                        if (agent.getStatus()) {
                            agent.setStatus(false);
                            agentService.addOrUpdate(agent);
                        }
                        //失败将其移除
                        connStatus.remove(agent);
                    } else {
                        if (!agent.getStatus()) {
                            agent.setStatus(true);
                            agentService.addOrUpdate(agent);
                        }
                    }
                }
            }
        }, 5*1000);

    }

    @SuppressWarnings("deprecation")
    public void stop() {
        if (running) running = false;
        if (connWatchDog != null) connWatchDog.stop();
    }

    class ConnWatchDog implements Runnable {
        public void run() {
            try {
                ServerSocket serverSocket = new ServerSocket(port, 5);
                while (running) {
                    Socket socket = serverSocket.accept();
                    new Thread(new SocketAction(socket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
                CronjobHeartBeat.this.stop();
            }

        }
    }

    class SocketAction implements Runnable {

        private Socket socket;

        private boolean run = true;

        public SocketAction(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            while (running && run) {
                try {
                    InputStream in = socket.getInputStream();
                    if (in.available() > 0) {
                        ObjectInputStream inputStream = new ObjectInputStream(in);
                        Object obj = inputStream.readObject();
                        ObjectAction action = actionMapping.get(obj.getClass());
                        action = action == null ? new DefaultObjectAction() : action;
                        action.doAction(obj);
                    } else {
                        Thread.sleep(10);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }


}
