import java.io.*;
import java.net.Socket;
import static org.junit.Assert.*;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.junit.Before;
import org.junit.Test;

public class AggregationServerTest {
    private MockSocket mockSocket;
    private ByteArrayOutputStream mockOutputData;

    @Before
    public void setUp() {
        mockSocket = new MockSocket();
        mockOutputData = new ByteArrayOutputStream();
        File dataDirectory = new File("data");
        if (!dataDirectory.exists()) {
            dataDirectory.mkdir();
        }
    }

    // Test the processing of valid JSON data
    // Ensure that valid JSON is correctly processed
    @Test
    public void testProcessDataValid() {
        StringBuilder jsonDataBuilder = new StringBuilder();
        boolean result = AggregationServer.processData("Lamport-Clock: 0 {\"id\":\"123\",\"name\":\"Test\"}", jsonDataBuilder);
        assertTrue(result);

        String expectedJSON = "{\"id\":\"123\",\"name\":\"Test\"}";
        try {
            JSONAssert.assertEquals(expectedJSON, jsonDataBuilder.toString(), false); // Ignore order
        } catch (JSONException e) {
            fail("JSON comparison failed: " + e.getMessage());
        }
    }

    // Test the processing of invalid data
    // Ensure that invalid data is handled correctly
    @Test
    public void testProcessDataInvalid() {
        StringBuilder jsonDataBuilder = new StringBuilder();
        boolean result = AggregationServer.processData("Invalid Data", jsonDataBuilder);
        assertFalse(result);
        assertEquals("", jsonDataBuilder.toString());
    }

    // Test data storage and file creation
    // Ensure that data is stored correctly and files are created
    @Test
    public void testStoreData() throws IOException  {
        String serverId = "TestServer";
        String jsonData = "{\"id\":\"123\",\"name\":\"Test\"}";

        // Store the data
        AggregationServer.storeData(jsonData, serverId);

        // Define the file path based on the server ID
        String filePath = "data/" + serverId + "_*.json";

        // Get a list of files matching the pattern (in case there are multiple)
        File[] files = new File("data/").listFiles((dir, name) -> name.matches(filePath));

        // Check if at least one file was found
        assertNotNull("No file was created", files);
        assertTrue("At least one file was created", files.length > 0);

        // Read the contents of the first matching file
        String fileContents = readFirstFileContents(files[0]);

        // Assert that the file contents match the stored JSON data
        assertEquals(jsonData, fileContents);
    }

    // Test handling of a valid client request
    // Ensure that a valid client request is handled correctly
    // Simulate a valid JSON message
    @Test
    public void testHandleClientValid() {
        // Simulate a valid JSON message
        String validJsonMessage = "{\"id\":\"123\",\"name\":\"Test\"}\r\n";
        ByteArrayInputStream validInput = new ByteArrayInputStream(validJsonMessage.getBytes());
        
        mockSocket.setInput(validInput);

        AggregationServer.handleClient(mockSocket, "TestServer");

        // Assert that the response was written to the mockOutputData stream
        String response = mockOutputData.toString();
        assertTrue(response.contains("HTTP/1.1 201 Created"));
        assertTrue(response.contains("Data received and stored."));
    }

    // Test handling of an invalid client request
    // Ensure that an invalid client request is handled correctly
    // Simulate an invalid JSON message
    @Test
    public void testHandleClientInvalid() {
        // Simulate an invalid JSON message
        String invalidJsonMessage = "Invalid Data\r\n";
        ByteArrayInputStream invalidInput = new ByteArrayInputStream(invalidJsonMessage.getBytes());
        mockSocket.setInput(invalidInput);

        AggregationServer.handleClient(mockSocket, "TestServer");

        // Assert that the response indicates an invalid request
        String response = mockOutputData.toString();
        assertTrue(response.contains("HTTP/1.1 400 Bad Request"));
        assertTrue(response.contains("Invalid request received."));
    }

    // Method to read the contents of a file
    private String readFirstFileContents(File file) throws IOException {
        // Read the contents of a file and return as a string
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder contents = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                contents.append(line);
            }
            return contents.toString();
        }
    }
    // MockSocket class to simulate Socket behavior for testing
    private class MockSocket extends Socket {
        private InputStream input;
        private OutputStream output;

        public MockSocket() {
            super();
        }

        public void setInput(InputStream input) {
            this.input = input;
        }

        @Override
        public InputStream getInputStream() {
            return input;
        }

        @Override
        public OutputStream getOutputStream() {
            return output;
        }
    }
}
