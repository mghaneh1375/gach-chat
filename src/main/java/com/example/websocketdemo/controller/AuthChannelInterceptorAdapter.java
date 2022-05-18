package com.example.websocketdemo.controller;

import org.bson.types.ObjectId;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static com.example.websocketdemo.controller.Utility.hasAccess;

@Component
public class AuthChannelInterceptorAdapter extends Router implements ChannelInterceptor {

    private final WebSocketAuthenticatorService webSocketAuthenticatorService;

    @Inject
    public AuthChannelInterceptorAdapter(final WebSocketAuthenticatorService webSocketAuthenticatorService) {
        this.webSocketAuthenticatorService = webSocketAuthenticatorService;
    }

    @Override
    public Message<?> preSend(final Message<?> message, final MessageChannel channel) {
        final StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT == accessor.getCommand()) {
//            System.out.println(accessor.toString());
//            System.out.println(accessor.getFirstNativeHeader("token"));
//            accessor.setUser(webSocketAuthenticatorService.getAuthenticatedOrFail(accessor.getFirstNativeHeader("token")));
//            System.out.println("success");

        } else if (StompCommand.SUBSCRIBE == accessor.getCommand()) {

            if (accessor.getFirstNativeHeader("self-subscribe") != null) {
                String[] splited = accessor.getHeader("simpDestination").toString().split("/");
                getUserWithToken(accessor.getFirstNativeHeader("token"), splited[splited.length - 1], null);
            } else {

                String[] splited = accessor.getHeader("simpDestination").toString().split("/");
                getUserWithToken(
                        accessor.getFirstNativeHeader("token"), null,
                        splited[splited.length - 1]
                );

//                hasAccess(userId, splited[splited.length - 1]);
            }

        }

        return message;
    }
}
