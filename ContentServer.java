import java.net.*;
import java.io.*;

public class ContentServer {
    // Variable to store LamportClock instance
    private static LamportClock lamportClock = new LamportClock();

    public static void main(String args[]) {
        // Spilt up the URL and file path from input
        String serverUrl = args[0];
        String feedFilePath = args[1];

        boolean sentSuccessfully = sendDataToServer(serverUrl, feedFilePath, lamportClock);

        if (!sentSuccessfully) {
            System.out.println("Failed to send data after retries.");
        }
    }

    /**
     * Sends data to the server with retries in case of failure.
     * 
     * @param serverUrl    The URL of the server.
     * @param feedFilePath The path to the feed file.
     * @param lamportClock The Lamport clock value.
     * @return True if data was sent successfully, false otherwise.
     */
    public static boolean sendDataToServer(String serverUrl, String feedFilePath, LamportClock lamportClock) {
        // Set max number or retries
        int maxRetries = 3;

        // Keep retrying to sending the data until threshold reached
        while (maxRetries > 0) {
            try (Socket s = createSocket(serverUrl)) {
                // Get the current Lamport clock value
                int currentClockValue = lamportClock.getValue();

                // Attempt to send weather data to the server
                boolean success = sendWeatherData(s, feedFilePath, currentClockValue);

                // If data was sent successfully, return true
                if (success) {
                    return true;
                }

                // In case of failure, retry sending data after 5 seconds.
                System.out.println("Failed to send data to server.");
                maxRetries--;
                System.out.println("Retrying... " + maxRetries + " retries left.");
                Thread.sleep(5000);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Sends weather data to the server over the given socket.
     * 
     * @param s                 The socket to communicate with the server.
     * @param feedFilePath      The path to the feed file.
     * @param lamportClockValue The current Lamport clock value.
     * @param lamportClock      The Lamport clock instance for synchronization.
     * @return True if the data was sent successfully, false otherwise.
     * @throws IOException If there is an issue with input/output operations.
     */
    public static boolean sendWeatherData(Socket s, String feedFilePath, int lamportClockValue) throws IOException {
        DataInputStream inputData = new DataInputStream(s.getInputStream());
        DataOutputStream outputData = new DataOutputStream(s.getOutputStream());

        // Convert the text file to JSON format
        String jsonData = convertToJson(feedFilePath);

        // Print to console if error has occoured in conversion
        if (jsonData == null) {
            System.out.println("Failed to convert feed to JSON.");
            return false;
        }

        // Constriction of PUT request for aggregation server
        String requestData = "\r\nPUT /weather_data.txt HTTP/1.1\r\nUser-Agent: // ATOMClient/1/0\r\nContent-Type: text/Json\r\nContent-Length: "
                + jsonData.length() + "\r\nLamport-Clock: " + lamportClockValue + "\r\n" + jsonData;

        // Write the request to the output stream and flush it to ensure data is sent
        // immediately
        outputData.writeUTF(requestData);
        outputData.flush();

        // Increment the Lamport clock
        lamportClock.increment();

        // Read the response from the server and print to the console
        String serverResponse = inputData.readUTF();
        System.out.println("Server Response: " + serverResponse);

        // Check if the server response indicates success
        return serverResponse.startsWith("HTTP/1.1 200") || serverResponse.startsWith("HTTP/1.1 201");
    }

    /**
     * Creates a socket and connects to the server.
     * 
     * @param serverUrl The URL of the server.
     * @return The created socket.
     * @throws IOException If there is an issue creating the socket.
     */
    public static Socket createSocket(String serverUrl) throws IOException {
        // Extract the host and port number from the server URL
        String host = serverUrl.split(":")[0];
        int port = Integer.parseInt(serverUrl.split(":")[1]);

        // Create a socket and connect to the server using the host and port
        Socket s = new Socket(host, port);

        return s;
    }

    /**
     * Converts the feed file to JSON format whilst checking that the input data is
     * vaild
     * 
     * @param feedFilePath The path to the feed file.
     * @return The JSON representation of the feed data, or null if there was an
     *         issue.
     */
    public static String convertToJson(String feedFilePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(feedFilePath))) {
            StringBuilder jsonDataBuilder = new StringBuilder(); // Initialize a StringBuilder to build the JSON data
            String line;
            boolean firstEntry = true;
            boolean validID = false;

            // Read a line from the input file, and continue while there are more lines
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    // Extract the key and value parts
                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    // Add a comma to separate JSON entries, if needed
                    if (jsonDataBuilder.length() > 0) {
                        jsonDataBuilder.append(",");
                    }

                    // Add the opening brace for the JSON object
                    if (firstEntry) {
                        jsonDataBuilder.append("{");
                        firstEntry = false;
                    }

                    // Indicate that the 'id' key exists in the input
                    if (!validID && key.equals("id")) {
                        validID = true;
                    }
                    // Build a JSON key-value pair
                    jsonDataBuilder.append("\"").append(key).append("\":\"").append(value).append("\"");
                }
            }

            // Print an error message if 'id' key is missing
            if (!validID) {
                System.err.println("Error: The first key is not 'id'.");
                return null;
            }

            // Add the closing brace for the JSON object
            if (!firstEntry) {
                jsonDataBuilder.append("}");
            }

            return jsonDataBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}