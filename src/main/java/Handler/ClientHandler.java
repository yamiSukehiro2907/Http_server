package Handler;

import dto.HttpRequest;
import enums.Method;
import helpers.Client;
import helpers.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class ClientHandler {

    private final Client client;
    private final String RESOURCES_FOLDER;
    private final int MAX_REQUEST_SIZE;
    private final String serverAddress;
    private LocalDateTime prevRequestTime;
    private String threadName;
    private int requestCount = 0;
    private static final int MAX_REQUESTS_PER_CONNECTION = 100;
    private static final int KEEP_ALIVE_TIMEOUT_SECONDS = 30;

    public ClientHandler(Client client, String RESOURCES_FOLDER, int MAX_REQUEST_SIZE, String serverAddress) {
        this.client = client;
        this.RESOURCES_FOLDER = RESOURCES_FOLDER;
        this.MAX_REQUEST_SIZE = MAX_REQUEST_SIZE;
        this.serverAddress = serverAddress;
        this.threadName = Thread.currentThread().getName();
        this.prevRequestTime = LocalDateTime.now();
    }

    public void handle() {

        boolean keepAlive = true;

        try {
            Logger.logWithThread(threadName, "Connection from " + client.getClientAddress());

            while (keepAlive && requestCount < MAX_REQUESTS_PER_CONNECTION) {

                if (requestCount > 0 && prevRequestTime != null) {
                    long secondsSinceLastRequest = ChronoUnit.SECONDS.between(prevRequestTime, LocalDateTime.now());
                    if (secondsSinceLastRequest > KEEP_ALIVE_TIMEOUT_SECONDS) {
                        Logger.logWithThread(threadName, "Connection timeout after " + secondsSinceLastRequest + " seconds");
                        break;
                    }
                }

                String rawRequest = readRequest(client.getBufferedReader());
                if (rawRequest == null || rawRequest.isEmpty()) {
                    if (requestCount == 0) {
                        Logger.errorWithThread(threadName, "Empty request received");
                    }
                    break;
                }

                requestCount++;
                prevRequestTime = LocalDateTime.now();

                HttpRequest httpRequest = RequestHandler.parseRequest(rawRequest);

                if (!httpRequest.isValid()) {
                    String error = httpRequest.getParseError();
                    Logger.errorWithThread(threadName, "Invalid request: " + error);
                    ResponseHandler.sendBadRequest(client.getOutputStream(), error, "close");
                    break;
                }

                Logger.logWithThread(threadName, "Request: " + httpRequest.toString());

                if (!RequestHandler.hasHostHeader(httpRequest)) {
                    Logger.errorWithThread(threadName, "Missing Host header");
                    ResponseHandler.sendBadRequest(client.getOutputStream(), "Missing required host header", "close");
                    break;
                }

                if (!RequestHandler.validateHostHeader(httpRequest, serverAddress)) {
                    String requestHost = httpRequest.getHost();
                    Logger.security(threadName, "Host header mismatch: expected " + serverAddress + ", got " + requestHost);
                    ResponseHandler.sendForbidden(client.getOutputStream(),
                            "Host header mismatch. Expected: " + serverAddress + ", Got: " + requestHost, "close");
                    break;
                }

                Logger.logWithThread(threadName, "Host validation: " + httpRequest.getHost() + " ✓");

                if (!RequestHandler.isPathSafe(httpRequest.getPath())) {
                    Logger.security(threadName, "Path traversal attempt blocked: " + httpRequest.getPath());
                    ResponseHandler.sendForbidden(client.getOutputStream(), "Path traversal detected", "close");
                    break;
                }

                if (!Method.isSupported(httpRequest.getMethod())) {
                    Logger.logWithThread(threadName, "Unsupported method: " + httpRequest.getMethod());
                    ResponseHandler.sendMethodNotAllowed(client.getOutputStream(), httpRequest.getConnectionType());
                    if (!httpRequest.isKeepAlive()) {
                        break;
                    }
                    continue;
                }

                if (httpRequest.isGet()) handleGetRequest(httpRequest);
                else handlePostRequest(httpRequest);

                keepAlive = httpRequest.isKeepAlive();

                if (!keepAlive) {
                    Logger.logWithThread(threadName, "Connection: close");
                    break;
                } else {
                    Logger.logWithThread(threadName, "Connection: keep-alive (request " + requestCount + "/" + MAX_REQUESTS_PER_CONNECTION + ")");
                }

            }

            if (requestCount >= MAX_REQUESTS_PER_CONNECTION)
                Logger.logWithThread(threadName, "Maximum requests per connection reached (" + MAX_REQUESTS_PER_CONNECTION + ")");


        } catch (Exception e) {
            Logger.logException("Error in ClientHandler", e);
        } finally {
            Logger.logWithThread(threadName, "Connection closed (total requests: " + requestCount + ")");
        }
    }

    public String readRequest(BufferedReader bufferedReader) throws IOException {
        StringBuilder requestStringBuilder = new StringBuilder();
        String line;
        int bytesRead;

        while ((line = bufferedReader.readLine()) != null) {
            bytesRead = line.getBytes().length + 2;
            if (bytesRead > MAX_REQUEST_SIZE) {
                Logger.errorWithThread(threadName, "Request exceeds size limit");
                return null;
            }

            requestStringBuilder.append(line).append("\r\n");

            if (line.isEmpty()) {
                if (bufferedReader.ready()) {
                    while (bufferedReader.ready() && bytesRead < MAX_REQUEST_SIZE) {
                        int c = bufferedReader.read();
                        if (c == -1) break;
                        requestStringBuilder.append((char) c);
                        bytesRead++;
                    }
                }
                break;
            }
        }
        return requestStringBuilder.toString();
    }

    private void handleGetRequest(HttpRequest httpRequest) {
        try {
            OutputStream outputStream = client.getOutputStream();
            String requestPath = httpRequest.getPath();

            String filePath = RequestHandler.extractFilePath(requestPath);
            String fileExtension = RequestHandler.getFileExtension(filePath);

            if (!RequestHandler.isSupportedFileType(fileExtension)) {
                Logger.logWithThread(threadName, "Unsupported file type: " + fileExtension);
                ResponseHandler.sendUnsupportedMediaType(outputStream, fileExtension, httpRequest.getConnectionType());
                return;
            }

            File file = new File(RESOURCES_FOLDER, filePath);
            Path path = Paths.get(file.getAbsolutePath()).normalize();

            Path resourcePath = Paths.get(RESOURCES_FOLDER).toAbsolutePath().normalize();
            if (!path.startsWith(resourcePath)) {
                Logger.security(threadName, "Attempted access outside resources folder: " + path);
                ResponseHandler.sendForbidden(outputStream, "Access denied", httpRequest.getConnectionType());
                return;
            }
            if (!file.exists() || !file.isFile()) {
                Logger.logWithThread(threadName, "File not found: " + filePath);
                ResponseHandler.sendNotFound(outputStream, requestPath, httpRequest.getConnectionType());
                return;
            }
            boolean isBinary = !fileExtension.equals(".html");

            if (isBinary) sendBinaryFile(file, filePath, outputStream, httpRequest.getConnectionType());
            else sendHtmlFile(file, outputStream, httpRequest.getConnectionType());
        } catch (IOException e) {
            Logger.logException("Error handling GET request", e);
            try {
                ResponseHandler.sendInternalServerError(client.getOutputStream(), httpRequest.getConnectionType());
            } catch (IOException e1) {
                Logger.errorWithThread(threadName, "Failed to send error response: " + e1.getMessage());
            }
        }
    }

    private void sendHtmlFile(File file, OutputStream outputStream, String connectionType) throws IOException {
        byte[] fileBytes = Files.readAllBytes(file.toPath());

        Logger.logWithThread(threadName, "Serving HTML file: " + file.getName() + " (" + fileBytes.length + " bytes)");

        ResponseHandler.sendHtmlResponse(outputStream, fileBytes, connectionType);

        Logger.logWithThread(threadName, "Response: 200 OK (" + fileBytes.length + " bytes transferred)");
    }

    private void sendBinaryFile(File file, String fileName, OutputStream outputStream, String connectionType) throws IOException {
        byte[] fileBytes = Files.readAllBytes(file.toPath());

        Logger.logWithThread(threadName, "Sending binary file: " + fileName + " (" + fileBytes.length + " bytes)");

        ResponseHandler.sendBinaryFileResponse(outputStream, fileBytes, fileName, connectionType);

        Logger.logWithThread(threadName, "Response: 200 OK (" + fileBytes.length + " bytes transferred)");
    }

    private void handlePostRequest(HttpRequest httpRequest) {
        try {
            OutputStream outputStream = client.getOutputStream();

            if (!httpRequest.isJsonType()) {
                String contentType = httpRequest.getContentType() != null ? httpRequest.getContentType() : "none";
                Logger.logWithThread(threadName, "Invalid content-type for POST: " + contentType);
                ResponseHandler.sendUnsupportedMediaType(
                        outputStream,
                        "Only application/json is supported for POST requests",
                        httpRequest.getConnectionType()
                );
                return;
            }

            if (!httpRequest.hasBody()) {
                Logger.logWithThread(threadName, "POST request has no body");
                ResponseHandler.sendBadRequest(outputStream, "Request body is required", httpRequest.getConnectionType());
                return;
            }

            String jsonBody = httpRequest.getBody();
            if (!RequestHandler.isValidJSON(jsonBody)) {
                Logger.logWithThread(threadName, "Invalid JSON in request body");
                ResponseHandler.sendBadRequest(outputStream, "Invalid JSON format in request body", httpRequest.getConnectionType());
                return;
            }

            File uploadsDir = new File(RESOURCES_FOLDER, "uploads");
            if (!uploadsDir.exists() && !uploadsDir.mkdirs()) {
                Logger.errorWithThread(threadName, "Failed to create uploads directory");
                ResponseHandler.sendInternalServerError(outputStream, httpRequest.getConnectionType());
                return;
            }

            String filename = RequestHandler.generateUploadFilename();
            File uploadFile = new File(uploadsDir, filename);

            Files.write(uploadFile.toPath(), jsonBody.getBytes());

            Logger.logWithThread(threadName, "File created: " + filename + " (" + jsonBody.length() + " bytes)");

            String filepath = "/uploads/" + filename;
            ResponseHandler.sendCreatedResponse(outputStream, filepath, httpRequest.getConnectionType());

            Logger.logWithThread(threadName, "Response: 201 Created");
        } catch (IOException e) {
            Logger.logException("Error handling POST request", e);
            try {
                ResponseHandler.sendInternalServerError(client.getOutputStream(), httpRequest.getConnectionType());
            } catch (IOException ex) {
                Logger.errorWithThread(threadName, "Failed to send error response: " + ex.getMessage());
            }
        }
    }
}
