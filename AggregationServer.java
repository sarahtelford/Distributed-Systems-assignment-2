import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import org.json.JSONObject;

public class AggregationServer {
    // Define data structures to store server IDs, last active server times, and recent weather data
    private static Map<Socket, String> serverIds = new ConcurrentHashMap<>();
    private static Map<Socket, Long> serverLastActiveTime = new ConcurrentHashMap<>();
    private static List<String> recentWeatherData = new ArrayList<>();
    private static final long CONNECTION_TIMEOUT = 30000; // Set connectin timeout to 30 seconds

    public static void main(String args[]) {
        // Take in the port number from the user input. If not defualt to 4567.
        int port;
        if (args.length != 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 4567;
        }

        // Start the thread for:
        // - cleaning up stale data
        // - managing client connections
        // - create a new server socket and handle client connections
        startDataCleanupThread();
        startConnectionManagerThread();
        createAndHandleClientConnections(port);
    }

    /**
     * Starts a dedicated thread for cleaning up stale data.
     */
    private static void startDataCleanupThread() {
        Thread dataCleanupThread = new Thread(() -> {
            while (true) {
                cleanupStaleData();
                try {
                    Thread.sleep(1000); // Sleep for 1 second before the next cleanup cycle.
                } catch (InterruptedException e) {
                    e.printStackTrace(); // Handle any interruption and print an error message.
                }
            }
        });
        dataCleanupThread.start();
    }

    /**
     * Starts a dedicated thread for managing client connections.
     */
    public static void startConnectionManagerThread() {
        Thread connectionManagerThread = new Thread(() -> {
            while (true) {
                manageConnections();
                try {
                    Thread.sleep(1000); // Sleep for 1 second before the next check connections cycle.
                } catch (InterruptedException e) {
                    e.printStackTrace(); // Handle any interruption and print an error message.
                }
            }
        });
        connectionManagerThread.start();
    }

    /**
     * Creates a new server socket and handles incoming client connections.
     *
     * @param port The port number on which the server socket listens for incoming
     *             connections.
     */
    public static void createAndHandleClientConnections(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            System.out.println("Aggregation Server started on port " + port);

            while (true) {
                // Accept incoming client connections
                Socket clientSocket = socket.accept();
                System.out.println("Received connection from client: " + clientSocket.getRemoteSocketAddress());

                // Generate a unique ID for the server and store data
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
                int maxUpdatesToRetain = 20;

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
            clientSocket.setSoTimeout(15000); // Set a timeout for 15 seconds

            if (message != null) {
                if (message.equals("Heartbeat")) {
                    // This is a heartbeat message (empty line), ignore it
                    System.out.println("Received heartbeat message.");
                    String response = "Heartbeat acknowledged.";
                    outputData.writeUTF(response);
                    outputData.flush();
                } else {
                    // Handle non-heartbeat message
                    System.out.println("Received message from client: " + message);

                    // Check if the request is for the latest weather data
                    if (message.startsWith("GET")) {
                        // Send the latest data as a response to the GET client
                        if (!recentWeatherData.isEmpty()) {
                            String latestData = recentWeatherData.get(recentWeatherData.size() - 1);

                            // Construct an HTTP response
                            StringBuilder responseBuilder = new StringBuilder();
                            responseBuilder.append("HTTP/1.1 200 OK\r\n");
                            responseBuilder.append("Content-Type: application/json\r\n");
                            responseBuilder.append("Content-Length: ").append(latestData.length()).append("\r\n");
                            responseBuilder.append("\r\n");
                            responseBuilder.append(latestData);

                            String response = responseBuilder.toString();
                            outputData.writeUTF(response);
                            outputData.flush();
                        } else {
                            // If there's no data available, return a 404 (Not Found) response
                            String response = "HTTP/1.1 404 Not Found\r\n\r\nNo weather data available.";
                            outputData.writeUTF(response);
                            outputData.flush();
                        }
                    } else {
                        // Processing data submission from content servers
                        StringBuilder jsonDataBuilder = new StringBuilder();
                        boolean parsingSuccessful = processData(message, jsonDataBuilder);

                        String jsonData = jsonDataBuilder.toString();

                        if (jsonData.trim().isEmpty()) {
                            // Empty JSON data received, send HTTP 204 No Content response
                            outputData.writeUTF("HTTP/1.1 204 No Content\r\n\r\nNo data received.");
                            outputData.flush();
                        } else {
                            // Process the received data and determine the response outcome
                            if (parsingSuccessful) {
                                boolean dataIsValid = true;

                                if (dataIsValid) {
                                    // Store the received data in recentWeatherData
                                    recentWeatherData.add(message);

                                    // Limit the number of stored data to a certain maximum
                                    int maxRecentDataCount = 10; // Adjust this value as needed
                                    if (recentWeatherData.size() > maxRecentDataCount) {
                                        recentWeatherData.remove(0); // Remove the oldest data if the list exceeds the
                                                                     // limit
                                    }

                                    storeData(message, serverId);
                                    if (!serverLastActiveTime.containsKey(clientSocket)) {
                                        outputData.writeUTF("HTTP/1.1 201 Created\r\n\r\nData received and stored.");
                                        outputData.flush();
                                    } else {
                                        outputData.writeUTF(
                                                "HTTP/1.1 200 OK\r\n\r\nData received and processed successfully.");
                                        outputData.flush();
                                    }
                                } else {
                                    // Invalid data or JSON parsing error, send HTTP 500 Internal Server Error
                                    // response
                                    outputData.writeUTF(
                                            "HTTP/1.1 500 Internal Server Error\r\n\r\nInternal server error occurred.");
                                    outputData.flush();
                                }
                                serverLastActiveTime.put(clientSocket, System.currentTimeMillis());
                            } else {
                                // Request is not a valid GET or PUT, send HTTP 400 Bad Request response
                                outputData.writeUTF("HTTP/1.1 400 Bad Request\r\n\r\nInvalid request received.");
                                outputData.flush();
                            }
                        }
                        clientSocket.close();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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

    /**
     * Manages client connections by checking for idle connections and cleaning up
     * stale data.
     */
    public static void manageConnections() {
        long currentTime = System.currentTimeMillis();
        List<Socket> socketsToClose = new ArrayList<>();

        // The client connection is idle for too long, mark it for closure
        for (Socket clientSocket : serverLastActiveTime.keySet()) {
            long lastActiveTime = serverLastActiveTime.get(clientSocket);
            if (currentTime - lastActiveTime > CONNECTION_TIMEOUT) {
                socketsToClose.add(clientSocket);
            }
        }

        // Close idle connections and perform cleanup
        for (Socket socketToClose : socketsToClose) {
            try {
                String serverId = serverIds.get(socketToClose);

                System.out.println("Closing idle connection with client: " + socketToClose.getRemoteSocketAddress());
                serverIds.remove(socketToClose);
                serverLastActiveTime.remove(socketToClose);
                socketToClose.close();

                cleanupClientFiles(serverId);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Perform cleanup of files generated or related to the closed client here.
     *
     * @param serverId The unique identifier of the closed client server.
     */
    public static void cleanupClientFiles(String serverId) {
        // Define the data directory where client files are stored
        String dataDirectory = "data/";

        // List all files in the data directory
        File[] files = new File(dataDirectory).listFiles();

        if (files != null) {
            for (File file : files) {
                // Check if the file's name contains the serverId
                if (file.getName().contains(serverId)) {
                    if (file.delete()) {
                        System.out.println("Deleted client-specific data file: " + file.getName());
                    } else {
                        System.err.println("Failed to delete client-specific data file: " + file.getName());
                    }
                }
            }
        }
    }
}