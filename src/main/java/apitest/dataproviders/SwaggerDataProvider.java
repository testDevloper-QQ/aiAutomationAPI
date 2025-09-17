package apitest.dataproviders;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import apitest.config.TestConfigManager;

import java.io.InputStream;
import java.util.*;

/**
 * Swagger数据源实现
 * 从Swagger JSON文件解析接口定义
 */
public class SwaggerDataProvider implements DataSourceProvider {
    
    private Map<String, JSONObject> swaggerCache = new HashMap<>();
    
    @Override
    public List<Map<String, Object>> getApiDefinitions(String module) {
        List<Map<String, Object>> definitions = new ArrayList<>();
        
        try {
            // 获取模块对应的Swagger文件
            Map<String, Object> dataSourceConfig = TestConfigManager.getInstance().getDataSourceConfig();
            Map<String, String> swaggerFiles = (Map<String, String>) dataSourceConfig.get("swaggerFiles");
            
            if (swaggerFiles != null && swaggerFiles.containsKey(module)) {
                String swaggerPath = swaggerFiles.get(module);
                JSONObject swaggerJson = loadSwaggerJson(swaggerPath);
                
                // 解析paths
                JSONObject paths = swaggerJson.getJSONObject("paths");
                for (String path : paths.keySet()) {
                    JSONObject pathItem = paths.getJSONObject(path);
                    for (String method : pathItem.keySet()) {
                        JSONObject operation = pathItem.getJSONObject(method);
                        Map<String, Object> definition = parseApiDefinitionAsMap(path, method, operation, module);
                        definitions.add(definition);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("解析Swagger文件失败: " + module, e);
        }
        
        return definitions;
    }
    
    @Override
    public Map<String, List<String>> getDependencies() {
        Map<String, List<String>> dependencies = new HashMap<>();
        
        // 从配置文件中读取依赖关系
        Map<String, Object> testCasesConfig = TestConfigManager.getInstance().getTestScenario("file_upload");
        if (testCasesConfig != null && testCasesConfig.containsKey("dependencyMapping")) {
            List<String> dependencyMappings = (List<String>) testCasesConfig.get("dependencyMapping");
            
            for (String mapping : dependencyMappings) {
                // 格式: setup:workspace:GET:/sdk/storage/workspace/v1
                String[] parts = mapping.split(":");
                if (parts.length >= 4) {
                    String type = parts[0];
                    String module = parts[1];
                    String method = parts[2];
                    String path = parts[3];
                    
                    // 构建依赖键值对，包含完整的接口信息
                    String dependencyInfo = type + ":" + module + ":" + method + ":" + path;
                    dependencies.computeIfAbsent(type, k -> new ArrayList<>()).add(dependencyInfo);
                }
            }
        }
        
        return dependencies;
    }
    
    /**
     * 根据模块、方法和路径获取API定义
     */
    public Map<String, Object> getApiDefinition(String module, String method, String path) {
        try {
            // 获取模块对应的Swagger文件
            Map<String, Object> dataSourceConfig = TestConfigManager.getInstance().getDataSourceConfig();
            Map<String, String> swaggerFiles = (Map<String, String>) dataSourceConfig.get("swaggerFiles");
            
            if (swaggerFiles != null && swaggerFiles.containsKey(module)) {
                String swaggerPath = swaggerFiles.get(module);
                JSONObject swaggerJson = loadSwaggerJson(swaggerPath);
                
                // 查找对应的接口定义
                JSONObject paths = swaggerJson.getJSONObject("paths");
                if (paths != null && paths.containsKey(path)) {
                    JSONObject pathItem = paths.getJSONObject(path);
                    if (pathItem.containsKey(method.toLowerCase())) {
                        JSONObject operation = pathItem.getJSONObject(method.toLowerCase());
                        Map<String, Object> definition = new HashMap<>();
                        definition.put("method", method.toUpperCase());
                        definition.put("path", path);
                        definition.put("description", operation.getString("summary"));
                        definition.put("module", module);
                        definition.put("parameters", operation.getJSONArray("parameters"));
                        definition.put("responses", operation.getJSONObject("responses"));
                        return definition;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("获取API定义失败: " + module + ":" + method + ":" + path, e);
        }
        
        return null;
    }
    
    @Override
    public List<Map<String, Object>> getTestCases(String apiPath) {
        return getTestCasesAsMap(apiPath);
    }
    
    /**
     * 获取Map格式的测试用例
     */
    public List<Map<String, Object>> getTestCasesAsMap(String apiPath) {
        List<Map<String, Object>> testCases = new ArrayList<>();
        
        // 根据配置生成测试用例
        Map<String, Object> testConfig = TestConfigManager.getInstance().getTestScenario("file_upload");
        boolean generateBoundary = (Boolean) testConfig.getOrDefault("generateBoundaryTests", false);
        boolean generateException = (Boolean) testConfig.getOrDefault("generateExceptionTests", false);
        
        // 生成正常测试用例
        testCases.add(createNormalTestCaseAsMap(apiPath));
        
        // 生成边界值测试用例
        if (generateBoundary) {
            testCases.addAll(createBoundaryTestCasesAsMap(apiPath));
        }
        
        // 生成异常场景测试用例
        if (generateException) {
            testCases.addAll(createExceptionTestCasesAsMap(apiPath));
        }
        
        return testCases;
    }
    
    private JSONObject loadSwaggerJson(String path) {
        if (swaggerCache.containsKey(path)) {
            return swaggerCache.get(path);
        }
        
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(path);
            if (is == null) {
                throw new RuntimeException("Swagger文件未找到: " + path);
            }
            byte[] bytes = new byte[is.available()];
            is.read(bytes);
            String content = new String(bytes);
            JSONObject swaggerJson = JSON.parseObject(content);
            swaggerCache.put(path, swaggerJson);
            return swaggerJson;
        } catch (Exception e) {
            throw new RuntimeException("加载Swagger文件失败: " + path, e);
        }
    }
    
    private Map<String, Object> parseApiDefinitionAsMap(String path, String method, JSONObject operation, String module) {
        Map<String, Object> definition = new HashMap<>();
        definition.put("path", path);
        definition.put("method", method.toUpperCase());
        definition.put("description", operation.getString("summary"));
        definition.put("module", module);
        
        // 解析参数
        Map<String, Object> parameters = new HashMap<>();
        if (operation.containsKey("parameters")) {
            parameters.put("parameters", operation.getJSONArray("parameters"));
        }
        definition.put("parameters", parameters);
        
        // 解析响应
        Map<String, Object> responses = new HashMap<>();
        if (operation.containsKey("responses")) {
            responses.put("responses", operation.getJSONObject("responses"));
        }
        definition.put("responses", responses);
        
        return definition;
    }
    
    private Map<String, Object> createNormalTestCaseAsMap(String apiPath) {
        Map<String, Object> testCase = new HashMap<>();
        testCase.put("testCaseId", "normal_" + apiPath.replaceAll("[^a-zA-Z0-9]", "_"));
        testCase.put("description", "正常场景测试");
        testCase.put("boundaryTest", false);
        testCase.put("exceptionTest", false);
        
        // 设置请求数据
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("expectedStatus", 200);
        testCase.put("requestData", requestData);
        
        // 设置期望结果
        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("statusCode", 200);
        testCase.put("expectedResult", expectedResult);
        
        return testCase;
    }
    
    private List<Map<String, Object>> createBoundaryTestCasesAsMap(String apiPath) {
        List<Map<String, Object>> testCases = new ArrayList<>();
        
        Map<String, Object> testCase1 = new HashMap<>();
        testCase1.put("testCaseId", "boundary_empty_file_" + apiPath.replaceAll("[^a-zA-Z0-9]", "_"));
        testCase1.put("description", "空文件边界测试");
        testCase1.put("boundaryTest", true);
        testCase1.put("exceptionTest", false);
        
        Map<String, Object> requestData1 = new HashMap<>();
        requestData1.put("fileSize", 0);
        requestData1.put("expectedStatus", 400);
        testCase1.put("requestData", requestData1);
        
        Map<String, Object> expectedResult1 = new HashMap<>();
        expectedResult1.put("statusCode", 400);
        testCase1.put("expectedResult", expectedResult1);
        
        testCases.add(testCase1);
        
        return testCases;
    }
    
    private List<Map<String, Object>> createExceptionTestCasesAsMap(String apiPath) {
        List<Map<String, Object>> testCases = new ArrayList<>();
        
        Map<String, Object> testCase1 = new HashMap<>();
        testCase1.put("testCaseId", "exception_invalid_file_" + apiPath.replaceAll("[^a-zA-Z0-9]", "_"));
        testCase1.put("description", "无效文件类型异常测试");
        testCase1.put("boundaryTest", false);
        testCase1.put("exceptionTest", true);
        
        Map<String, Object> requestData1 = new HashMap<>();
        requestData1.put("fileType", "invalid");
        requestData1.put("expectedStatus", 415);
        testCase1.put("requestData", requestData1);
        
        Map<String, Object> expectedResult1 = new HashMap<>();
        expectedResult1.put("statusCode", 415);
        testCase1.put("expectedResult", expectedResult1);
        
        testCases.add(testCase1);
        
        return testCases;
    }
}