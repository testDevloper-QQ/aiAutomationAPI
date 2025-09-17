package apitest.generators;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import com.alibaba.fastjson.JSON;
import org.yaml.snakeyaml.Yaml;

/**
 * 增强版测试用例生成器 - 兼容FileUploadTest.java结构
 * 支持从配置文件自动生成测试用例，包含前置、后置方法处理
 */
public class EnhancedTestCaseGenerator {
    
    private Map<String, Object> config;
    private String moduleName;
    private Map<String, Object> envConfig;
    
    public EnhancedTestCaseGenerator() {
        this.config = new HashMap<>();
        this.moduleName = "file_upload";
        this.envConfig = new HashMap<>();
        loadConfig();
    }
    
    public EnhancedTestCaseGenerator(String moduleName) {
        this.config = new HashMap<>();
        this.moduleName = moduleName;
        this.envConfig = new HashMap<>();
        loadConfig();
    }
    
    /**
     * 加载配置文件 - 支持模块特定配置和环境配置
     */
    private void loadConfig() {
        try {
            Yaml yaml = new Yaml();
            
            // 加载模块测试数据配置
            String configPath = "src/test/resources/testdata/test/module_testdata/" + moduleName + "_testdata.yml";
            FileInputStream fis = new FileInputStream(configPath);
            config = yaml.load(fis);
            fis.close();
            
            // 加载环境配置
            String env = (String) config.getOrDefault("env", "test");
            envConfig = loadEnvConfig(env);
            
            System.out.println("✅ 配置文件加载成功: " + configPath);
            System.out.println("✅ 环境配置加载成功: " + env);
            
        } catch (Exception e) {
            System.err.println("❌ 加载配置文件失败: " + e.getMessage());
            // 使用默认配置
            config = createDefaultConfig();
            envConfig = loadEnvConfig("test");
        }
    }
    
    /**
     * 创建默认配置
     */
    private Map<String, Object> createDefaultConfig() {
        Map<String, Object> defaultConfig = new HashMap<>();
        defaultConfig.put("module", moduleName);
        defaultConfig.put("env", "test");
        defaultConfig.put("account", "testuser");
        defaultConfig.put("password", "testpass");
        defaultConfig.put("testFiles", new ArrayList<Map<String, Object>>() {{
            add(new HashMap<String, Object>() {{
                put("name", "test.txt");
                put("path", "testdata/test/files/test.txt");
                put("type", "text/plain");
                put("size", 1024);
            }});
        }});
        return defaultConfig;
    }
    
    /**
     * 加载测试用例配置
     */
    private Map<String, Object> loadTestCasesConfig() {
        return config;
    }
    
    /**
     * 首字母大写
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * 将模块名转换为驼峰命名
     */
    private String convertToCamelCase(String moduleName) {
        String[] parts = moduleName.split("_");
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
     * 从配置文件生成测试方法
     */
    private void generateTestMethodsFromConfig(StringBuilder content, Map<String, Object> testScenarios) {
        // 前置准备方法
        content.append("    @Test(description = \"前置准备工作空间\")\n");
        content.append("    public void workspace_setup() {\n");
        content.append("        long startTime = System.currentTimeMillis();\n");
        content.append("        System.out.println(\"\\n🚀 开始执行前置准备工作空间 - \" + new Date());\n\n");
        content.append("        try {\n");
        content.append("            // 创建工作空间请求\n");
        content.append("            Map<String, Object> requestData = new HashMap<>();\n");
        content.append("            requestData.put(\"name\", \"测试工作空间\" + System.currentTimeMillis());\n");
        content.append("            requestData.put(\"description\", \"自动测试创建的工作空间\");\n\n");
        content.append("            // 打印请求信息\n");
        content.append("            System.out.println(\"📤 请求URL: \" + baseUrl + \"/api/v1/workspace\");\n");
        content.append("            System.out.println(\"📤 请求方法: POST\");\n");
        content.append("            System.out.println(\"📤 请求数据: \" + JSON.toJSONString(requestData, true));\n\n");
        content.append("            // 发送请求\n");
        content.append("            Response response = given()\n");
        content.append("                .headers(headers)\n");
        content.append("                .contentType(ContentType.JSON)\n");
        content.append("                .body(JSON.toJSONString(requestData))\n");
        content.append("                .when()\n");
        content.append("                .post(\"/api/v1/workspace\")\n");
        content.append("                .then()\n");
        content.append("                .extract()\n");
        content.append("                .response();\n\n");
        content.append("            // 获取工作空间ID\n");
        content.append("            String workspaceId = response.jsonPath().getString(\"data.id\");\n");
        content.append("            contextParams.put(\"workspaceId\", workspaceId);\n");
        content.append("            cleanupIds.add(workspaceId);\n\n");
        content.append("            System.out.println(\"✅ 工作空间创建成功，ID: \" + workspaceId);\n");
        content.append("        } catch (Exception e) {\n");
        content.append("            System.err.println(\"❌ 前置准备执行失败: \" + e.getMessage());\n");
        content.append("            throw new RuntimeException(\"前置准备执行失败\", e);\n");
        content.append("        }\n");
        content.append("    }\n\n");
        
        // 为每个测试场景生成测试方法
        for (Map.Entry<String, Object> entry : testScenarios.entrySet()) {
            String scenarioName = entry.getKey();
            Map<String, Object> scenario = (Map<String, Object>) entry.getValue();
            String testType = (String) scenario.getOrDefault("type", "normal");
            
            generateTestMethodFromScenario(content, scenarioName, testType);
        }
    }
    
    /**
     * 根据测试场景生成测试方法
     */
    private void generateTestMethodFromScenario(StringBuilder content, String scenarioName, String testType) {
        String methodName = "test" + convertToCamelCase(moduleName) + convertToCamelCase(scenarioName);
        
        content.append("    @Test(description = \"").append(scenarioName).append("场景测试\")\n");
        content.append("    public void ").append(methodName).append("() {\n");
        content.append("        long startTime = System.currentTimeMillis();\n");
        content.append("        String testCaseName = \"").append(scenarioName).append("\";\n");
        content.append("        System.out.println(\"\\n🧪 开始执行测试: \" + testCaseName + \" - \" + new Date());\n\n");
        content.append("        try {\n");
        content.append("            // 构造请求数据\n");
        content.append("            Map<String, Object> requestData = new HashMap<>();\n");
        content.append("            requestData.putAll(testData);\n");
        content.append("            requestData.putAll(contextParams);\n");
        content.append("            requestData.put(\"testType\", \"").append(testType).append("\");\n\n");
        content.append("            // 打印请求信息\n");
        content.append("            System.out.println(\"📤 请求URL: \" + baseUrl + \"/sdk/storage/upload/v1\");\n");
        content.append("            System.out.println(\"📤 请求方法: POST\");\n");
        content.append("            System.out.println(\"📤 请求数据: \" + JSON.toJSONString(requestData, true));\n\n");
        content.append("            // 发送请求\n");
        content.append("            Response response = given()\n");
        content.append("                .headers(headers)\n");
        content.append("                .contentType(ContentType.JSON)\n");
        content.append("                .body(JSON.toJSONString(requestData))\n");
        content.append("                .when()\n");
        content.append("                .post(\"/sdk/storage/upload/v1\")\n");
        content.append("                .then()\n");
        content.append("                .extract()\n");
        content.append("                .response();\n\n");
        content.append("            // 打印响应信息\n");
        content.append("            long responseTime = System.currentTimeMillis() - startTime;\n");
        content.append("            System.out.println(\"📥 响应状态码: \" + response.statusCode());\n");
        content.append("            System.out.println(\"⏱️ 响应时间: \" + responseTime + \"ms\");\n");
        content.append("            System.out.println(\"📥 响应内容: \" + response.asString());\n\n");
        content.append("            System.out.println(\"✅ 测试执行成功\");\n\n");
        content.append("        } catch (Exception e) {\n");
        content.append("            long responseTime = System.currentTimeMillis() - startTime;\n");
        content.append("            System.err.println(\"❌ 测试执行失败: \" + testCaseName + \" - \" + e.getMessage());\n");
        content.append("            System.err.println(\"⏱️ 失败耗时: \" + responseTime + \"ms\");\n");
        content.append("            throw new RuntimeException(\"测试执行失败: \" + testCaseName, e);\n");
        content.append("        }\n");
        content.append("    }\n\n");
    }
    
    /**
     * 生成所有测试用例
     */
    private void generateAllTests() {
        try {
            // 读取配置文件
            Map<String, Object> testCasesConfig = loadTestCasesConfig();
            
            // 获取测试场景
            Map<String, Object> scenarios = (Map<String, Object>) testCasesConfig.get("testScenarios");
            if (scenarios == null || scenarios.isEmpty()) {
                System.out.println("❌ 未找到测试场景配置");
                return;
            }
            
            // 为每个场景生成测试类
            for (Map.Entry<String, Object> entry : scenarios.entrySet()) {
                String scenarioName = entry.getKey();
                Map<String, Object> scenarioConfig = (Map<String, Object>) entry.getValue();
                
                System.out.println("📝 正在生成测试类: " + scenarioName);
                generateTestClass(scenarioName, scenarioConfig);
            }
            
            System.out.println("✅ 所有测试用例生成完成！");
            
        } catch (Exception e) {
            System.err.println("❌ 生成测试用例失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 加载环境配置
     */
    private Map<String, Object> loadEnvConfig(String env) {
        try {
            Yaml yaml = new Yaml();
            String configPath = "src/test/resources/config/env.yml";
            FileInputStream fis = new FileInputStream(configPath);
            Map<String, Object> envConfig = yaml.load(fis);
            fis.close();
            
            if (envConfig == null) {
                return new HashMap<>();
            }
            
            Map<String, Object> envData = (Map<String, Object>) envConfig.get(env);
            return envData != null ? envData : new HashMap<>();
            
        } catch (Exception e) {
            System.err.println("❌ 加载环境配置失败: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * 生成测试类 - 完全兼容FileUploadTest结构
     */
    private void generateTestClass(String scenarioName, Map<String, Object> scenarioConfig) {
        StringBuilder classContent = new StringBuilder();
        
        // 包声明
        String packageName = "com.apitest.tests.generated";
        classContent.append("package ").append(packageName).append(";\n\n");
        
        // 导入语句 - 完全匹配FileUploadTest
        classContent.append("import org.testng.annotations.*;\n");
        classContent.append("import io.restassured.RestAssured;\n");
        classContent.append("import io.restassured.http.ContentType;\n");
        classContent.append("import io.restassured.response.Response;\n");
        classContent.append("import static io.restassured.RestAssured.*;\n");
        classContent.append("import static org.hamcrest.Matchers.*;\n");
        classContent.append("import com.alibaba.fastjson.JSON;\n");
        classContent.append("import com.apitest.core.BaseTest;\n");
        classContent.append("import com.apitest.utils.ConfigManager;\n");
        classContent.append("import java.io.File;\n");
        classContent.append("import java.util.*;\n");
        classContent.append("import java.io.*;\n");
        classContent.append("import org.yaml.snakeyaml.Yaml;\n");
        classContent.append("import java.nio.charset.StandardCharsets;\n\n");
        
        // 类定义和注释
        String className = convertToCamelCase(moduleName) + "Test";
        classContent.append("/**\n");
        classContent.append(" * Auto-generated test class for: ").append(moduleName).append("\n");
        classContent.append(" * Test scope: ").append(scenarioConfig.getOrDefault("description", moduleName + " functionality test")).append("\n");
        classContent.append(" * Generated time: ").append(new Date()).append("\n");
        classContent.append(" */\n");
        classContent.append("public class ").append(className).append(" extends BaseTest {\n\n");
        
        // 成员变量 - 完全匹配FileUploadTest
        classContent.append("    // 测试配置\n");
        classContent.append("    private String baseUrl;\n");
        classContent.append("    private String environment;\n");
        classContent.append("    private Map<String, Object> testData;\n");
        classContent.append("    private Map<String, String> headers;\n");
        classContent.append("    private List<String> cleanupIds;\n");
        classContent.append("    private Map<String, Object> contextParams;\n\n");
        
        // 生命周期方法
        generateLifecycleMethods(classContent, scenarioConfig);
        
        // 测试方法 - 根据配置生成
        generateTestMethodsFromConfig(classContent, scenarioConfig);
        
        // 工具方法
        generateUtilityMethods(classContent, scenarioConfig);
        
        classContent.append("}\n");
        
        // 写入文件
        String outputDir = "src/test/java";
        String packagePath = packageName.replace(".", "/");
        String fullOutputDir = outputDir + "/" + packagePath;
        
        try {
            File dir = new File(fullOutputDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            String fileName = fullOutputDir + "/" + className + ".java";
            Files.write(Paths.get(fileName), classContent.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("✅ Generated test class: " + fileName);
            
        } catch (IOException e) {
            System.err.println("❌ Failed to generate test class: " + e.getMessage());
        }
    }
    
    /**
     * 生成生命周期方法 - 匹配FileUploadTest结构
     */
    private void generateLifecycleMethods(StringBuilder content, Map<String, Object> config) {
        // BeforeClass方法
        content.append("    @BeforeClass\n");
        content.append("    public void beforeClass() {\n");
        content.append("        long startTime = System.currentTimeMillis();\n");
        content.append("        System.out.println(\"\\\\n=== Starting test suite: \" + getClass().getSimpleName() + \" - \" + new Date());\n\n");
        
        content.append("        // Initialize configuration from BaseTest\n");
        content.append("        environment = apitest.utils.ConfigManager.get(\"environment\");\n");
        content.append("        baseUrl = apitest.utils.ConfigManager.get(\"baseUrl\");\n");
        content.append("        RestAssured.baseURI = baseUrl;\n");
        content.append("        RestAssured.basePath = \"\";\n\n");
        
        content.append("        // Initialize test variables\n");
        content.append("        cleanupIds = new ArrayList<>();\n");
        content.append("        contextParams = new HashMap<>();\n");
        content.append("        testData = loadTestData();\n");
        content.append("        headers = new HashMap<>();\n");
        content.append("        headers.put(\"Content-Type\", \"application/json\");\n\n");
        
        content.append("        long initTime = System.currentTimeMillis() - startTime;\n");
        content.append("        System.out.println(\"✅ Test environment initialized in: \" + initTime + \"ms\");\n");
        content.append("        System.out.println(\"🌐 Base URL: \" + baseUrl);\n");
        content.append("        System.out.println(\"🔧 Environment: \" + environment);\n");
        content.append("    }\n\n");
        
        // BeforeMethod方法
        content.append("    @BeforeMethod\n");
        content.append("    public void beforeMethod() {\n");
        content.append("        long startTime = System.currentTimeMillis();\n");
        content.append("        System.out.println(\"\\\\n=== Getting authentication info - \" + new Date());\n\n");
        
        content.append("        try {\n");
        content.append("            // Get authentication token\n");
        content.append("            String token = getAuthToken();\n");
        content.append("            headers.put(\"Authorization\", \"Bearer \" + token);\n\n");
        content.append("            // Set default headers\n");
        content.append("            headers.put(\"Accept\", \"application/json\");\n");
        content.append("            headers.put(\"User-Agent\", \"API-Test-Client/1.0\");\n\n");
        content.append("            long authTime = System.currentTimeMillis() - startTime;\n");
        content.append("            System.out.println(\"✅ Authentication successful, token: \" + token.substring(0, 10) + \"...\");\n");
        content.append("            System.out.println(\"⏱️ Auth time: \" + authTime + \"ms\");\n\n");
        content.append("        } catch (Exception e) {\n");
        content.append("            long authTime = System.currentTimeMillis() - startTime;\n");
        content.append("            System.err.println(\"❌ Authentication failed: \" + e.getMessage());\n");
        content.append("            System.err.println(\"⏱️ Auth failed time: \" + authTime + \"ms\");\n");
        content.append("            // Use mock token for testing\n");
        content.append("            headers.put(\"Authorization\", \"Bearer mock_token_for_testing\");\n");
        content.append("        }\n");
        content.append("    }\n\n");
        
        // AfterMethod方法
        content.append("    @AfterMethod\n");
        content.append("    public void afterMethod() {\n");
        content.append("        // Cleanup after each test method\n");
        content.append("        cleanupTestData();\n");
        content.append("    }\n\n");
        
        // AfterClass方法
        content.append("    @AfterClass\n");
        content.append("    public void afterClass() {\n");
        content.append("        long startTime = System.currentTimeMillis();\n");
        content.append("        System.out.println(\"\\\\n=== Cleaning up test resources - \" + new Date());\n\n");
        
        content.append("        // Cleanup uploaded files\n");
        content.append("        if (!cleanupIds.isEmpty()) {\n");
        content.append("            System.out.println(\"🗑️ Need to cleanup resources: \" + cleanupIds.size());\n");
        content.append("            cleanupTestData();\n");
        content.append("        } else {\n");
        content.append("            System.out.println(\"✅ No resources to cleanup\");\n");
        content.append("        }\n\n");
        content.append("        // Clear context\n");
        content.append("        contextParams.clear();\n");
        content.append("        long cleanupTime = System.currentTimeMillis() - startTime;\n");
        content.append("        System.out.println(\"✅ Cleanup completed in: \" + cleanupTime + \"ms\");\n");
        content.append("    }\n\n");
    }

    /**
     * 生成工具方法 - 匹配FileUploadTest结构
     */
    private void generateUtilityMethods(StringBuilder content, Map<String, Object> config) {
        // loadTestData方法
        content.append("    private Map<String, Object> loadTestData() {\n");
        content.append("        try {\n");
        content.append("            Yaml yaml = new Yaml();\n");
        content.append("            String testDataPath = \"src/test/resources/testdata/test/module_testdata/\" + \"").append(moduleName).append("_testdata.yml\";\n");
        content.append("            FileInputStream fis = new FileInputStream(testDataPath);\n");
        content.append("            Map<String, Object> testData = yaml.load(fis);\n");
        content.append("            fis.close();\n");
        content.append("            return testData != null ? testData : new HashMap<>();\n");
        content.append("        } catch (Exception e) {\n");
        content.append("            System.err.println(\"❌ Failed to load test data: \" + e.getMessage());\n");
        content.append("            return new HashMap<>();\n");
        content.append("        }\n");
        content.append("    }\n\n");
        
        // getAuthToken方法
        content.append("    private String getAuthToken() {\n");
        content.append("        // Get authentication info from test data\n");
        content.append("        Map<String, Object> authData = (Map<String, Object>) testData.get(\"auth\");\n");
        content.append("        if (authData == null) {\n");
        content.append("            return \"mock_token_\" + System.currentTimeMillis();\n");
        content.append("        }\n");
        content.append("        \n");
        content.append("        String username = (String) authData.get(\"username\");\n");
        content.append("        String password = (String) authData.get(\"password\");\n");
        content.append("        \n");
        content.append("        // In real scenario, call actual auth API\n");
        content.append("        return \"Bearer_\" + username + \"_\" + password + \"_\" + System.currentTimeMillis();\n");
        content.append("    }\n\n");
        
        // cleanupTestData方法
        content.append("    private void cleanupTestData() {\n");
        content.append("        System.out.println(\"🧹 Starting cleanup of test data...\");\n");
        content.append("        \n");
        content.append("        if (cleanupIds.isEmpty()) {\n");
        content.append("            System.out.println(\"✅ No test data to cleanup\");\n");
        content.append("            return;\n");
        content.append("        }\n");
        content.append("        \n");
        content.append("        System.out.println(\"✅ Test data cleanup completed: \" + cleanupIds.size() + \" items\");\n");
        content.append("    }\n\n");
    }
    
    /**
     * 生成测试方法 - 匹配FileUploadTest结构
     */
    private void generateTestMethods(StringBuilder content, String scenarioName, Map<String, Object> scenarioConfig) {
        // workspace_setup前置准备测试方法
        content.append("    @Test(description = \"Workspace setup - 前置准备\")\n");
        content.append("    public void workspace_setup() {\n");
        content.append("        LoggerUtil.info(\"🚀 Starting workspace setup for file upload tests\");\n");
        content.append("        \n");
        content.append("        // Load test data\n");
        content.append("        testData = loadTestData();\n");
        content.append("        AssertUtil.notNull(testData, \"Test data loaded successfully\");\n");
        content.append("        \n");
        content.append("        // Initialize context parameters\n");
        content.append("        contextParams = new HashMap<>();\n");
        content.append("        contextParams.put(\"testStartTime\", System.currentTimeMillis());\n");
        content.append("        contextParams.put(\"moduleName\", \"").append(moduleName).append("\");\n");
        content.append("        \n");
        content.append("        LoggerUtil.info(\"✅ Workspace setup completed\");\n");
        content.append("    }\n\n");

        // 正常场景测试
        content.append("    @Test(description = \"正常场景测试\", dependsOnMethods = \"workspace_setup\")\n");
        content.append("    public void testNormalScenario() {\n");
        content.append("        LoggerUtil.info(\"🧪 Starting normal scenario test\");\n");
        content.append("        \n");
        content.append("        try {\n");
        content.append("            // Prepare test file\n");
        content.append("            File testFile = FileUtil.createTempFile(\"test-upload.txt\", \"Hello, this is a test file!\");\n");
        content.append("            AssertUtil.notNull(testFile, \"Test file created successfully\");\n");
        content.append("            \n");
        content.append("            // Send upload request\n");
        content.append("            Response response = given()\n");
        content.append("                .header(\"Authorization\", \"Bearer \" + authToken)\n");
        content.append("                .contentType(ContentType.MULTIPART)\n");
        content.append("                .multiPart(\"file\", testFile)\n");
        content.append("                .multiPart(\"description\", \"Test file upload\")\n");
        content.append("            .when()\n");
        content.append("                .post(\"/api/file/upload\")\n");
        content.append("            .then()\n");
        content.append("                .extract()\n");
        content.append("                .response();\n");
        content.append("            \n");
        content.append("            // Validate response\n");
        content.append("            AssertUtil.assertEquals(response.statusCode(), 200, \"Status code should be 200\");\n");
        content.append("            AssertUtil.assertEquals(response.jsonPath().getInt(\"code\"), 0, \"Response code should be 0\");\n");
        content.append("            AssertUtil.assertNotNull(response.jsonPath().getString(\"data.id\"), \"File ID should not be null\");\n");
        content.append("            \n");
        content.append("            // Store file ID for cleanup\n");
        content.append("            String fileId = response.jsonPath().getString(\"data.id\");\n");
        content.append("            cleanupIds.add(fileId);\n");
        content.append("            \n");
        content.append("            LoggerUtil.info(\"✅ Normal scenario test passed\");\n");
        content.append("            \n");
        content.append("        } catch (Exception e) {\n");
        content.append("            LoggerUtil.error(\"❌ Normal scenario test failed: \" + e.getMessage());\n");
        content.append("            throw e;\n");
        content.append("        }\n");
        content.append("    }\n\n");

        // 边界值测试
        content.append("    @Test(description = \"边界值测试\", dependsOnMethods = \"workspace_setup\")\n");
        content.append("    public void testBoundaryValue() {\n");
        content.append("        LoggerUtil.info(\"🧪 Starting boundary value test\");\n");
        content.append("        \n");
        content.append("        try {\n");
        content.append("            // Create large file content (1MB)\n");
        content.append("            String largeContent = \"A\".repeat(1024 * 1024);\n");
        content.append("            File largeFile = FileUtil.createTempFile(\"large-file.txt\", largeContent);\n");
        content.append("            AssertUtil.notNull(largeFile, \"Large test file created successfully\");\n");
        content.append("            \n");
        content.append("            // Send upload request with large file\n");
        content.append("            Response response = given()\n");
        content.append("                .header(\"Authorization\", \"Bearer \" + authToken)\n");
        content.append("                .contentType(ContentType.MULTIPART)\n");
        content.append("                .multiPart(\"file\", largeFile)\n");
        content.append("                .multiPart(\"description\", \"Large file test\")\n");
        content.append("            .when()\n");
        content.append("                .post(\"/api/file/upload\")\n");
        content.append("            .then()\n");
        content.append("                .extract()\n");
        content.append("                .response();\n");
        content.append("            \n");
        content.append("            // Validate response\n");
        content.append("            AssertUtil.assertEquals(response.statusCode(), 200, \"Status code should be 200\");\n");
        content.append("            AssertUtil.assertEquals(response.jsonPath().getInt(\"code\"), 0, \"Response code should be 0\");\n");
        content.append("            \n");
        content.append("            // Store file ID for cleanup\n");
        content.append("            String fileId = response.jsonPath().getString(\"data.id\");\n");
        content.append("            cleanupIds.add(fileId);\n");
        content.append("            \n");
        content.append("            LoggerUtil.info(\"✅ Boundary value test passed\");\n");
        content.append("            \n");
        content.append("        } catch (Exception e) {\n");
        content.append("            LoggerUtil.error(\"❌ Boundary value test failed: \" + e.getMessage());\n");
        content.append("            throw e;\n");
        content.append("        }\n");
        content.append("    }\n\n");

        // 异常场景测试
        content.append("    @Test(description = \"异常场景测试\", dependsOnMethods = \"workspace_setup\")\n");
        content.append("    public void testExceptionScenario() {\n");
        content.append("        LoggerUtil.info(\"🧪 Starting exception scenario test\");\n");
        content.append("        \n");
        content.append("        try {\n");
        content.append("            // Test with empty file\n");
        content.append("            File emptyFile = FileUtil.createTempFile(\"empty-file.txt\", \"\");\n");
        content.append("            AssertUtil.notNull(emptyFile, \"Empty test file created successfully\");\n");
        content.append("            \n");
        content.append("            // Send upload request with empty file\n");
        content.append("            Response response = given()\n");
        content.append("                .header(\"Authorization\", \"Bearer \" + authToken)\n");
        content.append("                .contentType(ContentType.MULTIPART)\n");
        content.append("                .multiPart(\"file\", emptyFile)\n");
        content.append("            .when()\n");
        content.append("                .post(\"/api/file/upload\")\n");
        content.append("            .then()\n");
        content.append("                .extract()\n");
        content.append("                .response();\n");
        content.append("            \n");
        content.append("            // Validate error response\n");
        content.append("            AssertUtil.assertEquals(response.statusCode(), 400, \"Status code should be 400 for invalid file\");\n");
        content.append("            AssertUtil.assertTrue(response.jsonPath().getString(\"message\").contains(\"Invalid\"), \"Error message should contain 'Invalid'\");\n");
        content.append("            \n");
        content.append("            LoggerUtil.info(\"✅ Exception scenario test passed\");\n");
        content.append("            \n");
        content.append("        } catch (Exception e) {\n");
        content.append("            LoggerUtil.error(\"❌ Exception scenario test failed: \" + e.getMessage());\n");
        content.append("            throw e;\n");
        content.append("        }\n");
        content.append("    }\n\n");
    }
    
    /**
     * 生成测试方法体
     */
    private void generateTestMethodBody(StringBuilder content, String scenarioName, String testType) {
        content.append("        try {\n");
        content.append("            System.out.println(\"正在执行 ").append(testType).append(" 测试...\");\n\n");
        
        // 获取测试数据
        content.append("            // 获取测试数据\n");
        content.append("            Map<String, Object> testData = getTestData(\"").append(testType).append("\");\n");
        content.append("            if (testData == null || testData.isEmpty()) {\n");
        content.append("                throw new RuntimeException(\"测试数据未找到\");\n");
        content.append("            }\n\n");
        
        // 构建请求
        content.append("            // 构建请求参数\n");
        content.append("            String requestUrl = API_PATH;\n");
        content.append("            Map<String, String> requestHeaders = new HashMap<>(headers);\n");
        content.append("            \n");
        content.append("            // 设置认证信息\n");
        content.append("            if (authToken != null) {\n");
        content.append("                requestHeaders.put(\"Authorization\", \"Bearer \" + authToken);\n");
        content.append("            }\n\n");
        
        // 发送请求
        content.append("            // 发送HTTP请求\n");
        content.append("            Response response = given()\n");
        content.append("                    .headers(requestHeaders)\n");
        content.append("                    .contentType(ContentType.JSON)\n");
        content.append("                    .body(testData)\n");
        content.append("                    .when()\n");
        content.append("                    .post(requestUrl)\n");
        content.append("                    .then()\n");
        content.append("                    .extract()\n");
        content.append("                    .response();\n\n");
        
        // 断言
        content.append("            // 验证响应结果\n");
        content.append("            performAssertions(response, \"").append(testType).append("\");\n\n");
        
        content.append("            System.out.println(\"✅ ").append(testType).append(" 测试执行成功\");\n");
        content.append("            \n");
        content.append("        } catch (Exception e) {\n");
        content.append("            System.err.println(\"❌ ").append(testType).append(" 测试执行失败: \" + e.getMessage());\n");
        content.append("            Assert.fail(\"测试执行失败: \" + e.getMessage());\n");
        content.append("        }\n");
    }
    

    
    /**
     * 从配置文件生成测试用例
     */
    public void generateFromConfigFile(String configPath) {
        try {
            Yaml yaml = new Yaml();
            FileInputStream fis = new FileInputStream(configPath);
            Map<String, Object> config = yaml.load(fis);
            fis.close();
            
            System.out.println("📋 Loading test configuration from: " + configPath);
            
            // 获取测试场景配置
            Map<String, Object> testScenarios = (Map<String, Object>) config.getOrDefault("testScenarios", new HashMap<>());
            
            // 为每个模块生成测试类
            for (Map.Entry<String, Object> entry : testScenarios.entrySet()) {
                String moduleName = entry.getKey();
                Map<String, Object> moduleConfig = (Map<String, Object>) entry.getValue();
                
                // 生成测试类
                String className = (String) moduleConfig.getOrDefault("className", convertToCamelCase(moduleName) + "Test");
                generateTestClassFromModule(moduleName, moduleConfig, config);
            }
            
            System.out.println("✅ Generated test cases from configuration file");
            
        } catch (Exception e) {
            System.err.println("❌ Failed to generate from config file: " + e.getMessage());
            // 生成默认配置
            generateDefaultConfig(configPath);
        }
    }
    
    /**
     * 根据模块配置生成测试类
     */
    private void generateTestClassFromModule(String moduleName, Map<String, Object> moduleConfig, Map<String, Object> globalConfig) {
        try {
            String className = (String) moduleConfig.getOrDefault("className", convertToCamelCase(moduleName) + "Test");
            String packageName = (String) globalConfig.getOrDefault("packageName", "com.apitest.tests.generated");
            String outputDir = (String) globalConfig.getOrDefault("outputDir", "src/test/java");
            
            StringBuilder content = new StringBuilder();
            
            // 包声明
            content.append("package ").append(packageName).append(";\n\n");
            
            // 导入语句
            content.append(generateImports());
            
            // 类注释
            content.append("/**\n")
                   .append(" * Auto-generated test class for: ").append(moduleName).append("\n")
                   .append(" * Test scope: ").append(moduleConfig.getOrDefault("description", moduleName + " functionality test")).append("\n")
                   .append(" * Generated time: ").append(new java.util.Date()).append("\n")
                   .append(" */\n");
            
            // 类定义
            content.append("public class ").append(className).append(" extends BaseTest {\n\n");
            
            // 成员变量
            content.append(generateMemberVariables());
            
            // BeforeClass
            content.append(generateBeforeClass());
            
            // BeforeMethod
            content.append(generateBeforeMethod());
            
            // AfterMethod
            content.append(generateAfterMethod());
            
            // AfterClass
            content.append(generateAfterClass());
            
            // 前置方法
            content.append(generateWorkspaceSetup());
            
            // 根据测试用例生成测试方法
            Map<String, Object> testCases = (Map<String, Object>) moduleConfig.getOrDefault("testCases", new HashMap<>());
            
            // 正常场景
            if (Boolean.TRUE.equals(((Map<String, Object>) testCases.get("normal")).get("enabled"))) {
                content.append(generateNormalTestMethod(moduleName, moduleConfig));
            }
            
            // 边界场景
            if (Boolean.TRUE.equals(((Map<String, Object>) testCases.get("boundary")).get("enabled"))) {
                content.append(generateBoundaryTestMethod(moduleName, moduleConfig));
            }
            
            // 异常场景
            if (Boolean.TRUE.equals(((Map<String, Object>) testCases.get("exception")).get("enabled"))) {
                content.append(generateExceptionTestMethod(moduleName, moduleConfig));
            }
            
            // 工具方法
            content.append(generateUtilityMethods());
            
            content.append("}\n");
            
            // 写入文件
            writeTestClass(outputDir, packageName, className, content.toString());
            
        } catch (Exception e) {
            System.err.println("❌ Failed to generate test class for " + moduleName + ": " + e.getMessage());
        }
    }
    
    /**
     * 生成正常测试方法
     */
    private StringBuilder generateNormalTestMethod(String moduleName, Map<String, Object> moduleConfig) {
        StringBuilder content = new StringBuilder();
        Map<String, Object> api = (Map<String, Object>) moduleConfig.get("api");
        String methodName = "test" + convertToCamelCase(moduleName) + "Normal";
        
        content.append("    @Test(description = \"正常场景测试\", dependsOnMethods = \"workspace_setup\")\n");
        content.append("    public void ").append(methodName).append("() {\n");
        content.append("        long startTime = System.currentTimeMillis();\n");
        content.append("        String testCaseName = \"").append(moduleName).append("_normal\";\n");
        content.append("        System.out.println(\"\\n🧪 开始执行测试: \" + testCaseName + \" - \" + new Date());\n\n");
        
        content.append("        try {\n");
        content.append("            // 构造请求数据\n");
        content.append("            Map<String, Object> requestData = new HashMap<>();\n");
        content.append("            requestData.putAll(testData);\n");
        content.append("            requestData.putAll(contextParams);\n");
        content.append("            requestData.put(\"testType\", \"normal\");\n\n");
        
        content.append("            // 打印请求信息\n");
        content.append("            System.out.println(\"📤 请求URL: \" + baseUrl + \"").append(api.get("path")).append("\");\n");
        content.append("            System.out.println(\"📤 请求方法: \" + \"").append(api.get("method")).append("\");\n");
        content.append("            System.out.println(\"📤 请求数据: \" + JSON.toJSONString(requestData, true));\n\n");
        
        content.append("            // 发送请求\n");
        content.append("            Response response = given()\n");
        content.append("                .headers(headers)\n");
        content.append("                .contentType(ContentType.JSON)\n");
        content.append("                .body(JSON.toJSONString(requestData))\n");
        content.append("                .when()\n");
        content.append("                .post(\"").append(api.get("path")).append("\")\n");
        content.append("                .then()\n");
        content.append("                .extract()\n");
        content.append("                .response();\n\n");
        
        content.append("            // 打印响应信息\n");
        content.append("            long responseTime = System.currentTimeMillis() - startTime;\n");
        content.append("            System.out.println(\"📥 响应状态码: \" + response.statusCode());\n");
        content.append("            System.out.println(\"⏱️ 响应时间: \" + responseTime + \"ms\");\n");
        content.append("            System.out.println(\"📥 响应内容: \" + response.asString());\n\n");
        content.append("            System.out.println(\"✅ 测试执行成功\");\n\n");
        content.append("        } catch (Exception e) {\n");
        content.append("            long responseTime = System.currentTimeMillis() - startTime;\n");
        content.append("            System.err.println(\"❌ 测试执行失败: \" + testCaseName + \" - \" + e.getMessage());\n");
        content.append("            System.err.println(\"⏱️ 失败耗时: \" + responseTime + \"ms\");\n");
        content.append("            throw new RuntimeException(\"测试执行失败: \" + testCaseName, e);\n");
        content.append("        }\n");
        content.append("    }\n\n");
        
        return content;
    }
    
    /**
     * 生成边界测试方法
     */
    private StringBuilder generateBoundaryTestMethod(String moduleName, Map<String, Object> moduleConfig) {
        StringBuilder content = new StringBuilder();
        Map<String, Object> api = (Map<String, Object>) moduleConfig.get("api");
        String methodName = "test" + convertToCamelCase(moduleName) + "Boundary";
        
        content.append("    @Test(description = \"边界值测试\", dependsOnMethods = \"workspace_setup\")\n");
        content.append("    public void ").append(methodName).append("() {\n");
        content.append("        long startTime = System.currentTimeMillis();\n");
        content.append("        String testCaseName = \"").append(moduleName).append("_boundary\";\n");
        content.append("        System.out.println(\"\\n🧪 开始执行边界测试: \" + testCaseName + \" - \" + new Date());\n\n");
        
        content.append("        try {\n");
        content.append("            // 测试空文件\n");
        content.append("            testEmptyFile();\n\n");
        content.append("            // 测试大文件\n");
        content.append("            testLargeFile();\n\n");
        content.append("            // 测试特殊字符文件名\n");
        content.append("            testSpecialCharsFileName();\n\n");
        content.append("            long responseTime = System.currentTimeMillis() - startTime;\n");
        content.append("            System.out.println(\"✅ 边界测试全部完成，耗时: \" + responseTime + \"ms\");\n\n");
        content.append("        } catch (Exception e) {\n");
        content.append("            long responseTime = System.currentTimeMillis() - startTime;\n");
        content.append("            System.err.println(\"❌ 边界测试执行失败: \" + testCaseName + \" - \" + e.getMessage());\n");
        content.append("            System.err.println(\"⏱️ 失败耗时: \" + responseTime + \"ms\");\n");
        content.append("            throw new RuntimeException(\"边界测试执行失败: \" + testCaseName, e);\n");
        content.append("        }\n");
        content.append("    }\n\n");
        
        return content;
    }
    
    /**
     * 生成异常测试方法
     */
    private StringBuilder generateExceptionTestMethod(String moduleName, Map<String, Object> moduleConfig) {
        StringBuilder content = new StringBuilder();
        Map<String, Object> api = (Map<String, Object>) moduleConfig.get("api");
        String methodName = "test" + convertToCamelCase(moduleName) + "Exception";
        
        content.append("    @Test(description = \"异常场景测试\", dependsOnMethods = \"workspace_setup\")\n");
        content.append("    public void ").append(methodName).append("() {\n");
        content.append("        long startTime = System.currentTimeMillis();\n");
        content.append("        String testCaseName = \"").append(moduleName).append("_exception\";\n");
        content.append("        System.out.println(\"\\n🧪 开始执行异常测试: \" + testCaseName + \" - \" + new Date());\n\n");
        
        content.append("        try {\n");
        content.append("            // 测试无效文件类型\n");
        content.append("            testInvalidFileType();\n\n");
        content.append("            // 测试缺少必填参数\n");
        content.append("            testMissingRequiredParam();\n\n");
        content.append("            // 测试无效认证\n");
        content.append("            testInvalidAuth();\n\n");
        content.append("            long responseTime = System.currentTimeMillis() - startTime;\n");
        content.append("            System.out.println(\"✅ 异常测试全部完成，耗时: \" + responseTime + \"ms\");\n\n");
        content.append("        } catch (Exception e) {\n");
        content.append("            long responseTime = System.currentTimeMillis() - startTime;\n");
        content.append("            System.err.println(\"❌ 异常测试执行失败: \" + testCaseName + \" - \" + e.getMessage());\n");
        content.append("            System.err.println(\"⏱️ 失败耗时: \" + responseTime + \"ms\");\n");
        content.append("            throw new RuntimeException(\"异常测试执行失败: \" + testCaseName, e);\n");
        content.append("        }\n");
        content.append("    }\n\n");
        
        return content;
    }
    
    /**
     * 生成工具方法
     */
    private StringBuilder generateUtilityMethods() {
        StringBuilder content = new StringBuilder();
        
        content.append("    private void testEmptyFile() {\n");
        content.append("        System.out.println(\"🧪 测试空文件上传...\");\n");
        content.append("        // 实现空文件测试逻辑\n");
        content.append("    }\n\n");
        
        content.append("    private void testLargeFile() {\n");
        content.append("        System.out.println(\"🧪 测试大文件上传...\");\n");
        content.append("        // 实现大文件测试逻辑\n");
        content.append("    }\n\n");
        
        content.append("    private void testSpecialCharsFileName() {\n");
        content.append("        System.out.println(\"🧪 测试特殊字符文件名...\");\n");
        content.append("        // 实现特殊字符文件名测试逻辑\n");
        content.append("    }\n\n");
        
        content.append("    private void testInvalidFileType() {\n");
        content.append("        System.out.println(\"🧪 测试无效文件类型...\");\n");
        content.append("        // 实现无效文件类型测试逻辑\n");
        content.append("    }\n\n");
        
        content.append("    private void testMissingRequiredParam() {\n");
        content.append("        System.out.println(\"🧪 测试缺少必填参数...\");\n");
        content.append("        // 实现缺少必填参数测试逻辑\n");
        content.append("    }\n\n");
        
        content.append("    private void testInvalidAuth() {\n");
        content.append("        System.out.println(\"🧪 测试无效认证...\");\n");
        content.append("        // 实现无效认证测试逻辑\n");
        content.append("    }\n\n");
        
        return content;
    }
    
    /**
     * 生成导入语句
     */
    private StringBuilder generateImports() {
        StringBuilder imports = new StringBuilder();
        imports.append("import org.testng.annotations.*;\n");
        imports.append("import io.restassured.RestAssured;\n");
        imports.append("import io.restassured.http.ContentType;\n");
        imports.append("import io.restassured.response.Response;\n");
        imports.append("import static io.restassured.RestAssured.*;\n");
        imports.append("import static org.hamcrest.Matchers.*;\n");
        imports.append("import com.alibaba.fastjson.JSON;\n");
        imports.append("import com.apitest.core.BaseTest;\n");
        imports.append("import com.apitest.utils.ConfigManager;\n");
        imports.append("import java.io.File;\n");
        imports.append("import java.util.*;\n");
        imports.append("import java.io.*;\n");
        imports.append("import org.yaml.snakeyaml.Yaml;\n");
        imports.append("import java.nio.charset.StandardCharsets;\n\n");
        return imports;
    }
    
    /**
     * 生成成员变量
     */
    private StringBuilder generateMemberVariables() {
        StringBuilder content = new StringBuilder();
        content.append("    // 测试配置\n");
        content.append("    private String baseUrl;\n");
        content.append("    private String environment;\n");
        content.append("    private Map<String, Object> testData;\n");
        content.append("    private Map<String, String> headers;\n");
        content.append("    private List<String> cleanupIds;\n");
        content.append("    private Map<String, Object> contextParams;\n\n");
        return content;
    }
    
    /**
     * 生成BeforeClass方法
     */
    private StringBuilder generateBeforeClass() {
        StringBuilder content = new StringBuilder();
        content.append("    @BeforeClass\n");
        content.append("    public void beforeClass() {\n");
        content.append("        long startTime = System.currentTimeMillis();\n");
        content.append("        System.out.println(\"\\n=== Starting test suite: \" + getClass().getSimpleName() + \" - \" + new Date());\n\n");
        content.append("        // 初始化配置\n");
        content.append("        environment = apitest.utils.ConfigManager.get(\"environment\");\n");
        content.append("        baseUrl = apitest.utils.ConfigManager.get(\"baseUrl\");\n");
        content.append("        RestAssured.baseURI = baseUrl;\n");
        content.append("        RestAssured.basePath = \"\";\n\n");
        content.append("        // 初始化测试变量\n");
        content.append("        cleanupIds = new ArrayList<>();\n");
        content.append("        contextParams = new HashMap<>();\n");
        content.append("        testData = loadTestData();\n");
        content.append("        headers = new HashMap<>();\n");
        content.append("        headers.put(\"Content-Type\", \"application/json\");\n\n");
        content.append("        long initTime = System.currentTimeMillis() - startTime;\n");
        content.append("        System.out.println(\"✅ 测试环境初始化完成，耗时: \" + initTime + \"ms\");\n");
        content.append("    }\n\n");
        return content;
    }
    
    /**
     * 生成BeforeMethod方法
     */
    private StringBuilder generateBeforeMethod() {
        StringBuilder content = new StringBuilder();
        content.append("    @BeforeMethod\n");
        content.append("    public void beforeMethod() {\n");
        content.append("        long startTime = System.currentTimeMillis();\n");
        content.append("        System.out.println(\"\\n=== 获取认证信息 - \" + new Date());\n\n");
        content.append("        try {\n");
        content.append("            // 获取认证token\n");
        content.append("            String token = getAuthToken();\n");
        content.append("            headers.put(\"Authorization\", \"Bearer \" + token);\n\n");
        content.append("            // 设置默认headers\n");
        content.append("            headers.put(\"Accept\", \"application/json\");\n");
        content.append("            headers.put(\"User-Agent\", \"API-Test-Client/1.0\");\n\n");
        content.append("            long authTime = System.currentTimeMillis() - startTime;\n");
        content.append("            System.out.println(\"✅ 认证成功，token: \" + token.substring(0, 10) + \"...\");\n");
        content.append("            System.out.println(\"⏱️ 认证耗时: \" + authTime + \"ms\");\n\n");
        content.append("        } catch (Exception e) {\n");
        content.append("            long authTime = System.currentTimeMillis() - startTime;\n");
        content.append("            System.err.println(\"❌ 认证失败: \" + e.getMessage());\n");
        content.append("            System.err.println(\"⏱️ 认证失败耗时: \" + authTime + \"ms\");\n");
        content.append("            // 使用mock token进行测试\n");
        content.append("            headers.put(\"Authorization\", \"Bearer mock_token_for_testing\");\n");
        content.append("        }\n");
        content.append("    }\n\n");
        return content;
    }
    
    /**
     * 生成AfterMethod方法
     */
    private StringBuilder generateAfterMethod() {
        StringBuilder content = new StringBuilder();
        content.append("    @AfterMethod\n");
        content.append("    public void afterMethod() {\n");
        content.append("        // 每个测试方法后清理\n");
        content.append("        cleanupTestData();\n");
        content.append("    }\n\n");
        return content;
    }
    
    /**
     * 生成AfterClass方法
     */
    private StringBuilder generateAfterClass() {
        StringBuilder content = new StringBuilder();
        content.append("    @AfterClass\n");
        content.append("    public void afterClass() {\n");
        content.append("        long startTime = System.currentTimeMillis();\n");
        content.append("        System.out.println(\"\\n=== 清理测试资源 - \" + new Date());\n\n");
        content.append("        // 清理上传的文件\n");
        content.append("        if (!cleanupIds.isEmpty()) {\n");
        content.append("            System.out.println(\"🗑️ 需要清理的资源数量: \" + cleanupIds.size());\n");
        content.append("            cleanupTestData();\n");
        content.append("        } else {\n");
        content.append("            System.out.println(\"✅ 无需清理资源\");\n");
        content.append("        }\n\n");
        content.append("        // 清理上下文\n");
        content.append("        contextParams.clear();\n");
        content.append("        long cleanupTime = System.currentTimeMillis() - startTime;\n");
        content.append("        System.out.println(\"✅ 清理完成，耗时: \" + cleanupTime + \"ms\");\n");
        content.append("    }\n\n");
        return content;
    }
    
    /**
     * 生成工作空间设置方法
     */
    private StringBuilder generateWorkspaceSetup() {
        StringBuilder content = new StringBuilder();
        content.append("    @Test(description = \"前置准备工作空间\")\n");
        content.append("    public void workspace_setup() {\n");
        content.append("        long startTime = System.currentTimeMillis();\n");
        content.append("        System.out.println(\"\\n🚀 开始执行前置准备工作空间 - \" + new Date());\n\n");
        content.append("        try {\n");
        content.append("            // 创建工作空间请求\n");
        content.append("            Map<String, Object> requestData = new HashMap<>();\n");
        content.append("            requestData.put(\"name\", \"测试工作空间\" + System.currentTimeMillis());\n");
        content.append("            requestData.put(\"description\", \"自动测试创建的工作空间\");\n\n");
        content.append("            // 打印请求信息\n");
        content.append("            System.out.println(\"📤 请求URL: \" + baseUrl + \"/api/v1/workspace\");\n");
        content.append("            System.out.println(\"📤 请求方法: POST\");\n");
        content.append("            System.out.println(\"📤 请求数据: \" + JSON.toJSONString(requestData, true));\n\n");
        content.append("            // 发送请求\n");
        content.append("            Response response = given()\n");
        content.append("                .headers(headers)\n");
        content.append("                .contentType(ContentType.JSON)\n");
        content.append("                .body(JSON.toJSONString(requestData))\n");
        content.append("                .when()\n");
        content.append("                .post(\"/api/v1/workspace\")\n");
        content.append("                .then()\n");
        content.append("                .extract()\n");
        content.append("                .response();\n\n");
        content.append("            // 获取工作空间ID\n");
        content.append("            String workspaceId = response.jsonPath().getString(\"data.id\");\n");
        content.append("            contextParams.put(\"workspaceId\", workspaceId);\n");
        content.append("            cleanupIds.add(workspaceId);\n\n");
        content.append("            System.out.println(\"✅ 工作空间创建成功，ID: \" + workspaceId);\n");
        content.append("        } catch (Exception e) {\n");
        content.append("            System.err.println(\"❌ 前置准备执行失败: \" + e.getMessage());\n");
        content.append("            throw new RuntimeException(\"前置准备执行失败\", e);\n");
        content.append("        }\n");
        content.append("    }\n\n");
        return content;
    }
    
    /**
     * 从YAML文件加载测试数据
     */
    private Map<String, Object> loadTestData(String testDataPath) {
        try {
            Yaml yaml = new Yaml();
            try (InputStream input = new FileInputStream(testDataPath)) {
                return yaml.load(input);
            }
        } catch (Exception e) {
            System.err.println("❌ 加载测试数据失败: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * 获取认证token
     */
    private String getAuthToken() {
        // 这里应该实现实际的认证逻辑
        // 例如：从配置文件读取token，或通过API获取
        return "mock_token_" + System.currentTimeMillis();
    }
    
    /**
     * 清理测试数据
     */
    private void cleanupTestData() {
        // 实现清理逻辑
        System.out.println("🧹 清理测试数据...");
    }
    
    /**
     * 写入测试类文件
     */
    private void writeTestClass(String className, StringBuilder content, String outputDir) {
        try {
            File dir = new File(outputDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            File file = new File(dir, className + ".java");
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.write(content.toString());
            }
            
            System.out.println("✅ 测试类已生成: " + file.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("❌ 生成测试类失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成BeforeClass方法
     */
    private StringBuilder generateBeforeClass() {
        StringBuilder content = new StringBuilder();
        content.append("    @BeforeClass\n");
        content.append("    public void beforeClass() {\n");
        content.append("        long startTime = System.currentTimeMillis();\n");
        content.append("        System.out.println(\"\\n=== Starting test suite: \" + getClass().getSimpleName() + \" - \" + new Date());\n\n");
        content.append("        // Initialize configuration from BaseTest\n");
        content.append("        environment = apitest.utils.ConfigManager.get(\"environment\");\n");
        content.append("        baseUrl = apitest.utils.ConfigManager.get(\"baseUrl\");\n");
        content.append("        RestAssured.baseURI = baseUrl;\n");
        content.append("        RestAssured.basePath = \"\";\n\n");
        content.append("        // Initialize test variables\n");
        content.append("        cleanupIds = new ArrayList<>();\n");
        content.append("        contextParams = new HashMap<>();\n");
        content.append("        testData = loadTestData();\n");
        content.append("        headers = new HashMap<>();\n");
        content.append("        headers.put(\"Content-Type\", \"application/json\");\n\n");
        content.append("        long initTime = System.currentTimeMillis() - startTime;\n");
        content.append("        System.out.println(\"✅ Test environment initialized in: \" + initTime + \"ms\");\n");
        content.append("        System.out.println(\"🌐 Base URL: \" + baseUrl);\n");
        content.append("        System.out.println(\"🔧 Environment: \" + environment);\n");
        content.append("    }\n\n");
        return content;
    }
    
    /**
     * 生成BeforeMethod方法
     */
    private StringBuilder generateBeforeMethod() {
        StringBuilder content = new StringBuilder();
        content.append("    @BeforeMethod\n");
        content.append("    public void beforeMethod() {\n");
        content.append("        long startTime = System.currentTimeMillis();\n");
        content.append("        System.out.println(\"\\n=== Getting authentication info - \" + new Date());\n\n");
        content.append("        try {\n");
        content.append("            // Get authentication token\n");
        content.append("            String token = getAuthToken();\n");
        content.append("            headers.put(\"Authorization\", \"Bearer \" + token);\n\n");
        content.append("            // Set default headers\n");
        content.append("            headers.put(\"Accept\", \"application/json\");\n");
        content.append("            headers.put(\"User-Agent\", \"API-Test-Client/1.0\");\n\n");
        content.append("            long authTime = System.currentTimeMillis() - startTime;\n");
        content.append("            System.out.println(\"✅ Authentication successful, token: \" + token.substring(0, 10) + \"...\");\n");
        content.append("            System.out.println(\"⏱️ Auth time: \" + authTime + \"ms\");\n\n");
        content.append("        } catch (Exception e) {\n");
        content.append("            long authTime = System.currentTimeMillis() - startTime;\n");
        content.append("            System.err.println(\"❌ Authentication failed: \" + e.getMessage());\n");
        content.append("            System.err.println(\"⏱️ Auth failed time: \" + authTime + \"ms\");\n");
        content.append("            // Use mock token for testing\n");
        content.append("            headers.put(\"Authorization\", \"Bearer mock_token_for_testing\");\n");
        content.append("        }\n");
        content.append("    }\n\n");
        return content;
    }
    
    /**
     * 生成AfterMethod方法
     */
    private StringBuilder generateAfterMethod() {
        StringBuilder content = new StringBuilder();
        content.append("    @AfterMethod\n");
        content.append("    public void afterMethod() {\n");
        content.append("        // Cleanup after each test method\n");
        content.append("        cleanupTestData();\n");
        content.append("    }\n\n");
        return content;
    }
    
    /**
     * 生成AfterClass方法
     */
    private StringBuilder generateAfterClass() {
        StringBuilder content = new StringBuilder();
        content.append("    @AfterClass\n");
        content.append("    public void afterClass() {\n");
        content.append("        long startTime = System.currentTimeMillis();\n");
        content.append("        System.out.println(\"\\n=== Cleaning up test resources - \" + new Date());\n\n");
        content.append("        // Cleanup uploaded files\n");
        content.append("        if (!cleanupIds.isEmpty()) {\n");
        content.append("            System.out.println(\"🗑️ Need to cleanup resources: \" + cleanupIds.size());\n");
        content.append("            cleanupTestData();\n");
        content.append("        } else {\n");
        content.append("            System.out.println(\"✅ No resources to cleanup\");\n");
        content.append("        }\n\n");
        content.append("        // Clear context\n");
        content.append("        contextParams.clear();\n");
        content.append("        long cleanupTime = System.currentTimeMillis() - startTime;\n");
        content.append("        System.out.println(\"✅ Cleanup completed in: \" + cleanupTime + \"ms\");\n");
        content.append("    }\n\n");
        return content;
    }
    
    /**
     * 生成工作空间设置方法
     */
    private StringBuilder generateWorkspaceSetup() {
        StringBuilder content = new StringBuilder();
        content.append("    @Test(description = \"前置准备工作空间\")\n");
        content.append("    public void workspace_setup() {\n");
        content.append("        long startTime = System.currentTimeMillis();\n");
        content.append("        System.out.println(\"\\n🚀 开始执行前置准备工作空间 - \" + new Date());\n\n");
        content.append("        try {\n");
        content.append("            // 创建工作空间请求\n");
        content.append("            Map<String, Object> requestData = new HashMap<>();\n");
        content.append("            requestData.put(\"name\", \"测试工作空间\" + System.currentTimeMillis());\n");
        content.append("            requestData.put(\"description\", \"自动测试创建的工作空间\");\n\n");
        content.append("            // 打印请求信息\n");
        content.append("            System.out.println(\"📤 请求URL: \" + baseUrl + \"/api/v1/workspace\");\n");
        content.append("            System.out.println(\"📤 请求方法: POST\");\n");
        content.append("            System.out.println(\"📤 请求数据: \" + JSON.toJSONString(requestData, true));\n\n");
        content.append("            // 发送请求\n");
        content.append("            Response response = given()\n");
        content.append("                .headers(headers)\n");
        content.append("                .contentType(ContentType.JSON)\n");
        content.append("                .body(JSON.toJSONString(requestData))\n");
        content.append("                .when()\n");
        content.append("                .post(\"/api/v1/workspace\")\n");
        content.append("                .then()\n");
        content.append("                .extract()\n");
        content.append("                .response();\n\n");
        content.append("            // 获取工作空间ID\n");
        content.append("            String workspaceId = response.jsonPath().getString(\"data.id\");\n");
        content.append("            contextParams.put(\"workspaceId\", workspaceId);\n");
        content.append("            cleanupIds.add(workspaceId);\n\n");
        content.append("            System.out.println(\"✅ 工作空间创建成功，ID: \" + workspaceId);\n");
        content.append("        } catch (Exception e) {\n");
        content.append("            System.err.println(\"❌ 前置准备执行失败: \" + e.getMessage());\n");
        content.append("            throw new RuntimeException(\"前置准备执行失败\", e);\n");
        content.append("        }\n");
        content.append("    }\n\n");
        return content;
    }
    
    /**
     * 写入测试类文件
     */
    private void writeTestClass(String outputDir, String packageName, String className, String content) {
        try {
            String packagePath = packageName.replace(".", "/");
            File outputFile = new File(outputDir + "/" + packagePath + "/" + className + ".java");
            
            // 确保目录存在
            outputFile.getParentFile().mkdirs();
            
            // 写入文件
            FileWriter writer = new FileWriter(outputFile);
            writer.write(content);
            writer.close();
            
            System.out.println("📄 Generated test class: " + outputFile.getAbsolutePath());
            
        } catch (Exception e) {
            System.err.println("❌ Failed to write test class: " + e.getMessage());
        }
    }
    
    /**
     * 生成默认配置文件
     */
    private void generateDefaultConfig(String configPath) {
        try {
            Map<String, Object> defaultConfig = new HashMap<>();
            
            // 全局配置
            defaultConfig.put("outputDir", "src/test/java");
            defaultConfig.put("packageName", "com.apitest.tests.generated");
            
            // 环境配置
            Map<String, Object> environment = new HashMap<>();
            environment.put("configPath", "src/test/resources/config/env.yml");
            environment.put("defaultEnv", "test");
            defaultConfig.put("environment", environment);
            
            // 测试场景配置
            Map<String, Object> testScenarios = new HashMap<>();
            
            // 文件上传模块配置
            Map<String, Object> fileUploadConfig = new HashMap<>();
            fileUploadConfig.put("description", "文件上传功能测试");
            fileUploadConfig.put("className", "FileUploadTest");
            
            // API配置
            Map<String, Object> apiConfig = new HashMap<>();
            apiConfig.put("path", "/sdk/storage/upload/v1");
            apiConfig.put("method", "POST");
            apiConfig.put("description", "上传文件到指定工作空间");
            fileUploadConfig.put("api", apiConfig);
            
            // 测试数据配置
            Map<String, Object> testData = new HashMap<>();
            testData.put("configPath", "src/test/resources/testdata/test/module_testdata/file_testdata.yml");
            fileUploadConfig.put("testData", testData);
            
            // 测试用例配置
            Map<String, Object> testCases = new HashMap<>();
            
            Map<String, Object> normalCase = new HashMap<>();
            normalCase.put("enabled", true);
            normalCase.put("description", "正常场景测试");
            testCases.put("normal", normalCase);
            
            Map<String, Object> boundaryCase = new HashMap<>();
            boundaryCase.put("enabled", true);
            boundaryCase.put("description", "边界值测试");
            boundaryCase.put("scenarios", Arrays.asList("empty_file", "large_file", "special_chars_filename"));
            testCases.put("boundary", boundaryCase);
            
            Map<String, Object> exceptionCase = new HashMap<>();
            exceptionCase.put("enabled", true);
            exceptionCase.put("description", "异常场景测试");
            exceptionCase.put("scenarios", Arrays.asList("invalid_file_type", "missing_required_param", "invalid_auth"));
            testCases.put("exception", exceptionCase);
            
            fileUploadConfig.put("testCases", testCases);
            testScenarios.put("file_upload", fileUploadConfig);
            
            defaultConfig.put("testScenarios", testScenarios);
            
            // 前置方法配置
            defaultConfig.put("beforeMethods", Arrays.asList(
                createMethodConfig("setupEnvironment", "测试环境初始化", "setupTestEnvironment"),
                createMethodConfig("getAuthToken", "获取认证Token", "com.apitest.core.TokenProvider.getToken")
            ));
            
            // 后置方法配置
            defaultConfig.put("afterMethods", Arrays.asList(
                createMethodConfig("cleanupTestData", "清理测试数据", "com.apitest.core.CleanData.cleanData"),
                createMethodConfig("cleanupFiles", "清理上传的文件", "cleanupUploadedFiles")
            ));
            
            // 断言配置
            Map<String, Object> assertions = new HashMap<>();
            Map<String, Object> successAssertion = new HashMap<>();
            successAssertion.put("statusCode", 200);
            successAssertion.put("responseTime", 5000);
            successAssertion.put("jsonPath", "$.code");
            successAssertion.put("expectedValue", "0");
            
            Map<String, Object> errorAssertion = new HashMap<>();
            errorAssertion.put("statusCode", Arrays.asList(400, 401, 403, 404, 415));
            errorAssertion.put("jsonPath", "$.message");
            errorAssertion.put("notEmpty", true);
            
            assertions.put("success", successAssertion);
            assertions.put("error", errorAssertion);
            defaultConfig.put("assertions", assertions);
            
            // 写入配置文件
            Yaml yaml = new Yaml();
            FileWriter writer = new FileWriter(configPath);
            yaml.dump(defaultConfig, writer);
            writer.close();
            
            System.out.println("📄 Generated default config file: " + configPath);
            
        } catch (Exception e) {
            System.err.println("❌ Failed to generate default config: " + e.getMessage());
        }
    }
    
    /**
     * 创建方法配置
     */
    private Map<String, Object> createMethodConfig(String name, String description, String method) {
        Map<String, Object> config = new HashMap<>();
        config.put("name", name);
        config.put("description", description);
        config.put("method", method);
        return config;
    }
    
    /**
     * 主方法
     */
    public static void main(String[] args) {
        System.out.println("🚀 Starting test case generator...");
        
        try {
            EnhancedTestCaseGenerator generator = new EnhancedTestCaseGenerator();
            
            // 检查是否有配置文件参数
            if (args.length > 0) {
                generator.generateFromConfigFile(args[0]);
            } else {
                // 使用默认方式生成
                generator.generateAllTests();
            }
            
            System.out.println("✅ Test case generation completed!");
            
        } catch (Exception e) {
            System.err.println("❌ Test case generation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}