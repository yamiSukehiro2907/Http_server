package helpers;

import lombok.Getter;
import lombok.Setter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;


@Getter
@Setter

public class Client {

    private final BufferedReader bufferedReader;

    private final OutputStream outputStream;

    private final Socket connection;

    public Client(Socket connection) throws IOException {
        this.connection = connection;
        this.bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        this.outputStream = connection.getOutputStream();
    }

    public String getClientAddress() {
        return connection.getInetAddress().getHostAddress() + ":" + connection.getPort();
    }

    public void close() {
        try {
            if (bufferedReader != null) bufferedReader.close();
            if (outputStream != null) outputStream.close();
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (IOException e) {
            System.err.println("Error closing the client connection: " + e.getMessage());
        }
    }
}

