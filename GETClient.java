import java.net.*;
import java.io.*;

public class GETClient {
    public static void main(String args[]) {
        if (args.length < 1) {
            System.exit(1);
        }

        // Extract the server URL from command line arguments
        String clientUrl = args[0];
        String host = clientUrl.split(":")[0];
        int port = Integer.parseInt(clientUrl.split(":")[1]);

        // Create a Lamport clock instance to track request timestamps
        LamportClock lamportClock = new LamportClock();

        try (Socket socket = new Socket(host, port);
                DataOutputStream outputData = new DataOutputStream(socket.getOutputStream());
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Start a heartbeat thread to periodically send empty messages
            startHeartbeatThread(outputData);

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
    private static void startHeartbeatThread(DataOutputStream outputData) {
        Thread heartbeatThread = new Thread(() -> {
            try {
                while (true) {
                    String heartbeatMessage = "\r\n";
                    outputData.writeUTF(heartbeatMessage);
                    outputData.flush();
                    Thread.sleep(15000);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
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
    private static void sendGetRequest(DataOutputStream outputData, String host, int port, LamportClock lamportClock)
            throws IOException {
        String requestData = "GET /weather HTTP/1.1\r\n" + "Host: " + host + ":" + port + "\r\n" + "\r\n";
        lamportClock.increment();
        int currentClockValue = lamportClock.getValue();
        String request = currentClockValue + requestData;

        outputData.writeBytes(request);
        outputData.flush();
    }

    /**
     * Process and print the server's response.
     * 
     * @param inputReader The input reader to read the server's response.
     * @throws IOException If an I/O error occurs while reading the response.
     */
    private static void processServerResponse(BufferedReader inputReader) throws IOException {
        StringBuilder responseBuilder = new StringBuilder();
        String line;

        // Read the response line by line and build the complete response
        while ((line = inputReader.readLine()) != null) {
            responseBuilder.append(line).append("\n");
        }

        String serverResponse = responseBuilder.toString();
        System.out.println("Server Response:\n" + serverResponse);
    }
}