package apitest.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试配置管理器
 * 统一管理测试配置、环境配置、数据源配置
 */
public class TestConfigManager {
    private static TestConfigManager instance;
    private Map<String, Object> testCasesConfig;
    private Map<String, Object> testDataConfig;
    private Map<String, Object> envConfig;
    
    private TestConfigManager() {
        loadConfigurations();
    }
    
    public static synchronized TestConfigManager getInstance() {
        if (instance == null) {
            instance = new TestConfigManager();
        }
        return instance;
    }
    
    /**
     * 加载所有配置文件
     */
    private void loadConfigurations() {
        try {
            // 加载测试用例配置
            Yaml yaml = new Yaml();
            InputStream testCasesStream = getClass().getClassLoader()
                .getResourceAsStream("testdata/test/cases/component/file_cases.yml");
            testCasesConfig = yaml.load(testCasesStream);
            
            // 加载测试数据配置
            InputStream testDataStream = getClass().getClassLoader()
                .getResourceAsStream("testdata/test/module_testdata/file_testdata.yml");
            testDataConfig = yaml.load(testDataStream);
            
            // 根据环境加载对应配置
            String env = (String) testDataConfig.get("env");
            InputStream envStream = getClass().getClassLoader()
                .getResourceAsStream("config/env.yml");
            Map<String, Object> allEnvConfigs = yaml.load(envStream);
            envConfig = (Map<String, Object>) allEnvConfigs.get(env);
            
        } catch (Exception e) {
            throw new RuntimeException("加载配置文件失败", e);
        }
    }
    
    /**
     * 获取测试场景配置
     */
    public Map<String, Object> getTestScenario(String scenarioName) {
        Map<String, Object> scenarios = (Map<String, Object>) testCasesConfig.get("scenarios");
        return (Map<String, Object>) scenarios.get(scenarioName);
    }
    
    /**
     * 获取全局参数
     */
    public Map<String, String> getGlobalParameters() {
        Map<String, String> params = new HashMap<>();
        Map<String, Object> parameters = (Map<String, Object>) testCasesConfig.get("parameters");
        
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String value = entry.getValue().toString();
            // 替换变量引用
            if (value.startsWith("${") && value.endsWith("}")) {
                String varName = value.substring(2, value.length() - 1);
                value = getVariableValue(varName);
            }
            params.put(entry.getKey(), value);
        }
        
        return params;
    }
    
    /**
     * 获取测试数据
     */
    public Map<String, Object> getTestData() {
        return testDataConfig;
    }
    
    /**
     * 获取环境配置
     */
    public Map<String, Object> getEnvironmentConfig() {
        return envConfig;
    }
    
    /**
     * 获取变量值
     */
    private String getVariableValue(String varName) {
        Object value = testDataConfig.get(varName);
        if (value == null) {
            value = envConfig.get(varName);
        }
        return value != null ? value.toString() : "";
    }
    
    /**
     * 获取数据源配置
     */
    public Map<String, Object> getDataSourceConfig() {
        return (Map<String, Object>) testCasesConfig.get("dataSource");
    }
}