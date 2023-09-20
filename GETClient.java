import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;

public class GETClient extends WebSocketClient {

    private static int lamportClock = 0;

    public GETClient(URI serverURI) {
        super(serverURI);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        System.out.println("Connected to WebSocket server.");
    }

    @Override
    public void onMessage(String message) {
        // Process and display the received message
        System.out.println("Received message: " + message);

        try {
            // Parse the received message as JSON
            JSONObject jsonData = new JSONObject(message);

            // Iterate through the JSON keys (attributes) and their corresponding values
            for (String key : jsonData.keySet()) {
                String value = jsonData.getString(key);
                System.out.println("Attribute: " + key);
                System.out.println("Value: " + value);
            }

            // Increment Lamport clock after processing the message
            lamportClock++;
        } catch (JSONException e) {
            // Handle JSON parsing error
            System.err.println("Error parsing JSON: " + e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("WebSocket connection closed.");
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("WebSocket error: " + ex.getMessage());
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: GETClient <websocket-server-url> [station-id]");
            return;
        }
    
        String serverUrl = args[0];
        String stationId = (args.length > 1) ? args[1] : "default"; // Default station ID if not provided
    
        try {
            // Check if the provided URL starts with "http://" or "https://"
            if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
                // If not, add "http://" as the default protocol
                serverUrl = "http://" + serverUrl;
            }
    
            GETClient client = new GETClient(new URI(serverUrl));
            client.connect();
    
            // Wait for the client to establish the connection
            while (!client.isOpen()) {
                Thread.sleep(1000);
            }
    
            // Send a GET request message to the server with station ID
            String getRequest = "GET /weather?station=" + stationId + " HTTP/1.1\r\n\r\n";
            client.send(getRequest);
    
            // You can continue to send and receive WebSocket messages as needed
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
