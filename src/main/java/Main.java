import server.Server;

public class Main {


    private static final int PORT = 8080;

    private static final String ipAddress = "127.0.0.1";

    private static final int maxConnections = 10;

    public static void main(String[] args) {

        Server server = new Server(ipAddress, PORT, maxConnections);

        server.start();
    }
}