package com.jcronjob.agent;

import com.jcronjob.common.utils.LoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Created by benjobs on 2016/12/15.
 */
public class AgentHeartBeat {

    private String serverIp;
    private int port;
    private String clientIp;
    private Socket socket;
    private boolean running = false;
    private long lastSendTime;

    private Logger logger = LoggerFactory.getLogger(AgentHeartBeat.class);


    public AgentHeartBeat(String serverIp, int port, String clientIp) {
        this.serverIp = serverIp;
        this.port = port;
        this.clientIp = clientIp;
    }

    public void start() throws IOException {
        if (running) return;
        socket = new Socket(serverIp, port);
        lastSendTime = System.currentTimeMillis();
        running = true;
        new Thread(new KeepAliveWatchDog()).start();
    }

    public void stop() {
        if (running) {
            running = false;
        }
    }

    public void sendObject(Object obj) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        oos.writeObject(obj);
        logger.info("[cronjob]:heartBeat:"+this.lastSendTime);
        oos.flush();
    }

    class KeepAliveWatchDog implements Runnable {
        long checkDelay = 10;
        long keepAliveDelay = 5000;

        public void run() {
            while (running) {
                if (System.currentTimeMillis() - lastSendTime > keepAliveDelay) {
                    lastSendTime = System.currentTimeMillis();
                    try {
                        AgentHeartBeat.this.sendObject(AgentHeartBeat.this.clientIp);
                    } catch (IOException e) {
                        e.printStackTrace();
                        AgentHeartBeat.this.stop();
                    }
                } else {
                    try {
                        Thread.sleep(checkDelay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        AgentHeartBeat.this.stop();
                    }
                }
            }
        }
    }


}
