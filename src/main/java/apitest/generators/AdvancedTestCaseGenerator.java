package apitest.generators;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 高级测试用例生成器
 * 支持从配置文件自动读取前置依赖、swagger集成、后置清理、中文编码
 * 
 * @author 自动化测试团队
 * @version 2.0
 */
public class AdvancedTestCaseGenerator {
    
    // 配置缓存
    private Map<String, Object> caseConfig;
    private Map<String, Object> dataConfig;
    private Map<String, Object> envConfig;
    private Map<String, Object> swaggerCache;
    private String moduleName;
    private String environment;
    
    // 文件路径配置
    private static final String CASES_CONFIG_PATH = "src/test/resources/testdata/test/cases/component/";
    private static final String DATA_CONFIG_PATH = "src/test/resources/testdata/test/module_testdata/";
    private static final String ENV_CONFIG_PATH = "src/test/resources/apiconfig/";
    private static final String SWAGGER_PATH = "src/main/resources/swagger/";
    
    public AdvancedTestCaseGenerator(String moduleName) {
        this.moduleName = moduleName;
        this.swaggerCache = new HashMap<>();
        loadAllConfigs();
    }
    
    /**
     * 加载所有配置文件
     */
    private void loadAllConfigs() {
        try {
            Yaml yaml = new Yaml();
            
            // 1. 加载测试场景配置
            String casesFile = CASES_CONFIG_PATH + moduleName + "_cases.yml";
            caseConfig = loadYamlFile(casesFile);
            
            // 2. 加载测试数据配置
            String dataFile = DATA_CONFIG_PATH + moduleName + "_testdata.yml";
            dataConfig = loadYamlFile(dataFile);
            
            // 3. 获取环境配置
            environment = (String) dataConfig.getOrDefault("env", "dev");
            String envFile = ENV_CONFIG_PATH + "env.yml";
            Map<String, Object> allEnv = loadYamlFile(envFile);
            envConfig = (Map<String, Object>) allEnv.get(environment);
            
            // 4. 预加载swagger文件
            preloadSwaggerFiles();
            
            System.out.println("✅ 所有配置加载完成");
            System.out.println("   模块: " + moduleName);
            System.out.println("   环境: " + environment);
            
        } catch (Exception e) {
            System.err.println("❌ 配置加载失败: " + e.getMessage());
            throw new RuntimeException("配置加载失败", e);
        }
    }
    
    /**
     * 加载YAML文件
     */
    private Map<String, Object> loadYamlFile(String filePath) {
        try (InputStream input = new FileInputStream(filePath)) {
            Yaml yaml = new Yaml();
            return yaml.load(input);
        } catch (IOException e) {
            System.err.println("❌ 加载YAML文件失败: " + filePath);
            return new HashMap<>();
        }
    }
    
    /**
     * 预加载swagger文件
     */
    private void preloadSwaggerFiles() {
        Map<String, Object> dataSource = (Map<String, Object>) caseConfig.get("dataSource");
        if (dataSource != null && dataSource.containsKey("swagger")) {
            Map<String, Object> swaggerConfig = (Map<String, Object>) dataSource.get("swagger");
            String basePath = (String) swaggerConfig.get("basePath");
            Map<String, String> files = (Map<String, String>) swaggerConfig.get("files");
            
            for (Map.Entry<String, String> entry : files.entrySet()) {
                String fileName = entry.getKey();
                String filePath = basePath + "/" + entry.getValue();
                try {
                    String content = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
                    swaggerCache.put(fileName, JSON.parseObject(content));
                    System.out.println("✅ 加载swagger文件: " + fileName);
                } catch (Exception e) {
                    System.err.println("❌ 加载swagger文件失败: " + filePath);
                }
            }
        }
    }
    
    /**
     * 生成所有测试用例
     */
    public void generateAllTests() {
        try {
            Map<String, Object> scenarios = (Map<String, Object>) caseConfig.get("testScenarios");
            if (scenarios == null || scenarios.isEmpty()) {
                System.out.println("❌ 未找到测试场景配置");
                return;
            }
            
            for (Map.Entry<String, Object> entry : scenarios.entrySet()) {
                String scenarioName = entry.getKey();
                Map<String, Object> scenarioConfig = (Map<String, Object>) entry.getValue();
                
                System.out.println("📝 生成测试类: " + scenarioName);
                generateTestClass(scenarioName, scenarioConfig);
            }
            
            System.out.println("✅ 所有测试用例生成完成！");
            
        } catch (Exception e) {
            System.err.println("❌ 生成测试用例失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 生成测试类
     */
    private void generateTestClass(String scenarioName, Map<String, Object> scenarioConfig) {
        StringBuilder content = new StringBuilder();
        
        // 包声明和导入
        content.append("package apitest.testcases;\n\n");
        content.append(generateImports());
        
        // 类定义
        String className = convertToCamelCase(moduleName) + "Test";
        content.append("/**\n");
        content.append(" * 自动生成的测试类 - ").append(moduleName).append("模块\n");
        content.append(" * 测试场景: ").append(scenarioConfig.getOrDefault("description", "")).append("\n");
        content.append(" * 生成时间: ").append(new Date()).append("\n");
        content.append(" */\n");
        content.append("public class ").append(className).append(" extends BaseTest {\n\n");
        
        // 成员变量
        content.append(generateClassVariables());
        
        // 生命周期方法
        content.append(generateLifecycleMethods());
        
        // 前置依赖方法
        content.append(generatePreConditionMethods());
        
        // 测试方法
        content.append(generateTestMethods(scenarioConfig));
        
        // 后置清理方法
        content.append(generatePostConditionMethods());
        
        // 工具方法
        content.append(generateUtilityMethods());
        
        content.append("}\n");
        
        // 写入文件
        writeTestFile(className, content.toString());
    }
    
    /**
     * 生成导入语句
     */
    private String generateImports() {
        StringBuilder imports = new StringBuilder();
        imports.append("import org.testng.annotations.*;");
        imports.append("\nimport io.restassured.RestAssured;");
        imports.append("\nimport io.restassured.http.ContentType;");
        imports.append("\nimport io.restassured.response.Response;");
        imports.append("\nimport static io.restassured.RestAssured.*;");
        imports.append("\nimport static org.hamcrest.Matchers.*;");
        imports.append("\nimport com.alibaba.fastjson.JSON;");
        imports.append("\nimport com.apitest.core.BaseTest;");
        imports.append("\nimport com.apitest.core.CleanData;");
        imports.append("\nimport com.apitest.core.TokenProvider;");
        imports.append("\nimport com.apitest.utils.ConfigManager;");
        imports.append("\nimport java.io.*;");
        imports.append("\nimport java.util.*;");
        imports.append("\nimport java.nio.charset.StandardCharsets;");
        imports.append("\nimport org.apache.commons.io.FileUtils;");
        imports.append("\nimport org.yaml.snakeyaml.Yaml;");
        imports.append("\nimport org.testng.Assert;");
        imports.append("\nimport org.testng.SkipException;");
        imports.append("\n\n");
        return imports.toString();
    }
    
    /**
     * 生成类变量
     */
    private String generateClassVariables() {
        StringBuilder variables = new StringBuilder();
        variables.append("    // 环境配置\n");
        variables.append("    private String baseUrl;\n");
        variables.append("    private String environment;\n");
        variables.append("    private String authToken;\n\n");
        
        variables.append("    // 测试数据\n");
        variables.append("    private Map<String, Object> testData;\n");
        variables.append("    private Map<String, String> headers;\n");
        variables.append("    private Map<String, Object> contextParams;\n");
        variables.append("    private List<String> cleanupIds;\n");
        variables.append("    private List<String> fileIds;\n\n");
        
        variables.append("    // 前置依赖结果\n");
        variables.append("    private Map<String, Object> preConditionResults;\n");
        variables.append("    private String workspaceId;\n");
        variables.append("    private String workspaceName;\n");
        variables.append("    private String folderId;\n\n");
        
        return variables.toString();
    }
    
    /**
     * 生成前置依赖方法
     */
    private String generatePreConditionMethod(Map<String, Object> dep) {
        String methodName = (String) dep.get("name");
        String description = (String) dep.get("description");
        
        StringBuilder method = new StringBuilder();
        method.append("    @BeforeMethod(dependsOnMethods = \"setupAuthentication\")\n");
        method.append("    public void " + methodName + "() {\n");
        method.append("        System.out.println(\"\\n🔧 " + description + \");\n");
        method.append("        try {\n");
        
        String endpoint = getSwaggerEndpoint((String) dep.get("endpoint"));
        method.append("            String endpoint = \"" + endpoint + "\"\n");
        
        // 构造请求参数
        Map<String, Object> parameters = (Map<String, Object>) dep.get("parameters");
        if (parameters != null) {
            method.append("            Map<String, Object> requestData = new HashMap<>();\n");
            
            Map<String, Object> body = (Map<String, Object>) parameters.get("body");
            if (body != null) {
                for (Map.Entry<String, Object> entry : body.entrySet()) {
                    String value = entry.getValue().toString()
                        .replace("{{timestamp}}", "" + System.currentTimeMillis() + "")
                        .replace("{{workspaceId}}", contextParams.getOrDefault("workspaceId", "").toString())
                        .replace("{{folderId}}", contextParams.getOrDefault("folderId", "").toString());
                    
                    // 检查值是否为中文或包含特殊字符，需要添加引号
                    if (value.contains("测试") || value.contains("自动化") || value.contains("创建") || value.contains("文件夹") || 
                        value.contains("工作空间") || !value.matches("[a-zA-Z0-9._+\\-\\[\\]]+")) {
                        value = '"' + value + '"';
                    }
                    
                    method.append("            requestData.put(\"" + entry.getKey() + "\", " + value + ");\n");
                }
            }
        }
        
        // 发送请求
        method.append("\n            Response response = given()\n");
        method.append("                .headers(headers)\n");
        method.append("                .contentType(ContentType.JSON)\n");
        method.append("                .body(JSON.toJSONString(requestData))\n");
        method.append("                .when()\n");
        method.append("                .post(endpoint)\n");
        method.append("                .then()\n");
        method.append("                .statusCode(200)\n");
        method.append("                .extract()\n");
        method.append("                .response();\n\n");
        
        // 提取变量
        Map<String, String> extract = (Map<String, String>) dep.get("extract");
        if (extract != null) {
            for (Map.Entry<String, String> entry : extract.entrySet()) {
                method.append("            " + entry.getKey() + 
                    " = response.jsonPath().getString(\"" + entry.getValue().replace("$.", "") + "\");\n");
                method.append("            contextParams.put(\"" + entry.getKey() + "\", " + entry.getKey() + ");\n");
            }
        }
        
        method.append("\n            System.out.println(\"✅ " + description + "成功\");\n");
        method.append("        } catch (Exception e) {\n");
        method.append("            System.err.println(\"❌ " + description + "失败: " + e.getMessage() + \");\n");
        method.append("            throw new RuntimeException(\"前置依赖失败\", e);\n");
        method.append("        }\n");
        method.append("    }\n\n");
        
        return method.toString();
    }
    
    /**
     * 生成前置依赖方法集合
     */
    private String generatePreConditionMethods() {
        StringBuilder methods = new StringBuilder();
        
        try {
            if (caseConfig == null) return "";
            
            Map<String, Object> preConditions = (Map<String, Object>) caseConfig.get("preConditions");
            if (preConditions == null || !(Boolean) preConditions.getOrDefault("enabled", false)) {
                return "";
            }
            
            List<Map<String, Object>> dependencies = (List<Map<String, Object>>) preConditions.get("dependencies");
            if (dependencies != null) {
                for (Map<String, Object> dep : dependencies) {
                    if (dep != null) {
                        methods.append(generatePreConditionMethod(dep));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ 生成前置依赖方法时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        
        return methods.toString();
    }
    
    /**
     * 生成生命周期方法
     */
    private String generateLifecycleMethods() {
        StringBuilder methods = new StringBuilder();
        
        methods.append("    /**\n");
        methods.append("     * 测试类初始化\n");
        methods.append("     */\n");
        methods.append("    @BeforeClass\n");
        methods.append("    public void setupClass() {\n");
        methods.append("        System.out.println(\"\\n\\uD83D\\uDE80 初始化测试环境 - " + moduleName + "模块\");\n");
        methods.append("        \n");
        methods.append("        // 初始化变量\n");
        methods.append("        headers = new HashMap<>();\n");
        methods.append("        contextParams = new HashMap<>();\n");
        methods.append("        cleanupIds = new ArrayList<>();\n");
        methods.append("        fileIds = new ArrayList<>();\n");
        methods.append("        preConditionResults = new HashMap<>();\n");
        methods.append("        \n");
        methods.append("        // 加载测试数据\n");
        methods.append("        testData = loadTestData();\n");
        methods.append("        \n");
        methods.append("        // 获取环境配置\n");
        methods.append("        environment = testData.getOrDefault(\"env\", \"dev\").toString();\n");
        methods.append("        baseUrl = testData.getOrDefault(\"baseUrl\", \"http://localhost:8080\").toString();\n");
        methods.append("        \n");
        methods.append("        // 设置RestAssured基础URL\n");
        methods.append("        RestAssured.baseURI = baseUrl;\n");
        methods.append("        \n");
        methods.append("        System.out.println(\"\\u2705 测试环境初始化完成\");\n");
        methods.append("        System.out.println(\"   环境: " + environment + \");\n");
        methods.append("        System.out.println(\"   基础URL: " + baseUrl + \");\n");
        methods.append("    }\n\n");
        
        methods.append("    /**\n");
        methods.append("     * 设置认证信息
     */
    @BeforeMethod
    public void setupAuthentication() {
        try {
            // 获取认证令牌
            authToken = TokenProvider.getToken(environment);
            
            // 设置请求头
            headers.put("Authorization", "Bearer " + authToken);
            headers.put("Content-Type", "application/json");
            
            System.out.println("\uD83D\uDD11 认证信息设置完成");
        } catch (Exception e) {
            System.err.println("\u274C 认证失败: " + e.getMessage());
            throw new RuntimeException("认证失败", e);
        }
    }
    
        methods.append("    /**\n");
        methods.append("     * 测试方法执行后清理\n");
        methods.append("     */\n");
        methods.append("    @AfterMethod\n");
        methods.append("    public void teardownMethod() {\n");
        methods.append("        System.out.println(\"\\n🧹 执行测试清理任务\");\n");
        methods.append("        \n");
        methods.append("        try {\n");
        methods.append("            // 执行清理任务\n");
        methods.append("            executeCleanupTasks();\n");
        methods.append("            \n");
        methods.append("            // 清理临时数据\n");
        methods.append("            cleanupIds.clear();\n");
        methods.append("            \n");
        methods.append("            System.out.println(\"✅ 测试清理完成\");\n");
        methods.append("        } catch (Exception e) {\n");
        methods.append("            System.err.println(\"❌ 清理失败: " + e.getMessage() + "\");\n");
        methods.append("        }\n");
        methods.append("    }\n\n");
        
        methods.append("    /**\n");
        methods.append("     * 测试类执行完成后清理\n");
        methods.append("     */\n");
        methods.append("    @AfterClass\n");
        methods.append("    public void teardownClass() {\n");
        methods.append("        System.out.println(\"\\"\\n\\uD83D\\uDCCA 测试执行汇总\\");\n");
        methods.append("        System.out.println(\"\\"\\u2705 测试类执行完成\\");\n");
        methods.append("    }\n\n");
        
        return methods.toString();
    }

    /**
     * 生成清理任务
     */
    private String generateCleanupTask(Map<String, Object> task) {
        StringBuilder taskCode = new StringBuilder();
        
        try {
            // 检查task是否为null
            if (task == null) {
                System.err.println("⚠️ 清理任务配置为空，跳过生成");
                return "";
            }
            
            String name = (String) task.get("name");
            String description = (String) task.getOrDefault("description", "未知清理任务");
            String type = (String) task.get("type");
            
            if (type == null) {
                System.err.println("⚠️ 清理任务类型未配置，跳过生成");
                return "";
            }
            
            taskCode.append("        // " + description + "\n");
            taskCode.append("        try {\n");
            
            if ("method_call".equals(type)) {
                String className = (String) task.get("class");
                String methodName = (String) task.get("method");
                
                if (className != null && methodName != null) {
                    taskCode.append("            " + className + "." + methodName + "(");
                    
                    Map<String, Object> parameters = (Map<String, Object>) task.get("parameters");
                    if (parameters != null) {
                        List<String> paramList = new ArrayList<>();
                        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                            // 确保参数正确格式化
                            String paramValue = entry.getValue().toString();
                            if (paramValue.contains("{{") && paramValue.contains("}}")) {
                                // 直接使用变量引用
                                paramList.add(paramValue);
                            } else if ("fileIds".equals(paramValue) || "workspaceId".equals(paramValue)) {
                                // 直接使用变量引用
                                paramList.add(paramValue);
                            } else {
                                // 添加引号
                                paramList.add('"' + paramValue + '"');
                            }
                        }
                        taskCode.append(String.join(", ", paramList));
                    }
                    taskCode.append(");\n");
                }
            } else if ("api_call".equals(type)) {
                String endpoint = (String) task.get("endpoint");
                String method = (String) task.get("method");
                
                if (endpoint != null && method != null && !endpoint.isEmpty()) {
                    taskCode.append("            given()\n");
                    taskCode.append("                .headers(headers)\n");
                    taskCode.append("                .when()\n");
                    taskCode.append("                ." + method.toLowerCase() + "(\"/" + endpoint + ")\n");
                    taskCode.append("                .then()\n");
                    taskCode.append("                .statusCode(anyOf(is(200), is(204)));\n");
                }
            }
            
            taskCode.append("            System.out.println(\"✅ " + description + "完成\");\n");
            taskCode.append("        } catch (Exception e) {\n");
            taskCode.append("            System.err.println(\"❌ " + description + "失败: " + e.getMessage() + "\");\n");
            taskCode.append("        }\n\n");
            
        } catch (Exception e) {
            System.err.println("❌ 生成清理任务时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        
        return taskCode.toString();
    }
    
    /**
     * 生成测试方法
     */
    private String generateTestMethods(Map<String, Object> scenarioConfig) {
        StringBuilder methods = new StringBuilder();
        
        try {
            // 检查scenarioConfig是否包含testScenarios
            if (scenarioConfig == null) {
                System.err.println("⚠️ scenarioConfig为空");
                return methods.toString();
            }
            
            // 尝试直接获取测试场景（兼容不同的配置结构）
            Map<String, Object> testScenarios;
            
            if (scenarioConfig.containsKey("scenarios")) {
                // 场景配置直接在scenarioConfig中
                testScenarios = (Map<String, Object>) scenarioConfig.get("scenarios");
            } else if (scenarioConfig.containsKey(moduleName)) {
                // 场景配置在模块名下
                Map<String, Object> moduleScenarios = (Map<String, Object>) scenarioConfig.get(moduleName);
                if (moduleScenarios != null && moduleScenarios.containsKey("scenarios")) {
                    testScenarios = (Map<String, Object>) moduleScenarios.get("scenarios");
                } else {
                    testScenarios = moduleScenarios;
                }
            } else {
                // 使用scenarioConfig作为测试场景
                testScenarios = scenarioConfig;
            }
            
            if (testScenarios == null || testScenarios.isEmpty()) {
                System.err.println("⚠️ 未找到具体的测试场景");
                return methods.toString();
            }
            
            // 生成测试方法
            for (Map.Entry<String, Object> entry : testScenarios.entrySet()) {
                String scenarioName = entry.getKey();
                Map<String, Object> scenario = (Map<String, Object>) entry.getValue();
                
                if (scenario == null) {
                    System.err.println("⚠️ 场景" + scenarioName + "配置为空，跳过");
                    continue;
                }
                
                if (!(Boolean) scenario.getOrDefault("enabled", true)) {
                    continue;
                }
                
                methods.append(generateSingleTestMethod(scenarioName, scenario));
            }
        } catch (Exception e) {
            System.err.println("❌ 生成测试方法时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        
        return methods.toString();
    }

    /**
     * 生成单个测试方法
     */
    private String generateSingleTestMethod(String scenarioName, Map<String, Object> scenario) {
        String methodName = "test" + convertToCamelCase(scenarioName);
        String description = (String) scenario.get("description");
        
        StringBuilder method = new StringBuilder();
        
        // 添加标签
        List<String> tags = (List<String>) scenario.get("tags");
        if (tags != null && !tags.isEmpty()) {
            method.append("    @Test(description = \"").append(description).append("\", groups = {");
            method.append(String.join(",", tags.stream().map(t -> "\"" + t + "\"").toArray(String[]::new)));
            method.append("})");
        } else {
            method.append("    @Test(description = \"").append(description).append("\")");
        }
        method.append("\n");
        method.append("    public void ").append(methodName).append("() {\n");
        method.append("        long startTime = System.currentTimeMillis();\n");
        method.append("        String testCase = \"").append(description).append("\";\n");
        method.append("        System.out.println(\"\\n🧪 执行测试: \" + testCase + \" - \" + new Date());\n\n");
        
        // 获取测试数据
        method.append("        // 获取测试数据\n");
        method.append("        Map<String, Object> scenarioData = (Map<String, Object>) testData.get(\"").append(scenarioName).append("\");\n");
        method.append("        if (scenarioData == null) {\n");
        method.append("            throw new RuntimeException(\"未找到测试数据: ").append(scenarioName).append("\");\n");
        method.append("        }\n\n");
        
        // 构造请求
        method.append("        try {\n");
        method.append("            // 构造请求参数\n");
        method.append("            Map<String, Object> requestData = new HashMap<>();\n");
        method.append("            Map<String, Object> requestParams = (Map<String, Object>) scenarioData.get(\"request\");\n");
        method.append("            requestData.putAll(requestParams);\n");
        method.append("            requestData.putAll(contextParams);\n\n");
        
        // 获取API配置
        Map<String, Object> api = (Map<String, Object>) scenario.get("api");
        if (api != null) {
            String path = (String) api.get("path");
            String httpMethod = (String) api.get("method");
            
            method.append("            String endpoint = \"").append(path).append("\";\n");
            method.append("            String method = \"").append(httpMethod).append("\";\n\n");
            
            // 发送请求
            method.append("            Response response = given()\n");
            method.append("                .headers(headers)\n");
            method.append("                .contentType(ContentType.JSON)\n");
            method.append("                .body(JSON.toJSONString(requestData))\n");
            method.append("                .when()\n");
            method.append("                .request(method, endpoint)\n");
            method.append("                .then()\n");
            method.append("                .extract()\n");
            method.append("                .response();\n\n");
            
            // 验证响应
            method.append("            // 验证响应\n");
            Map<String, Object> validation = (Map<String, Object>) scenario.get("validation");
            if (validation != null) {
                Integer expectedStatus = (Integer) validation.get("statusCode");
                if (expectedStatus != null) {
                    method.append("            Assert.assertEquals(response.statusCode(), ").append(expectedStatus).append(");\n");
                }
                
                Integer responseTime = (Integer) validation.get("responseTime");
                if (responseTime != null) {
                    method.append("            long responseTimeMs = System.currentTimeMillis() - startTime;\n");
                    method.append("            Assert.assertTrue(responseTimeMs <= ").append(responseTime).append(");\n");
                }
                
                String errorCode = (String) validation.get("errorCode");
                if (errorCode != null) {
                    method.append("            Assert.assertEquals(response.jsonPath().getString(\"code\"), \"").append(errorCode).append("\");\n");
                }
            }
            
            // 记录结果用于清理
            method.append("\n            // 记录需要清理的资源\n");
            method.append("            String fileId = response.jsonPath().getString(\"data.fileId\");\n");
            method.append("            if (fileId != null) {\n");
            method.append("                cleanupIds.add(fileId);\n");
            method.append("            }\n");
            
            method.append("\n            System.out.println(\"✅ 测试执行成功: \" + testCase);\n");
            method.append("            System.out.println(\"⏱️ 响应时间: \" + (System.currentTimeMillis() - startTime) + \"ms\");\n");
            
        } else {
            method.append("            // 跳过测试 - 未配置API信息\n");
            method.append("            throw new SkipException(\"未配置API信息\");\n");
        }
        
        method.append("        } catch (Exception e) {\n");
        method.append("            System.err.println(\"❌ 测试执行失败: \" + testCase + \" - \" + e.getMessage());\n");
        method.append("            throw new RuntimeException(\"测试执行失败\", e);\n");
        method.append("        }\n");
        method.append("    }\n\n");
        
        return method.toString();
    }
    
    /**
     * 生成后置清理方法
     */
    private String generatePostConditionMethods() {
        StringBuilder methods = new StringBuilder();
        
        try {
            // 检查caseConfig是否为null
            if (caseConfig == null) {
                System.err.println("⚠️ caseConfig为空，跳过生成后置清理方法");
                return "";
            }
            
            Map<String, Object> postConditions = (Map<String, Object>) caseConfig.get("postConditions");
            if (postConditions == null || !(Boolean) postConditions.getOrDefault("enabled", false)) {
                return "";
            }
            
            methods.append("    /**\n");
            methods.append("     * 执行后置清理任务\n");
            methods.append("     */\n");
            methods.append("    private void executeCleanupTasks() {\n");
            methods.append("        System.out.println(\"\\n🧹 开始执行清理任务\");\n\n");
            
            List<Map<String, Object>> cleanupTasks = (List<Map<String, Object>>) postConditions.get("cleanupTasks");
            if (cleanupTasks != null) {
                for (Map<String, Object> task : cleanupTasks) {
                    if (task != null) {
                        methods.append(generateCleanupTask(task));
                    }
                }
            }
            
            methods.append("        System.out.println(\"✅ 清理任务执行完成\");\n");
            methods.append("    }\n\n");
            
        } catch (Exception e) {
            System.err.println("❌ 生成后置清理方法时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        
        return methods.toString();
    }
    
    /**
     * 生成工具方法
     */
    private String generateUtilityMethods() {
        StringBuilder methods = new StringBuilder();
        
        methods.append("    /**\n");
        methods.append("     * 加载测试数据\n");
        methods.append("     */\n");
        methods.append("    private Map<String, Object> loadTestData() {\n");
        methods.append("        try {\n");
        methods.append("            String dataFile = \"").append(DATA_CONFIG_PATH).append(moduleName).append("_testdata.yml\";\n");
        methods.append("            return new Yaml().load(new FileInputStream(dataFile));\n");
        methods.append("        } catch (IOException e) {\n");
        methods.append("            System.err.println(\"❌ 加载测试数据失败: \" + e.getMessage());\n");
        methods.append("            return new HashMap<>();\n");
        methods.append("        }\n");
        methods.append("    }\n\n");
        
        return methods.toString();
    }
    
    /**
     * 获取swagger接口路径
     */
    private String getSwaggerEndpoint(String endpointName) {
        Map<String, Object> dataSource = (Map<String, Object>) caseConfig.get("dataSource");
        if (dataSource == null) return "";
        
        Map<String, Object> swagger = (Map<String, Object>) dataSource.get("swagger");
        if (swagger == null) return "";
        
        Map<String, Object> mappings = (Map<String, Object>) swagger.get("mappings");
        if (mappings == null) return "";
        
        Map<String, Object> endpoint = (Map<String, Object>) mappings.get(endpointName);
        if (endpoint == null) return "";
        
        return (String) endpoint.get("path");
    }
    
    /**
     * 获取环境变量值
     */
    private String getEnvValue(String key) {
        String[] keys = key.split("\\.");
        Map<String, Object> current = envConfig;
        
        for (String k : keys) {
            if (current == null) return "";
            Object value = current.get(k);
            if (value instanceof Map) {
                current = (Map<String, Object>) value;
            } else {
                return value != null ? value.toString() : "";
            }
        }
        
        return "";
    }
    
    /**
     * 驼峰命名转换
     */
    private String convertToCamelCase(String input) {
        if (input == null || input.isEmpty()) return "";
        
        String[] parts = input.split("[_-]");
        StringBuilder result = new StringBuilder();
        
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    result.append(part.substring(1).toLowerCase());
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * 写入测试文件
     */
    private void writeTestFile(String className, String content) {
        try {
            Map<String, Object> global = (Map<String, Object>) caseConfig.get("global");
            String outputDir = (String) global.get("outputDir");
            String packageName = (String) global.get("packageName");
            
            String packagePath = packageName.replace(".", "/");
            String fullDir = outputDir + "/" + packagePath;
            
            File dir = new File(fullDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            String filePath = fullDir + "/" + className + ".java";
            Files.write(Paths.get(filePath), content.getBytes(StandardCharsets.UTF_8));
            
            System.out.println("✅ 生成测试类: " + filePath);
            
        } catch (IOException e) {
            System.err.println("❌ 写入测试文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 主方法
     */
    public static void main(String[] args) {
        String moduleName = args.length > 0 ? args[0] : "file_upload";
        
        System.out.println("🚀 开始生成测试用例...");
        System.out.println("模块: " + moduleName);
        
        AdvancedTestCaseGenerator generator = new AdvancedTestCaseGenerator(moduleName);
        generator.generateAllTests();
        
        System.out.println("🎉 测试用例生成完成！");
    }
}