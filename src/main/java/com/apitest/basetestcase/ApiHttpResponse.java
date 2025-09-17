package com.apitest.basetestcase;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * HTTP 响应封装
 */
public class ApiHttpResponse {
    private final int statusCode;
    private final String body;
    private final Map<String, List<String>> headers;
    private final Map<String, String> cookies;

    public ApiHttpResponse(int statusCode, String body,
                           Map<String, List<String>> headers,
                           Map<String, String> cookies) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers == null ? Collections.emptyMap() : headers;
        this.cookies = cookies == null ? Collections.emptyMap() : cookies;
    }

    public int getStatusCode() { return statusCode; }
    public String getBody() { return body; }
    public Map<String, List<String>> getHeaders() { return headers; }
    public Map<String, String> getCookies() { return cookies; }

    public JSONObject getBodyAsJson() {
        if (body == null || body.trim().isEmpty()) return null;
        try {
            return JSON.parseObject(body);
        } catch (Exception e) {
            return null;
        }
    }
}