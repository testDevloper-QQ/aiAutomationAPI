package com.apitest.core;

import com.apitest.utils.ConfigManager;
import com.apitest.utils.HttpClientUtil;
import com.alibaba.fastjson.JSONObject;

public class TokenProvider {
    private static String cachedToken = null;
    private static long tokenExpireTime = 0;

    /**
     * 获取token，自动读取apiconfig下的appkey/appsecret等配置
     */
    public static String getToken() {
        long now = System.currentTimeMillis();
        if (cachedToken != null && now < tokenExpireTime - 60 * 1000) { // 提前1分钟过期
            return cachedToken;
        }
        try {
            // 读取配置
            String appKey = ConfigManager.get("UCAppKey");
            String appSecret = ConfigManager.get("UCAppSecret");
            String tokenUrl = ConfigManager.get("tokenUrl"); // 需在apiconfig中配置tokenUrl
            // 构造请求体
            JSONObject body = new JSONObject();
            body.put("appKey", appKey);
            body.put("appSecret", appSecret);

            // 发起POST请求获取token
            String response = HttpClientUtil.sendRequest(tokenUrl, "POST", null, body.toJSONString());
            JSONObject respJson = JSONObject.parseObject(response);
            String token = respJson.getString("access_token");
            long expiresIn = respJson.getLongValue("expires_in"); // 单位秒
            cachedToken = token;
            tokenExpireTime = now + expiresIn * 1000;
            return token;
        } catch (Exception e) {
            throw new RuntimeException("获取token失败", e);
        }
    }
}