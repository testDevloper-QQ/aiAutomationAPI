package com.apitest.data;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONArray;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Swagger文档数据提供者
 * 用于解析Swagger/OpenAPI文档，提取接口信息用于生成测试用例
 */
public class SwaggerDataProvider {

    /**
     * 解析Swagger文档，提取接口信息
     * @param jsonPath Swagger文档路径
     * @param host 主机地址
     * @return 接口信息列表
     * @throws Exception 解析异常
     */
    public static List<Map<String, Object>> parse(String jsonPath, String host) throws Exception {
        // 判断是否为绝对路径，不是则拼接为绝对路径
        String json = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(jsonPath)), java.nio.charset.StandardCharsets.UTF_8);
        JSONObject openApi = JSON.parseObject(json, JSONObject.class);
        JSONObject paths = openApi.getJSONObject("paths");
        List<Map<String, Object>> cases = new ArrayList<>();
        
        // 获取基础信息
        String basePath = openApi.getString("basePath");
        if (basePath == null) {
            basePath = "";
        }
        
        // 遍历所有路径
        for (String path : paths.keySet()) {
            JSONObject pathObj = paths.getJSONObject(path);
            
            // 遍历该路径下的所有HTTP方法
            for (String method : pathObj.keySet()) {
                if (isValidHttpMethod(method)) {
                    JSONObject methodObj = pathObj.getJSONObject(method);
                    Map<String, Object> apiInfo = parseApiMethod(path, method, methodObj, host, basePath, openApi);
                    cases.add(apiInfo);
                }
            }
        }
        
        return cases;
    }
    
    /**
     * 解析单个API方法的信息
     * @param path 路径
     * @param method HTTP方法
     * @param methodObj 方法对象
     * @param host 主机地址
     * @param basePath 基础路径
     * @param openApi 完整的OpenAPI文档
     * @return API信息Map
     */
    private static Map<String, Object> parseApiMethod(String path, String method, JSONObject methodObj, 
                                                     String host, String basePath, JSONObject openApi) {
        Map<String, Object> apiInfo = new HashMap<>();
        
        // 基本信息
        apiInfo.put("method", method.toUpperCase());
        apiInfo.put("url", buildFullUrl(host, basePath, path));
        apiInfo.put("path", path);
        apiInfo.put("host", (host == null ? "" : (host.startsWith("http") ? host : ("https://" + host))));
        apiInfo.put("operationId", methodObj.getString("operationId"));
        apiInfo.put("name", methodObj.getString("operationId"));
        apiInfo.put("description", methodObj.getString("description"));
        
        // 解析参数
        JSONArray parameters = methodObj.getJSONArray("parameters");
        if (parameters != null) {
            Map<String, Object> paramInfo = parseParameters(parameters, path);
            apiInfo.put("queryParameters", paramInfo.get("query"));
            apiInfo.put("pathParameters", paramInfo.get("path"));
            apiInfo.put("headerParameters", paramInfo.get("header"));
            apiInfo.put("bodyParameters", paramInfo.get("body"));
        }
        
        // 解析响应
        JSONObject responses = methodObj.getJSONObject("responses");
        if (responses != null) {
            Map<String, Object> responseInfo = parseResponses(responses, openApi);
            apiInfo.put("responses", responseInfo);
        }
        
        return apiInfo;
    }
    
    /**
     * 解析参数信息
     * @param parameters 参数数组
     * @param path 路径（用于处理路径参数）
     * @return 参数信息Map
     */
    private static Map<String, Object> parseParameters(JSONArray parameters, String path) {
        Map<String, Object> paramInfo = new HashMap<>();
        Map<String, Object> queryParams = new HashMap<>();
        Map<String, Object> pathParams = new HashMap<>();
        Map<String, Object> headerParams = new HashMap<>();
        Map<String, Object> bodyParams = new HashMap<>();
        
        for (int i = 0; i < parameters.size(); i++) {
            JSONObject param = parameters.getJSONObject(i);
            String name = param.getString("name");
            String in = param.getString("in");
            String type = param.getString("type");
            String description = param.getString("description");
            boolean required = param.getBooleanValue("required");
            Object defaultValue = param.get("default");
            
            Map<String, Object> paramDetail = new HashMap<>();
            paramDetail.put("name", name);
            paramDetail.put("type", type);
            paramDetail.put("description", description);
            paramDetail.put("required", required);
            if (defaultValue != null) {
                paramDetail.put("default", defaultValue);
            }
            
            switch (in) {
                case "query":
                    queryParams.put(name, paramDetail);
                    break;
                case "path":
                    pathParams.put(name, paramDetail);
                    break;
                case "header":
                    headerParams.put(name, paramDetail);
                    break;
                case "body":
                    // 处理body参数
                    JSONObject schema = param.getJSONObject("schema");
                    if (schema != null) {
                        paramDetail.put("schema", schema);
                        bodyParams.put(name, paramDetail);
                    }
                    break;
            }
        }
        
        paramInfo.put("query", queryParams);
        paramInfo.put("path", pathParams);
        paramInfo.put("header", headerParams);
        paramInfo.put("body", bodyParams);
        
        return paramInfo;
    }
    
    /**
     * 解析响应信息
     * @param responses 响应对象
     * @param openApi 完整的OpenAPI文档（用于解析引用）
     * @return 响应信息Map
     */
    private static Map<String, Object> parseResponses(JSONObject responses, JSONObject openApi) {
        Map<String, Object> responseInfo = new HashMap<>();
        
        for (String statusCode : responses.keySet()) {
            JSONObject response = responses.getJSONObject(statusCode);
            Map<String, Object> responseDetail = new HashMap<>();
            
            responseDetail.put("description", response.getString("description"));
            
            // 解析响应schema
            JSONObject schema = response.getJSONObject("schema");
            if (schema != null) {
                Map<String, Object> schemaInfo = parseSchema(schema, openApi);
                responseDetail.put("schema", schemaInfo);
            }
            
            responseInfo.put(statusCode, responseDetail);
        }
        
        return responseInfo;
    }
    
    /**
     * 解析Schema信息
     * @param schema Schema对象
     * @param openApi 完整的OpenAPI文档
     * @return Schema信息Map
     */
    private static Map<String, Object> parseSchema(JSONObject schema, JSONObject openApi) {
        Map<String, Object> schemaInfo = new HashMap<>();
        
        String type = schema.getString("type");
        String ref = schema.getString("$ref");
        
        if (ref != null) {
            // 处理引用
            schemaInfo.put("$ref", ref);
            // 解析引用的定义
            if (ref.startsWith("#/definitions/")) {
                String definitionName = ref.substring("#/definitions/".length());
                JSONObject definitions = openApi.getJSONObject("definitions");
                if (definitions != null) {
                    JSONObject definition = definitions.getJSONObject(definitionName);
                    if (definition != null) {
                        schemaInfo.put("definition", definition);
                    }
                }
            }
        } else if (type != null) {
            schemaInfo.put("type", type);
            
            // 处理数组类型
            if ("array".equals(type)) {
                JSONObject items = schema.getJSONObject("items");
                if (items != null) {
                    schemaInfo.put("items", parseSchema(items, openApi));
                }
            }
            
            // 处理对象类型的属性
            if ("object".equals(type)) {
                JSONObject properties = schema.getJSONObject("properties");
                if (properties != null) {
                    Map<String, Object> props = new HashMap<>();
                    for (String propName : properties.keySet()) {
                        JSONObject prop = properties.getJSONObject(propName);
                        props.put(propName, parseSchema(prop, openApi));
                    }
                    schemaInfo.put("properties", props);
                }
            }
        }
        
        return schemaInfo;
    }
    
    /**
     * 构建完整的URL
     * @param host 主机地址
     * @param basePath 基础路径
     * @param path 接口路径
     * @return 完整URL
     */
    private static String buildFullUrl(String host, String basePath, String path) {
        StringBuilder url = new StringBuilder();
        
        // 添加协议和主机
        if (host != null && !host.isEmpty()) {
            // 如果传入的host已经包含协议
            if (host.startsWith("http://") || host.startsWith("https://")) {
                url.append(host);
            } else {
                url.append("https://").append(host);
            }
        } else {
            url.append("https://localhost");
        }
        
        // 添加基础路径（确保路径格式正确）
        if (basePath != null && !basePath.isEmpty()) {
            if (!basePath.startsWith("/")) {
                url.append("/");
            }
            url.append(basePath);
        }
        
        // 添加接口路径（确保路径格式正确）
        if (path != null && !path.isEmpty()) {
            if (!path.startsWith("/") && (basePath == null || !basePath.endsWith("/"))) {
                url.append("/");
            }
            url.append(path);
        }
        
        // 规范化URL，避免重复的斜杠
        return url.toString().replaceAll("(?<!http:|https:)//+", "/");
    }
    
    /**
     * 验证是否为有效的HTTP方法
     * @param method 方法名
     * @return 是否有效
     */
    private static boolean isValidHttpMethod(String method) {
        String[] validMethods = {"get", "post", "put", "delete", "patch", "head", "options"};
        for (String validMethod : validMethods) {
            if (validMethod.equalsIgnoreCase(method)) {
                return true;
            }
        }
        return false;
    }
}
