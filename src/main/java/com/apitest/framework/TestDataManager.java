package com.apitest.framework;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试数据管理器
 * 负责读取和管理测试数据，支持环境配置和测试数据分离
 */
public class TestDataManager {
    
    private static final Logger logger = LoggerFactory.getLogger(TestDataManager.class);
    
    private static final String ENVIRONMENTS_FILE = "apiconfig/environments.yaml";
    private static final String TESTDATA_DIR = "src/test/resources/testdata";
    
    private static final Map<String, Object> configCache = new HashMap<>();
    private static final Map<String, Map<String, Object>> testDataCache = new HashMap<>();
    
    private static String currentEnv;
    private static Map<String, Object> environmentConfig;
    private static boolean initialized = false;
    
    /**
     * 初始化测试数据管理器
     */
    public static void init(String environment) {
        if (environment == null || environment.trim().isEmpty()) {
            throw new IllegalArgumentException("Environment parameter cannot be null or empty");
        }
        
        currentEnv = environment.trim();
        
        try {
            logger.info("Initializing TestDataManager with environment: {}", currentEnv);
            loadEnvironmentConfig(currentEnv);
            initialized = true;
            logger.info("TestDataManager initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize TestDataManager", e);
            throw new RuntimeException("Failed to initialize TestDataManager", e);
        }
    }

    /**
     * 加载环境配置
     */
    private static void loadEnvironmentConfig(String environment) {
        try {
            Yaml yaml = new Yaml();
            InputStream inputStream = TestDataManager.class.getClassLoader()
                    .getResourceAsStream(ENVIRONMENTS_FILE);
            
            if (inputStream == null) {
                throw new RuntimeException("Environment config file not found: " + ENVIRONMENTS_FILE);
            }
            
            Map<String, Object> yamlData = yaml.load(inputStream);
            
            if (yamlData == null || !yamlData.containsKey("environments")) {
                throw new RuntimeException("Invalid YAML structure: missing 'environments' key");
            }
            
            Map<String, Object> environments = (Map<String, Object>) yamlData.get("environments");
            if (!environments.containsKey(environment)) {
                throw new RuntimeException("Environment '" + environment + "' not found");
            }
            
            environmentConfig = (Map<String, Object>) environments.get(environment);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to load environment config", e);
        }
    }
    
    /**
     * 获取环境配置
     */
    public static Map<String, Object> getEnvironmentConfig(String env) {
        if (env == null || env.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        String cacheKey = "env_" + env.trim();
        if (configCache.containsKey(cacheKey)) {
            return (Map<String, Object>) configCache.get(cacheKey);
        }
        
        try {
            Yaml yaml = new Yaml();
            InputStream inputStream = TestDataManager.class.getClassLoader()
                    .getResourceAsStream(ENVIRONMENTS_FILE);
            
            if (inputStream != null) {
                Map<String, Object> yamlData = yaml.load(inputStream);
                if (yamlData != null && yamlData.containsKey("environments")) {
                    Map<String, Object> environments = (Map<String, Object>) yamlData.get("environments");
                    if (environments.containsKey(env.trim())) {
                        Map<String, Object> envConfig = (Map<String, Object>) environments.get(env.trim());
                        configCache.put(cacheKey, envConfig);
                        return envConfig;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("读取环境配置失败: {}", e.getMessage());
        }
        
        return new HashMap<>();
    }
    
    /**
     * 获取全局参数
     */
    public static Map<String, Object> getGlobalParameters() {
        Map<String, Object> globalParams = new HashMap<>();
        
        try {
            Yaml yaml = new Yaml();
            InputStream inputStream = TestDataManager.class.getClassLoader()
                    .getResourceAsStream(ENVIRONMENTS_FILE);
            
            if (inputStream != null) {
                Map<String, Object> yamlData = yaml.load(inputStream);
                if (yamlData != null && yamlData.containsKey("parameters")) {
                    Object params = yamlData.get("parameters");
                    if (params instanceof Map) {
                        globalParams.putAll((Map<String, Object>) params);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("读取全局参数失败: {}", e.getMessage());
        }
        
        return globalParams;
    }
    
    /**
     * 获取指定环境的完整URL
     */
    public static String getBaseUrl(String env) {
        Map<String, Object> envConfig = getEnvironmentConfig(env);
        if (envConfig == null || envConfig.isEmpty()) {
            return "https://gccs.glodon.com";
        }
        return envConfig.getOrDefault("gccs_host", 
               envConfig.getOrDefault("baseUrl", "https://gccs.glodon.com")).toString();
    }
    
    /**
     * 获取测试数据（双参数版本）
     */
    public static Map<String, Object> getTestData(String module, String testCase) {
        if (module == null || module.trim().isEmpty() || testCase == null || testCase.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        String cacheKey = module.trim() + "_" + testCase.trim();
        if (testDataCache.containsKey(cacheKey)) {
            return testDataCache.get(cacheKey);
        }
        
        try {
            String resourcePath = "/testdata/" + module.trim() + "/" + testCase.trim() + ".json";
            InputStream inputStream = TestDataManager.class.getResourceAsStream(resourcePath);
            
            if (inputStream != null) {
                byte[] buffer = new byte[inputStream.available()];
                inputStream.read(buffer);
                String jsonContent = new String(buffer);
                JSONObject testData = JSON.parseObject(jsonContent);
                if (testData != null) {
                    Map<String, Object> dataMap = new HashMap<>(testData);
                    testDataCache.put(cacheKey, dataMap);
                    return dataMap;
                }
            } else {
                // 尝试文件系统方式
                String filePath = TESTDATA_DIR + "/" + module.trim() + "/" + testCase.trim() + ".json";
                Path filePathObj = Paths.get(filePath);
                
                if (Files.exists(filePathObj)) {
                    String jsonContent = new String(Files.readAllBytes(filePathObj));
                    JSONObject testData = JSON.parseObject(jsonContent);
                    if (testData != null) {
                        Map<String, Object> dataMap = new HashMap<>(testData);
                        testDataCache.put(cacheKey, dataMap);
                        return dataMap;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("读取测试数据失败: {}", e.getMessage());
        }
        
        return new HashMap<>();
    }
    
    /**
     * 获取测试数据（单参数版本）
     */
    public static Map<String, Object> getTestData(String module) {
        if (module == null || module.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        String moduleName = module.trim();
        
        // 尝试多种可能的测试用例文件名
        String[] possibleTestCases = {"default", moduleName, "test_" + moduleName, "data"};
        
        for (String testCase : possibleTestCases) {
            Map<String, Object> testData = getTestData(moduleName, testCase);
            if (!testData.isEmpty()) {
                return testData;
            }
        }
        
        return new HashMap<>();
    }
    
    /**
     * 合并配置和测试数据
     */
    public static Map<String, Object> mergeConfigAndData(String env, String module, String testCase) {
        Map<String, Object> merged = new HashMap<>();
        
        // 合并环境配置
        merged.putAll(getEnvironmentConfig(env));
        
        // 合并测试数据
        merged.putAll(getTestData(module, testCase));
        
        // 合并全局参数
        merged.putAll(getGlobalParameters());
        
        return merged;
    }
    
    /**
     * 获取认证信息
     */
    public static Map<String, String> getAuthConfig(String env) {
        Map<String, Object> envConfig = getEnvironmentConfig(env);
        Map<String, String> auth = new HashMap<>();
        if (envConfig != null) {
            auth.put("app_key", envConfig.getOrDefault("app_key", "").toString());
            auth.put("secret", envConfig.getOrDefault("secret", "").toString());
            auth.put("user_name", envConfig.getOrDefault("user_name", "").toString());
            auth.put("password", envConfig.getOrDefault("password", "").toString());
            auth.put("account", envConfig.getOrDefault("account", "").toString());
        }
        return auth;
    }
}