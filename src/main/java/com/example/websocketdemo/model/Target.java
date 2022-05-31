package com.example.websocketdemo.model;

import com.example.websocketdemo.db.UserRepository;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import static com.example.websocketdemo.utility.Statics.STATICS_SERVER;

public class Target {
    public ObjectId getClassId() {
        return classId;
    }

    public String getTargetPic() {
        return targetPic;
    }

    private final ChatMode chatMode;
    private final ObjectId targetId;
    private final String targetName;
    private final ObjectId classId;
    private final String targetPic;

    public Target(ChatMode chatMode, ObjectId targetId, String targetName) {
        this.chatMode = chatMode;
        this.targetId = targetId;
        this.targetName = targetName;
        classId = null;
        targetPic = "";
    }

    public Target(ObjectId targetId,
                  String targetName,
                  String targetPic,
                  ObjectId classId) {
        this.chatMode = ChatMode.PEER;
        this.targetId = targetId;
        this.targetName = targetName;
        this.classId = classId;
        this.targetPic = targetPic;
    }

    public static JSONArray toJSONArray(List<Target> targets) {

        JSONArray jsonArray = new JSONArray();
        for (Target target : targets) {
            jsonArray.put(target.toJSONObject());
        }

        return jsonArray;
    }

    public static Target searchInTargets(List<Target> targets, ChatMode chatMode, ObjectId id) {

        for (Target target : targets) {

            if (
                    ((target.getChatMode().equals(chatMode)) ||
                            chatMode == null
                    ) && target.getTargetId().equals(id)
            )
                return target;

        }

        return null;
    }

    public ChatMode getChatMode() {
        return chatMode;
    }

    public ObjectId getTargetId() {
        return targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public JSONObject toJSONObject() {

        JSONObject jsonObject = new JSONObject()
//                .put("mode", chatMode.name())
                .put("name", targetName)
                .put("id", targetId);

//        if(classId != null)
//            jsonObject.put("classId", classId);

        jsonObject.put("targetPic", getPicUrl());
        return jsonObject;
    }

    public String getPicUrl() {

        if (targetPic != null && !targetPic.isEmpty())
            return STATICS_SERVER + UserRepository.FOLDER + "/" + targetPic;

        return STATICS_SERVER + UserRepository.FOLDER + "/default.jpeg";

    }
}
