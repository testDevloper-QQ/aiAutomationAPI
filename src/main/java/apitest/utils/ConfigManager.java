package apitest.utils;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

/**
 * 配置管理工具类
 * 用于读取和管理配置文件
 */
public class ConfigManager {
    private static Map<String, Object> config;
    
    static {
        loadConfig();
    }
    
    /**
     * 加载配置文件
     */
    private static void loadConfig() {
        try {
            Yaml yaml = new Yaml();
            InputStream inputStream = ConfigManager.class
                .getClassLoader()
                .getResourceAsStream("config/env.yml");
            if (inputStream != null) {
                config = yaml.load(inputStream);
            }
        } catch (Exception e) {
            throw new RuntimeException("加载配置文件失败", e);
        }
    }
    
    /**
     * 获取配置值
     * @param key 配置键
     * @return 配置值
     */
    public static String get(String key) {
        if (config == null) {
            return null;
        }
        
        String[] keys = key.split("\\.");
        Object value = config;
        
        for (String k : keys) {
            if (value instanceof Map) {
                value = ((Map<?, ?>) value).get(k);
            } else {
                return null;
            }
        }
        
        return value != null ? value.toString() : null;
    }
    
    /**
     * 获取指定环境的配置值
     * @param env 环境名
     * @param key 配置键
     * @return 配置值
     */
    public static String get(String env, String key) {
        if (config == null || env == null) {
            return null;
        }
        
        Object envConfig = config.get(env);
        if (!(envConfig instanceof Map)) {
            return null;
        }
        
        Map<?, ?> envMap = (Map<?, ?>) envConfig;
        Object value = envMap.get(key);
        
        return value != null ? value.toString() : null;
    }
}