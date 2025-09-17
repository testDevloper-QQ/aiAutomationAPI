package apitest.core;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import apitest.utils.LoggerUtil;
import apitest.utils.ConfigManager;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据清理类
 * 提供统一的数据清理功能
 * 
 * @author 自动化测试团队
 * @version 1.0
 */
public class CleanData {
    
    private static final Map<String, List<String>> cleanupRegistry = new ConcurrentHashMap<>();
    private static final ThreadLocal<Map<String, List<String>>> threadCleanup = new ThreadLocal<>();
    
    /**
     * 注册清理任务
     */
    public static void registerCleanup(String type, String identifier) {
        Map<String, List<String>> cleanupMap = getThreadCleanupMap();
        cleanupMap.computeIfAbsent(type, k -> new ArrayList<>()).add(identifier);
    }
    
    /**
     * 获取线程清理映射
     */
    private static Map<String, List<String>> getThreadCleanupMap() {
        Map<String, List<String>> map = threadCleanup.get();
        if (map == null) {
            map = new HashMap<>();
            threadCleanup.set(map);
        }
        return map;
    }
    
    /**
     * 清理所有注册的资源
     */
    public static void cleanupAll() {
        LoggerUtil.info("🧹 开始执行全局清理任务");
        
        // 清理文件
        cleanupFiles();
        
        // 清理API资源
        cleanupApiResources();
        
        // 清理测试数据
        cleanupTestData();
        
        // 清理临时目录
        cleanupTempDirectories();
        
        LoggerUtil.info("✅ 全局清理任务完成");
    }
    
    /**
     * 清理文件
     */
    private static void cleanupFiles() {
        List<String> fileIds = getThreadCleanupMap().get("file");
        if (fileIds != null && !fileIds.isEmpty()) {
            LoggerUtil.info("🗑️ 开始清理测试文件");
            
            for (String fileId : fileIds) {
                try {
                    deleteFile(fileId);
                } catch (Exception e) {
                    LoggerUtil.error("清理文件失败: " + fileId, e);
                }
            }
        }
    }
    
    /**
     * 清理API资源
     */
    private static void cleanupApiResources() {
        List<String> apiIds = getThreadCleanupMap().get("api");
        if (apiIds != null && !apiIds.isEmpty()) {
            LoggerUtil.info("🔗 开始清理API资源");
            
            for (String apiId : apiIds) {
                try {
                    deleteApiResource(apiId);
                } catch (Exception e) {
                    LoggerUtil.error("清理API资源失败: " + apiId, e);
                }
            }
        }
    }
    
    /**
     * 清理测试数据
     */
    private static void cleanupTestData() {
        List<String> dataIds = getThreadCleanupMap().get("data");
        if (dataIds != null && !dataIds.isEmpty()) {
            LoggerUtil.info("📊 开始清理测试数据");
            
            for (String dataId : dataIds) {
                try {
                    deleteTestData(dataId);
                } catch (Exception e) {
                    LoggerUtil.error("清理测试数据失败: " + dataId, e);
                }
            }
        }
    }
    
    /**
     * 清理临时目录
     */
    private static void cleanupTempDirectories() {
        String tempDir = ConfigManager.get("temp.directory", "target/temp");
        File directory = new File(tempDir);
        if (directory.exists()) {
            deleteDirectory(directory);
            LoggerUtil.info("📁 清理临时目录: " + tempDir);
        }
    }
    
    /**
     * 删除文件
     */
    public static void deleteFile(String fileId) {
        String endpoint = "/api/files/" + fileId;
        
        try {
            Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .when()
                .delete(endpoint)
                .then()
                .extract()
                .response();
            
            if (response.statusCode() == 200 || response.statusCode() == 204) {
                LoggerUtil.info("✅ 文件删除成功: " + fileId);
            } else {
                LoggerUtil.warn("文件删除失败: " + fileId + " - 状态码: " + response.statusCode());
            }
        } catch (Exception e) {
            LoggerUtil.error("删除文件异常: " + fileId, e);
        }
    }
    
    /**
     * 删除API资源
     */
    public static void deleteApiResource(String resourceId) {
        String endpoint = "/api/resources/" + resourceId;
        
        try {
            Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .when()
                .delete(endpoint)
                .then()
                .extract()
                .response();
            
            if (response.statusCode() == 200 || response.statusCode() == 204) {
                LoggerUtil.info("✅ API资源删除成功: " + resourceId);
            } else {
                LoggerUtil.warn("API资源删除失败: " + resourceId + " - 状态码: " + response.statusCode());
            }
        } catch (Exception e) {
            LoggerUtil.error("删除API资源异常: " + resourceId, e);
        }
    }
    
    /**
     * 删除测试数据
     */
    public static void deleteTestData(String dataId) {
        String endpoint = "/api/test-data/" + dataId;
        
        try {
            Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .when()
                .delete(endpoint)
                .then()
                .extract()
                .response();
            
            if (response.statusCode() == 200 || response.statusCode() == 204) {
                LoggerUtil.info("✅ 测试数据删除成功: " + dataId);
            } else {
                LoggerUtil.warn("测试数据删除失败: " + dataId + " - 状态码: " + response.statusCode());
            }
        } catch (Exception e) {
            LoggerUtil.error("删除测试数据异常: " + dataId, e);
        }
    }
    
    /**
     * 删除目录
     */
    public static void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
    
    /**
     * 清理线程资源
     */
    public static void cleanupThreadResources() {
        Map<String, List<String>> cleanupMap = threadCleanup.get();
        if (cleanupMap != null && !cleanupMap.isEmpty()) {
            LoggerUtil.info("🧵 清理线程资源");
            cleanupAll();
            cleanupMap.clear();
        }
        threadCleanup.remove();
    }
    
    /**
     * 批量注册清理任务
     */
    public static void registerBatchCleanup(String type, List<String> identifiers) {
        for (String identifier : identifiers) {
            registerCleanup(type, identifier);
        }
    }
    
    /**
     * 获取清理统计信息
     */
    public static Map<String, Integer> getCleanupStats() {
        Map<String, List<String>> cleanupMap = getThreadCleanupMap();
        Map<String, Integer> stats = new HashMap<>();
        
        for (Map.Entry<String, List<String>> entry : cleanupMap.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().size());
        }
        
        return stats;
    }
}