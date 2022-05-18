package com.example.websocketdemo.controller;

import com.example.websocketdemo.config.SessionHandler;
import com.example.websocketdemo.exception.InvalidFieldsException;
import com.example.websocketdemo.model.ChatMessage;
import com.example.websocketdemo.model.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.HashMap;

@Component
public class WebSocketEventListener {

//    @Autowired
//    private SimpMessageSendingOperations messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
//        System.out.println("Received a new web socket connection");
//        System.out.println(event.getMessage().toString());
//        System.out.println(event.getMessage().getHeaders().keySet());
//        System.out.println(event.toString());
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionSubscribeEvent event) {
//        System.out.println("Received a new subscription socket connection");
//        System.out.println(event.getMessage().toString());
//        System.out.println(event.getMessage().getHeaders().keySet());
//        System.out.println(event.toString());
    }

    @EventListener
    public void handleWebSocketConnectedListener(SessionConnectedEvent event) {
//        sessionHandler.register(event);
//        System.out.println("Received a new subscription socket connection");
//        System.out.println(event.getMessage().toString());
//        System.out.println(event.getMessage().getHeaders().keySet());
//        System.out.println(event.toString());
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
//        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
//
//        String username = (String) headerAccessor.getSessionAttributes().get("username");
//        if(username != null) {
//            logger.info("User Disconnected : " + username);
//
//            ChatMessage chatMessage = new ChatMessage();
//            chatMessage.setType(MessageType.LEAVE);
//            chatMessage.setSender(username);
//
//            messagingTemplate.convertAndSend("/topic/public", chatMessage);
//        }
    }
}