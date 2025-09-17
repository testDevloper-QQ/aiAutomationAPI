package com.apitest.core;

import org.testng.Assert;

/**
 * 断言工具类，统一断言入口
 */
public class AssertUtil {
    public static void assertEquals(Object actual, Object expected, String message) {
        Assert.assertEquals(actual, expected, message);
    }
    public static void assertTrue(boolean condition, String message) {
        Assert.assertTrue(condition, message);
    }
    // 可扩展更多断言方法
} 