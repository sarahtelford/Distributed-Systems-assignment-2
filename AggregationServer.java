import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import org.json.JSONObject;

public class AggregationServer {
    // Define data structures to store server IDs, last active server times, and
    // recent weather data
    private static Map<Socket, String> serverIds = new ConcurrentHashMap<>();
    private static Map<Socket, Long> serverLastActiveTime = new ConcurrentHashMap<>();
    private static List<String> recentWeatherData = new ArrayList<>();

    public static void main(String args[]) {
        int port;

        // Take in the port number from the user input. If not defualt to 4567.
        if (args.length != 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 4567;
        }

        // Thread for cleaning up stale data.
        // Checks for updated data files every second.
        Thread dataCleanupThread = new Thread(() -> {
            while (true) {
                cleanupStaleData();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        dataCleanupThread.start();

        // Thread for creating a new server socket.
        try (ServerSocket socket = new ServerSocket(port)) {
            System.out.println("Aggregation Server started on port " + port);

            while (true) {
                // Accept incoming client connections
                Socket clientSocket = socket.accept();
                System.out.println("Received connection from client: " + clientSocket.getRemoteSocketAddress());

                // Generate a unique ID for the server and store dat
                String serverId = generateUniqueId();
                serverIds.put(clientSocket, serverId);

                // Create a new thread to handle each client connection
                Thread clientHandlerThread = new Thread(() -> handleClient(clientSocket, serverId));
                clientHandlerThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Cleans up stale data by deleting the oldest data files.
     * 
     */
    public static void cleanupStaleData() {
        try {
            // Define the directory where data files are stored
            String dataDirectory = "data/";

            // List all files in the data directory
            File[] files = new File(dataDirectory).listFiles();

            if (files != null) {
                // Maximum number of recent updates retained
                int maxUpdatesToRetain = 4;

                // Sort files by last modified timestamp in ascending order (oldest first)
                Arrays.sort(files, (file1, file2) -> Long.compare(file1.lastModified(), file2.lastModified()));

                // Determine the number of files to delete to retain the most recent updates
                int filesToDelete = Math.max(0, files.length - maxUpdatesToRetain);

                // Delete the oldest files (stale data)
                for (int i = 0; i < filesToDelete; i++) {
                    if (files[i].delete()) {
                        System.out.println("Deleted stale data file: " + files[i].getName());
                    } else {
                        System.err.println("Failed to delete stale data file: " + files[i].getName());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles incoming client connections, processes requests, and sends responses.
     *
     * @param clientSocket The socket representing the client connection.
     * @param serverId     The unique ID of the server handling the client
     *                     connection.
     */
    public static void handleClient(Socket clientSocket, String serverId) {
        try (
                DataInputStream inputData = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream outputData = new DataOutputStream(clientSocket.getOutputStream())) {
            String message = inputData.readUTF();
            System.out.println("Received message from client: " + message);

            // Check if the request is for the latest weather data
            if (message.startsWith("GET /latest-weather")) {
                // Send the latest data as a response to the GET client
                if (!recentWeatherData.isEmpty()) {
                    String latestData = recentWeatherData.get(recentWeatherData.size() - 1);
                    outputData.writeUTF(latestData);
                    outputData.flush();
                } else {
                    outputData.writeUTF("No weather data available.");
                    outputData.flush();
                }
            } else {
                // Processing data submission from content servers
                StringBuilder jsonDataBuilder = new StringBuilder();
                boolean parsingSuccessful = processData(message, jsonDataBuilder);

                // Process the received data and determine the response outcome
                if (parsingSuccessful) {
                    boolean dataIsValid = true;

                    if (dataIsValid) {
                        storeData(message, serverId);
                        if (!serverLastActiveTime.containsKey(clientSocket)) {
                            outputData.writeUTF("HTTP/1.1 201 Created\r\n\r\nData received and stored.");
                            outputData.flush();
                        } else {
                            outputData.writeUTF("HTTP/1.1 200 OK\r\n\r\nData received and processed successfully.");
                            outputData.flush();
                        }

                        // Create a new output stream for the client
                        DataOutputStream responseOutputData = new DataOutputStream(clientSocket.getOutputStream());

                        responseOutputData.writeUTF(message);
                        responseOutputData.flush();
                    } else if (message.isEmpty()) {
                        // No data received, send HTTP 204 No Content response
                        outputData.writeUTF("HTTP/1.1 204 No Content\r\n\r\nNo data received.");
                        outputData.flush();
                    } else {
                        // Invalid data or JSON parsing error, send HTTP 500 Internal Server Error response
                        outputData.writeUTF("HTTP/1.1 500 Internal Server Error\r\n\r\nInternal server error occurred.");
                        outputData.flush();
                    }
                    serverLastActiveTime.put(clientSocket, System.currentTimeMillis());
                } else {
                    // Request is not a valid GET or PUT, send HTTP 400 Bad Request response
                    outputData.writeUTF("HTTP/1.1 400 Bad Request\r\n\r\nInvalid request received.");
                    outputData.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                // Close the client socket
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Generates a unique ID using Universally Unique Identifier (UUID).
     *
     * @return A unique ID represented as a string.
     */
    public static String generateUniqueId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Process the received data from a content server and
     * extract the JSON data from the message.
     *
     * @param data The data received in JSON format.
     * @return true if the data is valid, false otherwise.
     */
    public static boolean processData(String data, StringBuilder jsonDataBuilder) {
        try {
            int jsonStartIndex = data.indexOf("{", data.indexOf("Lamport-Clock:"));
            if (jsonStartIndex == -1) {
                return false;
            }

            String jsonData = data.substring(jsonStartIndex);
            JSONObject jsonObject = new JSONObject(jsonData);
            jsonDataBuilder.append(jsonObject.toString());

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Stores received data in a data directory with a unique filename based on the
     * server ID and timestamp.
     *
     * @param data     The data to be stored.
     * @param serverId The unique identifier of the server associated with the data.
     */
    public static void storeData(String data, String serverId) {
        String dataDirectory = "data/";

        // Create the data directory if it doesn't exist
        File directory = new File(dataDirectory);
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                System.out.println("Data directory created: " + dataDirectory);
            } else {
                System.err.println("Failed to create data directory: " + dataDirectory);
                return;
            }
        }

        // Store the data in a file with a unique filename based on the server ID and
        // timestamp
        try {
            String fileName = dataDirectory + serverId + "_" + System.currentTimeMillis() + ".json";
            FileWriter fileWriter = new FileWriter(fileName);
            fileWriter.write(data);
            fileWriter.close();
        } catch (IOException e) {
            System.err.println("Error storing data: " + e.getMessage());
        }
    }
}