package com.example.websocketdemo.model;

public class ChatNotification {

    private String chatId;
    private int count;
    private String type;
    private String senderName;
    private String senderId;
    private ChatMessage chatMessage;

    public String getSenderId() {
        return senderId;
    }

    public String getType() {
        return type;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public ChatNotification(String senderName, int count, ChatMessage chatMessage) {
        this.senderName = senderName;
        this.chatId = chatMessage.getChatId();
        this.count = count;
        this.type = "NOTIF";
        this.chatMessage = chatMessage;
        this.senderId = chatMessage.getSenderId();
    }

    public ChatNotification(String senderName, String senderId,
                            int count, ChatMessage chatMessage) {
        System.out.println(senderId);
        this.senderName = senderName;
        this.chatId = chatMessage.getChatId();
        this.senderId = senderId;
        this.count = count;
        this.type = "NOTIF";
        this.chatMessage = chatMessage;
    }

    public ChatMessage getChatMessage() {
        return chatMessage;
    }

    public void setChatMessage(ChatMessage chatMessage) {
        this.chatMessage = chatMessage;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
