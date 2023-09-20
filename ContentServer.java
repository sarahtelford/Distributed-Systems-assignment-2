import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;

public class ContentServer extends WebSocketServer {
    public ContentServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onStart() {
        // Implementation not required, but this method must be present
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Content Server - Connection opened: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Content Server - Connection closed: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Content Server - Received message: " + message);

        // Process the message and send it to the aggregation server
        // For simplicity, we'll echo the message back to the client
        conn.send("Echo from Content Server: " + message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Content Server - WebSocket error: " + ex.getMessage());
    }

    public static void main(String[] args) {
        int port = 8887; // Change the port as needed
        ContentServer contentServer = new ContentServer(new InetSocketAddress(port));
        contentServer.start();
        System.out.println("Content Server started on port " + port);
    }
}
