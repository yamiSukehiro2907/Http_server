package server;

public class Server {

  private final int PORT;

  private final String ipAddress;

  private final int MAX_REQUEST_SIZE;

  private static final String RESOURCES_FOLDER = "src/main/resources";

  private static final int maxConnections = 5;

  public Server(String ipAddress, int PORT, int MAX_REQUEST_SIZE) {
    this.ipAddress = ipAddress;
    this.PORT = PORT;
    this.MAX_REQUEST_SIZE = MAX_REQUEST_SIZE;
  }


  private ServerSocket serverSocket;


  private void startServer(){
    this.serverSocket = new ServerSocket();
  }
  

}