package com.example.websocketdemo.db;


import com.example.websocketdemo.WebsocketDemoApplication;
import com.example.websocketdemo.utility.Utility;
import com.example.websocketdemo.validator.ObjectIdValidator;
import com.mongodb.client.FindIterable;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.util.ArrayList;


import static com.example.websocketdemo.utility.Statics.*;
import static com.example.websocketdemo.utility.Utility.printException;
import static com.mongodb.client.model.Filters.*;

public class UserRepository extends Common {

    public final static String FOLDER = "usersPic";

    public UserRepository() {
        init();
    }

    public synchronized Document findByUsername(String username) {

        Document user = isInCache(table, username);
        if (user != null)
            return user;

        FindIterable<Document> cursor = documentMongoCollection.find(
                or(
                        eq("phone", username),
                        eq("mail", username)

                )
        );
        if (cursor.iterator().hasNext()) {
            Document doc = cursor.iterator().next();
            addToCache(table, doc, username, 100, (int)ONE_DAY_MIL_SEC * 10);
            return doc;
        }

        return null;
    }


    @Override
    void init() {
        table = "user";
        secKey = "NID";
        documentMongoCollection = WebsocketDemoApplication.mongoDatabase.getCollection(table);
    }

}
