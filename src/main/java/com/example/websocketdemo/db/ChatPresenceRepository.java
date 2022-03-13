package com.example.websocketdemo.db;


import com.example.websocketdemo.WebsocketDemoApplication;

public class ChatPresenceRepository extends Common {

    public ChatPresenceRepository() {
        init();
    }

    @Override
    void init() {
        table = "chat_presence";
        secKey = "user_id";
        documentMongoCollection = WebsocketDemoApplication.mongoDatabase.getCollection(table);
    }
}
