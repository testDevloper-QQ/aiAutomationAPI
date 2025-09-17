package com.apitest.utils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;

public class WeComRobotUtil {
    public static void sendMsg(String webhook, String msg) {
        try {
            URL url = new URL(webhook);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            String payload = "{\"msgtype\":\"text\",\"text\":{\"content\":\"" + msg.replace("\"", "\\\"") + "\"}}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes("UTF-8"));
            }
            int code = conn.getResponseCode();
            conn.getInputStream().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
