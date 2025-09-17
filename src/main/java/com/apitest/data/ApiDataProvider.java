package com.apitest.data;

import java.util.List;
import java.util.Map;

/**
 * API数据提供者统一接口
 * 定义标准字段结构，确保Swagger和Postman解析结果一致
 */
public interface ApiDataProvider {
    
    /**
     * 解析API文档，提取接口信息
     * @param jsonPath 文档路径
     * @param host 主机地址
     * @return 接口信息列表
     * @throws Exception 解析异常
     */
    List<Map<String, Object>> parse(String jsonPath, String host) throws Exception;
    
    /**
     * 标准字段定义：
     * - method: HTTP方法 (GET, POST, PUT, DELETE等)
     * - url: 完整请求URL
     * - path: 请求路径
     * - host: 主机地址
     * - operationId: 操作ID，用于生成类名 (Swagger特有，Postman映射name字段)
     * - summary: 接口描述
     * - description: 详细描述
     * - headers/headerParameters: 请求头参数
     * - query/queryParameters: 查询参数
     * - pathParameters: 路径参数
     * - body: 请求体内容
     * - bodyParameters: 请求体参数详情
     * - responses: 响应信息
     * - tags: 标签分类
     * - security: 安全配置
     */
} 