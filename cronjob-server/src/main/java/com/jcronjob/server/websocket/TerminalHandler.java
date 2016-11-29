package com.jcronjob.server.websocket;

import com.alibaba.fastjson.JSON;
import com.jcronjob.common.utils.CommonUtils;
import com.jcronjob.server.job.Globals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.*;

import static com.jcronjob.server.service.TerminalService.*;


@Component
public class TerminalHandler implements WebSocketHandler {

	private static Logger log = LoggerFactory.getLogger(TerminalHandler.class);

	private String sessionId = null;

	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		this.sessionId = (String) session.getAttributes().get(Globals.SSH_SESSION_ID);
		Runnable run = new OutputRunner(sessionId, session);
		Thread thread = new Thread(run);
		thread.start();
	}

	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {

		if (session.isOpen() && CommonUtils.notEmpty(message)) {

			Map jsonMap = JSON.parseObject(message.getPayload().toString(),Map.class);

			String command = (String) jsonMap.get("command");
			Integer keyCode = (Integer) jsonMap.get("keyCode");;
			String id = (String) jsonMap.get("id");

			//get servletRequest.getSession() for user
			UserSchSession userSchSession =  TerminalSession.get(sessionId);
			if (userSchSession != null) {
				SchSession schSession = userSchSession.getUserSchSession().get(id);
				if (keyCode != null) {
					if (KeyCodeMap.containsKey(keyCode)) {
						try {
							schSession.getCommander().write(KeyCodeMap.get(keyCode));
						} catch (IOException ex) {
							log.error(ex.toString(), ex);
						}
					}
				} else {
					schSession.getCommander().print(command);
				}
			}
		}
	}

	/**
	 * 消息传输错误处理
	 */
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {

	}

	/**
	 * 关闭连接后
	 */
	public void afterConnectionClosed(WebSocketSession session,CloseStatus closeStatus) throws Exception {
		UserSchSession userSchSession = TerminalSession.remove(sessionId);
		if (userSchSession != null) {
			Map<String, SchSession> schSessionMap = userSchSession.getUserSchSession();

			for (String sessionKey : schSessionMap.keySet()) {
				SchSession schSession = schSessionMap.get(sessionKey);
				schSession.getChannel().disconnect();
				schSession.getSession().disconnect();
				schSession.setChannel(null);
				schSession.setSession(null);
				schSession.setInputToChannel(null);
				schSession.setCommander(null);
				schSession.setOutFromChannel(null);
				//remove from map
				schSessionMap.remove(sessionKey);
			}
			//clear and remove session map for user
			schSessionMap.clear();
			schSessionMap.remove(sessionId);
			removeUserSession(sessionId);
		}

	}

	public boolean supportsPartialMessages() {
		return false;
	}

}

