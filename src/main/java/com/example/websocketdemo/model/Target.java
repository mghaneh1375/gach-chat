package com.example.websocketdemo.model;

import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

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
        targetPic = null;
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

    public static boolean findInJSONArrayBool(JSONArray jsonArray,
                                         String mode, String id) {

        for (int i = 0; i < jsonArray.length(); i++) {

            JSONObject jsonObject = jsonArray.getJSONObject(i);

            if (
                    (mode != null &&
                            jsonObject.getString("mode").equalsIgnoreCase(mode) &&
                            jsonObject.getString("id").equals(id)
                    ) ||
                            (mode == null && jsonObject.getString("id").equals(id))
            )
                return true;
        }

        return false;
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

    private JSONObject toJSONObject() {

        JSONObject jsonObject = new JSONObject()
//                .put("mode", chatMode.name())
                .put("name", targetName)
                .put("id", targetId);

//        if(classId != null)
//            jsonObject.put("classId", classId);

        if(targetPic != null)
            jsonObject.put("targetPic", targetPic);

        return jsonObject;
    }
}
