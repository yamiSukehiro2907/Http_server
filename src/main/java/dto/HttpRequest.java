package dto;

import enums.Method;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter

public class HttpRequest {

    private Method method;
    private String path;
    private String httpVersion;
    private Map<String, String> headersMap;

    ///  keep-alive or not
    private String connectionType;

    private String body;

    private String contentType;

    /// host header value
    private String host;

    /// content length from headers
    private Integer contentLength;

    /// if this is a valid request
    private boolean isValid;

    private String parseError;

    public HttpRequest(Method method, String httpVersion, String path, Map<String, String> headersMap, String connectionType, String body) {
        this.method = method;
        this.path = path;
        this.httpVersion = httpVersion;
        this.headersMap = headersMap;
        this.connectionType = connectionType;
        this.body = body;
    }


    /// should we keep alive?
    public boolean isKeepAlive() {
        if (connectionType != null) {
            return connectionType.equalsIgnoreCase("keep-alive");
        }
        return httpVersion != null && httpVersion.equals("HTTP/1.1");
    }

    /// does it contain JSON
    public boolean isJsonType() {
        return contentType != null && contentType.toLowerCase().contains("application/json");
    }

    public boolean isGet() {
        if (method == null || method.toString().isEmpty()) return false;
        return method.toString().equals("GET");
    }

    public boolean hasBody() {
        return body != null && !body.isEmpty();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HttpRequest {\n");
        sb.append("  isValid=").append(isValid).append(",\n");
        if (!isValid) sb.append("  parseError='").append(parseError).append("',\n");
        sb.append("  method=").append(method).append(",\n");
        sb.append("  path='").append(path).append("',\n");
        sb.append("  httpVersion='").append(httpVersion).append("',\n");
        sb.append("  host='").append(host).append("',\n");
        sb.append("  headersMap=").append(headersMap).append(",\n");
        sb.append("  contentType='").append(contentType).append("',\n");
        sb.append("  contentLength=").append(contentLength).append(",\n");
        sb.append("  connectionType='").append(connectionType).append("',\n");
        sb.append("  body='").append(body).append("',\n");
        sb.append("  --- Derived Info ---\n");
        sb.append("  isKeepAlive=").append(isKeepAlive()).append(",\n");
        sb.append("  isJsonType=").append(isJsonType()).append(",\n");
        sb.append("  hasBody=").append(hasBody()).append("\n");
        sb.append("}");
        return sb.toString();
    }
}
