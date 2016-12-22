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

import com.jcronjob.server.domain.Terminal;
import com.jcronjob.server.job.Globals;
import static com.jcronjob.server.service.TerminalService.*;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.crypto.BadPaddingException;


public class TerminalHandler extends TextWebSocketHandler {

	private TerminalClient terminalClient;
	private String sessionId;

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		super.afterConnectionEstablished(session);
		sessionId = (String) session.getAttributes().get(Globals.SSH_SESSION_ID);
		if (sessionId != null) {
			final Terminal terminal = TerminalContext.remove(sessionId);

			if (terminal!=null) {
				try {
					session.sendMessage(new TextMessage("Welcome to Cronjob Terminal! Connect Starting. "));
					getClient(session,terminal);
					int cols = Integer.parseInt(session.getAttributes().get("cols").toString());
					int rows = Integer.parseInt(session.getAttributes().get("rows").toString());
					terminalClient.openTerminal(cols,rows);
				} catch (Exception e) {
					if (e.getLocalizedMessage().replaceAll("\\s+", "").contentEquals("Operationtimedout")) {
						session.sendMessage(new TextMessage("Sorry! Connect timed out, please try again. "));
					} else {
						session.sendMessage(new TextMessage("Sorry! Operation error, please try again. "));
					}
					terminalClient.disconnect();
					session.close();
				}
			}else {
				this.terminalClient.disconnect();
				session.sendMessage(new TextMessage("Sorry! Connect failed, please try again. "));
				session.close();
			}
		}
	}


	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		super.handleTextMessage(session, message);
		try {
			getClient(session,null);
			if (this.terminalClient != null ) {
				if ( !terminalClient.isClosed()) {
					if (Arrays.equals("exit".getBytes(), message.asBytes())) {
						if (this.terminalClient != null) {
							this.terminalClient.disconnect();
						}
						session.close();
						return;
					}
					terminalClient.write(new String(message.asBytes(), "UTF-8"));
				}else {
					session.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			session.sendMessage(new TextMessage("Sorry! Websocket is closed, please try again. "));
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
		this.terminalClient =  TerminalSession.get(session);
		if (this.terminalClient==null && terminal!=null) {
			this.terminalClient = new TerminalClient(session,terminal);
			TerminalSession.put(session,this.terminalClient);
		}
		return this.terminalClient;
	}


	private void closeTerminal(WebSocketSession session) throws IOException {
		terminalClient = TerminalSession.remove(session);
		if (terminalClient != null) {
			terminalClient.disconnect();
		}
	}

}

