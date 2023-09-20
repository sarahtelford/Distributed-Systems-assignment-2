import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ContentServer extends WebSocketServer {

    private Map<WebSocket, Long> serverLastActiveTime = new ConcurrentHashMap<>();
    private static final int MAX_INACTIVE_TIME = 30000; // 30 seconds

    private String feedFilePath;

    public ContentServer(InetSocketAddress address, String feedFilePath) {
        super(address);
        this.feedFilePath = feedFilePath;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: ContentServer <server-url> <feed-file-path>");
            return;
        }

        String serverUrl = args[0];
        String feedFilePath = args[1];

        int port = (args.length > 2) ? Integer.parseInt(args[2]) : 8888;
        ContentServer server = new ContentServer(new InetSocketAddress(port), feedFilePath);
        server.start();
        System.out.println("Content Server started on port " + port);
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
        System.out.println("Received message from aggregation server: " + message);

        // Parse and process the received data
        // Assuming each entry is separated by a newline
        String[] entries = message.split("\n");
        for (String entry : entries) {
            if (entry.trim().isEmpty()) {
                continue; // Skip empty lines
            }
            // Parse and assemble the JSON data
            String[] fields = entry.split(":");
            if (fields.length == 2) {
                String key = fields[0].trim();
                String value = fields[1].trim();
                // Assemble JSON format and append to the feed file
                String jsonData = "\"" + key + "\": \"" + value + "\",";
                try {
                    Files.write(Path.of(feedFilePath), jsonData.getBytes(), StandardOpenOption.APPEND);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        serverLastActiveTime.put(conn, System.currentTimeMillis());
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
                System.out.println("Removing inactive aggregation server: " + conn.getRemoteSocketAddress());
                serverLastActiveTime.remove(conn);
                conn.close();
            }
        }
    }

    @Override
    public void run() {
        // Periodically check for inactive aggregation servers and remove them
        while (!Thread.interrupted()) {
            removeInactiveServers();
            try {
                Thread.sleep(MAX_INACTIVE_TIME);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
