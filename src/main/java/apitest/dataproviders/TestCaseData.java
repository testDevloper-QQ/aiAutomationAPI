package apitest.dataproviders;

import java.util.Map;

/**
 * 测试用例数据类，用于存储测试用例信息
 */
public class TestCaseData {
    private String id;
    private String description;
    private String method;
    private String path;
    private Map<String, Object> parameters;
    private Map<String, Object> expectedResult;
    private Map<String, Object> headers;

    public TestCaseData() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public Map<String, Object> getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(Map<String, Object> expectedResult) {
        this.expectedResult = expectedResult;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }
}