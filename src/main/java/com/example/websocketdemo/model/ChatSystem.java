package com.example.websocketdemo.model;

public class ChatSystem {

    private String chatId;
    private String chatName;
    private String type;

    public String getType() {
        return type;
    }

    public String getChatName() {
        return chatName;
    }

    public void setChatName(String chatName) {
        this.chatName = chatName;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ChatSystem(String chatId, String chatName) {
        this.chatId = chatId;
        this.chatName = chatName;
        this.type = MessageType.WELCOME.name();
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }
}
