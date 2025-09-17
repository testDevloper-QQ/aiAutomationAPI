package com.apitest.basetestcase;

import com.apitest.core.ApiParamBuilder;
import com.apitest.core.LogUtil;
import com.apitest.utils.HttpClientUtil;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 测试基类：
 * 1) 加载环境与模块变量配置
 * 2) 变量解析器：用例变量/基础变量/环境变量
 * 3) 请求执行器：封装参数并调用 HttpClientUtil
 * 4) 响应读取：cookies、text、json、headers
 */
public abstract class BaseTestCase {

    protected final Logger logger = LogUtil.getLogger(getClass());

    // 环境配置（如 host、公共 header 等）
    protected Map<String, Object> envConfig = new HashMap<>();

    // 模块测试变量（从 apiconfig/variables/*.yaml 读取）
    protected Map<String, Object> testDataVars = new HashMap<>();

    // 最近一次响应
    protected ApiHttpResponse lastResponse;

    // 变量占位符：{var.path}
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{([^{}]+)\\}");

    protected void loadEnv(String envName) {
        String path = "apiconfig/" + envName + ".yaml";
        this.envConfig = loadYamlAsMap(path);
        if (envConfig == null) envConfig = new HashMap<>();
        logger.info("已加载环境配置: {} -> {} keys", envName, envConfig.size());
    }

    protected void loadModuleVars(String moduleName) {
        String path = "apiconfig/variables/" + moduleName + ".yaml";
        this.testDataVars = loadYamlAsMap(path);
        if (testDataVars == null) testDataVars = new HashMap<>();
        logger.info("已加载模块变量: {} -> {} keys", moduleName, testDataVars.size());
    }

    private Map<String, Object> loadYamlAsMap(String classpath) {
        try (InputStream in = BaseTestCase.class.getClassLoader().getResourceAsStream(classpath)) {
            if (in == null) return null;
            Yaml yaml = new Yaml();
            Object obj = yaml.load(in);
            if (obj instanceof Map) return (Map<String, Object>) obj;
            return null;
        } catch (Exception e) {
            logger.warn("加载YAML失败: {} -> {}", classpath, e.getMessage());
            return null;
        }
    }

    // ========== 变量解析 ==========

    protected String resolveString(String template, Map<String, Object> caseVars) {
        if (template == null) return null;
        Matcher m = VAR_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1).trim();
            Object val = findVar(key, caseVars, testDataVars, envConfig);
            String replacement = val == null ? "" : String.valueOf(val);
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    @SafeVarargs
    private final Object findVar(String key, Map<String, Object>... sources) {
        for (Map<String, Object> src : sources) {
            Object val = readByDotPath(src, key);
            if (val != null) return val;
        }
        return null;
    }

    private Object readByDotPath(Map<String, Object> src, String path) {
        if (src == null || path == null) return null;
        String[] parts = path.split("\\.");
        Object cur = src;
        for (String p : parts) {
            if (!(cur instanceof Map)) return null;
            cur = ((Map<String, Object>) cur).get(p);
            if (cur == null) return null;
        }
        return cur;
    }

    protected Map<String, Object> resolveApiInfo(Map<String, Object> apiInfo, Map<String, Object> caseVars) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (apiInfo == null) return out;
        for (Map.Entry<String, Object> e : apiInfo.entrySet()) {
            String k = e.getKey();
            Object v = e.getValue();
            if (v == null) { out.put(k, null); continue; }
            if (v instanceof String) {
                out.put(k, resolveString((String) v, caseVars));
            } else if (v instanceof Map) {
                Map<String, Object> nv = new LinkedHashMap<>();
                ((Map<?, ?>) v).forEach((kk, vv) -> nv.put(String.valueOf(kk), vv instanceof String ? resolveString((String) vv, caseVars) : vv));
                out.put(k, nv);
            } else {
                out.put(k, v);
            }
        }
        return out;
    }

    // ========== 请求执行 ==========

    @SuppressWarnings("unchecked")
    public ApiHttpResponse execute(Map<String, Object> rawApiInfo, Map<String, Object> caseVars) throws Exception {
        Map<String, Object> apiInfo = resolveApiInfo(rawApiInfo, caseVars);
        // 组装基础参数
        String url = ApiParamBuilder.buildUrl(apiInfo);
        Map<String, String> headers = ApiParamBuilder.buildHeaders(apiInfo);
        String body = ApiParamBuilder.buildBody(apiInfo);
        String method = ApiParamBuilder.buildMethod(apiInfo);

        // 根据 bodyParameters 自动设置 Content-Type
        Map<String, Object> bodyParameters = null;
        Object bp = apiInfo.get("bodyParameters");
        if (bp instanceof Map) {
            bodyParameters = (Map<String, Object>) bp;
            if (headers == null) headers = new LinkedHashMap<>();
            if (!headers.containsKey("Content-Type")) {
                if (bodyParameters.containsKey("urlencoded")) {
                    headers.put("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
                } else if (bodyParameters.containsKey("formdata")) {
                    headers.put("Content-Type", "multipart/form-data");
                } else if (body != null) {
                    headers.put("Content-Type", "application/json;charset=UTF-8");
                }
            }
        } else if (body != null) {
            if (headers == null) headers = new LinkedHashMap<>();
            headers.putIfAbsent("Content-Type", "application/json;charset=UTF-8");
        }

        logger.info("请求: {} {}", method, url);
        this.lastResponse = HttpClientUtil.sendRequestWithMetaAdvanced(url, method, headers, body, bodyParameters);
        logger.info("响应: status={}, len={}", lastResponse.getStatusCode(), lastResponse.getBody()==null?0:lastResponse.getBody().length());
        return lastResponse;
    }

    public Object[] executeAsArray(Map<String, Object> rawApiInfo, Map<String, Object> caseVars) throws Exception {
        ApiHttpResponse r = execute(rawApiInfo, caseVars);
        return new Object[]{ r.getStatusCode(), r.getHeaders(), r.getCookies(), r.getBody(), r.getBodyAsJson() };
    }

    // ========== 响应读取 ==========

    public String getResText() { return lastResponse == null ? null : lastResponse.getBody(); }
    public Map<String, List<String>> getResHeaders() { return lastResponse == null ? null : lastResponse.getHeaders(); }
    public Map<String, String> getResCookies() { return lastResponse == null ? null : lastResponse.getCookies(); }
    public com.alibaba.fastjson.JSONObject getResTextJson() { return lastResponse == null ? null : lastResponse.getBodyAsJson(); }
}