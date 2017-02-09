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


import org.opencron.common.utils.CommonUtils;
import org.opencron.common.utils.WebUtils;
import org.opencron.server.domain.User;
import org.opencron.server.job.OpencronTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 登陆权限拦截器
 */
@Component
public class SecurityHandlerInterceptor extends HandlerInterceptorAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SecurityHandlerInterceptor.class);

    public boolean preHandle(HttpServletRequest request,HttpServletResponse response, Object handler) throws Exception {

        HttpSession session = request.getSession();

        String requestURI = request.getContextPath() + request.getServletPath();

        //静态资源,页面
        if ( requestURI.contains("/css/")
                || requestURI.contains("/fonts/")
                || requestURI.contains("/img/")
                || requestURI.contains("/js/")
                || requestURI.contains("/WEB-INF") ) {
            return super.preHandle(request, response, handler);
        }

        //登陆
        if (requestURI.contains("/login")||requestURI.contains("/upload")) {
            return super.preHandle(request, response, handler);
        }

        String referer = request.getHeader("referer");
        if (referer != null && !referer.startsWith(WebUtils.getWebUrlPath(request))) {
            response.sendRedirect("/");
            logger.info("[opencron]Bad request,redirect to login page");
            OpencronTools.invalidSession(session);
            return false;
        }

        try {
            User user = OpencronTools.getUser(session);
            if (user == null) {
                logger.info(request.getRequestURL().toString());
                //跳到登陆页面
                response.sendRedirect("/");
                OpencronTools.invalidSession(session);
                logger.info("[opencron]session is null,redirect to login page");
                return false;
            }
        }catch (IllegalStateException e) {
            logger.info("[opencron]Session already invalidated,redirect to login page");
            response.sendRedirect("/");
            return false;
        }

        //普通管理员不可访问的资源
        if (!OpencronTools.isPermission(session) &&
                (requestURI.contains("/config/")
                        || requestURI.contains("/user/view")
                        || requestURI.contains("/user/add")
                        || requestURI.contains("/agent/add")
                        || requestURI.contains("/agent/edit"))) {
            logger.info("[opencron]illegal or limited access");
            return false;
        }

        if (handler instanceof HandlerMethod) {
            if (!verifyCSRF(request)) {
                response.sendRedirect("/");
                logger.info("[opencron]Bad request,redirect to login page");
                OpencronTools.invalidSession(session);
                return false;
            }
        }

        return super.preHandle(request, response, handler);
    }

    private boolean verifyCSRF(HttpServletRequest request) {

        String requstCSRF = OpencronTools.getCSRF(request);
        if (CommonUtils.isEmpty(requstCSRF)) {
            return false;
        }
        String sessionCSRF = OpencronTools.getCSRF(request.getSession());
        if (CommonUtils.isEmpty(sessionCSRF)) {
            return false;
        }
        return requstCSRF.equals(sessionCSRF);
    }
}
