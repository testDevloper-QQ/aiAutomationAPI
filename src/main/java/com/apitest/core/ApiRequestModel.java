package com.apitest.core;

import java.util.Map;

/**
 * API请求参数模型
 */
public class ApiRequestModel {
    private String url;
    private String method;
    private Map<String, String> headers;
    private String body;

    public ApiRequestModel(String url, String method, Map<String, String> headers, String body) {
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.body = body;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "ApiRequestModel{" +
                "url='" + url + '\'' +
                ", method='" + method + '\'' +
                ", headers=" + headers +
                ", body='" + body + '\'' +
                '}';
    }
} 