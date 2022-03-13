package com.example.websocketdemo.service;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.example.websocketdemo.WebsocketDemoApplication.chatRoomRepository;
import static com.mongodb.client.model.Filters.eq;

@Service
public class ChatRoomService {

    public Optional<String> getChatId(
            ObjectId senderId, ObjectId receiverId, boolean createIfNotExist) {

        String chatId =
                String.format("%s_%s", senderId, receiverId);

        if (chatRoomRepository.exist(eq("chat_id", chatId)))
            return Optional.of(chatId);

        if (!createIfNotExist)
            return Optional.empty();

        chatRoomRepository.insertOne(new Document("chat_id", senderId + "_" + receiverId));
        chatRoomRepository.insertOne(new Document("chat_id", receiverId + "_" + senderId));

        return Optional.of(chatId);
    }
}
