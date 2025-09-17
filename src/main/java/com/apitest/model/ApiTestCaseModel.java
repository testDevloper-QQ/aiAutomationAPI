package com.apitest.model;

import java.util.Map;
import java.util.List;

/**
 * API测试用例模型
 * 用于封装从Swagger或Postman解析的测试用例信息
 */
public class ApiTestCaseModel {
    private String testCaseID;
    private String method;
    private String host;
    private String path;
    private String url;
    private String name;
    private String description;
    private String operationId;
    
    // 参数信息
    private Map<String, Object> queryParameters;
    private Map<String, Object> pathParameters;
    private Map<String, Object> headerParameters;
    private Map<String, Object> bodyParameters;
    private String body;
    
    // 响应信息
    private Map<String, Object> responses;
    
    // 前置和后置方法配置
    private String beforeMethod;
    private Map<String, Object> beforeMethodParams;
    private String afterMethod;
    private Map<String, Object> afterMethodParams;
    
    // 依赖配置
    private List<String> dependencies;
    
    // 模块分组
    private String module;
    
    public ApiTestCaseModel() {}
    
    public ApiTestCaseModel(String testCaseID, String method, String host, String path, String url, 
                           String name, String description, String operationId) {
        this.testCaseID = testCaseID;
        this.method = method;
        this.host = host;
        this.path = path;
        this.url = url;
        this.name = name;
        this.description = description;
        this.operationId = operationId;
    }

    // Getter和Setter方法
    public String getTestCaseID() { return testCaseID; }
    public void setTestCaseID(String testCaseID) { this.testCaseID = testCaseID; }
    
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getOperationId() { return operationId; }
    public void setOperationId(String operationId) { this.operationId = operationId; }
    
    public Map<String, Object> getQueryParameters() { return queryParameters; }
    public void setQueryParameters(Map<String, Object> queryParameters) { this.queryParameters = queryParameters; }
    
    public Map<String, Object> getPathParameters() { return pathParameters; }
    public void setPathParameters(Map<String, Object> pathParameters) { this.pathParameters = pathParameters; }
    
    public Map<String, Object> getHeaderParameters() { return headerParameters; }
    public void setHeaderParameters(Map<String, Object> headerParameters) { this.headerParameters = headerParameters; }
    
    public Map<String, Object> getBodyParameters() { return bodyParameters; }
    public void setBodyParameters(Map<String, Object> bodyParameters) { this.bodyParameters = bodyParameters; }
    
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    
    public Map<String, Object> getResponses() { return responses; }
    public void setResponses(Map<String, Object> responses) { this.responses = responses; }
    
    public String getBeforeMethod() { return beforeMethod; }
    public void setBeforeMethod(String beforeMethod) { this.beforeMethod = beforeMethod; }
    
    public Map<String, Object> getBeforeMethodParams() { return beforeMethodParams; }
    public void setBeforeMethodParams(Map<String, Object> beforeMethodParams) { this.beforeMethodParams = beforeMethodParams; }
    
    public String getAfterMethod() { return afterMethod; }
    public void setAfterMethod(String afterMethod) { this.afterMethod = afterMethod; }
    
    public Map<String, Object> getAfterMethodParams() { return afterMethodParams; }
    public void setAfterMethodParams(Map<String, Object> afterMethodParams) { this.afterMethodParams = afterMethodParams; }
    
    public List<String> getDependencies() { return dependencies; }
    public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }
    
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    
    @Override
    public String toString() {
        return "ApiTestCaseModel{" +
                "testCaseID='" + testCaseID + '\'' +
                ", method='" + method + '\'' +
                ", host='" + host + '\'' +
                ", path='" + path + '\'' +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", operationId='" + operationId + '\'' +
                ", module='" + module + '\'' +
                '}';
    }
}