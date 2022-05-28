package com.example.websocketdemo.config;

import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

public class IpHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(org.springframework.http.server.ServerHttpRequest request, org.springframework.http.server.ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes
    ) {
        attributes.put("ip", request.getRemoteAddress());
        return true;
    }

    @Override
    public void afterHandshake(org.springframework.http.server.ServerHttpRequest request, org.springframework.http.server.ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
