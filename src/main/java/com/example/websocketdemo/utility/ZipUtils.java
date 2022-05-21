package com.example.websocketdemo.utility;

import org.json.JSONArray;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ZipUtils {

    public static String compress(String str) {

        if (str == null || str.length() == 0) {
            return str;
        }

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(out);
            gzip.write(str.getBytes());
            gzip.close();
            return out.toString("ISO-8859-1");

        } catch (IOException e) {
            e.printStackTrace();
        }

        return str;
    }

    public static JSONArray extract(String str) {

        JSONArray jsonArray;

        try {
            jsonArray = new JSONArray(str);
        }
        catch (Exception x) {

            try {

                ByteArrayInputStream bis = new ByteArrayInputStream(str.getBytes());
                GZIPInputStream gis = new GZIPInputStream(bis);

                BufferedReader br = new BufferedReader(new InputStreamReader(gis, "UTF-8"));
                StringBuilder sb = new StringBuilder();

                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();
                gis.close();
                bis.close();

                return jsonArray = new JSONArray(sb.toString());
            }
            catch (Exception e) {}

        }

        return new JSONArray();


    }
}
