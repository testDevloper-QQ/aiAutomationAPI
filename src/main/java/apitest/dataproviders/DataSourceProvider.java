package apitest.dataproviders;

import java.util.List;
import java.util.Map;

/**
 * 数据源接口
 * 统一不同数据源（Swagger、Postman、Excel）的访问方式
 */
public interface DataSourceProvider {
    
    /**
     * 获取接口定义信息
     */
    List<Map<String, Object>> getApiDefinitions(String module);
    
    /**
     * 获取接口依赖关系
     */
    Map<String, List<String>> getDependencies();
    
    /**
     * 获取测试用例数据
     */
    List<Map<String, Object>> getTestCases(String apiPath);
    
    /**
     * 支持的数据源类型
     */
    enum SourceType {
        SWAGGER, POSTMAN, EXCEL
    }
}