package com.example.websocketdemo.utility;

import com.mongodb.BasicDBObject;
import org.json.JSONObject;

public class Statics {

    //    public final static String STATICS_SERVER = "https://statics.okft.org/";
//    public final static String STATICS_SERVER = "http://185.239.106.26:8083/";
    public final static String STATICS_SERVER = "http://192.168.0.106/";

    public final static boolean DEV_MODE = true;


    public final static long TOKEN_EXPIRATION_MSEC = 60 * 60 * 24 * 7 * 1000;
//    public final static long SOCKET_TOKEN_EXPIRATION_MSEC = 60 * 5 * 1000;
    public final static long SOCKET_TOKEN_EXPIRATION_MSEC = 60 * 10 * 1000;

    public final static int TOKEN_EXPIRATION = 60 * 60 * 24 * 7;

    public final static int CLASS_LIMIT_CACHE_SIZE = 100;
    public final static int CLASS_EXPIRATION_SEC = 60 * 60 * 24 * 7;

    public final static int TOKEN_REUSABLE = 3;

    public final static int MAX_OBJECT_ID_SIZE = 100;
    public final static int MIN_OBJECT_ID_SIZE = 20;

    public final static BasicDBObject USER_DIGEST = new BasicDBObject("_id", 1)
            .append("username", 1)
            .append("name_fa", 1)
            .append("last_name_fa", 1)
            .append("name_en", 1)
            .append("last_name_en", 1)
            .append("NID", 1)
            .append("passport_no", 1)
            .append("pic", 1);

    public static final String JSON_NOT_ACCESS = new JSONObject().put("status", "nok").put("msg", "no access to this method").toString();
    public static final String JSON_NOT_VALID_PARAMS = new JSONObject().put("status", "nok").put("msg", "params is not valid").toString();
}
