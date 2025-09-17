package apitest.utils;

import java.util.List;
import java.util.Map;

/**
 * 模板引擎
 * 用于渲染测试用例模板
 */
public class TemplateEngine {
    
    /**
     * 渲染模板
     */
    public String renderTemplate(String templateName, Map<String, Object> data) {
        switch (templateName) {
            case "test-class-template":
                return renderTestClassTemplate(data);
            case "test-suite-template":
                return renderTestSuiteTemplate(data);
            default:
                throw new IllegalArgumentException("不支持的模板: " + templateName);
        }
    }
    
    /**
     * 渲染测试类模板
     */
    private String renderTestClassTemplate(Map<String, Object> data) {
        StringBuilder template = new StringBuilder();
        
        template.append("package ").append(data.get("packageName")).append(";\n\n");
        template.append("import org.testng.annotations.*;\n");
        template.append("import org.testng.Assert;\n");
        template.append("import io.restassured.RestAssured;\n");
        template.append("import io.restassured.http.ContentType;\n");
        template.append("import io.restassured.response.Response;\n\n");
        template.append("import static io.restassured.RestAssured.*;\n\n");
        
        template.append("/**\n");
        template.append(" * 自动生成的测试类\n");
        template.append(" * 场景: ").append(data.get("scenarioName")).append("\n");
        template.append(" */\n");
        template.append("public class ").append(data.get("className")).append(" {\n\n");
        
        // 添加全局变量
        template.append("    private String baseUrl;\n");
        template.append("    private String authToken;\n\n");
        
        // 添加@BeforeClass
        template.append("    @BeforeClass\n");
        template.append("    public void setUpClass() {\n");
        template.append("        baseUrl = \"").append(getBaseUrl(data)).append("\";\n");
        template.append("        RestAssured.baseURI = baseUrl;\n");
        template.append("    }\n\n");
        
        // 添加前置方法
        addBeforeMethods(template, data);
        
        // 添加测试方法
        addTestMethods(template, data);
        
        // 添加后置方法
        addAfterMethods(template, data);
        
        template.append("}\n");
        
        return template.toString();
    }
    
    /**
     * 渲染测试套件模板
     */
    private String renderTestSuiteTemplate(Map<String, Object> data) {
        StringBuilder template = new StringBuilder();
        
        template.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        template.append("<!DOCTYPE suite SYSTEM \"http://testng.org/testng-1.0.dtd\">\n");
        template.append("<suite name=\"").append(data.get("suiteName")).append("\" parallel=\"tests\" thread-count=\"3\">\n\n");
        
        List<String> testClasses = (List<String>) data.get("testClasses");
        for (String testClass : testClasses) {
            template.append("    <test name=\"").append(testClass).append("\">\n");
            template.append("        <classes>\n");
            template.append("            <class name=\"").append(testClass).append("\"/>\n");
            template.append("        </classes>\n");
            template.append("    </test>\n\n");
        }
        
        template.append("</suite>\n");
        
        return template.toString();
    }
    
    private void addBeforeMethods(StringBuilder template, Map<String, Object> data) {
        List<Map<String, Object>> beforeMethods = (List<Map<String, Object>>) data.get("beforeMethods");
        if (beforeMethods != null) {
            for (Map<String, Object> method : beforeMethods) {
                template.append("    @BeforeMethod\n");
                template.append("    public void ").append(method.get("name")).append("() {\n");
                template.append("        // ").append(method.get("description")).append("\n");
                
                // 处理依赖接口调用
                if (method.containsKey("setupApis")) {
                    List<Map<String, Object>> setupApis = (List<Map<String, Object>>) method.get("setupApis");
                    if (setupApis != null && !setupApis.isEmpty()) {
                        template.append("        // 前置依赖接口调用\n");
                        
                        for (Map<String, Object> apiInfo : setupApis) {
                            String module = (String) apiInfo.get("module");
                            String methodName = (String) apiInfo.get("method");
                            String path = (String) apiInfo.get("path");
                            
                            template.append("        // 调用").append(module).append("模块的").append(methodName).append("接口\n");
                            template.append("        Response setupResponse = given()\n");
                            template.append("            .contentType(ContentType.JSON)\n");
                            template.append("            .when()\n");
                            template.append("            .").append(methodName.toLowerCase()).append("(\"").append(path).append("\")\n");
                            template.append("            .then()\n");
                            template.append("            .statusCode(200)\n");
                            template.append("            .extract().response();\n\n");
                            
                            template.append("        // 提取依赖数据\n");
                            template.append("        // TODO: 根据实际需求提取响应数据\n");
                            template.append("        String dependencyData = setupResponse.jsonPath().getString(\"data.id\");\n\n");
                        }
                    }
                }
                
                template.append("    }\n\n");
            }
        }
    }
    
    private void addTestMethods(StringBuilder template, Map<String, Object> data) {
        List<Map<String, Object>> testMethods = (List<Map<String, Object>>) data.get("testMethods");
        if (testMethods != null) {
            for (Map<String, Object> method : testMethods) {
                Map<String, Object> api = (Map<String, Object>) method.get("api");
                Map<String, Object> testCase = (Map<String, Object>) method.get("testCase");
                
                template.append("    @Test(description = \"").append(testCase.get("description")).append("\")\n");
                template.append("    public void ").append(method.get("name")).append("() {\n");
                
                // 添加测试逻辑
                template.append("        // 准备测试数据\n");
                template.append("        String apiPath = \"").append(api.get("path")).append("\";\n");
                template.append("        String method = \"").append(api.get("method")).append("\";\n");
                
                template.append("        // 发送请求\n");
                template.append("        Response response = given()\n");
                template.append("            .contentType(ContentType.JSON)\n");
                template.append("            .when()\n");
                template.append("            .").append(((String)api.get("method")).toLowerCase()).append("(apiPath)\n");
                template.append("            .then()\n");
                
                Map<String, Object> expectedResult = (Map<String, Object>) testCase.get("expectedResult");
                template.append("            .statusCode(").append(expectedResult.get("statusCode")).append(")\n");
                template.append("            .extract().response();\n");
                
                template.append("        // 验证响应\n");
                template.append("        Assert.assertNotNull(response);\n");
                template.append("        // TODO: 添加更多断言\n");
                
                template.append("    }\n\n");
            }
        }
    }
    
    private void addAfterMethods(StringBuilder template, Map<String, Object> data) {
        List<Map<String, Object>> afterMethods = (List<Map<String, Object>>) data.get("afterMethods");
        if (afterMethods != null) {
            for (Map<String, Object> method : afterMethods) {
                template.append("    @AfterMethod\n");
                template.append("    public void ").append(method.get("name")).append("() {\n");
                template.append("        // ").append(method.get("description")).append("\n");
                template.append("        // TODO: 实现后置清理逻辑\n");
                template.append("    }\n\n");
            }
        }
    }
    
    private String getBaseUrl(Map<String, Object> data) {
        Map<String, String> globalParams = (Map<String, String>) data.get("globalParameters");
        return globalParams.getOrDefault("host", "http://localhost:8080");
    }
}