package com.example.websocketdemo.db;

import com.example.websocketdemo.WebsocketDemoApplication;

public class ClassRepository extends Common {

    public ClassRepository() {
        init();
    }

    @Override
    void init() {
        table = "class";
        documentMongoCollection = WebsocketDemoApplication.mongoDatabase.getCollection("class");
    }
}