package com.example.websocketdemo.db;

import com.example.websocketdemo.WebsocketDemoApplication;
import com.example.websocketdemo.model.ChatMode;
import com.example.websocketdemo.model.Target;
import com.mongodb.BasicDBObject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

import static com.example.websocketdemo.WebsocketDemoApplication.chatRoomRepository;
import static com.mongodb.client.model.Filters.*;


public class ChatRoomRepository extends Common {

    public ChatRoomRepository() {
        init();
    }

    @Override
    void init() {
        table = "chat_room";
//        generalCached.put(table, new ArrayList<>());
        documentMongoCollection = WebsocketDemoApplication.mongoDatabase.getCollection(table);
    }

    public ArrayList<Document> getBySenderOrReceiver(ObjectId userId,
                                                     boolean isTeacher,
                                                     List<Target> targets) {

        ArrayList<Document> constraints = new ArrayList<>();
        ArrayList<Bson> constraintsBson = new ArrayList<>();

        for (Target target : targets) {

            Bson filter;

            if (target.getChatMode().equals(ChatMode.PEER)) {

                if (isTeacher) {

                    //todo : consider old msgs and don't return them

                    filter = and(
                            eq("mode", "peer"),
                            eq("sender_id", userId)
                    );

                    if (!constraintsBson.contains(filter)) {
                        constraintsBson.add(filter);
                        constraints.add(new Document("mode", "peer")
                                .append("sender_id", userId));
                    }

                    filter = and(
                            eq("mode", "peer"),
                            eq("receiver_id", userId)
                    );

                    if (!constraintsBson.contains(filter)) {
                        constraintsBson.add(filter);
                        constraints.add(new Document("mode", "peer")
                                .append("receiver_id", userId)
                        );
                    }

                } else {
                    filter = and(
                            eq("mode", "peer"),
                            eq("sender_id", userId),
                            eq("receiver_id", target.getTargetId())
                    );

                    if (!constraintsBson.contains(filter)) {
                        constraintsBson.add(filter);
                        constraints.add(new Document("mode", "peer")
                                .append("sender_id", userId)
                                .append("receiver_id", target.getTargetId())
                        );
                    }

                    filter = and(
                            eq("mode", "peer"),
                            eq("receiver_id", userId),
                            eq("sender_id", target.getTargetId())
                    );

                    if (!constraintsBson.contains(filter)) {
                        constraintsBson.add(filter);
                        constraints.add(new Document("mode", "peer")
                                .append("receiver_id", userId)
                                .append("sender_id", target.getTargetId())
                        );
                    }
                }

            } else {

                filter = and(
                        eq("mode", "group"),
                        eq("receiver_id", target.getTargetId())
                );

                if (!constraintsBson.contains(filter)) {
                    constraintsBson.add(filter);
                    constraints.add(new Document("mode", "group")
                            .append("receiver_id", target.getTargetId())
                    );
                }

            }
        }

        ArrayList<Document> output = getWholeCache(constraints);
//        System.out.println("find from cache " + output.size());

        if (output.size() == targets.size())
            return output;

        ArrayList<ObjectId> excludes = new ArrayList<>();
        for (Document doc : output) {
//            System.out.println(doc);
            excludes.add(doc.getObjectId("_id"));
        }

        ArrayList<Document> docs = chatRoomRepository.find(and(
                nin("_id", excludes),
                or(constraintsBson)
                ), new BasicDBObject("sender_id", 1).append("receiver_id", 1)
                        .append("new_msgs", 1).append("new_msgs_rev", 1)
                        .append("mode", 1).append("persons", 1)
        );

        for (Document doc : docs) {

            if (output.contains(doc))
                continue;

            output.add(doc);
        }

        return output;
    }

    public Document findOneBySenderOrReceiver(ObjectId senderId, ObjectId receiverId) {

        ArrayList<Document> constraints = new ArrayList<>();

        constraints.add(new Document("mode", "peer")
                .append("sender_id", senderId)
                .append("receiver_id", receiverId)
        );

        constraints.add(new Document("mode", "peer")
                .append("receiver_id", senderId)
                .append("sender_id", receiverId)
        );

        constraints.add(new Document("mode", "group")
                .append("receiver_id", receiverId)
        );

        ArrayList<Document> docs = getWholeCache(constraints);
//        System.out.println(docs.size());

        if (docs.size() > 0)
            return docs.get(0);

        ArrayList<Bson> constraintsBson = new ArrayList<>();

        constraintsBson.add(
                and(
                        eq("mode", "peer"),
                        eq("sender_id", senderId),
                        eq("receiver_id", receiverId)
                )
        );

        constraintsBson.add(
                and(
                        eq("mode", "group"),
                        eq("receiver_id", receiverId)
                )
        );

        constraintsBson.add(
                and(
                        eq("mode", "peer"),
                        eq("receiver_id", senderId),
                        eq("sender_id", receiverId)
                )
        );


        return chatRoomRepository.findOne(
                or(constraintsBson), null
        );
    }

}
