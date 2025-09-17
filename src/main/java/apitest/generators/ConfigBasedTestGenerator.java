package apitest.generators;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * 基于配置文件的测试用例生成器
 * 支持从YAML配置文件读取测试场景、接口依赖、测试数据等信息
 * 自动生成对应的测试用例类
 */
public class ConfigBasedTestGenerator {
    
    private Map<String, Object> config;
    private String configPath;
    
    public ConfigBasedTestGenerator(String configPath) {
        this.configPath = configPath;
        loadConfig();
    }
    
    /**
     * 加载配置文件
     */
    private void loadConfig() {
        try (InputStream input = new FileInputStream(configPath)) {
            Yaml yaml = new Yaml();
            config = yaml.load(input);
        } catch (Exception e) {
            throw new RuntimeException("加载配置文件失败: " + configPath, e);
        }
    }
    
    /**
     * 生成所有配置的测试用例
     */
    public void generateAllTests() {
        Map<String, Object> testScenarios = (Map<String, Object>) config.get("testScenarios");
        if (testScenarios == null) {
            System.out.println("未找到测试场景配置");
            return;
        }
        
        for (Map.Entry<String, Object> entry : testScenarios.entrySet()) {
            String scenarioName = entry.getKey();
            Map<String, Object> scenarioConfig = (Map<String, Object>) entry.getValue();
            generateTestClass(scenarioName, scenarioConfig);
        }
    }
    
    /**
     * 根据场景配置生成测试类
     */
    private void generateTestClass(String scenarioName, Map<String, Object> scenarioConfig) {
        String className = (String) scenarioConfig.getOrDefault("className", scenarioName + "Test");
        String packageName = (String) config.getOrDefault("packageName", "com.apitest.tests.generated");
        String outputDir = (String) config.getOrDefault("outputDir", "src/test/java");

        StringBuilder testClass = new StringBuilder();

        // 包声明
        testClass.append("package ").append(packageName).append(";\n\n");

        // 导入语句
        testClass.append(generateImports());

        // 类注释和声明
        testClass.append("/**\n")
                .append(" * 自动生成的测试类: ").append(scenarioName).append("\n")
                .append(" * 测试场景: ").append(scenarioConfig.get("description")).append("\n")
                .append(" */\n");

        testClass.append("public class ").append(className).append(" extends BaseTest {\n\n");

        // 生成成员变量
        testClass.append(generateFields(scenarioConfig));

        // 生成前置方法
        testClass.append(generateBeforeMethods(scenarioConfig));

        // 生成测试方法
        testClass.append(generateTestMethods(scenarioName, scenarioConfig));

        // 生成后置方法
        testClass.append(generateAfterMethods(scenarioConfig));

        // 工具方法
        testClass.append(generateUtilityMethods(scenarioConfig));

        testClass.append("}\n");

        // 写入文件
        writeTestClassToFile(testClass.toString(), outputDir, packageName, className);
    }
    
    /**
     * 生成导入语句
     */
    private String generateImports() {
        StringBuilder imports = new StringBuilder();
        imports.append("import org.testng.annotations.*;\n")
               .append("import io.restassured.RestAssured;\n")
               .append("import io.restassured.http.ContentType;\n")
               .append("import io.restassured.response.Response;\n")
               .append("import static io.restassured.RestAssured.*;\n")
               .append("import static org.hamcrest.Matchers.*;\n")
               .append("import com.alibaba.fastjson.JSONObject;\n")
               .append("import com.apitest.core.BaseTest;\n")
               .append("import com.apitest.utils.ConfigManager;\n")
               .append("import java.io.File;\n")
               .append("import java.util.*;\n\n");
        return imports.toString();
    }
    
    /**
     * 生成成员变量
     */
    private String generateFields(Map<String, Object> scenarioConfig) {
        StringBuilder fields = new StringBuilder();
        fields.append("    // 测试数据\n")
              .append("    private Map<String, Object> testData;\n")
              .append("    private String baseUrl;\n")
              .append("    private String token;\n")
              .append("    private String apiPath;\n")
              .append("    private List<String> uploadedFileIds;\n\n");
        return fields.toString();
    }
    
    /**
     * 生成前置方法
     */
    private String generateBeforeMethods(Map<String, Object> scenarioConfig) {
        StringBuilder beforeMethods = new StringBuilder();
        
        beforeMethods.append("    @BeforeMethod\n")
                     .append("    public void setUp() {\n")
                     .append("        // 初始化测试环境\n")
                     .append("        RestAssured.baseURI = baseUrl;\n")
                     .append("        token = getAuthToken();\n")
                     .append("        uploadedFileIds = new ArrayList<>();\n")
                     .append("        \n")
                     .append("        // 执行前置依赖接口\n")
                     .append("        executePreRequests();\n")
                     .append("    }\n\n");
        
        return beforeMethods.toString();
    }
    
    /**
     * 生成前置依赖方法
     */
    private String generateSetupMethod(Map<String, Object> dependency) {
        StringBuilder method = new StringBuilder();
        String module = (String) dependency.get("module");
        String methodType = (String) dependency.get("method");
        String path = (String) dependency.get("path");
        
        method.append("    @BeforeMethod\n")
              .append("    public void setup").append(module).append("() {\n")
              .append("        // 前置依赖: ").append(module).append("\n")
              .append("        Map<String, Object> params = new HashMap<>();\n")
              .append("        // TODO: 根据实际需求设置参数\n")
              .append("        Response response = RestAssured.given()\n")
              .append("            .contentType(ContentType.JSON)\n")
              .append("            .header(\"Authorization\", \"Bearer \" + token)\n")
              .append("            .body(params)\n")
              .append("            .when()\n")
              .append("            .get(\"").append(path).append("\");\n")
              .append("        // 处理响应数据\n")
              .append("    }\n\n");
        
        return method.toString();
    }
    
    /**
     * 生成测试方法
     */
    private String generateTestMethods(String scenarioName, Map<String, Object> scenarioConfig) {
        StringBuilder methods = new StringBuilder();
        
        // 获取测试用例配置
        Map<String, Object> testCases = (Map<String, Object>) scenarioConfig.getOrDefault("testCases", new HashMap<>());
        
        // 正常测试用例
        Map<String, Object> normalConfig = (Map<String, Object>) testCases.getOrDefault("normal", new HashMap<>());
        if ((Boolean) normalConfig.getOrDefault("enabled", true)) {
            methods.append("    @Test(description = \"正常流程测试\")\n");
            methods.append("    public void test").append(capitalize(scenarioName)).append("Normal() {\n");
            methods.append("        // 获取测试数据\n");
            methods.append("        Map<String, Object> testData = getTestData();\n\n");
            methods.append("        // 准备上传文件\n");
            methods.append("        File uploadFile = getTestFile();\n\n");
            methods.append("        // 发送文件上传请求\n");
            methods.append("        Response response = given()\n");
            methods.append("            .multiPart(\"file\", uploadFile, \"text/plain\")\n");
            methods.append("            .multiPart(\"filename\", testData.getOrDefault(\"filename\", \"test.txt\"))\n");
            methods.append("            .header(\"Authorization\", \"Bearer \" + token)\n");
            methods.append("            .when()\n");
            methods.append("            .post(apiPath);\n\n");
            methods.append("        // 提取文件ID用于后续清理\n");
            methods.append("        String fileId = response.jsonPath().getString(\"data.fileId\");\n");
            methods.append("        testData.put(\"fileId\", fileId);\n\n");
            methods.append("        // 断言\n");
            methods.append("        response.then()\n");
            methods.append("            .statusCode(200)\n");
            methods.append("            .body(\"code\", equalTo(\"0\"))\n");
            methods.append("            .body(\"data.filename\", equalTo(uploadFile.getName()));\n");
            methods.append("    }\n\n");
        }
        
        // 边界测试用例
        Map<String, Object> boundaryConfig = (Map<String, Object>) testCases.getOrDefault("boundary", new HashMap<>());
        if ((Boolean) boundaryConfig.getOrDefault("enabled", true)) {
            methods.append("    @Test(description = \"边界值测试\")\n");
            methods.append("    public void test").append(capitalize(scenarioName)).append("Boundary() {\n");
            methods.append("        // 测试空文件\n");
            methods.append("        Response response = given()\n");
            methods.append("            .multiPart(\"file\", new byte[0], \"empty.txt\")\n");
            methods.append("            .header(\"Authorization\", \"Bearer \" + token)\n");
            methods.append("            .when()\n");
            methods.append("            .post(apiPath);\n\n");
            methods.append("        // 断言空文件处理\n");
            methods.append("        response.then()\n");
            methods.append("            .statusCode(anyOf(equalTo(200), equalTo(400)));\n");
            methods.append("    }\n\n");
        }
        
        // 异常测试用例
        Map<String, Object> exceptionConfig = (Map<String, Object>) testCases.getOrDefault("exception", new HashMap<>());
        if ((Boolean) exceptionConfig.getOrDefault("enabled", true)) {
            methods.append("    @Test(description = \"异常场景测试\")\n");
            methods.append("    public void test").append(capitalize(scenarioName)).append("Exception() {\n");
            methods.append("        // 测试无文件上传\n");
            methods.append("        Response response = given()\n");
            methods.append("            .header(\"Authorization\", \"Bearer \" + token)\n");
            methods.append("            .when()\n");
            methods.append("            .post(apiPath);\n\n");
            methods.append("        // 断言异常处理\n");
            methods.append("        response.then()\n");
            methods.append("            .statusCode(400)\n");
            methods.append("            .body(\"message\", containsString(\"文件不能为空\"));\n");
            methods.append("    }\n\n");
        }
        
        // 添加执行前置依赖接口的方法
        methods.append("    /**")
             .append("     * 执行前置依赖接口")
             .append("     */")
             .append("    private void executePreRequest() {")
             .append("        // 根据配置执行前置依赖接口")
             .append("        System.out.println(\"执行前置依赖接口...\");")
             .append("    }\n\n")
             .append("    /**")
             .append("     * 清理测试数据")
             .append("     */")
             .append("    private void cleanupTestData() {")
             .append("        // 清理测试过程中产生的数据")
             .append("        System.out.println(\"清理测试数据...\");")
             .append("    }\n\n");
        
        return methods.toString();
    }
    
    /**
     * 生成边界值测试方法
     */
    private String generateBoundaryTestMethod(String scenarioName, Map<String, Object> scenarioConfig) {
        StringBuilder method = new StringBuilder();
        method.append("    @Test(description = \"边界值测试\")\n")
              .append("    public void test").append(capitalize(scenarioName)).append("Boundary() {\n")
              .append("        // 边界值测试数据\n")
              .append("        Map<String, Object> boundaryData = new HashMap<>();\n")
              .append("        boundaryData.put(\"file\", \"\"); // 空文件\n")
              .append("        \n")
              .append("        Response response = RestAssured.given()\n")
              .append("            .contentType(ContentType.MULTIPART)\n")
              .append("            .header(\"Authorization\", \"Bearer \" + token)\n")
              .append("            .multiPart(\"file\", \"\", \"\".getBytes())\n")
              .append("            .when()\n")
              .append("            .post(\"").append(scenarioConfig.get("paths")).append("\");\n")
              .append("        \n")
              .append("        response.then()\n")
              .append("            .statusCode(400);\n")
              .append("    }\n\n");
        return method.toString();
    }
    
    /**
     * 生成异常场景测试方法
     */
    private String generateExceptionTestMethod(String scenarioName, Map<String, Object> scenarioConfig) {
        StringBuilder method = new StringBuilder();
        method.append("    @Test(description = \"异常场景测试\")\n")
              .append("    public void test").append(capitalize(scenarioName)).append("Exception() {\n")
              .append("        // 异常测试数据\n")
              .append("        Map<String, Object> exceptionData = new HashMap<>();\n")
              .append("        exceptionData.put(\"file\", \"invalid_content\");\n")
              .append("        \n")
              .append("        Response response = RestAssured.given()\n")
              .append("            .contentType(ContentType.MULTIPART)\n")
              .append("            .header(\"Authorization\", \"Bearer \" + token)\n")
              .append("            .multiPart(\"file\", \"test.exe\", \"invalid\".getBytes())\n")
              .append("            .when()\n")
              .append("            .post(\"").append(scenarioConfig.get("paths")).append("\");\n")
              .append("        \n")
              .append("        response.then()\n")
              .append("            .statusCode(415);\n")
              .append("    }\n\n");
        return method.toString();
    }
    
    /**
     * 生成后置方法
     */
    private String generateAfterMethods(Map<String, Object> scenarioConfig) {
        StringBuilder afterMethods = new StringBuilder();
        
        afterMethods.append("    @AfterMethod\n")
                   .append("    public void tearDown() {\n")
                   .append("        // 清理测试数据\n")
                   .append("        cleanupTestData();\n")
                   .append("    }\n\n");
        
        // 添加后置清理方法
        List<Map<String, Object>> dependencyMapping = (List<Map<String, Object>>) scenarioConfig.get("dependencyMapping");
        if (dependencyMapping != null) {
            for (Map<String, Object> dependency : dependencyMapping) {
                String cleanupType = (String) dependency.get("type");
                if ("cleanup".equals(cleanupType)) {
                    afterMethods.append(generateCleanupMethod(dependency));
                }
            }
        }
        
        return afterMethods.toString();
    }
    
    /**
     * 生成后置清理方法
     */
    private String generateCleanupMethod(Map<String, Object> dependency) {
        StringBuilder method = new StringBuilder();
        String module = (String) dependency.get("module");
        String methodType = (String) dependency.get("method");
        String path = (String) dependency.get("path");
        
        method.append("    @AfterMethod\n")
              .append("    public void cleanup").append(module).append("() {\n")
              .append("        // 后置清理: ").append(module).append("\n")
              .append("        if (uploadedFileIds != null && !uploadedFileIds.isEmpty()) {\n")
              .append("            for (String fileId : uploadedFileIds) {\n")
              .append("                try {\n")
              .append("                    given()\n")
              .append("                        .contentType(ContentType.JSON)\n")
              .append("                        .header(\"Authorization\", \"Bearer \" + token)\n")
              .append("                        .when()\n")
              .append("                        .delete(\"").append(path).append("\" + fileId)\n")
              .append("                        .then()\n")
              .append("                        .statusCode(anyOf(equalTo(200), equalTo(204)));\n")
              .append("                } catch (Exception e) {\n")
              .append("                    System.err.println(\"清理文件失败: \" + fileId + \", \" + e.getMessage());\n")
              .append("                }\n")
              .append("            }\n")
              .append("            uploadedFileIds.clear();\n")
              .append("        }\n")
              .append("    }\n\n");
        
        return method.toString();
    }
    
    /**
     * 生成工具方法
     */
    private String generateUtilityMethods(Map<String, Object> scenarioConfig) {
        StringBuilder methods = new StringBuilder();
        
        methods.append("    /**\n")
             .append("     * 获取测试数据\n")
             .append("     */\n")
             .append("    private Map<String, Object> getTestData() {\n")
             .append("        Map<String, Object> data = new HashMap<>();\n")
             .append("        data.put(\"filename\", \"test.txt\");\n")
             .append("        data.put(\"fileType\", \"text/plain\");\n")
             .append("        data.put(\"description\", \"测试文件上传\");\n")
             .append("        return data;\n")
             .append("    }\n\n")
             .append("    /**\n")
             .append("     * 获取测试文件\n")
             .append("     */\n")
             .append("    private File getTestFile() {\n")
             .append("        return new File(\"src/test/resources/testdata/test/files/test.txt\");\n")
             .append("    }\n\n")
             .append("    /**\n")
             .append("     * 获取认证token\n")
             .append("     */\n")
             .append("    private String getAuthToken() {\n")
             .append("        // 从配置文件获取token\n")
             .append("        String username = ConfigManager.get(\"test_username\");\n")
             .append("        String password = ConfigManager.get(\"test_password\");\n")
             .append("        \n")
             .append("        Map<String, Object> loginData = new HashMap<>();\n")
             .append("        loginData.put(\"username\", username);\n")
             .append("        loginData.put(\"password\", password);\n")
             .append("        \n")
             .append("        Response response = given()\n")
             .append("            .contentType(ContentType.JSON)\n")
             .append("            .body(loginData)\n")
             .append("            .when()\n")
             .append("            .post(\"/api/auth/login\");\n")
             .append("        \n")
             .append("        return response.jsonPath().getString(\"data.token\");\n")
             .append("    }\n\n")
             .append("    /**\n")
             .append("     * 执行前置依赖接口\n")
             .append("     */\n")
             .append("    private void executePreRequests() {\n")
             .append("        try {\n")
             .append("            // 从配置文件读取前置依赖\n")
             .append("            Map<String, Object> preRequests = (Map<String, Object>) getTestData().get(\"preRequests\");\n")
             .append("            if (preRequests != null) {\n")
             .append("                for (Map.Entry<String, Object> entry : preRequests.entrySet()) {\n")
             .append("                    String requestName = entry.getKey();\n")
             .append("                    Map<String, Object> requestConfig = (Map<String, Object>) entry.getValue();\n")
             .append("                    executeSinglePreRequest(requestName, requestConfig);\n")
             .append("                }\n")
             .append("            }\n")
             .append("        } catch (Exception e) {\n")
             .append("            System.err.println(\"执行前置依赖接口失败: \" + e.getMessage());\n")
             .append("        }\n")
             .append("    }\n\n")
             .append("    /**\n")
             .append("     * 执行单个前置依赖接口\n")
             .append("     */\n")
             .append("    private void executeSinglePreRequest(String requestName, Map<String, Object> requestConfig) {\n")
             .append("        String method = (String) requestConfig.getOrDefault(\"method\", \"POST\");\n")
             .append("        String path = (String) requestConfig.get(\"path\");\n")
             .append("        Map<String, Object> parameters = (Map<String, Object>) requestConfig.get(\"parameters\");\n")
             .append("        \n")
             .append("        if (path == null) {\n")
             .append("            System.err.println(\"前置依赖接口路径未配置: \" + requestName);\n")
             .append("            return;\n")
             .append("        }\n")
             .append("        \n")
             .append("        // 构建请求\n")
             .append("        Response response = given()\n")
             .append("            .contentType(ContentType.JSON)\n")
             .append("            .header(\"Authorization\", \"Bearer \" + token)\n")
             .append("            .body(parameters != null ? parameters : new HashMap<>())\n")
             .append("            .when()\n")
             .append("            .request(method, path)\n")
             .append("            .then()\n")
             .append("            .statusCode(anyOf(equalTo(200), equalTo(201)))\n")
             .append("            .extract().response();\n")
             .append("        \n")
             .append("        // 提取响应数据供后续使用\n")
             .append("        String responseData = response.jsonPath().getString(\"data\");\n")
             .append("        System.out.println(\"前置依赖接口执行成功: \" + requestName + \", 响应: \" + responseData);\n")
             .append("    }\n\n")
             .append("    /**\n")
             .append("     * 清理测试数据\n")
             .append("     */\n")
             .append("    private void cleanupTestData() {\n")
             .append("        try {\n")
             .append("            // 清理上传的文件\n")
             .append("            if (uploadedFileIds != null && !uploadedFileIds.isEmpty()) {\n")
             .append("                for (String fileId : uploadedFileIds) {\n")
             .append("                    try {\n")
             .append("                        given()\n")
             .append("                            .contentType(ContentType.JSON)\n")
             .append("                            .header(\"Authorization\", \"Bearer \" + token)\n")
             .append("                            .when()\n")
             .append("                            .delete(apiPath + \"/\" + fileId)\n")
             .append("                            .then()\n")
             .append("                            .statusCode(anyOf(equalTo(200), equalTo(204)));\n")
             .append("                        System.out.println(\"清理文件成功: \" + fileId);\n")
             .append("                    } catch (Exception e) {\n")
             .append("                        System.err.println(\"清理文件失败: \" + fileId + \", \" + e.getMessage());\n")
             .append("                    }\n")
             .append("                }\n")
             .append("                uploadedFileIds.clear();\n")
             .append("            }\n")
             .append("            \n")
             .append("            // 执行配置的后置清理\n")
             .append("            executeCleanupRequests();\n")
             .append("            \n")
             .append("        } catch (Exception e) {\n")
             .append("            System.err.println(\"清理测试数据失败: \" + e.getMessage());\n")
             .append("        }\n")
             .append("    }\n\n")
             .append("    /**\n")
             .append("     * 执行后置清理接口\n")
             .append("     */\n")
             .append("    private void executeCleanupRequests() {\n")
             .append("        try {\n")
             .append("            // 从配置文件读取后置清理\n")
             .append("            Map<String, Object> cleanupRequests = (Map<String, Object>) getTestData().get(\"cleanupRequests\");\n")
             .append("            if (cleanupRequests != null) {\n")
             .append("                for (Map.Entry<String, Object> entry : cleanupRequests.entrySet()) {\n")
             .append("                    String requestName = entry.getKey();\n")
             .append("                    Map<String, Object> requestConfig = (Map<String, Object>) entry.getValue();\n")
             .append("                    executeSingleCleanupRequest(requestName, requestConfig);\n")
             .append("                }\n")
             .append("            }\n")
             .append("        } catch (Exception e) {\n")
             .append("            System.err.println(\"执行后置清理接口失败: \" + e.getMessage());\n")
             .append("        }\n")
             .append("    }\n\n")
             .append("    /**\n")
             .append("     * 执行单个后置清理接口\n")
             .append("     */\n")
             .append("    private void executeSingleCleanupRequest(String requestName, Map<String, Object> requestConfig) {\n")
             .append("        String method = (String) requestConfig.getOrDefault(\"method\", \"DELETE\");\n")
             .append("        String path = (String) requestConfig.get(\"path\");\n")
             .append("        Map<String, Object> parameters = (Map<String, Object>) requestConfig.get(\"parameters\");\n")
             .append("        \n")
             .append("        if (path == null) {\n")
             .append("            System.err.println(\"后置清理接口路径未配置: \" + requestName);\n")
             .append("            return;\n")
             .append("        }\n")
             .append("        \n")
             .append("        // 构建清理请求\n")
             .append("        Response response = given()\n")
             .append("            .contentType(ContentType.JSON)\n")
             .append("            .header(\"Authorization\", \"Bearer \" + token)\n")
             .append("            .body(parameters != null ? parameters : new HashMap<>())\n")
             .append("            .when()\n")
             .append("            .request(method, path)\n")
             .append("            .then()\n")
             .append("            .statusCode(anyOf(equalTo(200), equalTo(204)))\n")
             .append("            .extract().response();\n")
             .append("        \n")
             .append("        System.out.println(\"后置清理接口执行成功: \" + requestName);\n")
             .append("    }\n\n");
        
        return methods.toString();
    }
    
    /**
     * 将测试类写入文件
     */
    private void writeTestClassToFile(String content, String outputDir, String packageName, String className) {
        try {
            String packagePath = packageName.replace(".", "/");
            String fullPath = outputDir + "/" + packagePath;
            
            // 创建目录
            Files.createDirectories(Paths.get(fullPath));
            
            // 写入文件
            String filePath = fullPath + "/" + className + ".java";
            Files.write(Paths.get(filePath), content.getBytes());
            
            System.out.println("测试类已生成: " + filePath);
        } catch (Exception e) {
            throw new RuntimeException("写入测试类文件失败", e);
        }
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
     * 主方法，用于直接运行生成器
     */
    public static void main(String[] args) {
        String configPath = "src/test/resources/testdata/test/cases/component/file_cases.yml";
        ConfigBasedTestGenerator generator = new ConfigBasedTestGenerator(configPath);
        generator.generateAllTests();
    }
}