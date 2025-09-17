package com.apitest.utils;

import io.qameta.allure.Allure;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Allure测试监听器
 * 用于集成Allure报告
 */
public class AllureTestListener implements ITestListener {
    
    @Override
    public void onTestStart(ITestResult result) {
        Allure.step("开始测试: " + result.getMethod().getMethodName());
    }
    
    @Override
    public void onTestSuccess(ITestResult result) {
        Allure.step("测试通过: " + result.getMethod().getMethodName());
    }
    
    @Override
    public void onTestFailure(ITestResult result) {
        Allure.step("测试失败: " + result.getMethod().getMethodName());
        Allure.addAttachment("错误信息", result.getThrowable().getMessage());
    }
}