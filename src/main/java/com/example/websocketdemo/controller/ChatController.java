package com.example.websocketdemo.controller;

import com.example.websocketdemo.exception.NotAccessException;
import com.example.websocketdemo.exception.NotActivateAccountException;
import com.example.websocketdemo.exception.UnAuthException;
import com.example.websocketdemo.model.*;
import com.example.websocketdemo.security.JwtTokenProvider;
import com.example.websocketdemo.utility.Authorization;
import com.example.websocketdemo.utility.PairValue;
import com.example.websocketdemo.utility.Utility;
import com.example.websocketdemo.validator.EnumValidatorImp;
import com.example.websocketdemo.validator.ObjectIdConstraint;
import com.example.websocketdemo.validator.StrongJSONConstraint;
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
import java.util.*;

import static com.example.websocketdemo.WebsocketDemoApplication.*;
import static com.example.websocketdemo.model.Target.searchInTargets;
import static com.example.websocketdemo.utility.Authorization.isAdvisor;
import static com.example.websocketdemo.utility.Statics.*;
import static com.mongodb.client.model.Filters.*;

@Controller
@Validated
public class ChatController extends Router {

    private final static Integer UPDATE_PERIOD_MSEC = 60000;
    private final static Integer UPDATE_BACK_PERIOD_MSEC = HEART_BEAT + 1000;
    private final static Integer PER_PAGE = 30;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;


    @GetMapping(value = "/api/getToken")
    @ResponseBody
    public String getToken(HttpServletRequest request) throws UnAuthException, NotActivateAccountException {

        Document user = getUserWithOutCheckCompleteness(request);

        try {
            PairValue p = jwtTokenProvider.createToken(user);
            String token = (String) p.getKey();

            return Utility.generateSuccessMsg(
                    new PairValue("token", token),
                    new PairValue("reminder", p.getValue()),
                    new PairValue("heartBeatInterval", HEART_BEAT),
                    new PairValue("validityDuration", SOCKET_TOKEN_EXPIRATION_MSEC)
            );
        } catch (Exception x) {
            return Utility.generateErr(x.getMessage());
        }
    }

    private class UserChatDocument {

        Document document;
        ObjectId id;
        String name;
        String filename;
        String path;
        long expireAt;

        public UserChatDocument(Document document,
                                ObjectId id, String name,
                                String filename, String path) {
            this.document = document;
            this.id = id;
            this.name = name;
            this.filename = filename;
            this.path = path;
            expireAt = System.currentTimeMillis() + 20000;
        }
    }

    HashMap<String, UserChatDocument> cachedUserChatDocs = new HashMap<>();

    @PostMapping(value = "/api/hasAccess")
    @ResponseBody
    public String hasAccess(HttpServletRequest request,
                            @RequestBody @StrongJSONConstraint(
                                    params = {
                                            "filename",
                                            "targetId",
                                            "token"
                                    },
                                    paramsType = {
                                            String.class,
                                            String.class, String.class
                                    }
                            ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException {

        JSONObject jsonObject = new JSONObject(jsonStr);

        isServerValid(request,
                new PairValue("filename", jsonObject.getString("filename")),
                new PairValue("targetId", jsonObject.getString("targetId"))
        );

        String token = jsonObject.getString("token");

        HashMap<String, Object> user = getClaims(token);
        ObjectId targetId = new ObjectId(jsonObject.getString("targetId"));

        Target target = searchInTargets(
                (List<Target>) user.get("targets"), targetId
        );

        if (target == null)
            throw new NotAccessException("not access");

        ObjectId senderId = (ObjectId) user.get("_id");
        Document chat = chatRoomRepository.findOne(
                or(
                        and(
                                eq("sender_id", senderId),
                                eq("receiver_id", targetId)
                        ),
                        and(
                                eq("receiver_id", senderId),
                                eq("sender_id", targetId)
                        )
                ), null
        );

        if (chat == null)
            throw new NotAccessException("not access");

        chat = chatRoomRepository.findById(chat.getObjectId("_id"));

        String nonce = senderId.toString() + "_" + System.currentTimeMillis();
        String filename = jsonObject.getString("filename");

        cachedUserChatDocs.put(nonce, new UserChatDocument(
                chat,
                senderId,
                user.get("name").toString(),
                filename,
                nonce + "_" + filename
        ));

        return Utility.generateSuccessMsg(
                new PairValue("nonce", nonce)
        );

    }

    @PostMapping(value = "/api/sendFile")
    @ResponseBody
    public String sendFile(HttpServletRequest request,
                           @RequestBody @StrongJSONConstraint(
                                   params = {"nonce"},
                                   paramsType = {String.class
                                   }
                           ) @NotBlank String jsonStr
    ) throws NotAccessException {

        JSONObject jsonObject = new JSONObject(jsonStr);

        String nonce = jsonObject.getString("nonce");

        isServerValid(request,
                new PairValue("nonce", nonce)
        );

        if (!cachedUserChatDocs.containsKey(nonce))
            return JSON_NOT_VALID_PARAMS;

        UserChatDocument cached = cachedUserChatDocs.get(nonce);
        cachedUserChatDocs.remove(nonce);

        long curr = System.currentTimeMillis();

        if (cached.expireAt < curr)
            return JSON_NOT_ACCESS;

        doSendMsg(
                "file&&&" + cached.filename + "##" + cached.path,
                cached.id,
                curr,
                cached.name,
                cached.document
        );

        return Utility.generateSuccessMsg();
    }

    @GetMapping(value = "/api/getStudents")
    @ResponseBody
    public String getStudents(HttpServletRequest request)
            throws UnAuthException {

        HashMap<String, Object> user = getClaims(request);
        if (!isAdvisor((List<String>) user.get("accesses")))
            return JSON_NOT_ACCESS;

        final List<Target> wanted = (List<Target>) user.get("targets");

        if (wanted.size() == 0)
            return JSON_NOT_ACCESS;

        return Utility.generateSuccessMsg(
                "students", Target.toJSONArray(wanted)
        );
    }

    @GetMapping(value = "/api/chats")
    @ResponseBody
    public String getChats(HttpServletRequest request)
            throws UnAuthException {

        HashMap<String, Object> user = getClaims(request);

        List<Target> targets = (List<Target>) user.get("targets");

        ObjectId senderId = (ObjectId) user.get("_id");
        boolean isAdvisor = Authorization.isAdvisor((List<String>) user.get("accesses"));

        ArrayList<Document> chats =
                chatRoomRepository.getBySenderOrReceiver(senderId, isAdvisor, targets);

        JSONArray jsonArray = new JSONArray();
        List<JSONObject> jsonObjects = new ArrayList<>();
        ArrayList<ObjectId> excludes = new ArrayList<>();

        for (Document chat : chats) {

            boolean amISender = senderId.equals(chat.getObjectId("sender_id"));

            Target t = searchInTargets(targets, amISender ?
                    chat.getObjectId("receiver_id") :
                    chat.getObjectId("sender_id")
            );

            if (t == null)
                continue;

            JSONObject jsonObject = new JSONObject()
                    .put("receiverName", t.getTargetName())
                    .put("receiverId", t.getTargetId())
                    .put("pic", t.getPicUrl());

            jsonObject.put("newMsgs", amISender ?
                    chat.getInteger("new_msgs") :
                    chat.getInteger("new_msgs_rev")
            );

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

            if (isAdvisor)
                continue;

            jsonArray.put(new JSONObject()
                    .put("receiverName", target.getTargetName())
                    .put("receiverId", target.getTargetId())
                    .put("pic", target.getPicUrl())
                    .put("newMsgs", 0)
            );

            excludes.add(target.getTargetId());
        }

        return Utility.generateSuccessMsg(
                new PairValue("chats", jsonArray),
                new PairValue("heartBeatInterval", HEART_BEAT),
                new PairValue("validityDuration", SOCKET_TOKEN_EXPIRATION_MSEC)
        );
    }

    @GetMapping(value = "/api/getFiles/{chatId}")
    @ResponseBody
    public String getFiles(HttpServletRequest request,
                           @PathVariable @ObjectIdConstraint ObjectId chatId)
            throws UnAuthException {

        Document chat = chatRoomRepository.findById(chatId);
        if (chat == null)
            return JSON_NOT_VALID_PARAMS;

        HashMap<String, Object> user = getClaims(request);
        ObjectId senderId = (ObjectId) user.get("_id");

        ObjectId targetId = senderId.equals(chat.getObjectId("sender_id")) ?
                chat.getObjectId("receiver_id") :
                chat.getObjectId("sender_id");

        List<Target> targets = (List<Target>) user.get("targets");
        if (searchInTargets(targets, targetId) == null)
            return JSON_NOT_ACCESS;

        List<Document> chats = chat.getList("chats", Document.class);
        JSONArray jsonArray = new JSONArray();

        for (Document msg : chats) {

            if (!msg.getString("content").startsWith("file&&&"))
                continue;

            String content = msg.getString("content");
            String originalFilename = "";

            String[] splited = content.replace("file&&&", "").split("##");
            content = STATICS_SERVER + "chat/" + splited[1];
            originalFilename = splited[0];

            jsonArray.put(new JSONObject()
                    .put("content", content)
                    .put("originalFilename", originalFilename)
                    .put("createdAt", msg.getLong("created_at"))
            );
        }

        return Utility.generateSuccessMsg(
                new PairValue("chats", jsonArray)
        );
    }

    @GetMapping(value = "/api/search")
    @ResponseBody
    public String search(HttpServletRequest request,
                         @RequestParam @NotBlank String key)
            throws UnAuthException {

        HashMap<String, Object> user = getClaims(request);

        List<Target> targets = (List<Target>) user.get("targets");
        JSONArray jsonArray = new JSONArray();

        for (Target t : targets) {

            if (!t.getTargetName().contains(key))
                continue;

            JSONObject jsonObject = new JSONObject()
                    .put("receiverName", t.getTargetName())
                    .put("receiverId", t.getTargetId())
                    .put("pic", t.getPicUrl())
                    .put("newMsgs", 0);

            jsonArray.put(jsonObject);
        }

        return Utility.generateSuccessMsg(
                new PairValue("chats", jsonArray)
        );
    }

    @GetMapping(value = "/api/chat/{receiverId}/{lastCreatedAt}")
    @ResponseBody
    public String getChat(HttpServletRequest request,
                          @PathVariable @ObjectIdConstraint ObjectId receiverId,
                          @PathVariable long lastCreatedAt)
            throws UnAuthException {

        HashMap<String, Object> user = getClaims(request);
        List<Target> targets = (List<Target>) user.get("targets");

        if (searchInTargets(targets, receiverId) == null)
            return JSON_NOT_ACCESS;

        ObjectId senderId = (ObjectId) user.get("_id");
        Document chatRoom = chatRoomRepository.findOneBySenderOrReceiver(senderId, receiverId);

        long curr = System.currentTimeMillis();

        if (chatRoom == null) {

            ObjectId chatId = chatRoomRepository.insertOneWithReturnId(
                    new Document("chats", new ArrayList<>())
                            .append("last_seen", curr)
                            .append("last_seen_rev", (long) -1)
                            .append("last_update", curr)
                            .append("new_msgs", 0)
                            .append("new_msgs_rev", 0)
                            .append("sender_id", senderId)
                            .append("receiver_id", receiverId)
            );


            return Utility.generateSuccessMsg("data", new JSONObject()
                    .put("chatId", chatId.toString())
                    .put("chats", new JSONArray())
                    .put("totalMsgs", 0)
            );

        }

        if (lastCreatedAt == -1) {

            lastCreatedAt = curr;

            if (chatRoom.getObjectId("sender_id").equals(senderId))
                chatRoom.put("new_msgs", 0);
            else
                chatRoom.put("new_msgs_rev", 0);
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
            Target sender = null;

            if (!amISender)
                sender = Target.searchInTargets(targets, chat.getObjectId("sender"));

            String content = chat.getString("content");
            boolean isFile = content.startsWith("file&&&");
            String originalFilename = "";

            if (isFile) {
                String[] splited = content.replace("file&&&", "").split("##");
                content = STATICS_SERVER + "chat/" + splited[1];
                originalFilename = splited[0];
            }

            JSONObject jsonObject = new JSONObject()
                    .put("content", content)
                    .put("originalFilename", originalFilename)
                    .put("file", isFile)
                    .put("amISender", amISender)
                    .put("id", chat.getObjectId("_id").toString())
                    .put("createdAt", chat.getLong("created_at"));

            if (sender != null)
                jsonObject.put("sender", sender.getTargetName());

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
                    sendMsg(jsonObject, senderId,
                            curr, (String) user.get("name")
                    );
                    return;

                default:
                    return;
            }
        } catch (Exception x) {
            System.out.println(x.getMessage());
            x.printStackTrace();
        }
    }


    private void sendMsg(JSONObject jsonObject, ObjectId senderId,
                         long curr, String user_name) {

        if (!jsonObject.has("chatId") ||
                !jsonObject.has("content")
        )
            return;

        Document chatRoom = chatRoomRepository.findById(new ObjectId(jsonObject.getString("chatId")));

        if (chatRoom == null)
            return;

        doSendMsg(jsonObject.getString("content"),
                senderId, curr, user_name, chatRoom
        );
    }

    private void doSendMsg(String content, ObjectId senderId,
                           long curr, String user_name,
                           Document chatRoom) {

        boolean amIStarter;
        long lastSeenTarget;
        List<Document> persons = null;
        String senderIdStr = senderId.toString();

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

        ObjectId newChatId = new ObjectId();

        List<Document> chats = chatRoom.getList("chats", Document.class);

        Document chat = new Document("content", content)
                .append("created_at", curr)
                .append("sender", senderId)
                .append("_id", newChatId);

        chats.add(chat);

        ChatMessage chatMessage = new ChatMessage(
                content,
                newChatId.toString(),
                curr,
                chatRoom.getObjectId("_id").toString(),
                senderIdStr,
                user_name
        );

        if (curr - lastSeenTarget > UPDATE_BACK_PERIOD_MSEC) {

            if (amIStarter)
                chatRoom.put("new_msgs_rev", chatRoom.getInteger("new_msgs_rev") + 1);
            else
                chatRoom.put("new_msgs", chatRoom.getInteger("new_msgs") + 1);

            Document chatPresenceTmp = chatPresenceRepository.findBySecKey(
                    amIStarter ? chatRoom.getObjectId("receiver_id") :
                            chatRoom.getObjectId("sender_id")
            );

            if (chatPresenceTmp != null &&
                    curr - chatPresenceTmp.getLong("last_seen") < HEART_BEAT) {

                String postfix = amIStarter ? chatRoom.getObjectId("receiver_id").toString() :
                        chatRoom.getObjectId("sender_id").toString();

                sendChatPresenceMsg(postfix,
                        user_name,
                        amIStarter ? chatRoom.getInteger("new_msgs_rev") :
                                chatRoom.getInteger("new_msgs"),
                        chatMessage
                );
            }


        }

        messagingTemplate.convertAndSend(
                "/chat/" + chatRoom.getObjectId("_id"),
                chatMessage
        );

        update(curr, chatRoom);
    }

    // send notif if user in online but not in wanted chat
    private void sendChatPresenceMsg(String postfix,
                                     String senderName,
                                     int newMsgs,
                                     ChatMessage chatMessage) {

        messagingTemplate.convertAndSend(
                "/chat/" + postfix,
                new ChatNotification(
                        senderName,
                        newMsgs,
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

}
