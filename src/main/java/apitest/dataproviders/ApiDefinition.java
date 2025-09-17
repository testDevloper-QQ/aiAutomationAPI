package apitest.dataproviders;

/**
 * API定义类，用于存储接口信息
 */
public class ApiDefinition {
    private String method;
    private String path;
    private String description;
    private String module;
    private Object parameters;
    private Object responses;

    public ApiDefinition() {}

    public ApiDefinition(String method, String path, String description) {
        this.method = method;
        this.path = path;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Object getParameters() {
        return parameters;
    }

    public void setParameters(Object parameters) {
        this.parameters = parameters;
    }

    public Object getResponses() {
        return responses;
    }

    public void setResponses(Object responses) {
        this.responses = responses;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }
}