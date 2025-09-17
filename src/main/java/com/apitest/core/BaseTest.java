package com.apitest.core;

import io.restassured.RestAssured;
import org.testng.annotations.BeforeClass;

/**
 * 基础测试类
 * 提供通用的测试配置和初始化
 */
public class BaseTest {
    
    @BeforeClass
    public void setUpBase() {
        // 基础配置
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
}