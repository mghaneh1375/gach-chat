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

        System.out.println(userId);
        System.out.println(chatId);

        Document chatRoom = chatRoomRepository.findById(new ObjectId(chatId));

        if (chatRoom == null)
            throw new AuthenticationCredentialsNotFoundException("Has no access");

        if (chatRoom.getString("mode").equals("peer")) {

            if (!chatRoom.getObjectId("sender_id").equals(userId) &&
                    !chatRoom.getObjectId("receiver_id").equals(userId)
            )
                throw new AuthenticationCredentialsNotFoundException("Has no access");
        }

        List<Document> persons = chatRoom.getList("persons", Document.class);
        Document doc = com.example.websocketdemo.utility.Utility.searchInDocumentsKeyVal(persons, "user_id", userId);

        if (doc == null)
            throw new AuthenticationCredentialsNotFoundException("Has no access");

    }

}
