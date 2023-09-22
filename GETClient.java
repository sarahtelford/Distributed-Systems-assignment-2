import java.net.*;
import java.io.*;

public class GETClient {
    public static void main(String args[]) {
        String clientUrl = args[0];
        String host = clientUrl.split(":")[0];
        int port = Integer.parseInt(clientUrl.split(":")[1]);

        System.out.println(host);
        System.out.println(port);

        try {
            Socket socket = new Socket(host, port);

            // Create an input stream to receive data from the server
            DataInputStream inputFromServer = new DataInputStream(socket.getInputStream());

            // Read the response from the server
            String serverResponse = inputFromServer.readUTF();
            
            // Print the response received from the server
            System.out.println("Server Response:\n" + serverResponse);

            // Close the socket
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}