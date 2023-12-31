package com.example.websocketdemo.controller;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import java.util.concurrent.ExecutionException;

import static com.example.websocketdemo.WebsocketDemoApplication.SOCKET_MAX_REQUESTS_PER_MIN;
import static com.example.websocketdemo.WebsocketDemoApplication.socketRequestCountsPerIpAddress;

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

        String ip = accessor.getSessionAttributes().get("ip").toString();

        if(isMaximumRequestsPerSecondExceeded(ip)){
            throw new AuthenticationCredentialsNotFoundException("too many request");
        }

        if (StompCommand.CONNECT == accessor.getCommand()) {
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
            }

        }

        return message;
    }


    private boolean isMaximumRequestsPerSecondExceeded(final String ip){

        int requests;
        try {
            requests = socketRequestCountsPerIpAddress.get(ip);
            System.out.println(ip + "  :  " + requests);

            if(requests > SOCKET_MAX_REQUESTS_PER_MIN)
                return true;

        } catch (ExecutionException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return true;
        }

        if(requests == 0) {

            new java.util.Timer().schedule(
                    new java.util.TimerTask() {

                        @Override
                        public void run() {
                            socketRequestCountsPerIpAddress.put(ip, 0);
                        }
                    }, 60000
            );
        }
        requests++;
        socketRequestCountsPerIpAddress.put(ip, requests);
        return false;
    }
}
