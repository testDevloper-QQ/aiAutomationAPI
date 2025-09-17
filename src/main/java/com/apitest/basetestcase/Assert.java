package com.apitest.basetestcase;

import com.apitest.core.LogUtil;
import com.jayway.jsonpath.JsonPath;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 高级断言工具类（基于 TestNG）
 * 支持：
 * - 基础字段断言：code、message
 * - data 长度判断（字符串长度、数组长度、对象字段数量）
 * - 数组大小与元素值断言
 * - 值是否为空/非空
 * - 任意 JSONPath 路径的 size/长度判断
 * - 随机值长度判断
 */
public final class Assert {

    private static final Logger LOGGER = LogUtil.getLogger(Assert.class);

    private Assert() {}

    // ========== 基础字段断言 ==========

    public static void assertCodeEquals(String responseBody, int expectedCode) {
        Integer actual = readInt(responseBody, "$.code");
        LOGGER.info("断言 code: expected={}, actual={}", expectedCode, actual);
        org.testng.Assert.assertNotNull(actual, "响应中缺少字段: code");
        org.testng.Assert.assertEquals(actual.intValue(), expectedCode, "code 不匹配");
    }

    public static void assertMessageContains(String responseBody, String expectedContains) {
        String actual = readString(responseBody, "$.message");
        LOGGER.info("断言 message 包含: expected contains='{}', actual='{}'", expectedContains, actual);
        org.testng.Assert.assertNotNull(actual, "响应中缺少字段: message");
        org.testng.Assert.assertTrue(actual.contains(expectedContains), "message 不包含期望片段");
    }

    // ========== data 相关断言 ==========

    /**
     * 断言 $.data 的长度/大小：
     * - 字符串: 字符长度
     * - 数组: 元素数量
     * - 对象: 字段数量
     */
    public static void assertDataLengthEquals(String responseBody, int expectedLength) {
        Object data = readObject(responseBody, "$.data");
        int actual = sizeOf(data);
        LOGGER.info("断言 data 长度: expected={}, actual={}", expectedLength, actual);
        org.testng.Assert.assertEquals(actual, expectedLength, "data 长度不匹配");
    }

    /**
     * 断言 $.data 是数组且大小为 expectedSize
     */
    public static void assertDataArraySize(String responseBody, int expectedSize) {
        List<?> list = readList(responseBody, "$.data");
        int actual = list == null ? -1 : list.size();
        LOGGER.info("断言 data 数组大小: expected={}, actual={}", expectedSize, actual);
        org.testng.Assert.assertNotNull(list, "data 不是数组或缺失");
        org.testng.Assert.assertEquals(actual, expectedSize, "data 数组大小不匹配");
    }

    /**
     * 断言 $.data[index] 的值（或其内部 JSONPath）
     * @param innerJsonPath 元素内部路径，如 "name" 或 "meta.id"；为空则直接比较元素本身
     */
    public static void assertDataArrayElementEquals(String responseBody, int index, String innerJsonPath, Object expectedValue) {
        List<?> list = readList(responseBody, "$.data");
        org.testng.Assert.assertNotNull(list, "data 不是数组或缺失");
        org.testng.Assert.assertTrue(index >= 0 && index < list.size(), "索引越界: " + index);
        Object element = list.get(index);
        Object actual;
        if (innerJsonPath == null || innerJsonPath.trim().isEmpty()) {
            actual = element;
        } else {
            actual = JsonPath.read(element, toJsonPath(innerJsonPath));
        }
        LOGGER.info("断言 data[{}] 值: expected={}, actual={}", index, expectedValue, actual);
        org.testng.Assert.assertEquals(String.valueOf(actual), String.valueOf(expectedValue), "数组元素值不匹配");
    }

    // ========== 通用 JSONPath 断言 ==========

    public static void assertNotNullAtPath(String responseBody, String jsonPath) {
        Object actual = readObject(responseBody, jsonPath);
        LOGGER.info("断言 非空 at {}: actual={}", jsonPath, actual);
        org.testng.Assert.assertNotNull(actual, "路径 " + jsonPath + " 的值应为非空");
    }

    public static void assertNullAtPath(String responseBody, String jsonPath) {
        Object actual = readObject(responseBody, jsonPath);
        LOGGER.info("断言 为空 at {}: actual={}", jsonPath, actual);
        org.testng.Assert.assertNull(actual, "路径 " + jsonPath + " 的值应为空");
    }

    public static void assertContainsAt(String responseBody, String jsonPath, String expectedSubstring) {
        String actual = String.valueOf(readObject(responseBody, jsonPath));
        LOGGER.info("断言 包含 at {}: expected contains='{}', actual='{}'", jsonPath, expectedSubstring, actual);
        org.testng.Assert.assertTrue(actual.contains(expectedSubstring), "路径 " + jsonPath + " 的值不包含期望片段");
    }

    /**
     * 断言任意路径的 size（数组长度/对象字段数/字符串长度）
     */
    public static void assertSizeAt(String responseBody, String jsonPath, int expectedSize) {
        Object obj = readObject(responseBody, jsonPath);
        int actual = sizeOf(obj);
        LOGGER.info("断言 size at {}: expected={}, actual={}", jsonPath, expectedSize, actual);
        org.testng.Assert.assertEquals(actual, expectedSize, "路径 " + jsonPath + " 的 size 不匹配");
    }

    /**
     * 断言任意路径的字符串长度
     */
    public static void assertFieldLength(String responseBody, String jsonPath, int expectedLength) {
        Object obj = readObject(responseBody, jsonPath);
        String str = obj == null ? null : String.valueOf(obj);
        int actual = str == null ? -1 : str.length();
        LOGGER.info("断言 字符串长度 at {}: expected={}, actual={}", jsonPath, expectedLength, actual);
        org.testng.Assert.assertNotNull(str, "路径 " + jsonPath + " 的值为空");
        org.testng.Assert.assertEquals(actual, expectedLength, "路径 " + jsonPath + " 的字符串长度不匹配");
    }

    // ========== 随机值断言 ==========

    public static void assertRandomStringLength(String randomString, int expectedLength) {
        int actual = randomString == null ? -1 : randomString.length();
        LOGGER.info("断言 随机字符串长度: expected={}, actual={}", expectedLength, actual);
        org.testng.Assert.assertNotNull(randomString, "随机字符串为空");
        org.testng.Assert.assertEquals(actual, expectedLength, "随机字符串长度不匹配");
    }

    // ========== 私有帮助方法 ==========

    private static String toJsonPath(String path) {
        if (path == null || path.isEmpty()) return "$";
        if (path.startsWith("$")) return path;
        return "$." + path;
    }

    private static Object readObject(String body, String path) {
        return JsonPath.read(body, toJsonPath(path));
    }

    private static String readString(String body, String path) {
        Object obj = readObject(body, path);
        return obj == null ? null : String.valueOf(obj);
    }

    private static Integer readInt(String body, String path) {
        Object obj = readObject(body, path);
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).intValue();
        return Integer.parseInt(String.valueOf(obj));
    }

    private static List<?> readList(String body, String path) {
        Object obj = readObject(body, path);
        if (obj instanceof List) return (List<?>) obj;
        return null;
    }

    private static int sizeOf(Object obj) {
        if (obj == null) return -1;
        if (obj instanceof CharSequence) return ((CharSequence) obj).length();
        if (obj instanceof Collection) return ((Collection<?>) obj).size();
        if (obj instanceof Map) return ((Map<?, ?>) obj).size();
        // 对于数组元素支持：data[*] 由 JsonPath 层面控制
        return String.valueOf(obj).length();
    }
}