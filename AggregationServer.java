import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class AggregationServer extends WebSocketServer {
    private Map<WebSocket, String> contentServerMap = new HashMap<>();

    public AggregationServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onStart() {
        // Implementation not required, but this method must be present
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Aggregation Server - Connection opened: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Aggregation Server - Connection closed: " + conn.getRemoteSocketAddress());

        // Remove the content server from the map when it disconnects
        if (contentServerMap.containsKey(conn)) {
            contentServerMap.remove(conn);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Aggregation Server - Received message from Content Server: " + message);

        // Store the message with the content server's identifier
        contentServerMap.put(conn, message);

        // Process and aggregate data as needed (not implemented in this example)
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Aggregation Server - WebSocket error: " + ex.getMessage());
    }

    public static void main(String[] args) {
        int port = 8888; // Change the port as needed
        AggregationServer aggregationServer = new AggregationServer(new InetSocketAddress(port));
        aggregationServer.start();
        System.out.println("Aggregation Server started on port " + port);
    }
}
