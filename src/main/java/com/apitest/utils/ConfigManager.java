package com.apitest.utils;

import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.util.Map;

public class ConfigManager {
    private static Map<String, Object> config;

    static {
        try (InputStream in = ConfigManager.class.getClassLoader().getResourceAsStream("file_cases.yaml")) {
            Yaml yaml = new Yaml();
            config = yaml.load(in);
        } catch (Exception e) {
            throw new RuntimeException("读取file_cases.yaml失败", e);
        }
    }

    public static String get(String key) {
        return config.get(key).toString();
    }

    public static Map<String, Object> getConfig() {
        return config;
    }
} 