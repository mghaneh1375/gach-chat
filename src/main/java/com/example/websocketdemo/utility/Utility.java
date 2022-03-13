package com.example.websocketdemo.utility;

import org.bson.Document;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;


public class Utility {

    private static final Pattern postalCodePattern = Pattern.compile("^\\d{10}$");
    private static final Pattern justNumPattern = Pattern.compile("^\\d+$");
    private static final Pattern passwordStrengthPattern = Pattern.compile("^(?=.*[0-9])(?=.*[A-z])(?=\\S+$).{8,}$");
    private static final Pattern mailPattern = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
    private static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String ABC = "0123456789";
    private static Random random = new Random();
    private static SecureRandom rnd = new SecureRandom();

    public static boolean isNewPerson(List<Document> passed) {

        if (passed != null) {
            for (Document itr : passed) {
                if (itr.containsKey("class_id"))
                    return false;
            }
        }

        return true;
    }

    public static ArrayList<Document> searchInDocumentsKeyValMulti(List<Document> arr, String key, Object val) {

        if (arr == null)
            return null;

        ArrayList<Document> docs = new ArrayList<>();

        for (Document doc : arr) {
            if (doc.containsKey(key) && doc.get(key).equals(val))
                docs.add(doc);
        }

        return docs;
    }

    public static Document searchInDocumentsKeyVal(List<Document> arr, String key, Object val) {

        if (arr == null)
            return null;

        for (Document doc : arr) {
            if (doc.containsKey(key) && doc.get(key).equals(val))
                return doc;
        }

        return null;
    }

    public static Document searchInDocumentsKeyVal(List<Document> arr, String key, Object val,
                                                   String key2, Object val2) {

        if (arr == null)
            return null;

        for (Document doc : arr) {
            if (doc.containsKey(key) && doc.get(key).equals(val) &&
                    doc.containsKey(key2) && (
                    (val2 == null && doc.get(key2) == null) ||
                            (doc.get(key2) != null && doc.get(key2).equals(val2))
            ))
                return doc;
        }

        return null;
    }

    public static int searchInDocumentsKeyValIdx(List<Document> arr, String key, Object val,
                                                 String key2, Object val2) {

        if (arr == null)
            return -1;

        for (int i = 0; i < arr.size(); i++) {
            Document doc = arr.get(i);
            if (doc.containsKey(key) && doc.get(key).equals(val) && (
                    (val2 == null && doc.get(key2) == null) ||
                            (doc.get(key2) != null && doc.get(key2).equals(val2))
            ))
                return i;
        }

        return -1;
    }

    public static int searchInDocumentsKeyValIdx(List<Document> arr, String key, Object val) {

        if (arr == null)
            return -1;

        for (int i = 0; i < arr.size(); i++) {
            if (arr.get(i).containsKey(key) && arr.get(i).get(key).equals(val))
                return i;
        }

        return -1;
    }

    public static String dayFormatter(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static String dayFormatterDut(String str) {
        switch (str.toLowerCase()) {
            case "sun":
                str = "son";
                break;
            case "tue":
                str = "die";
                break;
            case "wed":
                str = "mit";
                break;
            case "thu":
                str = "don";
        }

        return camel(str.substring(0, 2), true);
    }

    public static String convertStringToDate(String date, String delimeter) {
        return date.substring(0, 4) + delimeter + date.substring(4, 6) + delimeter + date.substring(6, 8);
    }

    public static int convertStringToDate(String date) {
        return Integer.parseInt(date.substring(0, 4) + date.substring(5, 7) + date.substring(8, 10));
    }

    public static int convertTimeToInt(String time) {
        return Integer.parseInt(time.replace(":", ""));
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

    public static String randomString(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        return sb.toString();
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

    public static void convertPersian(JSONObject jsonObject) {

        for (String key : jsonObject.keySet()) {
            if (jsonObject.get(key) instanceof Integer)
                jsonObject.put(key, Integer.parseInt(Utility.convertPersianDigits(jsonObject.getInt(key) + "")));
            else if (jsonObject.get(key) instanceof String)
                jsonObject.put(key, Utility.convertPersianDigits(jsonObject.getString(key)));
        }
    }

    public static String formatPrice(int price) {
        return String.format("%,d", price);
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

    public static String camel(String text, boolean firstLetterCapital) {
        String[] words = text.split("[\\W_]+");
        StringBuilder builder = new StringBuilder();
        boolean firstLetter = false;

        for (int i = 0; i < words.length; i++) {

            String word = words[i];
            if (word.isEmpty())
                continue;

            char first = Character.toUpperCase(word.charAt(0));

            if (!firstLetter && !firstLetterCapital) {
                first = word.charAt(0);
                firstLetter = true;
            }

            word = first + word.substring(1).toLowerCase();
            builder.append(word);
        }
        return builder.toString();
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
}
