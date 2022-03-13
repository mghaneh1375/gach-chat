package com.example.websocketdemo.model;

import org.bson.types.ObjectId;

public class ChatRoom {

    private ObjectId id;
    private String chatId;
    private ObjectId senderId;
    private ObjectId receiverId;

    public ChatRoom(String chatId, ObjectId senderId, ObjectId receiverId) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.receiverId = receiverId;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public ObjectId getSenderId() {
        return senderId;
    }

    public void setSenderId(ObjectId senderId) {
        this.senderId = senderId;
    }

    public ObjectId getreceiverId() {
        return receiverId;
    }

    public void setreceiverId(ObjectId receiverId) {
        this.receiverId = receiverId;
    }
}
