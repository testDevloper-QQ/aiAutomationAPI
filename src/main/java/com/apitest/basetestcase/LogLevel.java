package com.apitest.basetestcase;

import org.apache.logging.log4j.Level;

/**
 * 日志级别封装（不继承 Level，提供统一访问入口）
 */
public final class LogLevel {

    private LogLevel() {}

    public static final Level INFO = Level.INFO;
    public static final Level WARN = Level.WARN;
    public static final Level ERROR = Level.ERROR;
    public static final Level FATAL = Level.FATAL;
    public static final Level DEBUG = Level.DEBUG;
    public static final Level TRACE = Level.TRACE;

    public static Level of(String name) {
        if (name == null) return INFO;
        switch (name.toUpperCase()) {
            case "WARN":
            case "WARNING":
                return WARN;
            case "ERROR":
                return ERROR;
            case "FATAL":
                return FATAL;
            case "DEBUG":
                return DEBUG;
            case "TRACE":
                return TRACE;
            case "INFO":
            default:
                return INFO;
        }
    }
}