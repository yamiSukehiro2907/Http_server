package Handler;

import dto.HttpRequest;
import enums.Method;
import helpers.Logger;

import java.util.HashMap;
import java.util.Map;

public class ParseHandler {

    private static final int MAX_REQUEST_SIZE = 8192;

    public static HttpRequest parseRequest(String requestString) {
        if (requestString == null || requestString.trim().isEmpty()) return createErrorRequest("Empty Request");
        try {
            if (requestString.getBytes().length > 8192) {
                return createErrorRequest("Request size exceeds maximum allowed size of " + MAX_REQUEST_SIZE + " bytes");
            }
            String[] lines = requestString.split("\\r?\\n");
            /// if the request does not contain any part
            if (lines.length == 0) return createErrorRequest("Empty Request");
            String requestLine = lines[0].trim();
            if (requestLine.isEmpty()) return createErrorRequest("Empty Request line");
            String[] requestLineParts = requestLine.split("\\s+");
            if (requestLineParts.length != 3) {
                return createErrorRequest("Malformed request line. Expected: METHOD PATH HTTP_VERSION");
            }
            Method method = Method.fromString(requestLineParts[0]);
            String path = requestLineParts[1];
            String httpVersion = requestLineParts[2];
            if (method == null) return createErrorRequest("Invalid or unsupported HTTP Method: " + requestLineParts[0]);
            if (!httpVersion.startsWith("HTTP/")) return createErrorRequest("Invalid http version: " + httpVersion);
            Map<String, String> headersMap = new HashMap<>();
            int bodyIndex = fillHeadersMap(headersMap, lines);
            String body = extractBody(bodyIndex, lines);
            String connectionType = findConnectionType(headersMap, httpVersion);
            HttpRequest httpRequest = new HttpRequest(
                    method,
                    httpVersion,
                    path,
                    headersMap,
                    connectionType,
                    body
            );
            httpRequest.setValid(true);
            return httpRequest;
        } catch (Exception e) {
            Logger.error("Error parsing request: " + e.getMessage());
            return createErrorRequest("Error parsing request: " + e.getMessage());
        }
    }

    private static String extractBody(int bodyIndex, String[] lines) {
        String body = null;
        if (bodyIndex > 0 && bodyIndex < lines.length) {
            StringBuilder bodyBuilder = new StringBuilder();
            for (int i = bodyIndex; i < lines.length; i++) {
                if (i > bodyIndex) bodyBuilder.append("\n");
                bodyBuilder.append(lines[i]);
            }
            body = bodyBuilder.toString().trim();
        }
        return body;
    }

    private static HttpRequest createErrorRequest(String error) {
        HttpRequest httpRequest = new HttpRequest(
                null,
                null,
                null,
                new HashMap<>(),
                "close",
                null
        );
        httpRequest.setValid(false);
        httpRequest.setParseError(error);
        return httpRequest;
    }

    private static String findConnectionType(Map<String, String> headersMap, String httpVersion) {
        String connectionHeader = headersMap.get("Connection");
        if (connectionHeader != null) {
            if (connectionHeader.equalsIgnoreCase("keep-alive")) return "keep-alive";
            else if (connectionHeader.equalsIgnoreCase("close")) return "close";
        }
        if (httpVersion.equals("HTTP/1.1")) return "keep-alive";
        else return "close";
    }

    private static int fillHeadersMap(Map<String, String> headersMap, String[] lines) {
        int rowIndex = -1;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().isEmpty()) {
                rowIndex = i + 1;
                break;
            }
            int colIndex = line.indexOf(":");
            if (colIndex > 0) {
                String header = line.substring(0, colIndex).trim();
                String value = line.substring(colIndex).trim();
                headersMap.put(normalizeHeaderName(header), value);
            }
        }
        return rowIndex;
    }

    private static String normalizeHeaderName(String header) {
        if (header == null || header.isEmpty()) return header;
        String lowerCase = header.toLowerCase();
        switch (lowerCase) {
            case "host":
                return "Host";
            case "content-type":
                return "Content-Type";
            case "content-length":
                return "Content-Length";
            case "connection":
                return "Connection";
            case "user-agent":
                return "User-agent";
            case "accept":
                return "Accept";
            case "content-disposition":
                return "Content-Disposition";
            default:
                String[] parts = lowerCase.split("-");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) sb.append("-");
                    if (!parts[i].isEmpty()) {
                        sb.append(Character.toUpperCase(parts[i].charAt(0)));
                        if (parts[i].length() > 1) sb.append(parts[i].substring(1));
                    }
                }
                return sb.toString();
        }
    }

    public static boolean isValidRequest(HttpRequest httpRequest) {
        if (httpRequest == null || !httpRequest.isValid()) return false;
        if (httpRequest.getMethod() != Method.GET && httpRequest.getMethod() != Method.POST) return false;
        if (httpRequest.getPath() == null || httpRequest.getPath().isEmpty()) return false;
        return httpRequest.getHttpVersion() != null && !httpRequest.getHttpVersion().isEmpty();
    }

    public static boolean hasHostHeader(HttpRequest httpRequest) {
        return httpRequest != null &&
                httpRequest.getHeadersMap() != null &&
                httpRequest.getHeadersMap().containsKey("Host") &&
                httpRequest.getHeadersMap().get("Host") != null &&
                httpRequest.getHeadersMap().get("Host").trim().isEmpty();
    }

    public static boolean validateHostHeader(HttpRequest httpRequest, String expectedHost) {
        if (!hasHostHeader(httpRequest)) return false;
        String requestHost = httpRequest.getHeadersMap().get("Host").trim();
        if (requestHost.contains("/")) requestHost = requestHost.substring(0, requestHost.indexOf("/"));
        if (requestHost.equalsIgnoreCase(expectedHost)) return true;
        String[] parts = expectedHost.split(":");
        if (parts.length == 2) {
            String port = parts[1];
            return requestHost.equalsIgnoreCase("localhost:" + port)
                    || requestHost.equalsIgnoreCase("127.0.0.1:" + port)
                    || requestHost.equalsIgnoreCase("0.0.0.0:" + port);
        }
        return false;
    }

    public static boolean isPathSafe(String path) {
        if (path == null || path.isEmpty()) return false;
        String normalizedPath = path.trim();
        if (normalizedPath.contains("..")
                || normalizedPath.contains("./")
                || normalizedPath.startsWith("//")
                || normalizedPath.contains("\\")) {
            return false;
        }
        String lowerPath = normalizedPath.toLowerCase();
        if (lowerPath.contains("%2e%2e") /// URL encoded ..
                || lowerPath.contains("%2f") ///  URL encoded /
                || lowerPath.startsWith("%5c")) {
            return false;
        }
        return normalizedPath.startsWith("/");
    }

    public static String extractFilePath(String requestPath) {
        if (requestPath == null || requestPath.isEmpty()) return "index.html";
        String path = requestPath.trim();
        if (path.startsWith("/")) path = path.substring(1); /// removing the slash
        if (path.isEmpty()) return "index.html";
        return path;
    }

    public static String getFileExtension(String path) {
        if (path == null || path.isEmpty()) return "";
        int lastDotIndex = path.indexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < path.length() - 1) return path.substring(lastDotIndex).toLowerCase();
        return "";
    }

    public static boolean isSupportedFileType(String extension) {
        if (extension == null || extension.isEmpty()) return false;
        String ext = extension.toLowerCase();
        return ext.equals(".html")
                || ext.equals(".txt")
                || ext.equals(".png")
                || ext.equals("jpg")
                || ext.equals("jpeg");
    }
}
