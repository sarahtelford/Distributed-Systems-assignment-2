import java.net.*;
import java.io.*;

public class GETClient {
    public static void main(String args[]) {
        String clientUrl = args[0];
        String host = clientUrl.split(":")[0];
        int port = Integer.parseInt(clientUrl.split(":")[1]);

       try (Socket socket = new Socket(host, port);
             DataOutputStream outputData = new DataOutputStream(socket.getOutputStream());
             DataInputStream inputData = new DataInputStream(socket.getInputStream())) {

            // Create a PUT request with the data you want to send
            String requestData = "GET /weather HTTP/1.1\r\n" + "Host: " + host + ":" + port + "\r\n" + "\r\n";
            // Send the PUT request to the content server
            outputData.writeUTF(requestData);
            outputData.flush();

            StringBuilder responseBuilder = new StringBuilder();
            String line;
            // while ((line = inputData.readLine()) != null) {
            //     responseBuilder.append(line).append("\n");
            // }

            String serverResponse = responseBuilder.toString();
            System.out.println("Server Response:\n" + serverResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}