package com.example.websocketdemo.model;

import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Target {

    private ChatMode chatMode;
    private ObjectId targetId;
    private String targetName;

    public Target(ChatMode chatMode, ObjectId targetId, String targetName) {
        this.chatMode = chatMode;
        this.targetId = targetId;
        this.targetName = targetName;
    }

    public static JSONArray toJSONArray(List<Target> targets) {

        JSONArray jsonArray = new JSONArray();
        for (Target target : targets) {
            jsonArray.put(target.toJSONObject());
        }

        return jsonArray;
    }

    public static List<Target> buildFromJSONArray(JSONArray jsonArray) {

        List<Target> targets = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {

            JSONObject jsonObject = jsonArray.getJSONObject(i);

            targets.add(new Target(
                    jsonObject.getString("mode").equals(ChatMode.GROUP.name()) ?
                            ChatMode.GROUP : ChatMode.PEER,
                    new ObjectId(jsonObject.getString("id")),
                    jsonObject.getString("name")
            ));
        }

        return targets;
    }

    public static boolean findInJSONArray(JSONArray jsonArray,
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
        return new JSONObject()
                .put("mode", chatMode.toString())
                .put("name", targetName)
                .put("id", targetId);
    }
}
