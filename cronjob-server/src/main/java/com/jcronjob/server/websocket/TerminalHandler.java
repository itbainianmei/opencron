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


import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.jcronjob.server.domain.Terminal;
import com.jcronjob.server.job.Globals;
import static com.jcronjob.server.service.TerminalService.*;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;


public class TerminalHandler extends TextWebSocketHandler {

	private Map<String,TerminalClient> terminalClientMap = new ConcurrentHashMap<String, TerminalClient>(0);

	private TerminalClient terminalClient;

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		super.afterConnectionEstablished(session);
		String sessionId = (String) session.getAttributes().get(Globals.SSH_SESSION_ID);
		if (sessionId != null) {
			Terminal terminal = TerminalSession.remove(sessionId);
			if (terminal!=null) {
				try {
					session.sendMessage(new TextMessage("Welcome to cronjob terminal!Connect Starting...\r"));
					getClient(session,terminal);
					if (terminalClient.connect()) {
						terminalClient.sendMessage(session);
					} else {
						terminalClient.disconnect();
						session.sendMessage(new TextMessage("Connect failed, please try agin..."));
						session.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else {
				this.terminalClient.disconnect();
				session.sendMessage(new TextMessage("Connect failed, please try agin..."));
				session.close();
			}
		}
	}


	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		super.handleTextMessage(session, message);
		try {
			getClient(session,null);
			if (terminalClient != null) {
				//receive a close cmd ?
				if (Arrays.equals("exit".getBytes(), message.asBytes())) {
					if (terminalClient != null) {
						terminalClient.disconnect();
					}
					session.close();
					return ;
				}
				terminalClient.write(new String(message.asBytes(), "UTF-8"));
			}
		} catch (Exception e) {
			e.printStackTrace();
			session.sendMessage(new TextMessage("An error occured, websocket is closed..."));
			session.close();
		}
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		super.handleTransportError(session, exception);
		this.closeTerminal(session);
		session.close();
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		super.afterConnectionClosed(session, status);
		this.closeTerminal(session);

	}

	private TerminalClient getClient(WebSocketSession session, Terminal terminal){
		this.terminalClient = this.terminalClientMap.get(session.getId());
		if (this.terminalClient==null && terminal!=null) {
			this.terminalClient = new TerminalClient(terminal);
			this.terminalClientMap.put(session.getId(),this.terminalClient);
		}
		return this.terminalClient;
	}


	private void closeTerminal(WebSocketSession session) throws IOException {
		terminalClient = this.terminalClientMap.remove(session.getId());
		if (terminalClient != null) {
			terminalClient.disconnect();
		}
	}

}

