package apitest.utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

/**
 * 日志工具类
 * 提供统一的日志管理功能
 * 
 * @author 自动化测试团队
 * @version 1.0
 */
public class LoggerUtil {
    
    private static final Logger logger = Logger.getLogger(LoggerUtil.class.getName());
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    
    static {
        try {
            // 创建日志目录
            File logDir = new File("logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            // 设置日志级别
            logger.setLevel(Level.INFO);
            
            // 创建文件处理器
            FileHandler fileHandler = new FileHandler("logs/test.log", 10 * 1024 * 1024, 5, true);
            fileHandler.setFormatter(new SimpleFormatter() {
                @Override
                public String format(LogRecord record) {
                    return String.format("[%s] [%s] %s: %s%n",
                        dateFormat.format(new Date(record.getMillis())),
                        record.getLevel(),
                        record.getLoggerName(),
                        record.getMessage()
                    );
                }
            });
            
            // 创建控制台处理器
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new SimpleFormatter() {
                @Override
                public String format(LogRecord record) {
                    String color = "";
                    String reset = "";
                    
                    switch (record.getLevel().getName()) {
                        case "INFO":
                            color = "\u001B[32m"; // 绿色
                            break;
                        case "WARNING":
                            color = "\u001B[33m"; // 黄色
                            break;
                        case "SEVERE":
                            color = "\u001B[31m"; // 红色
                            break;
                        default:
                            color = "\u001B[0m"; // 默认
                    }
                    
                    return String.format("%s[%s] %s%s%n",
                        color,
                        record.getLevel(),
                        record.getMessage(),
                        reset
                    );
                }
            });
            
            // 清除默认处理器
            logger.setUseParentHandlers(false);
            
            // 添加处理器
            logger.addHandler(fileHandler);
            logger.addHandler(consoleHandler);
            
        } catch (Exception e) {
            System.err.println("初始化日志系统失败: " + e.getMessage());
        }
    }
    
    /**
     * 记录信息日志
     */
    public static void info(String message) {
        logger.info(message);
    }
    
    /**
     * 记录警告日志
     */
    public static void warn(String message) {
        logger.warning(message);
    }
    
    /**
     * 记录错误日志
     */
    public static void error(String message) {
        logger.severe(message);
    }
    
    /**
     * 记录错误日志（带异常）
     */
    public static void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }
    
    /**
     * 记录调试日志
     */
    public static void debug(String message) {
        logger.fine(message);
    }
    
    /**
     * 记录测试步骤
     */
    public static void step(String stepName, String description) {
        info(String.format("📝 步骤: %s - %s", stepName, description));
    }
    
    /**
     * 记录测试通过
     */
    public static void pass(String testName) {
        info(String.format("✅ 测试通过: %s", testName));
    }
    
    /**
     * 记录测试失败
     */
    public static void fail(String testName, String reason) {
        error(String.format("❌ 测试失败: %s - 原因: %s", testName, reason));
    }
    
    /**
     * 记录API调用
     */
    public static void apiCall(String method, String endpoint, int statusCode, long responseTime) {
        info(String.format("🔗 API调用: %s %s - 状态码: %d - 响应时间: %dms", 
            method, endpoint, statusCode, responseTime));
    }
}