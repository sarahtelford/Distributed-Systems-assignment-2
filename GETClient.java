import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;

public class GETClient extends WebSocketClient {

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
            System.err.println("Usage: WebSocketGETClient <websocket-server-url>");
            return;
        }

        String serverUrl = args[0];

        try {
            WebSocketGETClient client = new WebSocketGETClient(new URI(serverUrl));
            client.connect();

            // Wait for the client to establish the connection
            while (!client.isOpen()) {
                Thread.sleep(1000);
            }

            // Send a GET request message to the server
            String getRequest = "GET /weather HTTP/1.1\r\n\r\n";
            client.send(getRequest);

            // You can continue to send and receive WebSocket messages as needed
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
