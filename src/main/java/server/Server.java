package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends Thread {

    private final int PORT;

    private final String ipAddress;

    private final int MAX_REQUEST_SIZE = 4096;

    private static final String RESOURCES_FOLDER = "src/main/resources";

    private final int maxConnections;

    public Server(String ipAddress, int PORT, int maxConnections) {
        this.ipAddress = ipAddress;
        this.PORT = PORT;
        this.maxConnections = maxConnections;
    }


    private ServerSocket serverSocket;


    private void createServer() {
        try {
            this.serverSocket = new ServerSocket(PORT, maxConnections, InetAddress.getByName(ipAddress));
            System.out.println("Server socket created and created and bound to " + ipAddress + ":" + PORT);
        } catch (IOException e) {
            System.err.println("Error: Creating serverSocket: " + e.getMessage());
            System.exit(1);
        }
    }


    private void startServer() {

        startServer();
        ///  start the server

        Thread shutDownThread = createShutDownThread();
        ///  get the thread to run before shutting down the server....

        Runtime.getRuntime().addShutdownHook(shutDownThread);
        /// This thread when JVM will be ending or shutting down the server....


        while (!serverSocket.isClosed()) {
            try (Socket connection = serverSocket.accept(); ///  accept the connection
                 /// by using bufferedReader we can set the request size and all as well as it takes input from any type of source and it is thread-safe
                 BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                 OutputStream outputStream = connection.getOutputStream();) {

                /// create threads for each client to allow multiple client connection
                Thread connectionThread = new Thread(() -> {
                    handleClient(connection);
                });

                ///  start the thread
                connectionThread.start();

            } catch (IOException e) {
                System.err.println("Error handling the client connection: " + e.getMessage());
            }
        }

    }

    private Thread createShutDownThread() {
        return new Thread(() -> {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing the serverSocket: " + e.getMessage());
            }
        });
    }

    private void handleClient(Socket connection) {

    }
}