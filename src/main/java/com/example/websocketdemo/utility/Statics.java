package com.example.websocketdemo.utility;

import org.json.JSONObject;

public class Statics {

    public final static String STATICS_SERVER = "https://statics.irysc.com/";

    public final static boolean DEV_MODE = true;
    public static final long ONE_DAY_MIL_SEC = 86400000;

    public final static long SOCKET_TOKEN_EXPIRATION_MSEC = 60 * 10 * 1000;
    public final static long SOCKET_TOKEN_CAUTION_TIME = 40 * 1000;

    public final static int CLASS_LIMIT_CACHE_SIZE = 100;
    public final static int CLASS_EXPIRATION_SEC = 60 * 60 * 24 * 7;

    public final static int TOKEN_REUSABLE = 3;
    public final static int HEART_BEAT = 10000;

    public static final String JSON_NOT_ACCESS = new JSONObject().put("status", "nok").put("msg", "no access to this method").toString();
    public static final String JSON_NOT_VALID_PARAMS = new JSONObject().put("status", "nok").put("msg", "params is not valid").toString();
}
