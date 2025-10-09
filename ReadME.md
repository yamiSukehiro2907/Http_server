# Multi-threaded HTTP Server in Java

![Java](https://img.shields.io/badge/language-Java-blue.svg?style=for-the-badge&logo=java)
![Maven](https://img.shields.io/badge/build-Maven-red.svg?style=for-the-badge&logo=apache-maven)
![License](https://img.shields.io/badge/license-MIT-green.svg?style=for-the-badge)

A multi-threaded HTTP/1.1 server built from scratch using low-level Java socket programming. This
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
    - [Testing with `curl`](#testing-with-curl)
    - [Concurrency Testing](#concurrency-testing)
- [Technical Deep Dive](#technical-deep-dive)
    - [Thread Pool Architecture](#thread-pool-architecture)
    - [Request Handling Pipeline](#request-handling-pipeline)
    - [Security Measures](#security-measures)
- [Configuration](#configuration)
- [License](#license)

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
│   │   │   └── Main.java             # Application entry point
│   │   └── resources/
│   │       ├── uploads/                  # Directory for POST uploads
│   │       ├── about.html
│   │       ├── contact.html
│   │       ├── index.html
│   │       ├── logo.png
│   │       ├── photo.jpg
│   │       └── sample.txt
│   └── test/
│       └── test_server.ps1               # PowerShell test script
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
      java -jar http_server.jar
      ```
    - To run with custom settings (e.g., port 8000, any host, 20 threads):
      ```sh
      java -jar http_server.jar 8000 0.0.0.0 20
      ```

---

## Usage & Testing

Once the server is running, you can interact with it using a browser, `curl`, or the provided test script.

### Testing with `curl`

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

### Concurrency Testing

Use a tool like Apache Bench (`ab`) to simulate concurrent connections.

```sh
ab -n 100 -c 10 http://localhost:8080/index.html
```

You can also use the **`test_server.ps1`** PowerShell script located in the `src/test` directory for automated testing
scenarios.

---

## Technical Deep Dive

### Thread Pool Architecture

The `Server.java` class initializes a fixed-size thread pool. It runs an infinite loop to accept incoming TCP
connections. Each accepted client `Socket` is wrapped in a `Client` object and handed off as a task to the thread pool.
This design decouples connection acceptance from request processing, allowing the server to remain responsive to new
clients even while handling long-running requests.

### Request Handling Pipeline

For each client connection, a `ClientHandler` instance is executed by a worker thread. The process is as follows:

1. **Read & Parse**: `ClientHandler` reads the raw request from the socket's input stream.
2. **Validation**: `RequestHandler` parses the raw string into a structured `HttpRequest` DTO. It validates the request
   format, method, headers (like `Host`), and path safety.
3. **Dispatch**: Based on the HTTP method, `ClientHandler` proceeds:
    - **GET**: It serves static or binary files from the `resources` directory.
    - **POST**: It processes the JSON body and saves it to the `uploads` directory.
4. **Response**: `ResponseHandler` constructs the appropriate HTTP response (e.g., `200 OK`, `201 Created`,
   `404 Not Found`) and writes it to the socket's output stream.

### Security Measures

- **Path Traversal**: `RequestHandler` canonicalizes all requested file paths and strictly ensures they resolve to a
  location *within* the `resources` folder. Any request containing `..` or attempting to access a parent directory is
  rejected with a `403 Forbidden` error.
- **Host Header Validation**: All requests are required to have a `Host` header that matches the server's own address.
  This is a key requirement of HTTP/1.1 and helps prevent certain types of attacks.

---

## Configuration

The server can be configured via command-line arguments:

```sh
java -jar http_server.jar [port] [host] [thread_pool_size]
```

- **`port`**: The port number to bind to. (Default: `8080`)
- **`host`**: The host address to bind to. (Default: `127.0.0.1`)
- **`thread_pool_size`**: The number of worker threads. (Default: `10`)

---

## License

This project is licensed under the MIT License.