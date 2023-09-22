import java.net.*;
import java.util.*;
import java.io.*;

public class ContentServer {
    private static final Map<Socket, Long> serverLastActiveTime = new HashMap<>();

    public static void main(String args[]) {
        String serverUrl = args[0];
        String feedFilePath = args[1];
        int lamportClock = 0;

        boolean sentSuccessfully = sendDataToServer(serverUrl, feedFilePath, lamportClock);

        if (!sentSuccessfully) {
            System.out.println("Failed to send data after retries.");
        }
    }

    /**
     * Sends data to the server with retries in case of failure.
     * @param serverUrl The URL of the server.
     * @param feedFilePath The path to the feed file.
     * @param lamportClock The Lamport clock value.
     * @return True if data was sent successfully, false otherwise.
     */
    private static boolean sendDataToServer(String serverUrl, String feedFilePath, int lamportClock) {
        int maxRetries = 3; // Maximum number of retries

        while (maxRetries > 0) {
            try (Socket s = createSocket(serverUrl)) {
                DataInputStream inputData = new DataInputStream(s.getInputStream());
                DataOutputStream outputData = new DataOutputStream(s.getOutputStream());

                String jsonData = convertToJson(feedFilePath);
                if (jsonData == null) {
                    System.out.println("Failed to convert feed to JSON.");
                    return false;
                }

                sendDataToServer(outputData, jsonData, lamportClock);

                String serverResponse = inputData.readUTF();
                System.out.println("Server Response: " + serverResponse);

                s.close();

                if (serverResponse.startsWith("HTTP/1.1 200") || serverResponse.startsWith("HTTP/1.1 201")) {
                    return true;
                } else {
                    // Server did not respond with a success status, retry
                    maxRetries--;
                    System.out.println("Retrying... " + maxRetries + " retries left.");
                    Thread.sleep(1000); // Wait for a while before retrying (adjust as needed)
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Creates a socket and connects to the server.
     * @param serverUrl The URL of the server.
     * @return The created socket.
     * @throws IOException If there is an issue creating the socket.
     */
    private static Socket createSocket(String serverUrl) throws IOException {
        String host = serverUrl.split(":")[0];
        int port = Integer.parseInt(serverUrl.split(":")[1]);

        Socket s = new Socket(host, port);
        System.out.println("Socket created");

        serverLastActiveTime.put(s, System.currentTimeMillis());

        return s;
    }

    /**
     * Converts the feed file to JSON format.
     * @param feedFilePath The path to the feed file.
     * @return The JSON representation of the feed data, or null if there was an issue.
     */
    private static String convertToJson(String feedFilePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(feedFilePath))) {
            StringBuilder jsonDataBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    if (jsonDataBuilder.length() > 0) {
                        jsonDataBuilder.append(",");
                    }

                    jsonDataBuilder.append("\"").append(key).append("\":\"").append(value).append("\"");
                }
            }

            return "{" + jsonDataBuilder.toString() + "}";
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Sends data to the server using the provided output stream.
     * @param outputData The output stream for sending data.
     * @param jsonData The JSON data to send.
     * @param lamportClock The Lamport clock value.
     * @throws IOException If there is an issue sending the data.
     */
    private static void sendDataToServer(DataOutputStream outputData, String jsonData, int lamportClock) throws IOException {
        String requestData = "PUT /weather_data.txt HTTP/1.1\r\nUser-Agent: // ATOMClient/1/0\r\nContent-Type: text/Json\r\nContent-Length: "
                + jsonData.length() + "\r\nLamport-Clock: " + lamportClock + "\r\n" + jsonData;

        outputData.writeUTF(requestData);
        outputData.flush();
    }
}
