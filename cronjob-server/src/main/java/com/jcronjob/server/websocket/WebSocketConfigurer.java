package com.jcronjob.server.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.annotation.Resource;

/**
 * WebScoket配置处理器
 *
 * @author Goofy
 * @Date 2015年6月11日 下午1:15:09
 */
@Component
@EnableWebSocket
public class WebSocketConfigurer extends WebMvcConfigurerAdapter implements org.springframework.web.socket.config.annotation.WebSocketConfigurer {

    @Resource
    private TerminalHandler handler;

    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/terminal.ws").addInterceptors(new WebSocketHandShaker());
        registry.addHandler(handler, "/terminal.wsjs").addInterceptors(new WebSocketHandShaker()).withSockJS();
    }

}
