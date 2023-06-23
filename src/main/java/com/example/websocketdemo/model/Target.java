package com.example.websocketdemo.model;

import com.example.websocketdemo.db.UserRepository;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import static com.example.websocketdemo.utility.Statics.STATICS_SERVER;

public class Target {

    private final ObjectId targetId;
    private final String targetName;
    private final String targetPic;

    public Target(ObjectId targetId, String targetName) {
        this.targetId = targetId;
        this.targetName = targetName;
        targetPic = "";
    }

    public Target(ObjectId targetId,
                  String targetName,
                  String targetPic) {
        this.targetId = targetId;
        this.targetName = targetName;
        this.targetPic = targetPic;
    }

    public static JSONArray toJSONArray(List<Target> targets) {

        JSONArray jsonArray = new JSONArray();
        for (Target target : targets) {
            jsonArray.put(target.toJSONObject());
        }

        return jsonArray;
    }

    public static Target searchInTargets(List<Target> targets, ObjectId id) {

        for (Target target : targets) {

            if (target.getTargetId().equals(id))
                return target;

        }

        return null;
    }

    public ObjectId getTargetId() {
        return targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public JSONObject toJSONObject() {

        JSONObject jsonObject = new JSONObject()
                .put("name", targetName)
                .put("id", targetId);

        jsonObject.put("targetPic", getPicUrl());
        return jsonObject;
    }

    public String getPicUrl() {

        if (targetPic != null && !targetPic.isEmpty())
            return STATICS_SERVER + UserRepository.FOLDER + "/" + targetPic;

        return STATICS_SERVER + UserRepository.FOLDER + "/default.jpeg";

    }
}
