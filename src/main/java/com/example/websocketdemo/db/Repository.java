package com.example.websocketdemo.db;

import com.example.websocketdemo.utility.Cache;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;


public class Repository {

    static HashMap<String, ArrayList<Cache>> generalCached = new HashMap<>();

    static Document isInCache(String section, Object id) {

        if(!generalCached.containsKey(section))
            return null;

        ArrayList<Cache> cached = generalCached.get(section);

        for (int i = 0; i < cached.size(); i++) {

            if (cached.get(i).equals(id)) {

                if (cached.get(i).checkExpiration())
                    return (Document) cached.get(i).getValue();

                cached.remove(i);
                return null;
            }

        }

        return null;
    }

    static ArrayList<Document> getWholeCacheRepo(String section, ArrayList<Document> filters) {

        if(!generalCached.containsKey(section))
            return new ArrayList<>();

        ArrayList<Cache> cached = generalCached.get(section);
        ArrayList<Document> output = new ArrayList<>();

        for (Cache cache : cached) {

            if (!cache.checkExpiration())
                continue;

            Document doc = (Document) cache.getValue();
            if(output.contains(doc))
                continue;

            boolean allowAdd = false;

            for(Document filter : filters) {

                boolean match = true;

                for(String key : filter.keySet()) {

//                    System.out.println("cache elem " + key + " " + doc.get(key));
//                    System.out.println("filter elem " + key + " " + filter.get(key));

                    if(!doc.containsKey(key) ||
                            !doc.get(key).equals(filter.get(key))
                    ) {
                        match = false;
                        break;
                    }

                }

//                System.out.println("match is " + match);
                if(match) {
                    allowAdd = true;
                    break;
                }
            }

            if(allowAdd)
                output.add(doc);
        }

        return output;
    }

    static void addToCache(String section, Document doc, Object secKey, int limit, int expirationSec) {

        ArrayList<Cache> cached;

        if(!generalCached.containsKey(section))
            cached = new ArrayList<>();
        else
            cached = generalCached.get(section);

        if(cached.size() >= limit)
            cached.remove(0);

        cached.add(new Cache(expirationSec, doc, doc.getObjectId("_id"), secKey));
        generalCached.put(section, cached);
    }

    static void addToCache(String section, Document doc, int limit, int expirationSec) {

        ArrayList<Cache> cached;

        if(!generalCached.containsKey(section))
            cached = new ArrayList<>();
        else
            cached = generalCached.get(section);

        if(cached.size() >= limit)
            cached.remove(0);

        cached.add(new Cache(expirationSec, doc, doc.getObjectId("_id")));
        generalCached.put(section, cached);
    }
}
