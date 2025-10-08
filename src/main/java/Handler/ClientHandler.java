package Handler;

import helpers.Client;
import helpers.Logger;

import java.io.IOException;
import java.time.LocalDateTime;

public class ClientHandler {

    private final Client client;

    private final String RESOURCES_FOLDER;

    private final int MAX_REQUEST_SIZE;

    private final String serverAddress;

    private LocalDateTime prevRequestTime;

    public ClientHandler(Client client, String RESOURCES_FOLDER, int MAX_REQUEST_SIZE, String serverAddress) {
        this.client = client;
        this.RESOURCES_FOLDER = RESOURCES_FOLDER;
        this.MAX_REQUEST_SIZE = MAX_REQUEST_SIZE;
        this.serverAddress = serverAddress;
    }

    public void handle() {
        Logger.log("Connection from " + client.getClientAddress() + " is being served");
        while (!client.getConnection().isClosed()) {
            try {
                char[] buffer = new char[MAX_REQUEST_SIZE];
                int bytesRead = client.getBufferedReader().read(buffer, 0, MAX_REQUEST_SIZE);
                if(bytesRead == -1) continue;
                String requestString = new String(buffer , 0 , MAX_REQUEST_SIZE);


            } catch (IOException e) {
                Logger.error("Error accepting the request from client: " + e.getMessage());
            }

        }
    }
}
