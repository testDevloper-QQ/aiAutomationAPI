package com.apitest.core;

/**
 * API断言工具类
 */
public class ApiAssert {
    /**
     * 简单响应断言，后续可扩展为JsonPath等
     */
    public static void assertResponse(String response, String expected) {
        AssertUtil.assertTrue(response.contains(expected), "响应校验失败，期望包含: " + expected);
    }
} 