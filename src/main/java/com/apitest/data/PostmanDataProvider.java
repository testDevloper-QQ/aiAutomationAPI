package com.apitest.data;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class PostmanDataProvider {
    /**
     * 解析 Postman Collection v2.x，提取关键请求信息
     * 与 Swagger 解析对齐的关键字段：method, url, path, summary(name),
     * queryParameters, headerParameters, pathParameters, body, bodyParameters, responses。
     * 为兼容历史，保留 headers/query/body 三个键。
     */
    public static List<Map<String, Object>> parse(String jsonPath, String host) throws Exception {
        // 判断是否为绝对路径，不是则拼接为绝对路径
        Path path = Paths.get(jsonPath);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir"), jsonPath);
        }
        String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        JSONObject collection = JSON.parseObject(json, JSONObject.class);

        List<Map<String, Object>> cases = new ArrayList<>();
        JSONArray items = collection.getJSONArray("item");
        if (items != null) {
            traverseItems(items, host, new ArrayList<>(), cases);
        }
        return cases;
    }

    private static void traverseItems(JSONArray items, String host, List<String> folderStack, List<Map<String, Object>> out) {
        for (int i = 0; i < items.size(); i++) {
            JSONObject item = items.getJSONObject(i);
            // folder or request
            if (item.containsKey("item")) {
                // folder
                List<String> nextStack = new ArrayList<>(folderStack);
                String folderName = item.getString("name");
                if (folderName != null) nextStack.add(folderName);
                traverseItems(item.getJSONArray("item"), host, nextStack, out);
                continue;
            }
            if (!item.containsKey("request")) {
                continue;
            }
            JSONObject request = item.getJSONObject("request");
            String method = safeUpper(request.getString("method"));
            String name = item.getString("name");

            // URL 处理
            JSONObject urlObj = request.getJSONObject("url");
            String finalUrl = buildUrl(urlObj, host);
            String pathStr = extractPath(urlObj);

            // headers（忽略 disabled=true 或值为空）
            Map<String, String> headerMap = new LinkedHashMap<>();
            JSONArray headerArr = request.getJSONArray("header");
            if (headerArr != null) {
                for (int j = 0; j < headerArr.size(); j++) {
                    JSONObject h = headerArr.getJSONObject(j);
                    if (h.getBooleanValue("disabled")) continue;
                    String key = h.getString("key");
                    String value = h.getString("value");
                    if (isNotBlank(key) && value != null) headerMap.put(key, value);
                }
            }

            // query
            Map<String, String> queryMap = new LinkedHashMap<>();
            JSONArray queryArr = urlObj == null ? null : urlObj.getJSONArray("query");
            if (queryArr != null) {
                for (int j = 0; j < queryArr.size(); j++) {
                    JSONObject q = queryArr.getJSONObject(j);
                    if (q.getBooleanValue("disabled")) continue;
                    String key = q.getString("key");
                    String value = q.getString("value");
                    if (isNotBlank(key)) queryMap.put(key, value);
                }
            }

            // path variables
            Map<String, Object> pathVars = new LinkedHashMap<>();
            JSONArray varArr = urlObj == null ? null : urlObj.getJSONArray("variable");
            if (varArr != null) {
                for (int j = 0; j < varArr.size(); j++) {
                    JSONObject v = varArr.getJSONObject(j);
                    String key = v.getString("key");
                    Object value = v.get("value");
                    if (isNotBlank(key)) pathVars.put(key, value);
                }
            }

            // body
            JSONObject bodyObj = request.getJSONObject("body");
            String bodyRaw = null;
            Map<String, Object> bodyParams = new LinkedHashMap<>();
            if (bodyObj != null) {
                String mode = bodyObj.getString("mode");
                if ("raw".equalsIgnoreCase(mode)) {
                    bodyRaw = bodyObj.getString("raw");
                } else if ("urlencoded".equalsIgnoreCase(mode)) {
                    JSONArray urlenc = bodyObj.getJSONArray("urlencoded");
                    if (urlenc != null) {
                        Map<String, String> urlEncMap = new LinkedHashMap<>();
                        for (int j = 0; j < urlenc.size(); j++) {
                            JSONObject p = urlenc.getJSONObject(j);
                            if (p.getBooleanValue("disabled")) continue;
                            String key = p.getString("key");
                            String value = p.getString("value");
                            if (isNotBlank(key)) urlEncMap.put(key, value);
                        }
                        bodyParams.put("urlencoded", urlEncMap);
                    }
                } else if ("formdata".equalsIgnoreCase(mode)) {
                    JSONArray formdata = bodyObj.getJSONArray("formdata");
                    if (formdata != null) {
                        List<Map<String, Object>> formList = new ArrayList<>();
                        for (int j = 0; j < formdata.size(); j++) {
                            JSONObject p = formdata.getJSONObject(j);
                            if (p.getBooleanValue("disabled")) continue;
                            Map<String, Object> entry = new LinkedHashMap<>();
                            entry.put("key", p.getString("key"));
                            entry.put("type", p.getString("type")); // text/file
                            entry.put("value", p.getString("value"));
                            if (p.containsKey("src")) entry.put("src", p.get("src"));
                            formList.add(entry);
                        }
                        bodyParams.put("formdata", formList);
                    }
                } else if ("graphql".equalsIgnoreCase(mode)) {
                    Map<String, Object> gql = new LinkedHashMap<>();
                    gql.put("query", bodyObj.getString("graphql"));
                    JSONObject gqlObj = bodyObj.getJSONObject("graphql");
                    if (gqlObj != null) {
                        gql.put("query", gqlObj.getString("query"));
                        gql.put("variables", gqlObj.getString("variables"));
                    }
                    bodyParams.put("graphql", gql);
                } else if ("file".equalsIgnoreCase(mode)) {
                    JSONObject fileObj = bodyObj.getJSONObject("file");
                    if (fileObj != null) bodyParams.put("file", fileObj);
                }
            }

            // responses（按状态码聚合，与Swagger一致）
            Map<String, Object> responses = new LinkedHashMap<>();
            JSONArray respArr = item.getJSONArray("response");
            if (respArr != null) {
                for (int r = 0; r < respArr.size(); r++) {
                    JSONObject resp = respArr.getJSONObject(r);
                    String code = String.valueOf(resp.getInteger("code"));
                    Map<String, Object> respDetail = new LinkedHashMap<>();
                    respDetail.put("name", resp.getString("name"));
                    respDetail.put("status", resp.getString("status"));
                    respDetail.put("code", resp.getInteger("code"));
                    // headers
                    Map<String, String> rh = new LinkedHashMap<>();
                    JSONArray rhArr = resp.getJSONArray("header");
                    if (rhArr != null) {
                        for (int j = 0; j < rhArr.size(); j++) {
                            JSONObject h = rhArr.getJSONObject(j);
                            String k = h.getString("key");
                            String v = h.getString("value");
                            if (isNotBlank(k) && v != null) rh.put(k, v);
                        }
                    }
                    respDetail.put("headers", rh);
                    // body
                    respDetail.put("body", resp.getString("body"));
                    responses.put(code, respDetail);
                }
            }

            Map<String, Object> caseInfo = new LinkedHashMap<>();
            caseInfo.put("method", method);
            caseInfo.put("url", finalUrl);
            caseInfo.put("path", pathStr);
            caseInfo.put("name", name != null ? name : "");
            // 将Postman的name字段映射为operationId，用于兼容
            caseInfo.put("operationId", name != null ? name : "");
            // host：优先使用传入的host覆盖，否则从url结构提取
            String hostUsed = host != null && !host.trim().isEmpty() ? host : joinHost(urlObj == null ? null : urlObj.get("host"));
            if (hostUsed == null) hostUsed = "";
            caseInfo.put("host", hostUsed.startsWith("http") ? hostUsed : (hostUsed.isEmpty() ? "" : ("https://" + hostUsed)));
            

            // 与Swagger对齐命名
            caseInfo.put("headerParameters", headerMap.isEmpty() ? null : headerMap);
            caseInfo.put("queryParameters", queryMap.isEmpty() ? null : queryMap);
            caseInfo.put("pathParameters", pathVars.isEmpty() ? null : pathVars);
            caseInfo.put("body", bodyRaw); // 原始体
            caseInfo.put("bodyParameters", bodyParams.isEmpty() ? null : bodyParams);
            caseInfo.put("responses", responses.isEmpty() ? null : responses);

            // 兼容旧键
            caseInfo.put("headers", headerMap.isEmpty() ? null : headerMap);
            caseInfo.put("query", queryMap.isEmpty() ? null : queryMap);

            out.add(caseInfo);
        }
    }

    private static String buildUrl(JSONObject urlObj, String hostOverride) {
        if (urlObj == null) return null;
        String raw = urlObj.getString("raw");
        if (isNotBlank(hostOverride)) {
            // 尝试使用结构化字段重建 URL，以便替换 host
            String protocol = urlObj.getString("protocol");
            if (!isNotBlank(protocol)) protocol = "https";
            String host = hostOverride;
            String path = extractPath(urlObj);
            String query = buildQueryString(urlObj.getJSONArray("query"));
            String base = protocol + "://" + host;
            String full = joinUrl(base, path);
            if (isNotBlank(query)) full = full + "?" + query;
            return full;
        }
        if (isNotBlank(raw)) return raw;
        String protocol = urlObj.getString("protocol");
        if (!isNotBlank(protocol)) protocol = "https";
        String host = joinHost(urlObj.get("host"));
        String path = extractPath(urlObj);
        String query = buildQueryString(urlObj.getJSONArray("query"));
        String base = protocol + "://" + (host == null ? "" : host);
        String full = joinUrl(base, path);
        if (isNotBlank(query)) full = full + "?" + query;
        return full;
    }

    private static String extractPath(JSONObject urlObj) {
        if (urlObj == null) return null;
        Object path = urlObj.get("path");
        if (path instanceof JSONArray) {
            List<String> seg = ((JSONArray) path).stream()
                    .map(Objects::toString)
                    .collect(Collectors.toList());
            return "/" + String.join("/", seg);
        } else if (path instanceof String) {
            String p = (String) path;
            if (p.startsWith("/")) return p;
            return "/" + p;
        }
        // 从 raw 回退
        String raw = urlObj.getString("raw");
        if (isNotBlank(raw)) {
            int idx = raw.indexOf("//");
            if (idx > -1) {
                int slash = raw.indexOf('/', idx + 2);
                if (slash > -1) {
                    String p = raw.substring(slash);
                    int q = p.indexOf('?');
                    return q > -1 ? p.substring(0, q) : p;
                }
            }
        }
        return null;
    }

    private static String joinHost(Object hostField) {
        if (hostField == null) return null;
        if (hostField instanceof JSONArray) {
            JSONArray arr = (JSONArray) hostField;
            return arr.stream().map(Objects::toString).collect(Collectors.joining("."));
        }
        return String.valueOf(hostField);
    }

    private static String buildQueryString(JSONArray queryArr) {
        if (queryArr == null || queryArr.isEmpty()) return null;
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < queryArr.size(); i++) {
            JSONObject q = queryArr.getJSONObject(i);
            if (q.getBooleanValue("disabled")) continue;
            String key = q.getString("key");
            String value = q.getString("value");
            if (!isNotBlank(key)) continue;
            parts.add(value == null ? encode(key) : encode(key) + "=" + encode(value));
        }
        return parts.isEmpty() ? null : String.join("&", parts);
    }

    private static String encode(String s) {
        // 简化处理，避免引入额外依赖；真实环境可使用 URLEncoder
        return s.replace(" ", "%20");
    }

    private static String joinUrl(String base, String path) {
        if (!isNotBlank(base)) return path;
        if (!isNotBlank(path)) return base;
        if (base.endsWith("/") && path.startsWith("/")) return base + path.substring(1);
        if (!base.endsWith("/") && !path.startsWith("/")) return base + "/" + path;
        return base + path;
    }

    private static String safeUpper(String s) {
        return s == null ? null : s.toUpperCase();
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}