package com.example.websocketdemo.utility;

import org.bson.Document;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static com.example.websocketdemo.utility.Statics.ONE_DAY_MIL_SEC;


public class Utility {

    private static final Pattern justNumPattern = Pattern.compile("^\\d+$");
    private static final Pattern passwordStrengthPattern = Pattern.compile("^(?=.*[0-9])(?=.*[A-z])(?=\\S+$).{8,}$");
    private static final Pattern mailPattern = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    public static Document searchInDocumentsKeyVal(List<Document> arr, String key, Object val) {

        if (arr == null)
            return null;

        for (Document doc : arr) {
            if (doc.containsKey(key) && doc.get(key).equals(val))
                return doc;
        }

        return null;
    }

    public static String convertIntToTime(int time) {
        String timeStr = (time < 1000) ? "0" + time : time + "";
        return timeStr.substring(0, 2) + ":" + timeStr.substring(2);
    }

    public static boolean isValidMail(String in) {
        return mailPattern.matcher(convertPersianDigits(in)).matches();
    }

    public static boolean isValidPostalCode(String in) {
        return justNumPattern.matcher(convertPersianDigits(in)).matches();
    }

    public static boolean isValidPassword(String in) {
        return passwordStrengthPattern.matcher(convertPersianDigits(in)).matches();
    }

    public static boolean isValidNum(String in) {
        return justNumPattern.matcher(convertPersianDigits(in)).matches();
    }

    public static String convertPersianDigits(String number) {

        char[] chars = new char[number.length()];
        for (int i = 0; i < number.length(); i++) {

            char ch = number.charAt(i);

            if (ch >= 0x0660 && ch <= 0x0669)
                ch -= 0x0660 - '0';
            else if (ch >= 0x06f0 && ch <= 0x06F9)
                ch -= 0x06f0 - '0';

            chars[i] = ch;
        }

        return new String(chars);
    }

    public static int getCurrTime() {

        SimpleDateFormat df = new SimpleDateFormat("HHmm");
        df.setTimeZone(TimeZone.getTimeZone("GMT+4:30"));
        Date date = new Date();
        return Integer.parseInt(df.format(date));
    }

    public static long getTimestamp(String date) {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return formatter.parse(date).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public static long getTimestamp(String date, String time) {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try {
            return formatter.parse(date + " " + time).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public static String generateErr(String msg) {
        return new JSONObject()
                .put("status", "nok")
                .put("msg", msg)
                .toString();
    }

    public static String generateSuccessMsg(String key, Object val) {
        return new JSONObject()
                .put("status", "ok")
                .put(key, val)
                .toString();
    }

    public static String generateSuccessMsg(PairValue ... params) {

        JSONObject jsonObject = new JSONObject()
                .put("status", "ok");

        for(PairValue p : params)
            jsonObject.put(p.getKey().toString(), p.getValue());

        return jsonObject.toString();
    }

    public static int getPast(int days) {
        Locale loc = new Locale("en_US");
        SolarCalendar sc = new SolarCalendar(-ONE_DAY_MIL_SEC * days);
        return Integer.parseInt(String.valueOf(sc.year) + String.format(loc, "%02d",
                sc.month) + String.format(loc, "%02d", sc.date));
    }

    public static int getToday() {
        Locale loc = new Locale("en_US");
        SolarCalendar sc = new SolarCalendar();
        return Integer.parseInt(String.valueOf(sc.year) + String.format(loc, "%02d",
                sc.month) + String.format(loc, "%02d", sc.date));
    }
}
