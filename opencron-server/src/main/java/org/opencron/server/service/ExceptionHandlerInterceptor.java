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


package org.opencron.server.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.opencron.common.exception.InvalidException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;


public class ExceptionHandlerInterceptor implements Filter,ApplicationContextAware {

	private static final Logger logger = Logger.getLogger(ExceptionHandlerInterceptor.class);

	private NoticeService noticeService;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.noticeService = applicationContext.getBean(NoticeService.class);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		// TODO Auto-generated method stub
		HttpServletRequest req = (HttpServletRequest) request;   
		HttpServletResponse res = (HttpServletResponse) response;
		
		String requestURL = req.getRequestURL().toString();  
        String requestName = requestURL.substring(requestURL.lastIndexOf("/")+1);  
        requestName = requestName.toLowerCase();
        
        //过滤静态资源
        if(requestName.matches(".*\\.js$") 
        		|| requestName.matches(".*\\.css$") 
        		|| requestName.matches(".*\\.swf$")
        		|| requestName.matches(".*\\.jpg$")
        		|| requestName.matches(".*\\.png$")
        		|| requestName.matches(".*\\.jpeg$")
        		|| requestName.matches(".*\\.html$")
        		|| requestName.matches(".*\\.htm$")
        		|| requestName.matches(".*\\.xml$")
        		|| requestName.matches(".*\\.txt$")
        		|| requestName.matches(".*\\.ico$")
        		){  
            chain.doFilter(req, res);  
            return;  
        }  
		
		StatusExposingServletResponse statusres = new StatusExposingServletResponse((HttpServletResponse)response);
		int status = 0;
		String msg = "";
		try {
			 chain.doFilter(request, statusres);
			 status = statusres. getStatus();
		} catch (Exception e) {//获取异常错误信息

			ByteArrayOutputStream baos = new ByteArrayOutputStream();  
			e.printStackTrace(new PrintStream(baos));  
			String exception = baos.toString();

			if (e.getCause() instanceof InvalidException) {
				statusres.setStatus(520);
				status = 520;
			}else {
				if (exception.indexOf("Caused by:") > -1) {
					msg = StringUtils.substringAfter(exception, "Caused by:");
				} else {
					msg = exception;
				}
				status = 500;
			}
			logger.error("URL:"+requestURL+" STATUS:"+status+"  msg:"+msg);

		}finally{
			 if(status==404){
				 msg = statusres.getHttpMsg()+"该请求资源不存在！";
				 /**
				  * 发送错误信息给系统管理员
				  * noticeService.notice(404,msg);
				  */
				 return;
			 }else if(status==500){
				 /**
				  * 发送错误信息给系统管理员
				  * noticeService.notice(500,msg);
				  */
				return;
			 }else if(status==520){
				 res.sendRedirect("/error/invalid.jsp");
			 }
		}
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
	}


	class StatusExposingServletResponse extends HttpServletResponseWrapper {

		private int httpStatus;
		private String httpMsg;

		public StatusExposingServletResponse(HttpServletResponse response) {
			super(response);
		}

		@Override
		public void sendError(int sc) throws IOException {
			httpStatus = sc;
			super.sendError(sc);
		}

		@Override
		public void sendError(int sc, String msg) throws IOException {
			httpStatus = sc;
			httpMsg = msg;
			super.sendError(sc, msg);
		}

		@Override
		public void setStatus(int sc) {
			httpStatus = sc;
			super.setStatus(sc);
		}

		public int getStatus() {
			return httpStatus;
		}

		public String getHttpMsg() {
			return httpMsg;
		}

	}

}
