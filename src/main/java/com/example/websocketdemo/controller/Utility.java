package com.example.websocketdemo.controller;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;

import java.util.List;

import static com.example.websocketdemo.WebsocketDemoApplication.chatRoomRepository;

class Utility {

    static void hasAccess(ObjectId userId, String chatId
    ) throws AuthenticationException {

        Document chatRoom = chatRoomRepository.findById(new ObjectId(chatId));
        System.out.println("IN UTILITY");

        if (chatRoom == null)
            throw new AuthenticationCredentialsNotFoundException("Has no access");

        if (!chatRoom.getObjectId("sender_id").equals(userId) &&
                !chatRoom.getObjectId("receiver_id").equals(userId)
        )
            throw new AuthenticationCredentialsNotFoundException("Has no access");

    }

}
