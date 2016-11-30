/**
 * Copyright 2016 benjobs
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
import java.util.concurrent.ConcurrentHashMap;

import static com.jcronjob.server.service.TerminalService.*;


@Component
public class TerminalHandler implements WebSocketHandler {

	private static Logger logger = LoggerFactory.getLogger(TerminalHandler.class);

	private Map<Long,String> sessionIds = new ConcurrentHashMap<Long, String>(0);

	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		sessionIds.put(Thread.currentThread().getId(),(String) session.getAttributes().get(Globals.SSH_SESSION_ID));
		Runnable run = new MessageSender(sessionIds.get(Thread.currentThread().getId()), session);
		Thread thread = new Thread(run);
		thread.start();
	}

	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {

		if (session.isOpen() && CommonUtils.notEmpty(message)) {

			Map jsonMap = JSON.parseObject(message.getPayload().toString(),Map.class);

			String command = (String) jsonMap.get("command");
			Integer keyCode = (Integer) jsonMap.get("keyCode");
			String token = (String) jsonMap.get("token");

			//get servletRequest.getSession() for user
			SchSession schSession = TerminalSession.get(token);
			if (keyCode != null && schSession!=null) {
				if (TerminalKeyMap.containsKey(keyCode)) {
					try {
						schSession.getCommander().write(TerminalKeyMap.get(keyCode));
					} catch (IOException ex) {
						logger.error(ex.toString(), ex);
					}
				}
			} else {
				schSession.getCommander().print(command);
			}
		}
	}


	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {

	}


	public void afterConnectionClosed(WebSocketSession session,CloseStatus closeStatus) throws Exception {
		String sessionId = sessionIds.get(Thread.currentThread().getId());
		SchSession schSession = TerminalSession.remove(sessionId);
		if (schSession != null) {
			schSession.getChannel().disconnect();
			schSession.getSession().disconnect();
			schSession.setChannel(null);
			schSession.setSession(null);
			schSession.setInputToChannel(null);
			schSession.setCommander(null);
			schSession.setOutFromChannel(null);
			removeUserSession(sessionId);
		}
		sessionIds.remove(Thread.currentThread().getId());
	}

	public boolean supportsPartialMessages() {
		return false;
	}

}

