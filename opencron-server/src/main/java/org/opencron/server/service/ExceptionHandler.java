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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.commons.lang3.StringUtils;
import org.opencron.common.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;


public class ExceptionHandler implements Filter,HandlerExceptionResolver {

	private static final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);

	@Override
	public ModelAndView resolveException(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception ex) {
		if (ex instanceof MaxUploadSizeExceededException) {
			WebUtils.writeJson(httpServletResponse,"长传的文件大小超过"+((MaxUploadSizeExceededException)ex).getMaxUploadSize() + "字节限制,上传失败!");
			return null;
		}
		ModelAndView view = new ModelAndView();

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ex.printStackTrace(new PrintStream(byteArrayOutputStream));
		String exception = byteArrayOutputStream.toString();

		view.getModel().put("error","URL:"+ WebUtils.getWebUrlPath(httpServletRequest)+httpServletRequest.getRequestURI()+"\r\n\r\nERROR:"+exception);
		logger.error("[opencron]error:{}",ex.getLocalizedMessage());
		view.setViewName("/error/500");
		return view;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {}

	@Override
	public void destroy() { }

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

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
		
		StatusExposingServletResponse statusResponse = new StatusExposingServletResponse((HttpServletResponse)response);
		int status = 0;

		try {
			 chain.doFilter(request, statusResponse);
			 status = statusResponse. getStatus();
		} catch (Exception e) {//获取异常错误信息
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(byteArrayOutputStream));
			String exception = byteArrayOutputStream.toString();
			status = 500;
			logger.error("URL:"+requestURL+" STATUS:"+status+"  error:"+exception);
		}finally{
			 if(status==404){
				 return;
			 }else if(status==500){
				return;
			 }else if(status==520){
				 res.sendRedirect("/error/invalid.jsp");
			 }
		}
	}

	static class StatusExposingServletResponse extends HttpServletResponseWrapper {

		private int httpStatus;

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
	}
}
