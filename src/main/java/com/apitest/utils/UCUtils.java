package com.apitest.utils;

import java.util.Base64;

public class UCUtils {
    /**
     * 生成Basic Auth Header
     */
    public static String getBasicAuthHeader(String appKey, String appSecret) {
        String auth = appKey + ":" + appSecret;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
    }
}
