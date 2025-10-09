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

    /// does this has host?
    public boolean hasHost() {
        return host != null && !host.isEmpty();
    }

    /// does it contain JSON
    public boolean isJsonType() {
        return contentType != null && contentType.toLowerCase().contains("application/json");
    }

    public boolean isGet() {
        if (method == null || method.toString().isEmpty()) return false;
        return method.toString().equals("GET");
    }

    public boolean hasBody(){
        return body != null && !body.isEmpty();
    }
}
