import java.net.*;
import java.util.*;
import java.io.*;

public class ContentServer {
    private static final Map<Socket, Long> serverLastActiveTime = new HashMap<>();
    private static LamportClock lamportClock = new LamportClock();

    private static Queue<Socket> serverQueue = new PriorityQueue<>((s1, s2) -> {
        long time1 = serverLastActiveTime.getOrDefault(s1, 0L);
        long time2 = serverLastActiveTime.getOrDefault(s2, 0L);
        int cmp = Long.compare(time1, time2);
        return (cmp != 0) ? cmp : s1.hashCode() - s2.hashCode();
    });

    public static void main(String args[]) {
        String serverUrl = args[0];
        String feedFilePath = args[1];

        boolean sentSuccessfully = sendDataToServer(serverUrl, feedFilePath, lamportClock);

        if (!sentSuccessfully) {
            System.out.println("Failed to send data after retries.");
        }
    }

    /**
     * Sends data to the server with retries in case of failure.
     * 
     * @param serverUrl    The URL of the server.
     * @param feedFilePath The path to the feed file.
     * @param lamportClock The Lamport clock value.
     * @return True if data was sent successfully, false otherwise.
     */
    private static boolean sendDataToServer(String serverUrl, String feedFilePath, LamportClock lamportClock) {
        int maxRetries = 3;

        while (maxRetries > 0) {
            try (Socket s = createSocket(serverUrl)) {
                synchronized (serverQueue) {
                    serverQueue.add(s);
                }

                boolean success = false;

                while (!success && !serverQueue.isEmpty()) {
                    Socket serverSocket;
                    synchronized (serverQueue) {
                        serverSocket = serverQueue.poll();
                    }

                    if (serverSocket != null) {
                        try {
                            int currentClockValue = lamportClock.getValue();
                            success = sendWeatherData(serverSocket, feedFilePath, currentClockValue);

                            // Attempt resending data 3 time if failed
                            if (!success) {
                                System.out.println("Failed to send data to server.");
                                maxRetries--;
                                System.out.println("Retrying... " + maxRetries + " retries left.");
                                Thread.sleep(1000);
                            }
                        } finally {
                            serverSocket.close();
                        }
                    }
                }
                if (success) {
                    return true;
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Sends weather data to the server over the given socket.
     * 
     * @param s                 The socket to communicate with the server.
     * @param feedFilePath      The path to the feed file.
     * @param lamportClockValue The current Lamport clock value.
     * @param lamportClock      The Lamport clock instance for synchronization.
     * @return True if the data was sent successfully, false otherwise.
     * @throws IOException If there is an issue with input/output operations.
     */
    private static boolean sendWeatherData(Socket s, String feedFilePath, int lamportClockValue) throws IOException {
        // Increment the Lamport clock
        lamportClock.increment();

        // Construct and send the HTTP PUT request with the weather data
        DataInputStream inputData = new DataInputStream(s.getInputStream());
        DataOutputStream outputData = new DataOutputStream(s.getOutputStream());

        String jsonData = convertToJson(feedFilePath);
        if (jsonData == null) {
            System.out.println("Failed to convert feed to JSON.");
            return false;
        }

        String requestData = "PUT /weather_data.txt HTTP/1.1\r\nUser-Agent: // ATOMClient/1/0\r\nContent-Type: text/Json\r\nContent-Length: "
                + jsonData.length() + "\r\nLamport-Clock: " + lamportClock + "\r\n" + jsonData;

        outputData.writeUTF(requestData);
        outputData.flush();

        String serverResponse = inputData.readUTF();
        System.out.println("Server Response: " + serverResponse);

        return serverResponse.startsWith("HTTP/1.1 200") || serverResponse.startsWith("HTTP/1.1 201");
    }

    /**
     * Creates a socket and connects to the server.
     * 
     * @param serverUrl The URL of the server.
     * @return The created socket.
     * @throws IOException If there is an issue creating the socket.
     */
    private static Socket createSocket(String serverUrl) throws IOException {
        String host = serverUrl.split(":")[0];
        int port = Integer.parseInt(serverUrl.split(":")[1]);

        Socket s = new Socket(host, port);
        System.out.println("Socket created");

        serverLastActiveTime.put(s, System.currentTimeMillis());

        return s;
    }

    /**
     * Converts the feed file to JSON format.
     * 
     * @param feedFilePath The path to the feed file.
     * @return The JSON representation of the feed data, or null if there was an
     *         issue.
     */
    private static String convertToJson(String feedFilePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(feedFilePath))) {
            StringBuilder jsonDataBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    if (jsonDataBuilder.length() > 0) {
                        jsonDataBuilder.append(",");
                    }
                    jsonDataBuilder.append("\"").append(key).append("\":\"").append(value).append("\"");
                }
            }

            return "{" + jsonDataBuilder.toString() + "}";
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}