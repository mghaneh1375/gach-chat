package com.example.websocketdemo.db;


import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;

import static com.example.websocketdemo.utility.Statics.*;
import static com.mongodb.client.model.Filters.eq;

public abstract class Common extends Repository {

    MongoCollection<Document> documentMongoCollection = null;
    String table = "";
    String secKey = "";

    public boolean exist(Bson filter) {
        return documentMongoCollection.countDocuments(filter) > 0;
    }

    public ArrayList<Document> find(Bson filter, Bson project) {

        FindIterable<Document> cursor;

        if(project == null) {
            if(filter == null)
                cursor = documentMongoCollection.find();
            else
                cursor = documentMongoCollection.find(filter);
        }
        else {
            if(filter == null)
                cursor = documentMongoCollection.find().projection(project);
            else
                cursor = documentMongoCollection.find(filter).projection(project);
        }

        ArrayList<Document> result = new ArrayList<>();

        for(Document doc : cursor)
            result.add(doc);

        return result;
    }

    public Document findOne(Bson filter, Bson project) {

        FindIterable<Document> cursor;
        if(project == null)
            cursor = documentMongoCollection.find(filter);
        else
            cursor = documentMongoCollection.find(filter).projection(project);

        if(cursor.iterator().hasNext())
            return cursor.iterator().next();

        return null;
    }

    public ArrayList<Document> getWholeCache(ArrayList<Document> filters) {
        return getWholeCacheRepo(table, filters);
    }

    public synchronized Document findById(ObjectId id) {

        Document cached = isInCache(table, id);
        if(cached != null)
            return cached;

        FindIterable<Document> cursor = documentMongoCollection.find(eq("_id", id));
        if(cursor.iterator().hasNext()) {
            Document doc = cursor.iterator().next();

            if(!table.isEmpty()) {
                if(secKey.isEmpty() || !doc.containsKey(secKey))
                    addToCache(table, doc, CLASS_LIMIT_CACHE_SIZE, CLASS_EXPIRATION_SEC);
                else
                    addToCache(table, doc, doc.get(secKey), CLASS_LIMIT_CACHE_SIZE, CLASS_EXPIRATION_SEC);
            }

            return doc;
        }

        return null;
    }

    public synchronized Document findBySecKey(Object val) {

        Document cached = isInCache(table, val);
        if(cached != null)
            return cached;

        FindIterable<Document> cursor = documentMongoCollection.find(eq(secKey, val));

        if(cursor.iterator().hasNext()) {

            Document doc = cursor.iterator().next();

            if(!table.isEmpty())
                addToCache(table, doc, val, CLASS_LIMIT_CACHE_SIZE, CLASS_EXPIRATION_SEC);

            return doc;
        }

        return null;
    }

    public ObjectId insertOneWithReturnId(Document document) {
        documentMongoCollection.insertOne(document);
        return document.getObjectId("_id");
    }

    public void insertOne(Document newDoc) {
        documentMongoCollection.insertOne(newDoc);
    }

    public void updateOne(Bson filter, Bson update) {
        documentMongoCollection.updateOne(filter, update);
    }

    public void replaceOne(ObjectId id, Document newDoc) {
        documentMongoCollection.replaceOne(eq("_id", id), newDoc);
    }

    abstract void init();
}
