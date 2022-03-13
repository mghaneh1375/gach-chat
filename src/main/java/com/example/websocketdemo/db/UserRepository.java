package com.example.websocketdemo.db;


import com.example.websocketdemo.WebsocketDemoApplication;
import com.example.websocketdemo.utility.Utility;
import com.example.websocketdemo.validator.ObjectIdValidator;
import com.mongodb.client.FindIterable;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;


import static com.example.websocketdemo.utility.Statics.*;
import static com.mongodb.client.model.Filters.*;

public class UserRepository extends Common {

    public final static String FOLDER = "usersPic";

    public UserRepository() {
        init();
    }

    public synchronized Document findByUnique(String unique, boolean searchInAll) {

        ArrayList<Bson> constraints = new ArrayList<>();

        if(Utility.isValidMail(unique))
            constraints.add(eq("mail", unique));

        if(Utility.isValidNum(unique)) {
            constraints.add(eq("username", unique));

            if(searchInAll)
                constraints.add(and(
                        exists("NID"),
                        eq("NID", unique)
                ));
        }

        if(searchInAll) {

            constraints.add(and(
                    exists("passport_no"),
                    eq("passport_no", unique)
            ));

            try {
                if (ObjectIdValidator.isValid(unique))
                    constraints.add(eq("_id", new ObjectId(unique)));
            }
            catch (Exception x) {
                x.printStackTrace();
            }
        }

        if(constraints.size() == 0)
            return null;

        FindIterable<Document> cursor = documentMongoCollection.find(or(constraints));

        if(cursor.iterator().hasNext())
            return cursor.iterator().next();

        return null;
    }

    public synchronized Document findByUsername(String username) {

        Document user = isInCache(table, username);
        if(user != null)
            return user;

        FindIterable<Document> cursor = documentMongoCollection.find(eq("username", username));
        if(cursor.iterator().hasNext()) {
            Document doc = cursor.iterator().next();
            addToCache(table, doc, username, CLASS_LIMIT_CACHE_SIZE, CLASS_EXPIRATION_SEC);
            return doc;
        }

        return null;
    }

    @Override
    void init() {
        table = "user";
        documentMongoCollection = WebsocketDemoApplication.mongoDatabase.getCollection(table);
    }
}
