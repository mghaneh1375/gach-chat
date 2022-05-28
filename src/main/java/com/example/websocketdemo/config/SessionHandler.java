package com.example.websocketdemo.config;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.websocketdemo.utility.Statics.SOCKET_TOKEN_EXPIRATION_MSEC;

@Service
public class SessionHandler {

    private final Map<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    public SessionHandler() {

//        scheduler.scheduleAtFixedRate(() -> sessionMap.keySet().forEach(k -> {
//            try {
//                sessionMap.get(k).close();
//                sessionMap.remove(k);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }), 10, 10, TimeUnit.SECONDS);
    }

    public void register(WebSocketSession session) {

        final String sessionId = session.getId();

        sessionMap.put(sessionId, session);

        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {

                        for(String k : sessionMap.keySet()) {
                            try {
                                if(sessionMap.get(k).getId().equals(sessionId)) {
                                    sessionMap.get(k).close();
                                    sessionMap.remove(k);
                                    break;
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                },
                SOCKET_TOKEN_EXPIRATION_MSEC
        );
    }

}
