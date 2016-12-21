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


import com.jcronjob.server.domain.Agent;
import com.jcronjob.server.domain.Terminal;
import com.jcronjob.server.job.MornitorClient;
import com.jcronjob.server.service.AgentService;
import com.jcronjob.server.service.TerminalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

public class MornitorHandler extends TextWebSocketHandler {

	private MornitorClient mornitorClient;

	@Autowired
	private AgentService agentService;

	@Autowired
	private TerminalService terminalService;

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		super.afterConnectionEstablished(session);
		Long userId = Long.parseLong(session.getAttributes().get("userId").toString());
		Long agentId = Long.parseLong(session.getAttributes().get("agentId").toString());
		if (agentId != null) {
			Agent agent = agentService.getAgent(agentId);
			final Terminal terminal = terminalService.getTerm(userId,agent.getIp());
			if (terminal!=null) {
				try {
					this.mornitorClient = new MornitorClient(session,terminal);
					if (mornitorClient.connect()) {
						mornitorClient.sendMessage();
					} else {
						mornitorClient.disconnect();
						session.close();
					}
				} catch (IOException e) {
					if (e.getLocalizedMessage().replaceAll("\\s+","").contentEquals("Operationtimedout")) {
						session.sendMessage(new TextMessage("Sorry! Connect timed out, please try again. "));
					}else {
						session.sendMessage(new TextMessage("Sorry! Operation error, please try again. "));
					}
					mornitorClient.disconnect();
					session.close();
				}
			}else {
				this.mornitorClient.disconnect();
				session.sendMessage(new TextMessage("Sorry! Connect failed, please try again. "));
				session.close();
			}
		}
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		super.handleTextMessage(session, message);
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

	private void closeTerminal(WebSocketSession session) throws IOException {
		if (mornitorClient != null) {
			mornitorClient.disconnect();
		}
	}

}
