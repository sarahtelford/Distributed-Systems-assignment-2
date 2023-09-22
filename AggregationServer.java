import java.net.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AggregationServer {
    private static Map<Socket, String> serverIds = new ConcurrentHashMap<>();
    private static Map<Socket, Long> serverLastActiveTime = new ConcurrentHashMap<>();

    public static void main(String args[]) {
        int port;

        if (args.length != 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 4567;
        }

        try (ServerSocket socket = new ServerSocket(port)) {
            System.out.println("Aggregation Server started on port " + port);

            while (true) {
                // Accept incoming client connections
                Socket clientSocket = socket.accept();
                System.out.println("Received connection from client: " + clientSocket.getRemoteSocketAddress());

                // Generate a unique ID for the server
                String serverId = generateUniqueId();
                serverIds.put(clientSocket, serverId); // Store the server ID
      
                // Create a new thread to handle each client connection
                Thread clientHandlerThread = new Thread(() -> handleClient(clientSocket, serverId));
                clientHandlerThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void handleClient(Socket clientSocket, String serverId) {
        try (
            DataInputStream inputData = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream outputData = new DataOutputStream(clientSocket.getOutputStream())
        ) {
            String message = inputData.readUTF();
            System.out.println("Received message from content client: " + message);

            StringBuilder jsonDataBuilder = new StringBuilder();
            boolean parsingSuccessful = processData(message, jsonDataBuilder);

            if (parsingSuccessful) {
                // Parsing was successful, jsonDataBuilder now contains the JSON data
                String jsonData = jsonDataBuilder.toString();
                System.out.println("JSON Data: " + jsonData);
        
                boolean dataIsValid = true; 

                // Process the received data and determine the outcome
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
                    DataOutputStream ResponseOutputData = new DataOutputStream(clientSocket.getOutputStream());

                    ResponseOutputData.writeUTF(message);
                    ResponseOutputData.flush();
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
            clientSocket.close();
        }catch(IOException e)
    {
        e.printStackTrace();
    }
}

    // Generate a unique ID using UUID
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
            // Find the start of JSON data after "Lamport-Clock:"
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

    public static void storeData(String data, String serverId) {
        // Store the data in a file with a unique filename based on the server ID and timestamp
        try {
            String fileName = serverId + "_" + System.currentTimeMillis() + ".json";
            FileWriter fileWriter = new FileWriter(fileName);
            fileWriter.write(data);
            fileWriter.close();
        } catch (IOException e) {
            // Handle file I/O error
            System.err.println("Error storing data: " + e.getMessage());
        }
    }
}
