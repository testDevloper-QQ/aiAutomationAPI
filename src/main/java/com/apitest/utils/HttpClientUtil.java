package com.apitest.utils;

import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.content.FileBody;
import java.io.File;
import java.util.*;
import java.nio.charset.StandardCharsets;

import com.apitest.basetestcase.ApiHttpResponse;

/**
 * HTTP请求工具类，支持GET/POST/PUT/DELETE/PATCH，多种Body模式
 */
public class HttpClientUtil {
    public static String sendRequest(String url, String method, Map<String, String> headers, String body) throws Exception {
        ApiHttpResponse resp = sendRequestWithMeta(url, method, headers, body);
        return resp.getBody();
    }

    public static ApiHttpResponse sendRequestWithMeta(String url, String method, Map<String, String> headers, String body) throws Exception {
        return sendRequestWithMetaAdvanced(url, method, headers, body, null);
    }

    public static ApiHttpResponse sendRequestWithMetaAdvanced(String url, String method,
                                                              Map<String, String> headers,
                                                              String body,
                                                              Map<String, Object> bodyParameters) throws Exception {
        CookieStore cookieStore = new BasicCookieStore();
        CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(cookieStore).build();
        String httpMethod = method == null ? "GET" : method.toUpperCase();
        HttpRequestBase request;
        switch (httpMethod) {
            case "POST":
                HttpPost post = new HttpPost(url);
                attachEntity(post, headers, body, bodyParameters);
                request = post;
                break;
            case "PUT":
                HttpPut put = new HttpPut(url);
                attachEntity(put, headers, body, bodyParameters);
                request = put;
                break;
            case "PATCH":
                HttpPatch patch = new HttpPatch(url);
                attachEntity(patch, headers, body, bodyParameters);
                request = patch;
                break;
            case "DELETE":
                request = new HttpDelete(url);
                break;
            default:
                request = new HttpGet(url);
        }

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                request.setHeader(entry.getKey(), entry.getValue());
            }
        }
        HttpResponse response = client.execute(request);
        int status = response.getStatusLine().getStatusCode();
        String result = response.getEntity() == null ? null : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

        // headers
        Map<String, List<String>> headerMap = new LinkedHashMap<>();
        for (Header h : response.getAllHeaders()) {
            headerMap.computeIfAbsent(h.getName(), k -> new ArrayList<>()).add(h.getValue());
        }

        // cookies
        Map<String, String> cookieMap = new LinkedHashMap<>();
        for (Cookie c : cookieStore.getCookies()) {
            cookieMap.put(c.getName(), c.getValue());
        }
        client.close();
        return new ApiHttpResponse(status, result, headerMap, cookieMap);
    }

    @SuppressWarnings("unchecked")
    private static void attachEntity(HttpEntityEnclosingRequestBase request,
                                     Map<String, String> headers,
                                     String body,
                                     Map<String, Object> bodyParameters) throws Exception {
        if (bodyParameters != null) {
            if (bodyParameters.containsKey("urlencoded")) {
                // x-www-form-urlencoded
                Map<String, Object> kv = (Map<String, Object>) bodyParameters.get("urlencoded");
                List<BasicNameValuePair> pairs = new ArrayList<>();
                for (Map.Entry<String, Object> e : kv.entrySet()) {
                    pairs.add(new BasicNameValuePair(e.getKey(), e.getValue()==null?"":String.valueOf(e.getValue())));
                }
                request.setEntity(new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8));
                return;
            }
            if (bodyParameters.containsKey("formdata")) {
                // multipart/form-data (支持文本与文件)
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                List<?> list = (List<?>) bodyParameters.get("formdata");
                for (Object o : list) {
                    Map<String, Object> entry = (Map<String, Object>) o;
                    String key = String.valueOf(entry.get("key"));
                    String type = String.valueOf(entry.get("type"));
                    if ("file".equalsIgnoreCase(type)) {
                        Object src = entry.get("src");
                        if (src instanceof String) {
                            File f = new File((String) src);
                            if (f.exists()) builder.addBinaryBody(key, f, ContentType.APPLICATION_OCTET_STREAM, f.getName());
                        } else if (src instanceof List) {
                            for (Object s : (List<?>) src) {
                                File f = new File(String.valueOf(s));
                                if (f.exists()) builder.addBinaryBody(key, f, ContentType.APPLICATION_OCTET_STREAM, f.getName());
                            }
                        }
                    } else {
                        String value = entry.get("value") == null ? "" : String.valueOf(entry.get("value"));
                        builder.addTextBody(key, value, ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));
                    }
                }
                request.setEntity(builder.build());
                return;
            }
        }
        // 默认按 JSON 文本发送
        if (body != null && !body.trim().isEmpty()) {
            request.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
        }
    }
} 