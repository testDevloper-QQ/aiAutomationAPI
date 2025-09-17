package com.apitest.core;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.apitest.utils.UCUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * API参数组装工具类（Map驱动）
 */
public class ApiParamBuilder {
    /**
     * 由接口信息Map组装请求URL
     */
    public static String buildUrl(Map<String, Object> apiInfo) throws UnsupportedEncodingException {
        String host = (String) apiInfo.get("host");
        String path = (String) apiInfo.get("path");
        String url = (host.endsWith("/") ? host.substring(0, host.length() - 1) : host)
                + (path.startsWith("/") ? path : "/" + path);

        // 处理 query
        Object queryObj = apiInfo.get("query");
        if (queryObj != null && !queryObj.toString().trim().isEmpty()) {
            String queryString = buildQueryString(queryObj);
            if (!queryString.isEmpty()) {
                url += (url.contains("?") ? "&" : "?") + queryString;
            }
        }
        return url;
    }

    /**
     * 支持 query 为 Map 或 JSON字符串
     */
    private static String buildQueryString(Object queryObj) throws UnsupportedEncodingException {
        if (queryObj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) queryObj;
            StringBuilder sb = new StringBuilder();
            for (Object key : map.keySet()) {
                if (sb.length() > 0) sb.append("&");
                String value = map.get(key) == null ? "" : map.get(key).toString();
                sb.append(URLEncoder.encode(key.toString(), StandardCharsets.UTF_8.name()))
                  .append("=")
                  .append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
            }
            return sb.toString();
        } else if (queryObj instanceof String) {
            String query = ((String) queryObj).trim();
            if (query.startsWith("{") && query.endsWith("}")) {
                JSONObject obj = JSON.parseObject(query);
                StringBuilder sb = new StringBuilder();
                for (String key : obj.keySet()) {
                    if (sb.length() > 0) sb.append("&");
                    String value = obj.getString(key);
                    sb.append(URLEncoder.encode(key, StandardCharsets.UTF_8.name()))
                      .append("=")
                      .append(URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8.name()));
                }
                return sb.toString();
            } else {
                // key1=val1&key2=val2格式
                StringBuilder sb = new StringBuilder();
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    if (pair.trim().isEmpty()) continue;
                    int idx = pair.indexOf('=');
                    if (idx > 0) {
                        String key = pair.substring(0, idx);
                        String value = pair.substring(idx + 1);
                        if (sb.length() > 0) sb.append("&");
                        sb.append(URLEncoder.encode(key, StandardCharsets.UTF_8.name()))
                          .append("=")
                          .append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
                    } else {
                        if (sb.length() > 0) sb.append("&");
                        sb.append(URLEncoder.encode(pair, StandardCharsets.UTF_8.name()));
                    }
                }
                return sb.toString();
            }
        }
        return "";
    }

    /**
     * 组装请求头
     */
    public static Map<String, String> buildHeaders(Map<String, Object> apiInfo) {
        Object headerObj = apiInfo.get("headers");
        Map<String, String> headers = new HashMap<>();

        if (headerObj == null) return headers;
        if (headerObj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) headerObj;
            for (Object key : map.keySet()) {
                String value = map.get(key) == null ? "" : map.get(key).toString();
                headers.put(key.toString(), value);
            }
        } else if (headerObj instanceof String) {
            try {
                JSONObject obj = JSON.parseObject((String) headerObj);
                for (String key : obj.keySet()) {
                    headers.put(key, obj.getString(key));
                }
            } catch (Exception e) {
                // ignore
            }
        }
            // 自动加token
    String token = TokenProvider.getToken();
    if (token != null && !token.isEmpty()) {
        headers.put("Authorization", "Bearer " + token);
    }
        return headers;
    }

    /**
     * 组装请求体
     */
    public static String buildBody(Map<String, Object> apiInfo) {
        Object body = apiInfo.get("body");
        return body == null ? null : body.toString();
    }

    /**
     * 组装请求方法
     */
    public static String buildMethod(Map<String, Object> apiInfo) {
        Object method = apiInfo.get("method");
        return method == null ? "GET" : method.toString();
    }
}