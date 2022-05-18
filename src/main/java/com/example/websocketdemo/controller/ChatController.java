package com.example.websocketdemo.controller;

import com.example.websocketdemo.exception.NotActivateAccountException;
import com.example.websocketdemo.exception.UnAuthException;
import com.example.websocketdemo.model.*;
import com.example.websocketdemo.security.JwtTokenProvider;
import com.example.websocketdemo.utility.PairValue;
import com.example.websocketdemo.utility.Utility;
import com.example.websocketdemo.validator.EnumValidator;
import com.example.websocketdemo.validator.EnumValidatorImp;
import com.example.websocketdemo.validator.ObjectIdConstraint;
import com.example.websocketdemo.validator.StrongJSONConstraint;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.example.websocketdemo.WebsocketDemoApplication.*;
import static com.example.websocketdemo.utility.Statics.*;

@Controller
@Validated
public class ChatController extends Router {

    private final static Integer UPDATE_PERIOD_MSEC = 60000;
    private final static Integer UPDATE_BACK_PERIOD_MSEC = 6000;
    private final static Integer PER_PAGE = 7;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping(value = "/api/getToken")
    @ResponseBody
    public String getToken(HttpServletRequest request,
                           @RequestBody @StrongJSONConstraint(
                                   params = {"captcha"},
                                   paramsType = {String.class},
                                   optionals = {"oldToken"},
                                   optionalsType = {String.class}
                           ) @NotBlank String jsonStr
    ) throws UnAuthException, NotActivateAccountException {

        Document user = getUserWithOutCheckCompleteness(request);
        JSONObject jsonObject = new JSONObject(jsonStr);
        String captcha = jsonObject.getString("captcha");

        //todo validate captcha

        try {
            String token = jwtTokenProvider.createToken(user, Role.ROLE_CLIENT,
                    jsonObject.has("oldToken") ? jsonObject.getString("oldToken") : null);

            return Utility.generateSuccessMsg(
                    new PairValue("token", token),
                    new PairValue("validityDuration", SOCKET_TOKEN_EXPIRATION_MSEC)
            );
        } catch (Exception x) {
            return Utility.generateErr(x.getMessage());
        }
    }

    @GetMapping(value = "/api/chats")
    @ResponseBody
    public String getChats(HttpServletRequest request)
            throws UnAuthException {

        HashMap<String, Object> user = getClaims(request);

        JSONArray targetsJSON = new JSONArray(user.get("targets").toString());
        List<Target> targets = Target.buildFromJSONArray(targetsJSON);

        ObjectId senderId = (ObjectId) user.get("_id");
        boolean isTeacher = user.get("access").equals(Access.TEACHER.getName());

        ArrayList<Document> chats =
                chatRoomRepository.getBySenderOrReceiver(senderId, isTeacher, targets);

        JSONArray jsonArray = new JSONArray();
        List<JSONObject> jsonObjects = new ArrayList<>();
        ArrayList<ObjectId> excludes = new ArrayList<>();

        for (Document chat : chats) {

            boolean amISender;
            Target t;

            if (chat.getString("mode").equals("peer")) {

                amISender = senderId.equals(chat.getObjectId("sender_id"));

                t = searchInTargets(targets, ChatMode.PEER, amISender ?
                        chat.getObjectId("receiver_id") :
                        chat.getObjectId("sender_id")
                );

                if (t == null)
                    continue;

            } else {

                amISender = true;
                t = searchInTargets(targets, ChatMode.GROUP, chat.getObjectId("receiver_id"));

                if (t == null)
                    continue;

            }

            JSONObject jsonObject = new JSONObject()
                    .put("receiverName", t.getTargetName())
                    .put("receiverId", t.getTargetId())
                    .put("mode", t.getChatMode().toString());

            if (t.getChatMode().toString().equals("peer")) {
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

        for (Target target : targets) {

            if (excludes.contains(target.getTargetId()))
                continue;

            jsonArray.put(new JSONObject()
                    .put("receiverName", target.getTargetName())
                    .put("receiverId", target.getTargetId())
                    .put("mode", target.getChatMode().toString())
                    .put("newMsgs", 0)
            );

            excludes.add(target.getTargetId());
        }

        return Utility.generateSuccessMsg("chats", jsonArray);
    }

    @GetMapping(value = "/api/chat/{mode}/{receiverId}/{lastCreatedAt}")
    @ResponseBody
    public String getChat(HttpServletRequest request,
                          @PathVariable @EnumValidator(enumClazz = ChatMode.class) @NotBlank String mode,
                          @PathVariable @ObjectIdConstraint ObjectId receiverId,
                          @PathVariable long lastCreatedAt)
            throws UnAuthException {

        HashMap<String, Object> user = getClaims(request);
        mode = mode.toLowerCase();

        JSONArray targetsJSON = new JSONArray(user.get("targets").toString());

        if (!Target.findInJSONArray(targetsJSON, mode, receiverId.toString()))
            return JSON_NOT_ACCESS;

        ObjectId senderId = (ObjectId) user.get("_id");

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

        try {
            JSONObject jsonObject = new JSONObject(message);

            if (!jsonObject.has("type") || !EnumValidatorImp.isValid(
                    jsonObject.getString("type").toLowerCase(),
                    MessageType.class
            ))
                return;

            HashMap<String, Object> user = getClaims(jsonObject.getString("token"));

            ObjectId senderId = (ObjectId) user.get("_id");
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
                            (String) user.get("name")
                    );

                    if (curr - lastSeenTarget > UPDATE_BACK_PERIOD_MSEC) {

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
                                        (String) user.get("name"),
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


    // send notif if user in online but not in wanted chat
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

    private Target searchInTargets(List<Target> targets, ChatMode chatMode, ObjectId id) {

        for (Target target : targets) {

            if (target.getChatMode().equals(chatMode) &&
                    target.getTargetId().equals(id)
            )
                return target;

        }

        return null;
    }
}
