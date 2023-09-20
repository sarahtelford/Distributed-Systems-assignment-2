import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AggregationServer extends WebSocketServer {

    private Map<WebSocket, Long> serverLastActiveTime = new ConcurrentHashMap<>();
    private static final int MAX_INACTIVE_TIME = 30000; // 30 seconds

    private JSONObject jsonData = new JSONObject();

    public AggregationServer(InetSocketAddress address) {
        super(address);
    }

    public static void main(String[] args) {
        int port = 4567; // Default port number
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number provided. Using default port 4567.");
            }
        }
        AggregationServer server = new AggregationServer(new InetSocketAddress(port));
        server.start();
        System.out.println("Aggregation Server started on port " + port);
    }

    @Override
    public void onStart() {
        // Empty implementation
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Connection opened: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Connection closed: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Received message from content server: " + message);

        // Check if the received message is a valid GET or PUT request
        if (isGetOrPutRequest(message)) {
            // Process the received data and determine the outcome
            boolean dataIsValid = processData(message);

            if (dataIsValid) {
                storeData(message); 
                if (!serverLastActiveTime.containsKey(conn)) {
                    // This is the first data upload, send HTTP 201 Created response
                    String response201 = "HTTP/1.1 201 Created\r\n\r\nData received and stored.";
                    conn.send(response201);
                } else {
                    // Data is valid, but this is not the first upload, send HTTP 200 OK response
                    String response200 = "HTTP/1.1 200 OK\r\n\r\nData received and processed successfully.";
                    conn.send(response200);
                }
            } else if (message.isEmpty()) {
                // No data received, send HTTP 204 No Content response
                String response204 = "HTTP/1.1 204 No Content\r\n\r\nNo data received.";
                conn.send(response204);
            } else {
                // Invalid data or JSON parsing error, send HTTP 500 Internal Server Error
                // response
                String response500 = "HTTP/1.1 500 Internal Server Error\r\n\r\nInternal server error occurred.";
                conn.send(response500);
            }
            serverLastActiveTime.put(conn, System.currentTimeMillis());
        } else {
            // Request is not a valid GET or PUT, send HTTP 400 Bad Request response
            String response400 = "HTTP/1.1 400 Bad Request\r\n\r\nInvalid request received.";
            conn.send(response400);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("WebSocket error: " + ex.getMessage());
    }

    private void removeInactiveServers() {
        long currentTime = System.currentTimeMillis();
        for (WebSocket conn : serverLastActiveTime.keySet()) {
            long lastActiveTime = serverLastActiveTime.get(conn);
            if (currentTime - lastActiveTime > MAX_INACTIVE_TIME) {
                System.out.println("Removing inactive content server: " + conn.getRemoteSocketAddress());
                serverLastActiveTime.remove(conn);
                conn.close();
            }
        }
    }

    /**
     * Process the received data from a content server.
     *
     * @param data The data received in JSON format.
     * @return true if the data is valid, false otherwise.
     */
    private boolean processData(String data) {
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
    private boolean isGetOrPutRequest(String message) {
        // Convert the message to lowercase for case-insensitive comparison
        String lowerCaseMessage = message.toLowerCase();
        
        // Check if the message starts with "get" or "put"
        return lowerCaseMessage.startsWith("get") || lowerCaseMessage.startsWith("put");
    }

    private void storeData(String data) {
        // Store the data in your JSON storage
        try {
            JSONObject newData = new JSONObject(data);
            String id = newData.getString("id");
            jsonData.put(id, newData);
        } catch (Exception e) {
            // Handle JSON storage error
            System.err.println("Error storing data: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        // Periodically check for inactive content servers and remove them
        while (!Thread.interrupted()) {
            removeInactiveServers();
            // Remove items from jsonData that haven't been communicated with for 30 seconds
            long currentTime = System.currentTimeMillis();
            for (String id : jsonData.keySet()) {
                JSONObject data = jsonData.getJSONObject(id);
                long lastActiveTime = data.optLong("lastActiveTime", 0);
                if (currentTime - lastActiveTime > MAX_INACTIVE_TIME) {
                    System.out.println("Removing inactive data for ID: " + id);
                    jsonData.remove(id);
                }
            }
            try {
                Thread.sleep(MAX_INACTIVE_TIME);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
