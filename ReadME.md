# Multi-threaded HTTP Server in Java

![Java](https://img.shields.io/badge/language-Java-blue.svg?style=for-the-badge&logo=java)
![Maven](https://img.shields.io/badge/build-Maven-red.svg?style=for-the-badge&logo=apache-maven)

A Multi-threaded HTTP/1.1 server built from scratch using low-level Java socket programming. This
project demonstrates a deep understanding of the HTTP protocol, concurrent programming, and network security
fundamentals.

---

## Table of Contents

- [Key Features](#key-features)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Installation & Running](#installation--running)
- [Usage & Testing](#usage--testing)
    - [Web-Based Test Interface](#web-based-test-interface)
    - [Testing with `curl`](#testing-with-curl)
    - [Concurrency Testing](#concurrency-testing)
- [Technical Deep Dive](#technical-deep-dive)
    - [Thread Pool Architecture](#thread-pool-architecture)
    - [Request Handling Pipeline](#request-handling-pipeline)
    - [Security Measures](#security-measures)
- [Configuration](#configuration)

---

## Key Features

- **⚙️ Multi-threaded Architecture**: Utilizes a fixed-size thread pool to handle multiple concurrent client connections
  efficiently.
- **🔄 HTTP/1.1 Protocol Support**: Parses and handles `GET` and `POST` requests, with proper error handling for other
  methods (`405 Method Not Allowed`).
- **📁 Static & Binary File Serving**: Serves `.html` files for in-browser rendering and other types (`.png`, `.jpg`,
  `.txt`) as downloadable binaries.
- **📄 JSON Data Processing**: Accepts `POST` requests with `application/json`, validates the payload, and saves it to a
  file, returning a `201 Created` response.
- **🛡️ Security Hardening**: Implements Path Traversal protection and mandatory Host header validation.
- **🔌 Connection Management**: Supports persistent connections via `Connection: keep-alive` with idle timeouts and
  request limits.
- **📊 Comprehensive Logging**: Detailed, timestamped logs for all server activities, managed by a dedicated `Logger`
  class.
- **🧪 Interactive Test Suite**: Built-in web interface for testing all server endpoints and features.

---

## Project Structure

The project is organized into a standard Maven layout, separating application logic, resources, and tests.

```
Http_server/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── dto/
│   │   │   │   └── HttpRequest.java      # Data Transfer Object for requests
│   │   │   ├── enums/
│   │   │   │   └── Method.java           # Enum for HTTP methods
│   │   │   ├── handlers/
│   │   │   │   ├── ClientHandler.java    # Handles individual client connections
│   │   │   │   ├── RequestHandler.java   # Parses and validates requests
│   │   │   │   └── ResponseHandler.java  # Constructs and sends HTTP responses
│   │   │   ├── helpers/
│   │   │   │   ├── Client.java           # Wrapper for client socket and streams
│   │   │   │   └── Logger.java           # Centralized logging utility
│   │   │   ├── server/
│   │   │   │   └── Server.java           # Main server loop and thread pool management
│   │   │   └── Main.java                 # Application entry point
│   │   └── resources/
│   │       ├── uploads/                  # Directory for POST uploads
│   │       ├── about.html                # Sample HTML page
│   │       ├── contact.html              # Sample HTML page
│   │       ├── index.html                # Default home page
│   │       ├── test.html                 # Interactive test suite
│   │       ├── logo.png                  # Sample PNG image
│   │       ├── photo.jpg                 # Sample JPEG image
│   │       └── sample.txt                # Sample text file
│   └── test/
│       └── test_server.ps1               # PowerShell test script
├── logs/                                 # Server logs directory
└── README.md
```

---

## Getting Started

### Prerequisites

- Java Development Kit (JDK) 11 or newer
- Apache Maven
- Git

### Installation & Running

1. **Clone the repository:**
   ```sh
   git clone https://github.com/yamiSukehiro2907/Http_server.git
   cd Http_server
   ```

2. **Build the project using Maven:**
   This command compiles the source code and packages it into a runnable JAR.
   ```sh
   mvn clean package
   ```

3. **Run the server:**
    - To run with default settings (`127.0.0.1:8080`, 10 threads):
      ```sh
      java -cp target/myartifactid-0.0-SNAPSHOT.jar Main
      ```
    - To run with custom settings (e.g., port 8000, any host, 20 threads):
      ```sh
      java -cp target/myartifactid-0.0-SNAPSHOT.jar Main 8000 0.0.0.0 20
      ```

---

## Usage & Testing

Once the server is running, you can interact with it using multiple methods.

### Web-Based Test Interface

The easiest way to test all server features is through the built-in web interface:

1. **Start the server:**
   ```sh
   java -cp target/myartifactid-0.0-SNAPSHOT.jar Main
   ```

2. **Open your browser and navigate to:**
   ```
   http://127.0.0.1:8080/test.html
   ```

3. **Interactive Test Suite Features:**
    - ✅ **GET Requests**: Test HTML pages, image downloads, and text files
    - ✅ **POST Requests**: Upload JSON data with editable text areas
    - ❌ **Error Cases**: Test 404, 405, 415, and 400 error responses
    - 🔒 **Security Tests**: Verify path traversal protection
    - 📊 **Live Response Display**: View status codes, headers, and response bodies in real-time

The test interface provides a beautiful, user-friendly way to verify that all server features are working correctly.

### Testing with `curl`

For command-line testing, you can use `curl`:

- **Get HTML page:**
  ```sh
  curl -v http://localhost:8080/index.html
  ```

- **Download a binary file:**
  ```sh
  curl -v http://localhost:8080/logo.png --output downloaded_logo.png
  ```

- **Send a POST request with JSON data:**
  ```sh
  curl -v -X POST http://localhost:8080/upload \
  -H "Content-Type: application/json" \
  -H "Host: localhost:8080" \
  -d '{"message": "Testing POST request"}'
  ```

- **Test path traversal protection:**
  ```sh
  curl -v http://localhost:8080/../etc/passwd
  ```

- **Test unsupported method:**
  ```sh
  curl -v -X PUT http://localhost:8080/index.html
  ```

### Concurrency Testing

Use Apache Bench (`ab`) to simulate concurrent connections and test server performance:

```sh
ab -n 1000 -c 50 http://localhost:8080/index.html
```

This command sends 1000 requests with 50 concurrent connections.

You can also use the **`test_server.ps1`** PowerShell script located in the `src/test` directory for automated testing
scenarios.

---

## Technical Deep Dive

### Thread Pool Architecture

The `Server.java` class initializes a fixed-size thread pool using `ExecutorService`. It runs an infinite loop to accept
incoming TCP
connections. Each accepted client `Socket` is wrapped in a `Client` object and added to a `LinkedBlockingQueue`. A
separate queue processor thread continuously dequeues clients and submits them to the thread pool for processing.

**Key Design Benefits:**

- Decouples connection acceptance from request processing
- Prevents thread exhaustion through pool size limits
- Graceful handling of connection spikes through request queuing
- Efficient resource utilization with thread reuse

### Request Handling Pipeline

For each client connection, a `ClientHandler` instance is executed by a worker thread. The process is as follows:

1. **Read & Parse**: `ClientHandler` reads the raw HTTP request from the socket's input stream using a `BufferedReader`.
2. **Validation**: `RequestHandler` parses the raw string into a structured `HttpRequest` DTO. It validates:
    - Request format and HTTP version
    - HTTP method (GET/POST only)
    - Required headers (especially `Host`)
    - Path safety (prevents directory traversal)
    - Content-Type for POST requests
3. **Dispatch**: Based on the HTTP method, `ClientHandler` proceeds:
    - **GET**: Serves static HTML files or binary files (images, text) from the `resources` directory
    - **POST**: Validates JSON payload, generates unique filename, and saves to `uploads` directory
4. **Response**: `ResponseHandler` constructs the appropriate HTTP response with proper status codes:
    - `200 OK` - Successful GET
    - `201 Created` - Successful POST
    - `400 Bad Request` - Malformed request or invalid JSON
    - `403 Forbidden` - Path traversal or host mismatch
    - `404 Not Found` - Resource doesn't exist
    - `405 Method Not Allowed` - Unsupported HTTP method
    - `415 Unsupported Media Type` - Wrong Content-Type or file type
    - `500 Internal Server Error` - Server-side errors

### Security Measures

- **Path Traversal Protection**: `RequestHandler` validates all requested paths before file access:
    - Blocks `..`, `./`, `//`, and URL-encoded variants
    - Canonicalizes paths using Java's `Path.normalize()`
    - Ensures resolved paths stay within `resources` directory
    - Returns `403 Forbidden` for any violations

- **Host Header Validation**:
    - All HTTP/1.1 requests must include a valid `Host` header
    - Server validates the header matches its own address
    - Accepts `localhost`, `127.0.0.1`, or `0.0.0.0` variations
    - Returns `400 Bad Request` if missing, `403 Forbidden` if mismatched

- **Request Size Limiting**:
    - Maximum request size enforced at 8192 bytes
    - Prevents memory exhaustion attacks

- **Input Validation**:
    - JSON validation using Google's Gson library
    - File type restrictions (only HTML, TXT, PNG, JPG/JPEG)
    - Content-Type verification for POST requests

---

## Configuration

The server can be configured via command-line arguments:

```sh
java -cp target/myartifactid-0.0-SNAPSHOT.jar Main [port] [host] [thread_pool_size]
```

**Parameters:**

- **`port`**: The port number to bind to. (Default: `8080`)
- **`host`**: The host address to bind to. (Default: `127.0.0.1`)
- **`thread_pool_size`**: The number of worker threads. (Default: `10`)

**Examples:**

```sh
# Default configuration
java -cp target/myartifactid-0.0-SNAPSHOT.jar Main

# Custom port
java -cp target/myartifactid-0.0-SNAPSHOT.jar Main 9000

# Bind to all interfaces
java -cp target/myartifactid-0.0-SNAPSHOT.jar Main 8080 0.0.0.0

# Custom port, host, and 20 threads
java -cp target/myartifactid-0.0-SNAPSHOT.jar Main 8000 0.0.0.0 20
```

---

## Testing Checklist

Use the interactive test interface at `http://127.0.0.1:8080/test.html` to verify:

- ✅ GET / → Serves index.html
- ✅ GET /about.html → Serves HTML page
- ✅ GET /logo.png → Downloads PNG as binary
- ✅ GET /photo.jpg → Downloads JPEG as binary
- ✅ GET /sample.txt → Downloads text file as binary
- ✅ POST /upload (JSON) → Creates file, returns 201
- ❌ GET /nonexistent.html → Returns 404
- ❌ PUT /index.html → Returns 405
- ❌ DELETE /file.txt → Returns 405
- ❌ POST /upload (XML) → Returns 415
- ❌ POST /upload (invalid JSON) → Returns 400
- ❌ GET /document.pdf → Returns 415
- 🔒 GET /../etc/passwd → Returns 403
- 🔒 GET /../../sensitive.txt → Returns 403
- 🔒 GET //etc/hosts → Returns 403

## Author

**Vimal Kumar**

- GitHub: [@yamiSukehiro2907](https://github.com/yamiSukehiro2907)

---

## Acknowledgments

This project was built as part of a Computer Networks assignment to demonstrate understanding of:

- Low-level socket programming
- HTTP/1.1 protocol implementation
- Multi-threaded server architecture
- Network security best practices
- Concurrent programming patterns