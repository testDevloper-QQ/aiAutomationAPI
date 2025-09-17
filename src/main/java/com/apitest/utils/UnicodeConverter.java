package com.apitest.utils;

/**
 * Unicode转换工具类
 * 用于将中文字符串转换为Unicode转义序列，避免编码问题
 */
public class UnicodeConverter {
    
    /**
     * 将字符串中的中文字符转换为Unicode转义序列
     * @param input 输入字符串
     * @return 转换后的字符串
     */
    public static String toUnicode(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (isChineseChar(c)) {
                result.append("\\u").append(String.format("%04X", (int) c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
    
    /**
     * 检查字符是否为中文
     * @param c 字符
     * @return 是否为中文
     */
    private static boolean isChineseChar(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION;
    }
    
    /**
     * 转义字符串中的特殊字符，用于Java字符串
     * @param input 输入字符串
     * @return 转义后的字符串
     */
    public static String escapeJavaString(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '\n':
                    result.append("\\n");
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                case '"':
                    result.append("\\\"");
                    break;
                case '\\':
                    result.append("\\\\");
                    break;
                default:
                    if (isChineseChar(c)) {
                        result.append("\\u").append(String.format("%04X", (int) c));
                    } else if (c < 32 || c > 126) {
                        result.append("\\u").append(String.format("%04X", (int) c));
                    } else {
                        result.append(c);
                    }
                    break;
            }
        }
        return result.toString();
    }
}