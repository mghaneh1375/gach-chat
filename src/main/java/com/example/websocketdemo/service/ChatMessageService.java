//package com.example.websocketdemo.service;
//
//import com.example.websocketdemo.exception.ResourceNotFoundException;
//import com.example.websocketdemo.model.ChatMessage;
//import org.bson.types.ObjectId;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//
//import static com.mongodb.client.model.Filters.and;
//import static com.mongodb.client.model.Filters.eq;
//import static com.mongodb.client.model.Updates.set;
//
//@Service
//public class ChatMessageService {
//
//    @Autowired private ChatRoomService chatRoomService;
//
//    public void save(ChatMessage chatMessage) {
//        ChatRepository.save(chatMessage);
//    }
//
//    public long countNewMessages(ObjectId senderId, ObjectId receiverId) {
//        return ChatRepository.getCount(and(
//                    eq("sender_id", senderId),
//                    eq("receiver_id", receiverId),
//                    eq("status", ChatMessage.MessageStatus.RECEIVED)
//                ));
//    }
//
//    public List<ChatMessage> findChatMessages(ObjectId senderId, ObjectId receiverId) {
//
//        Optional<String> chatId = chatRoomService.getChatId(senderId, receiverId, false);
//
//        if(chatId.isPresent() && !chatId.get().isEmpty()) {
//
//            List<ChatMessage> messages = ChatRepository.find(eq("chat_id", chatId.get()));
//
//            if(messages.size() > 0)
//                updateStatuses(senderId, receiverId, ChatMessage.MessageStatus.DELIVERED);
//        }
//
//
//        return new ArrayList<>();
//    }
//
//    public ChatMessage findById(ObjectId id) {
//
//        ChatMessage chatMessage = ChatRepository.findAndUpdate(eq("_id", id), set("status", ChatMessage.MessageStatus.DELIVERED));
//        if(chatMessage == null)
//            new ResourceNotFoundException("can't find message (" + id + ")");
//
//        return chatMessage;
//    }
//
//    public void updateStatuses(ObjectId senderId, ObjectId receiverId, ChatMessage.MessageStatus status) {
//        ChatRepository.update(and(
//                eq("sender_id", senderId),
//                eq("receiver_id", receiverId)), set("status", status));
//    }
//}
