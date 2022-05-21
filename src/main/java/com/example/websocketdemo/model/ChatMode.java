package com.example.websocketdemo.model;

public enum ChatMode {

    GROUP, PEER;

    public String getName() {
        return name().toLowerCase();
    }
}
