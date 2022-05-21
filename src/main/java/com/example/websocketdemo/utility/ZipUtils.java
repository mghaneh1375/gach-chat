package com.example.websocketdemo.utility;

import org.json.JSONArray;

import java.io.*;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ZipUtils {

    public static String compress(String str) {

        if (str == null || str.length() == 0)
            return str;

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(str.length());
            GZIPOutputStream gzip = new GZIPOutputStream(out);
            gzip.write(str.getBytes());
            gzip.close();
            byte[] compressed = out.toByteArray();
            out.close();
            return Base64.getEncoder().encodeToString(compressed); //"ISO-8859-1"

        } catch (IOException e) {
            e.printStackTrace();
        }

        return str;
    }

    public static JSONArray extract(String str) {

        JSONArray jsonArray = new JSONArray();

        try {
            jsonArray = new JSONArray(str);
        }
        catch (Exception x) {

            try {

                byte[] in = Base64.getDecoder().decode(str);

                ByteArrayInputStream bis = new ByteArrayInputStream(in);
                GZIPInputStream gis = new GZIPInputStream(bis);

                BufferedReader br = new BufferedReader(new InputStreamReader(gis, "UTF-8"));
                StringBuilder sb = new StringBuilder();

                String line;
                while ((line = br.readLine()) != null)
                    sb.append(line);

                br.close();
                gis.close();
                bis.close();

                jsonArray = new JSONArray(sb.toString());

                return jsonArray;
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
            }

        }

        return jsonArray;
    }
}
