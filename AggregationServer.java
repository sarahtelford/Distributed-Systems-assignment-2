// A Java program for a Server
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import org.json.JSONObject;

public class AggregationServer
{
    private static Map<Socket, Long> serverLastActiveTime = new ConcurrentHashMap<>();
    private static final int MAX_INACTIVE_TIME = 30000; 
	public static void main(String args[])
	{
        int port;

        if (args.length != 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 4567;
        }

        try (ServerSocket socket = new ServerSocket(port)) {
            System.out.println("Aggregation Server started on port " + port);

            while (true) {
                Socket clientSocket = socket.accept();
                System.out.println("Received connection from client: " + clientSocket.getRemoteSocketAddress());

                // Create a new thread to handle the client connection
                Thread clientHandlerThread = new Thread(() -> handleClient(clientSocket));
                clientHandlerThread.start();

                // if (isServerConnection(clientSocket)) {
                //     Thread serverHandlerThread = new Thread(() -> handleServer(clientSocket));
                //     serverHandlerThread.start();
                // }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

        // try{
        //     ServerSocket socket = new ServerSocket(port);
        //     Socket s = socket.accept();

        //     DataInputStream inputData = new DataInputStream(s.getInputStream());
        //     DataOutputStream  outputData = new DataOutputStream(s.getOutputStream());

        //     String message = (String)inputData.readUTF();
    
        //     System.out.println("Aggregation Server started on port "+ port);

        //     System.out.println("Received message from content server: " + message);

        //     // Check if the received message is a valid GET or PUT request
        //     if (isGetOrPutRequest(message)) {
        //         // Process the received data and determine the outcome
        //         boolean dataIsValid = processData(message);

        //         // if (dataIsValid) {
        //             storeData(message);
        //             if (!serverLastActiveTime.containsKey(s)) {
        //                 outputData.writeUTF("HTTP/1.1 201 Created\r\n\r\nData received and stored.");
        //                 outputData.flush();
        //             } else {
        //                 outputData.writeUTF("HTTP/1.1 200 OK\r\n\r\nData received and processed successfully.");
        //                 outputData.flush();
        //             }

        //                 // Create a new output stream for the client
        //             DataOutputStream clientOutputData = new DataOutputStream(s.getOutputStream());

        //             clientOutputData.writeUTF(message);
        //             clientOutputData.flush();

        //         // } else if (message.isEmpty()) {
        //         //     // No data received, send HTTP 204 No Content response
        //         //     outputData.writeUTF("HTTP/1.1 204 No Content\r\n\r\nNo data received.");
        //         //     outputData.flush();
                    
        //         // } else {
        //         //     // Invalid data or JSON parsing error, send HTTP 500 Internal Server Error response
        //         //     outputData.writeUTF("HTTP/1.1 500 Internal Server Error\r\n\r\nInternal server error occurred.");
        //         //     outputData.flush(); 
        //         // }
        //         // serverLastAcftiveTime.put(conn, System.currentTimeMillis());
        //     } else {
        //         // Request is not a valid GET or PUT, send HTTP 400 Bad Request response
        //         outputData.writeUTF("HTTP/1.1 400 Bad Request\r\n\r\nInvalid request received.");
        //         outputData.flush(); 
        //     }
        //     socket.close();
        // }
        //     catch(
        //     IOException e){e.printStackTrace();
        //     }
        // }


        private static void handleClient(Socket clientSocket) {
            try (
                DataInputStream inputData = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream outputData = new DataOutputStream(clientSocket.getOutputStream())
            ) {
                String message = inputData.readUTF();
                System.out.println("Received message from client: " + message);
    
                // Handle client request here (similar to previous code)
                outputData.writeUTF(message);
                outputData.flush();
        
    
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // public static void removeInactiveServers() {
        //     long currentTime = System.currentTimeMillis();
        //     for (Socket conn : serverLastActiveTime.keySet()) {
        //         long lastActiveTime = serverLastActiveTime.get(conn);
        //         if (currentTime - lastActiveTime > MAX_INACTIVE_TIME) {
        //             System.out.println("Removing inactive content server: " + conn.getRemoteSocketAddress());
        //             serverLastActiveTime.remove(conn);
        //             conn.close();
        //         }
        //         catch(IOException e){e.printStackTrace();
        //         }
              
        //     }
        // }
    
        /**
         * Process the received data from a content server.
         *
         * @param data The data received in JSON format.
         * @return true if the data is valid, false otherwise.
         */
        public static boolean processData(String data) {
            try {
                // Parse the received data as JSON
                JSONObject jsonData = new JSONObject(data);
    
                // Check if the JSON object contains the required fields
                if (jsonData.has("id") && jsonData.has("air_temp") && jsonData.has("wind_spd_kt")) {
                    // Data is valid
                    return true;
                } else {
                    // Data is missing required fields
                    return false;
                }
            } catch (Exception e) {
                // JSON parsing or other exception occurred, data is invalid
                return false;
            }
        }
    
        /**
         * Checks if the given message represents a valid GET or PUT request.
         *
         * @param message The message to be checked.
         * @return True if the message is a valid GET or PUT request, otherwise false.
         */
        public static boolean isGetOrPutRequest(String message) {
            // Convert the message to lowercase for case-insensitive comparison
            String lowerCaseMessage = message.toLowerCase();
    
            // Check if the message starts with "get" or "put"
            return lowerCaseMessage.startsWith("get") || lowerCaseMessage.startsWith("put");
        }
    
        public static void storeData(String data) {
            // Store the data in your JSON storage
            try {
                JSONObject newData = new JSONObject(data);
                String id = newData.getString("id");
                // jsonData.put(id, newData);
            } catch (Exception e) {
                // Handle JSON storage error
                System.err.println("Error storing data: " + e.getMessage());
            }
        }
	}

