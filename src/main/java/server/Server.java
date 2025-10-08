package server;

import Handler.ClientHandler;
import helpers.Client;
import helpers.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Server extends Thread {

    private final int PORT;
    private final String ipAddress;
    private final int MAX_REQUEST_SIZE = 8192;
    private static final String RESOURCES_FOLDER = "src/main/resources";
    /// it tells max how many connections can wait in the queue
    private final int maxConnections;
    private final int maxThreads;
    private final ExecutorService executorService;
    private final LinkedBlockingQueue<Client> clientQueue;
    private ServerSocket serverSocket;
    private volatile boolean running = true;

    private final AtomicInteger activeThreads = new AtomicInteger(0);

    public Server(String ipAddress, int PORT, int maxConnections, int maxThreads) {
        this.ipAddress = ipAddress;
        this.PORT = PORT;
        this.maxConnections = maxConnections;
        this.maxThreads = maxThreads;
        this.executorService = Executors.newFixedThreadPool(maxThreads);
        this.clientQueue = new LinkedBlockingQueue<>();
    }


    private void createServer() {
        try {
            this.serverSocket = new ServerSocket(PORT, maxConnections, InetAddress.getByName(ipAddress));
            Logger.log("HTTP Server started on http://" + ipAddress + ":" + PORT);
            Logger.log("Thread pool size: " + maxThreads);
            Logger.log("Serving files from '" + RESOURCES_FOLDER + "' directory");
            Logger.log("Press Ctrl+C to stop the server");
        } catch (IOException e) {
            Logger.error("Error creating serverSocket: " + e.getMessage());
            System.exit(1);
        }
    }


    private void startServer() {

        createServer();
        ///  start the server

        startQueueProcessor();
        /// start the thread that will be processing

        Thread shutDownThread = createShutDownThread();
        ///  get the thread to run before shutting down the server....

        Runtime.getRuntime().addShutdownHook(shutDownThread);
        /// This thread when JVM will be ending or shutting down the server....


        while (running && !serverSocket.isClosed()) {
            try {
                Socket connection = serverSocket.accept(); ///  accept the connection
                Client client = new Client(connection);
                clientQueue.offer(client);
                logThreadPoolStatus();
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting the client connection: " + e.getMessage());
                }
            }
        }

    }

    private void startQueueProcessor() {
        Thread queueProcessor = new Thread(() -> {
            while (running) {
                try {
                    Client client = clientQueue.take();
                    int active = activeThreads.get();
                    if (active >= maxThreads) {
                        Logger.log("Warning: No threads available , queuing connection");
                    }

                    executorService.submit(() -> {
                        activeThreads.incrementAndGet();
                        String threadName = Thread.currentThread().getName();

                        try {
                            Logger.logWithThread(threadName, "Connection dequeued. assigned to :" + threadName);

                            ClientHandler clientHandler = new ClientHandler(
                                    client,
                                    RESOURCES_FOLDER,
                                    MAX_REQUEST_SIZE,
                                    ipAddress + ":" + PORT
                            );

                            clientHandler.handle();
                        } catch (Exception e) {
                            Logger.errorWithThread(threadName, "Error handling client: " + e.getMessage());
                        } finally {
                            activeThreads.decrementAndGet();
                            client.close();
                        }

                    });
                } catch (InterruptedException e) {
                    if (running) {
                        Logger.error("Queue processor interrupted: " + e.getMessage());
                    }
                    break;
                }
            }
            ;
        }, "QueueProcessor");

        queueProcessor.setDaemon(true);
        queueProcessor.start();

    }

    private Thread createShutDownThread() {
        return new Thread(() -> {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
                running = false;

                executorService.shutdown();

                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }

                Logger.log("Server stopped successfully");
                Logger.close();

            } catch (IOException | InterruptedException e) {
                Logger.error("Error during shutdown: " + e.getMessage());
                Logger.close();
            }
        });
    }

    private void logThreadPoolStatus() {
        int active = activeThreads.get();
        int queued = clientQueue.size();

        if (active > 0 || queued > 0) {
            Logger.log("Active clients being served: " + active + " , Clients waiting : " + queued);
        }
    }
}