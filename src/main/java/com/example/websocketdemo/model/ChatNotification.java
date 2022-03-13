package com.example.websocketdemo.model;

public class ChatNotification {

    private String chatId;
    private int count;
    private String type;
    private String senderName;
    private String mode;
    private ChatMessage chatMessage;

    public String getType() {
        return type;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public ChatNotification(String senderName, String chatId,
                            int count, String mode, ChatMessage chatMessage) {
        this.senderName = senderName;
        this.chatId = chatId;
        this.count = count;
        this.type = "NOTIF";
        this.mode = mode;
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
