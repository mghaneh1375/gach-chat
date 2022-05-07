package com.example.websocketdemo.controller;

import com.example.websocketdemo.exception.NotActivateAccountException;
import com.example.websocketdemo.exception.UnAuthException;
import com.example.websocketdemo.model.*;
import com.example.websocketdemo.utility.Authorization;
import com.example.websocketdemo.utility.PairValue;
import com.example.websocketdemo.utility.Utility;
import com.example.websocketdemo.validator.EnumValidator;
import com.example.websocketdemo.validator.EnumValidatorImp;
import com.example.websocketdemo.validator.ObjectIdConstraint;
import com.mongodb.BasicDBObject;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

import static com.example.websocketdemo.WebsocketDemoApplication.*;
import static com.example.websocketdemo.utility.Statics.JSON_NOT_ACCESS;
import static com.example.websocketdemo.utility.Statics.JSON_NOT_VALID_PARAMS;
import static com.mongodb.client.model.Filters.*;

@Controller
public class ChatController extends Router {

    private final static Integer UPDATE_PERIOD_MSEC = 60000;
    private final static Integer UPDATE_BACK_PERIOD_MSEC = 6000;
    private final static Integer PER_PAGE = 7;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping(value = "/api/chats")
    @ResponseBody
    public String getChats(HttpServletRequest request)
            throws UnAuthException, NotActivateAccountException {

        Document user = getUserWithOutCheckCompleteness(request);
        ObjectId senderId = user.getObjectId("_id");

        ArrayList<Object> currentClassAndTeachers = Authorization.isTeacher(user.getString("access")) ?
                getCurrentClassIds(senderId) :
                getCurrentClassAndTeacherIds(user);

        ArrayList<Document> chats =
                chatRoomRepository.getBySenderOrReceiver(senderId, currentClassAndTeachers);

        JSONArray jsonArray = new JSONArray();
        List<JSONObject> jsonObjects = new ArrayList<>();
        ArrayList<ObjectId> excludes = new ArrayList<>();

        for (Document chat : chats) {

            Document target;
            String targetName;
            boolean amISender;
            String mode;

            if (chat.getString("mode").equals("peer")) {
                amISender = senderId.equals(chat.getObjectId("sender_id"));
                target = amISender ?
                        userRepository.findById(chat.getObjectId("receiver_id")) :
                        userRepository.findById(chat.getObjectId("sender_id"));

                targetName = target.getString("name_fa") + " " + target.getString("last_name_fa");
                mode = "peer";
            } else {
                amISender = true;
                target = classRepository.findById(chat.getObjectId("receiver_id"));
                targetName = target.getString("name");
                mode = "group";
            }

            JSONObject jsonObject = new JSONObject()
                    .put("receiverName", targetName)
                    .put("receiverId", target.getObjectId("_id"))
                    .put("mode", mode);

            if (mode.equals("peer")) {
//                System.out.println(amISender);
//                System.out.println(amISender ?
//                        chat.getInteger("new_msgs") :
//                        chat.getInteger("new_msgs_rev"));

                jsonObject.put("newMsgs", amISender ?
                        chat.getInteger("new_msgs") :
                        chat.getInteger("new_msgs_rev")
                );
            } else {

                Document doc = Utility.searchInDocumentsKeyVal(chat.getList("persons", Document.class),
                        "user_id", senderId
                );

                if (doc == null)
                    jsonObject.put("newMsgs", chat.getInteger("new_msgs"));
                else
                    jsonObject.put("newMsgs", chat.getInteger("new_msgs") - doc.getInteger("seen"));
            }

            jsonObjects.add(jsonObject);

            excludes.add(amISender ?
                    chat.getObjectId("receiver_id") :
                    chat.getObjectId("sender_id")
            );
        }

        jsonObjects.sort(Comparator.comparingInt(a -> a.getInt("newMsgs")));

        for (JSONObject jsonObject : jsonObjects)
            jsonArray.put(jsonObject);

        if (user.getString("access").equals(Access.STUDENT.getName())) {

            for (Object itr : currentClassAndTeachers) {

                PairValue pairValue = (PairValue) itr;

                if (!excludes.contains((ObjectId) pairValue.getKey())) {

                    jsonArray.put(new JSONObject()
                            .put("receiverName", classRepository.findById((ObjectId) pairValue.getKey()).getString("name"))
                            .put("receiverId", pairValue.getKey())
                            .put("mode", "group")
                            .put("newMsgs", 0)
                    );

                    excludes.add((ObjectId) pairValue.getKey());
                }

                if (!excludes.contains((ObjectId) pairValue.getValue())) {

                    Document targetUser = userRepository.findById((ObjectId) pairValue.getValue());
                    jsonArray.put(new JSONObject()
                            .put("receiverName", targetUser.getString("name_fa") + " " + targetUser.getString("last_name_fa"))
                            .put("receiverId", pairValue.getValue())
                            .put("mode", "peer")
                            .put("newMsgs", 0)
                    );

                    excludes.add((ObjectId) pairValue.getValue());

                }

            }


        } else {
            for (Object itr : currentClassAndTeachers) {

                if (!excludes.contains((ObjectId) itr)) {

                    jsonArray.put(new JSONObject()
                            .put("receiverName", classRepository.findById((ObjectId) itr).getString("name"))
                            .put("receiverId", itr)
                            .put("mode", "group")
                            .put("newMsgs", 0)
                    );

                }
            }
        }

        return Utility.generateSuccessMsg("chats", jsonArray);
    }

    @GetMapping(value = "/api/chat/{mode}/{receiverId}/{lastCreatedAt}")
    @ResponseBody
    public String getChat(HttpServletRequest request,
                          @PathVariable @EnumValidator(enumClazz = ChatMode.class) @NotBlank String mode,
                          @PathVariable @ObjectIdConstraint ObjectId receiverId,
                          @PathVariable long lastCreatedAt)
            throws UnAuthException, NotActivateAccountException {

        Document user = getUserWithOutCheckCompleteness(request);
        ObjectId senderId = user.getObjectId("_id");

        if (Authorization.isPureStudent(user.getString("access"))) {

            ArrayList<Object> currentClassAndTeachers = getCurrentClassAndTeacherIds(user);

            boolean allow = false;

            for (Object itr : currentClassAndTeachers) {

                PairValue pairValue = (PairValue) itr;

                if (mode.equals("group") &&
                        pairValue.getKey().equals(receiverId)
                ) {
                    allow = true;
                    break;
                }

                if (mode.equals("peer") &&
                        pairValue.getValue().equals(receiverId)
                ) {
                    allow = true;
                    break;
                }
            }

            if (!allow)
                return JSON_NOT_ACCESS;
        } else if (mode.equals("group")) {

            ArrayList<Object> currentClasses = getCurrentClassIds(senderId);
            if (!currentClasses.contains(receiverId))
                return JSON_NOT_ACCESS;

        }

        Document chatRoom = chatRoomRepository.findOneBySenderOrReceiver(senderId, receiverId);

        long curr = System.currentTimeMillis();

        if (chatRoom == null) {

            ObjectId chatId;

            if (mode.equals("peer")) {
                chatId = chatRoomRepository.insertOneWithReturnId(
                        new Document("chats", new ArrayList<>())
                                .append("last_seen", curr)
                                .append("last_seen_rev", (long) -1)
                                .append("last_update", curr)
                                .append("mode", "peer")
                                .append("new_msgs", 0)
                                .append("new_msgs_rev", 0)
                                .append("sender_id", senderId)
                                .append("receiver_id", receiverId)
                );
            } else {

                List<Document> persons = new ArrayList<>();
                Document theClass = classRepository.findById(receiverId);
                List<Document> students = theClass.getList("students", Document.class);

                for (Document student : students)
                    persons.add(new Document("user_id", student.getObjectId("_id"))
                            .append("seen", 0)
                            .append("last_seen", senderId.equals(student.getObjectId("_id")) ? curr : (long) -1)
                    );

                persons.add(new Document("user_id", theClass.getObjectId("teacher_id"))
                        .append("seen", 0)
                        .append("last_seen", senderId.equals(theClass.getObjectId("teacher_id")) ? curr : (long) -1)
                );

                chatId = chatRoomRepository.insertOneWithReturnId(
                        new Document("chats", new ArrayList<>())
                                .append("mode", "group")
                                .append("last_update", curr)
                                .append("new_msgs", 0)
                                .append("persons", persons)
                                .append("receiver_id", receiverId)
                );
            }

            return Utility.generateSuccessMsg("data", new JSONObject()
                    .put("chatId", chatId.toString())
                    .put("chats", new JSONArray())
                    .put("totalMsgs", 0)
            );

        }

        if (!chatRoom.getString("mode").equals(mode))
            return JSON_NOT_VALID_PARAMS;

        if (lastCreatedAt == -1) {

            lastCreatedAt = curr;

            if (mode.equals("peer")) {
                if (chatRoom.getObjectId("sender_id").equals(senderId))
                    chatRoom.put("new_msgs", 0);
                else
                    chatRoom.put("new_msgs_rev", 0);
            } else {

                List<Document> persons = chatRoom.getList("persons", Document.class);
                Document doc = Utility.searchInDocumentsKeyVal(persons, "user_id", senderId);

                if (doc == null)
                    persons.add(new Document("user_id", senderId)
                            .append("seen", chatRoom.getInteger("new_msgs"))
                            .append("last_seen", curr)
                    );
                else {
                    doc.put("seen", chatRoom.getInteger("new_msgs"));
                    doc.put("last_seen", curr);
                }

            }
        }

        List<Document> chats = chatRoom.getList("chats", Document.class);
        JSONArray jsonArray = new JSONArray();

        if (chats.size() == 0)
            return Utility.generateSuccessMsg("data", new JSONObject()
                    .put("chatId", chatRoom.getObjectId("_id").toString())
                    .put("chats", jsonArray)
                    .put("totalMsgs", 0)
            );

        Stack<JSONObject> stack = new Stack<>();

        for (int i = chats.size() - 1; i >= 0; i--) {

            Document chat = chats.get(i);

            if (chat.getLong("created_at") >= lastCreatedAt)
                continue;

            boolean amISender = chat.getObjectId("sender").equals(senderId);
            Document sender = null;

            if (!amISender)
                sender = userRepository.findById(chat.getObjectId("sender"));

            JSONObject jsonObject = new JSONObject()
                    .put("content", chat.getString("content"))
                    .put("amISender", amISender)
                    .put("id", chat.getObjectId("_id").toString())
                    .put("createdAt", chat.getLong("created_at"))
//                    .put("status", chat.getString("status"))
                    ;

            if (sender != null)
                jsonObject.put("sender",
                        sender.getString("name_fa") + " " + sender.getString("last_name_fa")
                );

            stack.push(jsonObject);

            if (stack.size() == PER_PAGE)
                break;
        }

        int stackSize = stack.size();

        for (int i = 0; i < stackSize; i++)
            jsonArray.put(stack.pop());

        update(curr, chatRoom);

        return Utility.generateSuccessMsg("data", new JSONObject()
                .put("chatId", chatRoom.getObjectId("_id").toString())
                .put("chats", jsonArray)
                .put("totalMsgs", chats.size())
        );
    }

    @MessageMapping("/chat")
    public void processMessage(@Payload String message) {

        System.out.println(message);
        try {
            JSONObject jsonObject = new JSONObject(message);

            if (!jsonObject.has("type") || !EnumValidatorImp.isValid(
                    jsonObject.getString("type").toLowerCase(),
                    MessageType.class
            ))
                return;

            Document user = getUserWithToken(jsonObject.getString("token"), null);

            ObjectId senderId = user.getObjectId("_id");
            Document chatRoom;

            long curr = System.currentTimeMillis();

            switch (jsonObject.getString("type").toLowerCase()) {

                case "heart":

                    Document chatPresence = chatPresenceRepository.findBySecKey(senderId);
                    if (chatPresence == null) {
                        chatPresenceRepository.insertOne(
                                new Document("last_seen", curr)
                                        .append("last_update", curr)
                                        .append("user_id", senderId)
                        );
                    } else {
                        chatPresence.put("last_seen", curr);
                        update2(curr, chatPresence);
                    }

                    if (jsonObject.has("chatId")) {

                        chatRoom = chatRoomRepository.findById(new ObjectId(jsonObject.getString("chatId")));
                        if (chatRoom == null)
                            return;

                        if (chatRoom.getString("mode").equals("peer")) {

                            if (!chatRoom.getObjectId("sender_id").equals(senderId) &&
                                    !chatRoom.getObjectId("receiver_id").equals(senderId)
                            )
                                return;

                            if (chatRoom.getObjectId("sender_id").equals(senderId)) {
                                chatRoom.put("last_seen", curr);
                                chatRoom.put("new_msgs", 0);
                            } else {
                                chatRoom.put("last_seen_rev", curr);
                                chatRoom.put("new_msgs_rev", 0);
                            }

                        } else {

                            List<Document> persons = chatRoom.getList("persons", Document.class);
                            Document doc = Utility.searchInDocumentsKeyVal(persons, "user_id", senderId);
                            if (doc == null)
                                return;

                            doc.put("seen", chatRoom.getInteger("new_msgs"));
                            doc.put("last_seen", curr);
                        }

                        update(curr, chatRoom);
                    }

                    return;

                case "leave":

                    if (!jsonObject.has("chatId"))
                        return;

                    chatRoom = chatRoomRepository.findById(new ObjectId(jsonObject.getString("chatId")));

                    if (chatRoom == null)
                        return;

                    if (chatRoom.getString("mode").equals("peer")) {

                        if (!chatRoom.getObjectId("sender_id").equals(senderId) &&
                                !chatRoom.getObjectId("receiver_id").equals(senderId)
                        )
                            return;

                        if (chatRoom.getObjectId("sender_id").equals(senderId))
                            chatRoom.put("last_seen", chatRoom.getLong("last_seen") - UPDATE_BACK_PERIOD_MSEC);
                        else
                            chatRoom.put("last_seen_rev", chatRoom.getLong("last_seen_rev") - UPDATE_BACK_PERIOD_MSEC);

                    } else {

                        List<Document> persons = chatRoom.getList("persons", Document.class);
                        Document doc = Utility.searchInDocumentsKeyVal(persons, "user_id", senderId);
                        if (doc == null)
                            return;

                        doc.put("last_seen", doc.getLong("last_seen") - UPDATE_BACK_PERIOD_MSEC);
                    }

                    update(curr, chatRoom);
                    return;

                case "send":

                    if (!jsonObject.has("chatId") ||
                            !jsonObject.has("content")
                    )
                        return;

                    chatRoom = chatRoomRepository.findById(new ObjectId(jsonObject.getString("chatId")));

                    if (chatRoom == null)
                        return;

//                    System.out.println("salam");
//                    System.out.println(chatRoom);

                    boolean amIStarter = false;
                    long lastSeenTarget = -1;
                    List<Document> persons = null;

                    if (chatRoom.getString("mode").equals("peer")) {

                        if (!chatRoom.getObjectId("sender_id").equals(senderId) &&
                                !chatRoom.getObjectId("receiver_id").equals(senderId)
                        )
                            return;

                        amIStarter = senderId.equals(
                                chatRoom.getObjectId("sender_id")
                        );

                        lastSeenTarget = amIStarter ?
                                chatRoom.getLong("last_seen_rev") :
                                chatRoom.getLong("last_seen");

//                        System.out.println(amIStarter + " " + lastSeenTarget);

                    } else {

                        persons = chatRoom.getList("persons", Document.class);
                        Document doc = Utility.searchInDocumentsKeyVal(persons, "user_id", senderId);

                        if (doc == null)
                            return;

                        doc.put("seen", doc.getInteger("seen") + 1);
                    }

                    ObjectId newChatId = new ObjectId();

                    List<Document> chats = chatRoom.getList("chats", Document.class);
                    Document chat = new Document("content", jsonObject.getString("content"))
                            .append("created_at", curr)
                            .append("sender", senderId)
                            .append("_id", newChatId)
//                            .append("status", ChatMessage.MessageStatus.RECEIVED.toString())
                            ;

                    chats.add(chat);

                    ChatMessage chatMessage = new ChatMessage(
                            jsonObject.getString("content"),
                            newChatId.toString(),
                            curr,
                            jsonObject.getString("chatId"),
                            senderId.toString(),
                            user.getString("name_fa") + " " + user.getString("last_name_fa")
                    );

                    if (curr - lastSeenTarget > UPDATE_BACK_PERIOD_MSEC) {

//                        System.out.println("xxx");
                        if (amIStarter)
                            chatRoom.put("new_msgs_rev", chatRoom.getInteger("new_msgs_rev") + 1);
                        else
                            chatRoom.put("new_msgs", chatRoom.getInteger("new_msgs") + 1);

                        if (chatRoom.getString("mode").equals("peer")) {

                            Document chatPresenceTmp = chatPresenceRepository.findBySecKey(
                                    amIStarter ? chatRoom.getObjectId("receiver_id") :
                                            chatRoom.getObjectId("sender_id")
                            );

                            if (chatPresenceTmp != null &&
                                    curr - chatPresenceTmp.getLong("last_seen") < 8000) {

                                String postfix = amIStarter ? chatRoom.getObjectId("receiver_id").toString() :
                                        chatRoom.getObjectId("sender_id").toString();

                                sendChatPresenceMsg(postfix,
                                        user.getString("name_fa") + " " + user.getString("last_name_fa"),
                                        senderId.toString(),
                                        amIStarter ? chatRoom.getInteger("new_msgs_rev") :
                                                chatRoom.getInteger("new_msgs"),
                                        "peer",
                                        chatMessage
                                );
                            }
                        } else if (persons != null) {

                            for (Document doc : persons) {

                                if (doc.getObjectId("user_id").equals(senderId))
                                    continue;

                                if (curr - doc.getLong("last_seen") <= UPDATE_BACK_PERIOD_MSEC) {
                                    doc.put("seen", chatRoom.getInteger("new_msgs"));
                                    continue;
                                }

                                Document chatPresenceTmp = chatPresenceRepository.findBySecKey(
                                        doc.getObjectId("user_id")
                                );

                                if (chatPresenceTmp != null &&
                                        curr - chatPresenceTmp.getLong("last_seen") < 8000) {

                                    String postfix = doc.getObjectId("user_id").toString();

                                    sendChatPresenceMsg(postfix,
                                            "",
                                            chatRoom.getObjectId("receiver_id").toString(),
                                            chatRoom.getInteger("new_msgs") - doc.getInteger("seen"),
                                            "group",
                                            chatMessage
                                    );
                                }

                            }

                        }

                    }

                    messagingTemplate.convertAndSend(
                            "/chat/" + chatRoom.getObjectId("_id"),
                            chatMessage
                    );

                    update(curr, chatRoom);
                    return;

                default:
                    return;
            }
        } catch (Exception x) {
            System.out.println(x.getMessage());
            x.printStackTrace();
        }
    }

    private void sendChatPresenceMsg(String postfix,
                                     String senderName,
                                     String sender,
                                     int newMsgs,
                                     String mode,
                                     ChatMessage chatMessage) {

        messagingTemplate.convertAndSend(
                "/chat/" + postfix,
                new ChatNotification(
                        senderName,
                        sender,
                        newMsgs,
                        mode,
                        chatMessage
                )
        );

    }

    private void update(long curr, Document chatRoom) {
        if (curr - chatRoom.getLong("last_update") > UPDATE_PERIOD_MSEC) {
            chatRoom.put("last_update", curr);
            chatRoomRepository.replaceOne(chatRoom.getObjectId("_id"), chatRoom);
        }
    }

    private void update2(long curr, Document chatPresence) {
        if (curr - chatPresence.getLong("last_update") > UPDATE_PERIOD_MSEC) {
            chatPresence.put("last_update", curr);
            chatPresenceRepository.replaceOne(chatPresence.getObjectId("_id"), chatPresence);
        }
    }

    private ArrayList<Object> getCurrentClassAndTeacherIds(Document user) {

        List<Document> passed = user.getList("passed", Document.class);
        ArrayList<Object> classAndTeacherIds = new ArrayList<>();
        ArrayList<ObjectId> courseIds = new ArrayList<>();
        int today = Utility.getToday();

        for (Document itr : passed) {

            if (!itr.containsKey("class_id") ||
                    itr.containsKey("success") ||
                    itr.containsKey("final_result") ||
                    courseIds.contains(itr.getObjectId("course_id"))
            )
                continue;

            Document theClass = classRepository.findById(itr.getObjectId("class_id"));

            if (theClass == null || !theClass.containsKey("teacher_id"))
                continue;

            if (theClass.getInteger("start") > today ||
                    theClass.getInteger("end") < today
            )
                continue;

            Document teacher = userRepository.findById(theClass.getObjectId("teacher_id"));
            if (teacher == null)
                continue;

            PairValue p = new PairValue(
                    theClass.getObjectId("_id"),
                    teacher.getObjectId("_id")
            );

            if (classAndTeacherIds.contains(p))
                continue;

            courseIds.add(itr.getObjectId("course_id"));
            classAndTeacherIds.add(p);
        }

        return classAndTeacherIds;
    }

    private ArrayList<Object> getCurrentClassIds(ObjectId teacherId) {

        int today = Utility.getToday();

        ArrayList<Document> docs = classRepository.find(and(
                eq("teacher_id", teacherId),
                lt("start", today),
                gt("end", today)
        ), new BasicDBObject("_id", 1));

        ArrayList<Object> classes = new ArrayList<>();
        for (Document doc : docs)
            classes.add(doc.getObjectId("_id"));

        return classes;
    }
}
