package com.example.websocketdemo.config;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Service
public class SessionHandler {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final Map<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();
    private static boolean my = false;

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

                        if(my)
                            return;

                        sessionMap.keySet().forEach(k -> {
                            try {
                                System.out.println(sessionMap.get(k).getId());
                                if(sessionMap.get(k).getId().equals(sessionId)) {
                                    sessionMap.get(k).close();
                                    sessionMap.remove(k);
                                    my = true;
                                    return;
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });

                    }
                },
                40000
        );
    }

}
