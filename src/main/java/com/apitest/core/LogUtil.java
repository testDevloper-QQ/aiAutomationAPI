package com.apitest.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 日志工具类
 */
public class LogUtil {
    public static Logger getLogger(Class<?> clazz) {
        return LogManager.getLogger(clazz);
    }
} 