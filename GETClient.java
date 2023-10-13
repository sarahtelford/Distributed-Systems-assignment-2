import java.net.*;
import java.io.*;

public class GETClient {
    // Create a Lamport clock instance
    private static LamportClock lamportClock = new LamportClock();
    public static void main(String args[]) {
        if (args.length < 1) {
            System.exit(1);
        }

        // Extract the server URL from command line arguments
        String clientUrl = args[0];
        String host = clientUrl.split(":")[0];
        int port = Integer.parseInt(clientUrl.split(":")[1]);

        // Open a socket connection to the server
        try (Socket socket = new Socket(host, port);
                DataOutputStream outputData = new DataOutputStream(socket.getOutputStream());
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Heartbeat has been commented out as was causing issues. 
            // startHeartbeatThread(outputData);
            // Send a GET request to the server
            sendGetRequest(outputData, host, port, lamportClock);

            // Process the server's response
            processServerResponse(inputReader);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Start a thread to periodically send heartbeat messages over the provided
     * output stream.
     * 
     * @param outputData The output stream to send heartbeat messages.
     */
    public static void startHeartbeatThread(DataOutputStream outputData) {
        Thread heartbeatThread = new Thread(() -> {
            try {
                while (true) {
                    // Define an empty heartbeat message
                    String heartbeatMessage = "Heartbeat"; 
                
                    // Write the message to the output stream and flush it
                    outputData.writeUTF(heartbeatMessage);
                    outputData.flush();

                    System.out.println("Heartbeat sent");
                    // Sleep for 20 seconds before sending the next heartbeat
                    Thread.sleep(20000);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
        // Start the heartbeat thread
        heartbeatThread.start();
    }
    
    /**
     * Send a GET request to the server with Lamport clock information.
     * 
     * @param outputData   The output stream to send the GET request.
     * @param host         The host address of the server.
     * @param port         The port number of the server.
     * @param lamportClock The Lamport clock used to timestamp the request.
     * @throws IOException If an I/O error occurs while sending the request.
     */

    public static void sendGetRequest(DataOutputStream outputData, String host, int port, LamportClock lamportClock)
        throws IOException {

        // Construct the request message
        String requestData = "GET /weather HTTP/1.1\r\n" + "Host: " + host + ":" + port + "\r\n" +
                            "Lamport-Clock: " + lamportClock.getValue() + "\r\n\r\n";

        // Write the request to the output stream and flush it
        outputData.writeUTF(requestData);
        outputData.flush();

        // Increment the Lamport clock to timestamp the request
        lamportClock.increment();

        // Print out the request sent to the server
        System.out.println("Request sent:\r\n" + requestData);
    }

    /**
     * Process and print the server's response.
     * 
     * @param inputReader The input reader to read the server's response.
     * @throws IOException If an I/O error occurs while reading the response.
     */
    public static void processServerResponse(BufferedReader inputReader) throws IOException {
        StringBuilder responseBuilder = new StringBuilder();
        String line;
    
        // Read the response line by line and build the response
        while ((line = inputReader.readLine()) != null) {
            responseBuilder.append(line).append("\n");
        }
    
        String serverResponse = responseBuilder.toString();

        // Find the content within the curly braces {}
        int startIndex = serverResponse.indexOf("{");
        int endIndex = serverResponse.lastIndexOf("}");
    
        if (startIndex >= 0 && endIndex >= 0 && endIndex > startIndex) {
            String jsonResponse = serverResponse.substring(startIndex, endIndex + 1);
            // Split the JSON object into key-value pairs and add a new line after each pair
            String[] keyValuePairs = jsonResponse.split(",");
            StringBuilder formattedJson = new StringBuilder();
            for (int i = 0; i < keyValuePairs.length; i++) {
                String pair = keyValuePairs[i].trim();
                formattedJson.append(pair);
                if (i < keyValuePairs.length - 1) {
                    formattedJson.append(",\n");
                }
            }
        
            System.out.println("Server Response:\n" + formattedJson);
        }
    }
}