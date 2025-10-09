package handlers;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ResponseHandler {

    private static final String SERVER_NAME = "Multi-threaded HTTP Server";
    private static final int KEEP_ALIVE_TIMEOUT = 30;
    private static final int KEEP_ALIVE_MAX = 100;

    private static String getHttpDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(new Date());
    }

    private static String buildHeaders(String statusLine, String contentType, int contentLength, String connectionType) {
        StringBuilder headers = new StringBuilder();
        headers.append(statusLine).append("\r\n");
        headers.append("Content-Type: ").append(contentType).append("\r\n");
        headers.append("Content-Length: ").append(contentLength).append("\r\n");
        headers.append("Date: ").append(getHttpDate()).append("\r\n");
        headers.append("Server: ").append(SERVER_NAME).append("\r\n");
        headers.append("Connection: ").append(connectionType).append("\r\n");
        if ("keep-alive".equalsIgnoreCase(connectionType)) {
            headers.append("Keep-Alive: timeout=").append(KEEP_ALIVE_TIMEOUT)
                    .append(", max=").append(KEEP_ALIVE_MAX).append("\r\n");
        }
        headers.append("\r\n");
        return headers.toString();
    }

    public static void sendBadRequest(OutputStream outputStream, String error, String connectionType) throws IOException {
        String statusLine = "HTTP/1.1 400 Bad Request";
        String body = buildErrorPage("400 Bad Request", error);
        byte[] content = body.getBytes();

        String headers = buildHeaders(statusLine, "text/html; charset=utf-8", content.length, connectionType);

        outputStream.write(headers.getBytes());
        outputStream.write(content);
        outputStream.flush();

    }

    public static void sendForbidden(OutputStream outputStream, String error, String connectionType) throws IOException {
        String statusLine = "HTTP/1.1 403 Forbidden";
        String body = buildErrorPage("403 Forbidden", error);
        byte[] content = body.getBytes();

        String headers = buildHeaders(statusLine, "text/html; charset=utf-8", content.length, connectionType);

        outputStream.write(headers.getBytes());
        outputStream.write(content);
        outputStream.flush();
    }

    public static void sendMethodNotAllowed(OutputStream outputStream, String connectionType) throws IOException {
        String statusLine = "HTTP/1.1 405 Method Not Allowed";
        String message = "The requested method is not supported. Allowed methods: GET, POST";
        String body = buildErrorPage("405 Method Not Allowed", message);
        byte[] content = body.getBytes();

        StringBuilder headers = new StringBuilder();
        headers.append(statusLine).append("\r\n");
        headers.append("Content-Type: text/html; charset=utf-8\r\n");
        headers.append("Content-Length: ").append(content.length).append("\r\n");
        headers.append("Allow: GET, POST\r\n");
        headers.append("Date: ").append(getHttpDate()).append("\r\n");
        headers.append("Server: ").append(SERVER_NAME).append("\r\n");
        headers.append("Connection: ").append(connectionType).append("\r\n");

        if ("keep-alive".equalsIgnoreCase(connectionType)) {
            headers.append("Keep-Alive: timeout=").append(KEEP_ALIVE_TIMEOUT)
                    .append(", max=").append(KEEP_ALIVE_MAX).append("\r\n");
        }

        headers.append("\r\n");

        outputStream.write(headers.toString().getBytes());
        outputStream.write(content);
        outputStream.flush();
    }

    public static void sendUnsupportedMediaType(OutputStream outputStream, String errorMessage, String connectionType) throws IOException {
        String statusLine = "HTTP/1.1 415 Unsupported Media Type";
        String body = buildErrorPage("415 Unsupported Media Type", errorMessage);
        byte[] content = body.getBytes();

        String headers = buildHeaders(statusLine, "text/html; charset=utf-8", content.length, connectionType);

        outputStream.write(headers.getBytes());
        outputStream.write(content);
        outputStream.flush();
    }

    public static void sendNotFound(OutputStream outputStream, String requestPath, String connectionType) throws IOException {
        String statusLine = "HTTP/1.1 404 Not Found";
        String message = "The requested resource '" + requestPath + "' could not be found.";
        String body = buildErrorPage("404 Not Found", message);
        byte[] content = body.getBytes();

        String headers = buildHeaders(statusLine, "text/html; charset=utf-8", content.length, connectionType);

        outputStream.write(headers.getBytes());
        outputStream.write(content);
        outputStream.flush();
    }

    public static void sendInternalServerError(OutputStream outputStream, String connectionType) throws IOException {
        String statusLine = "HTTP/1.1 500 Internal Server Error";
        String message = "An internal server error occurred while processing your request.";
        String body = buildErrorPage("500 Internal Server Error", message);
        byte[] content = body.getBytes();

        String headers = buildHeaders(statusLine, "text/html; charset=utf-8", content.length, connectionType);

        outputStream.write(headers.getBytes());
        outputStream.write(content);
        outputStream.flush();
    }

    public static void sendHtmlResponse(OutputStream outputStream, byte[] fileBytes, String connectionType) throws IOException {
        String statusLine = "HTTP/1.1 200 OK";
        String headers = buildHeaders(statusLine, "text/html; charset=utf-8", fileBytes.length, connectionType);
        outputStream.write(headers.getBytes());
        outputStream.write(fileBytes);
        outputStream.flush();
    }

    public static void sendBinaryFileResponse(OutputStream outputStream, byte[] fileBytes, String fileName, String connectionType) throws IOException {
        String statusLine = "HTTP/1.1 200 OK";
        StringBuilder headers = new StringBuilder();
        headers.append(statusLine).append("\r\n");
        headers.append("Content-Type: application/octet-stream\r\n");
        headers.append("Content-Length: ").append(fileBytes.length).append("\r\n");
        headers.append("Content-Disposition: attachment; filename=\"").append(fileName).append("\"\r\n");
        headers.append("Date: ").append(getHttpDate()).append("\r\n");
        headers.append("Server: ").append(SERVER_NAME).append("\r\n");
        headers.append("Connection: ").append(connectionType).append("\r\n");
        if ("keep-alive".equalsIgnoreCase(connectionType)) {
            headers.append("Keep-Alive: timeout=").append(KEEP_ALIVE_TIMEOUT)
                    .append(", max=").append(KEEP_ALIVE_MAX).append("\r\n");
        }
        headers.append("\r\n");
        outputStream.write(headers.toString().getBytes());
        outputStream.write(fileBytes);
        outputStream.flush();
    }

    public static void sendCreatedResponse(OutputStream outputStream, String filePath, String connectionType) throws IOException {
        String statusLine = "HTTP/1.1 201 Created";
        String jsonResponse = String.format(
                "{\"status\":\"success\",\"message\":\"File created successfully\",\"filepath\":\"%s\"}",
                filePath
        );
        byte[] content = jsonResponse.getBytes();
        String headers = buildHeaders(statusLine, "application/json", content.length, connectionType);

        outputStream.write(headers.getBytes());
        outputStream.write(content);
        outputStream.flush();
    }

    private static String buildErrorPage(String title, String message) {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <title>" + title + "</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1>" + title + "</h1>\n" +
                "    <p>" + message + "</p>\n" +
                "    <hr>\n" +
                "    <p><em>" + SERVER_NAME + "</em></p>\n" +
                "</body>\n" +
                "</html>";
    }

    public static void sendServiceUnavailable(OutputStream outputStream, int retryAfterSeconds) throws IOException {
        String statusLine = "HTTP/1.1 503 Service Unavailable";
        String message = "The server is currently unable to handle the request. Please try again later.";
        String body = buildErrorPage("503 Service Unavailable", message);
        byte[] content = body.getBytes();

        String headers = statusLine + "\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: " + content.length + "\r\n" +
                "Retry-After: " + retryAfterSeconds + "\r\n" +
                "Date: " + getHttpDate() + "\r\n" +
                "Server: " + SERVER_NAME + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";

        outputStream.write(headers.getBytes());
        outputStream.write(content);
        outputStream.flush();
    }
}
