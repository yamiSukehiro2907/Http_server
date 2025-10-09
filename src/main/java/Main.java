import server.Server;

public class Main {

    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_IP_ADDRESS = "127.0.0.1";
    private static final int DEFAULT_MAX_CONNECTIONS = 50;
    private static final int DEFAULT_MAX_THREADS = 10;

    public static void main(String[] args) {
        // Parse command-line arguments
        // Usage: java Main [port] [ipAddress] [maxThreads]
        // Example: java Main 8000 0.0.0.0 20

        int port = DEFAULT_PORT;
        String ipAddress = DEFAULT_IP_ADDRESS;
        int maxThreads = DEFAULT_MAX_THREADS;
        int maxConnections = DEFAULT_MAX_CONNECTIONS;

        try {
            if (args.length > 0) {
                port = Integer.parseInt(args[0]);
            }
            if (args.length > 1) {
                ipAddress = args[1];
            }
            if (args.length > 2) {
                maxThreads = Integer.parseInt(args[2]);
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid arguments. Usage: java Main [port] [ipAddress] [maxThreads]");
            System.err.println("Example: java Main 8000 0.0.0.0 20");
            System.exit(1);
        }

        if (port < 1 || port > 65535) {
            System.err.println("Error: Port must be between 1 and 65535");
            System.exit(1);
        }

        if (maxThreads < 1 || maxThreads > 1000) {
            System.err.println("Error: maxThreads must be between 1 and 1000");
            System.exit(1);
        }

        System.out.println("Starting server with configuration:");
        System.out.println("  Port: " + port);
        System.out.println("  IP Address: " + ipAddress);
        System.out.println("  Max Threads: " + maxThreads);
        System.out.println("  Max Connections Queue: " + maxConnections);
        System.out.println();

        Server server = new Server(ipAddress, port, maxConnections, maxThreads);
        server.start();
    }
}