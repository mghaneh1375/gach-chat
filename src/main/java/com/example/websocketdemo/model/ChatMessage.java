package com.example.websocketdemo.model;

public class ChatMessage {

    private String content;
    private String id;
    private long timestamp;

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    private String chatId;
    private String senderId;
    private String sender;

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public ChatMessage(String content, String id,
                       long timestamp, String chatId,
                       String senderId, String sender) {
        this.content = content;
        this.id = id;
        this.timestamp = timestamp;
        this.chatId = chatId;
        this.senderId = senderId;
        this.sender = sender;
    }
}
