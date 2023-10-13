import static org.junit.Assert.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.Mockito;

public class GETClientTest {

    private static final int PORT = 12345;
    private static final String HOST = "localhost:" + PORT;
    
    @Test
    public void testSendGetRequest() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream outputData = new DataOutputStream(outputStream);

        LamportClock lamportClock = new LamportClock();

        String host = "example.com";
        int port = 80;

        try {
            GETClient.sendGetRequest(outputData, host, port, lamportClock);
            String expectedRequest = "GET /weather HTTP/1.1\r\n" +
                    "Host: example.com:80\r\n" +
                    "Lamport-Clock: 0\r\n\r\n";
            assertEquals(expectedRequest, outputStream.toString());
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }
    
    @Test
    public void testProcessServerResponse() {
        String testResponse = "{'id':'123','name':'TestCity','temp':'25.5'}";
        BufferedReader inputReader = new BufferedReader(new StringReader(testResponse));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        try {
            GETClient.processServerResponse(inputReader);

            String expectedOutput = "Server Response:\n{'id':'123',\n'name':'TestCity',\n'temp':'25.5'}";
            assertEquals(expectedOutput, outputStream.toString().trim());
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        } finally {
            System.setOut(System.out);
        }
    }

    @Test
    public void testMainMethod() {
        // Set up a mock socket for the client
        Socket socket = Mockito.mock(Socket.class);
        DataOutputStream mockOutput = Mockito.mock(DataOutputStream.class);
    
        try {
            Mockito.when(socket.getOutputStream()).thenReturn(mockOutput);
    
            // Use a CountDownLatch to control the test timing
            CountDownLatch serverStarted = new CountDownLatch(1);
            Thread serverThread = new Thread(() -> {
                try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                    serverStarted.countDown();
                    Socket clientSocket = serverSocket.accept();
    
                    // Simulate the server's response
                    try (DataInputStream input = new DataInputStream(clientSocket.getInputStream())) {
                        String clientRequest = input.readUTF();
                        if (clientRequest.contains("GET /weather")) {
                            // Send an HTTP response
                            try (DataOutputStream outputData = new DataOutputStream(clientSocket.getOutputStream())) {
                                String httpResponse = "HTTP/1.1 200 OK\r\n" +
                                                    "Content-Length: 5\r\n";
                                outputData.writeUTF(httpResponse);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            serverThread.start();
    
            serverStarted.await(5, TimeUnit.SECONDS);
    
            // Redirect the standard output to capture the response
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outputStream));
    
            GETClient.main(new String[]{HOST});
    
            String expectedOutput = "HTTP/1.1 200 OK\r\nContent-Length: 5";
            assertEquals(expectedOutput, outputStream.toString().trim());
    
            // Reset the standard output
            System.setOut(System.out);
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }
    
}
