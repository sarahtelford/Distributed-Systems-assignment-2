import org.junit.Before;
import org.junit.Test;
import org.junit.Assert.*;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class AggregationServerTest {
    private AggregationServer server;
    private MockSocket mockSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private ByteArrayOutputStream mockOutputData;
    private ByteArrayOutputStream mockResponseOutputData;

    @Before
    public void setUp() {
        server = new AggregationServer();
        mockSocket = new MockSocket();
        inputStream = new ByteArrayInputStream("Sample JSON Data".getBytes());
        mockOutputData = new ByteArrayOutputStream();
        mockResponseOutputData = new ByteArrayOutputStream();
    }

    @Test
    public void testProcessDataValid() {
        StringBuilder jsonDataBuilder = new StringBuilder();
        boolean result = server.processData("Lamport-Clock: 0 {\"id\":\"123\",\"name\":\"Test\"}", jsonDataBuilder);
        assertTrue(result);
        assertEquals("{\"id\":\"123\",\"name\":\"Test\"}", jsonDataBuilder.toString());
    }

    @Test
    public void testProcessDataInvalid() {
        StringBuilder jsonDataBuilder = new StringBuilder();
        boolean result = server.processData("Invalid Data", jsonDataBuilder);
        assertFalse(result);
        assertEquals("", jsonDataBuilder.toString());
    }

    @Test
    public void testStoreData() {
        String serverId = "TestServer";
        server.storeData("Sample JSON Data", serverId);
        // You can add assertions here to check if the file was created and contains the correct data
    }

    @Test
    public void testHandleClient() {
        // Create a mock socket with custom input and output streams
        MockSocket mockSocket = new MockSocket(inputStream, mockOutputData, mockResponseOutputData);
        
        // Simulate a valid JSON message
        String validJsonMessage = "Lamport-Clock: 0 {\"id\":\"123\",\"name\":\"Test\"}\r\n";
        ByteArrayInputStream validInput = new ByteArrayInputStream(validJsonMessage.getBytes());
        mockSocket.setInput(validInput);

        server.handleClient(mockSocket, "TestServer");

        // Assert that the response was written to the mockOutputData stream
        String response = mockOutputData.toString();
        assertTrue(response.contains("HTTP/1.1 201 Created"));
        assertTrue(response.contains("Data received and stored."));

        // Assert that the JSON data was stored correctly
        // Simulate an invalid JSON message
        String invalidJsonMessage = "Invalid Data\r\n";
        ByteArrayInputStream invalidInput = new ByteArrayInputStream(invalidJsonMessage.getBytes());
        mockSocket.setInput(invalidInput);
        
        mockOutputData.reset(); // Reset the output data stream

        server.handleClient(mockSocket, "TestServer");

        // Assert that the response indicates an invalid request
        response = mockOutputData.toString();
        assertTrue(response.contains("HTTP/1.1 400 Bad Request"));
        assertTrue(response.contains("Invalid request received."));
    }

    // MockSocket class to simulate Socket behavior for testing
    private class MockSocket extends Socket {
        private InputStream input;
        private OutputStream output;

        public MockSocket() {
            super();
        }

        public MockSocket(InputStream input, OutputStream output) {
            super();
            this.input = input;
            this.output = output;
        }

        public MockSocket(InputStream input, OutputStream output, OutputStream responseOutput) {
            super();
            this.input = input;
            this.output = output;
            this.mockResponseOutputData = responseOutput;
        }

        @Override
        public InputStream getInputStream() {
            return input;
        }

        @Override
        public OutputStream getOutputStream() {
            return output;
        }

        // Additional method to simulate receiving a response
        public OutputStream getResponseOutputStream() {
            return mockResponseOutputData;
        }
    }
}
