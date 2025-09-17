package apitest.core;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import apitest.utils.ConfigManager;
import apitest.utils.ExcelDataProvider;
import apitest.utils.LoggerUtil;

import java.util.*;

/**
 * 基础测试类
 * 提供公共的测试方法和配置
 * 
 * @author 自动化测试团队
 * @version 1.0
 */
public class BaseTest {
    
    protected static final ThreadLocal<Map<String, Object>> testContext = new ThreadLocal<>();
    protected static final ThreadLocal<String> currentTestCase = new ThreadLocal<>();
    
    /**
     * 测试套件初始化
     */
    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() {
        LoggerUtil.info("🚀 测试套件开始执行");
        
        // 初始化配置
        ConfigManager.loadEnvironmentConfig();
        
        // 设置RestAssured配置
        RestAssured.baseURI = ConfigManager.get("baseUrl");
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        
        LoggerUtil.info("✅ 测试环境初始化完成");
    }
    
    /**
     * 测试套件清理
     */
    @AfterSuite(alwaysRun = true)
    public void afterSuite() {
        LoggerUtil.info("🧹 测试套件执行完成");
        
        // 清理全局资源
        CleanData.cleanupAll();
        
        LoggerUtil.info("✅ 测试套件清理完成");
    }
    
    /**
     * 数据提供者
     */
    @DataProvider(name = "excelData")
    public Object[][] excelDataProvider() {
        return ExcelDataProvider.getTestData();
    }
    
    /**
     * 获取测试上下文
     */
    protected Map<String, Object> getTestContext() {
        Map<String, Object> context = testContext.get();
        if (context == null) {
            context = new HashMap<>();
            testContext.set(context);
        }
        return context;
    }
    
    /**
     * 设置当前测试用例
     */
    protected void setCurrentTestCase(String testCase) {
        currentTestCase.set(testCase);
        getTestContext().put("testCase", testCase);
    }
    
    /**
     * 获取当前测试用例
     */
    protected String getCurrentTestCase() {
        return currentTestCase.get();
    }
    
    /**
     * 通用API调用方法
     */
    protected Response callApi(String method, String endpoint, Map<String, Object> params, Map<String, String> headers) {
        return given()
            .contentType(ContentType.JSON)
            .headers(headers)
            .body(JSON.toJSONString(params))
            .when()
            .request(method, endpoint)
            .then()
            .extract()
            .response();
    }
    
    /**
     * 文件上传方法
     */
    protected Response uploadFile(String endpoint, File file, Map<String, Object> params, Map<String, String> headers) {
        return given()
            .headers(headers)
            .multiPart("file", file)
            .formParams(params)
            .when()
            .post(endpoint)
            .then()
            .extract()
            .response();
    }
    
    /**
     * 验证响应
     */
    protected void validateResponse(Response response, Map<String, Object> expected) {
        // 状态码验证
        Integer statusCode = (Integer) expected.get("statusCode");
        if (statusCode != null) {
            response.then().statusCode(statusCode);
        }
        
        // 响应时间验证
        Long maxResponseTime = (Long) expected.get("responseTime");
        if (maxResponseTime != null) {
            long actualTime = response.time();
            assert actualTime <= maxResponseTime : "响应时间超过限制: " + actualTime + "ms";
        }
        
        // 字段验证
        Map<String, Object> fields = (Map<String, Object>) expected.get("fields");
        if (fields != null) {
            for (Map.Entry<String, Object> entry : fields.entrySet()) {
                response.then().body(entry.getKey(), equalTo(entry.getValue()));
            }
        }
    }
}