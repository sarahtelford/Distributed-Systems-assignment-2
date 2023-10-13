import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.net.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class ContentServerTest {

    @Mock
    private LamportClock lamportClock;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSendDataToServerSuccess() throws IOException {
        // Mock socket creation
        Socket socket = Mockito.mock(Socket.class);
        Mockito.when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        Mockito.when(socket.getInputStream()).thenReturn(new ByteArrayInputStream("HTTP/1.1 201 Created".getBytes()));

        Mockito.when(lamportClock.getValue()).thenReturn(1);

        // Create a temporary file with some data
        File tempFile = File.createTempFile("temp-feed", ".txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            writer.write("id: value\n");
        }

        assertTrue(ContentServer.sendDataToServer("example.com:80", tempFile.getAbsolutePath(), lamportClock));
    }

    @Test
    public void testSendDataToServerFailure() throws IOException {
        // Mock socket creation
        Socket socket = Mockito.mock(Socket.class);
        Mockito.when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        Mockito.when(socket.getInputStream()).thenReturn(new ByteArrayInputStream("HTTP/1.1 500 Internal Server Error".getBytes()));

        Mockito.when(lamportClock.getValue()).thenReturn(1);

        // Create a temporary file with some data
        File tempFile = File.createTempFile("temp-feed", ".txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            writer.write("id1: value1\n");
        }

        assertFalse(ContentServer.sendDataToServer("example.com:80", tempFile.getAbsolutePath(), lamportClock));
    }

    @Test
    public void testSendWeatherDataSuccess() throws IOException {
        Socket socket = Mockito.mock(Socket.class);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayInputStream inputStream = new ByteArrayInputStream("HTTP/1.1 201 Created".getBytes());

        Mockito.when(socket.getOutputStream()).thenReturn(outputStream);
        Mockito.when(socket.getInputStream()).thenReturn(inputStream);

        Mockito.when(lamportClock.getValue()).thenReturn(1);

        // Create a temporary file with some data
        File tempFile = File.createTempFile("temp-feed", ".txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            writer.write("id1: value1\n");
        }

        assertTrue(ContentServer.sendWeatherData(socket, tempFile.getAbsolutePath(), 1));
    }

    @Test
    public void testSendWeatherDataFailure() throws IOException {
        Socket socket = Mockito.mock(Socket.class);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayInputStream inputStream = new ByteArrayInputStream("HTTP/1.1 500 Internal Server Error".getBytes());

        Mockito.when(socket.getOutputStream()).thenReturn(outputStream);
        Mockito.when(socket.getInputStream()).thenReturn(inputStream);

        Mockito.when(lamportClock.getValue()).thenReturn(1);

        // Create a temporary file with some data
        File tempFile = File.createTempFile("temp-feed", ".txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            writer.write("id1: value1\n");
        }
        assertFalse(ContentServer.sendWeatherData(socket, tempFile.getAbsolutePath(), 1));
    }

    @Test
    public void testConvertToJson() {
        // Create a temporary test file
        String testFilePath = "testFeed.txt";
        createTestFile(testFilePath);

        // Mock the behavior of LamportClock
        Mockito.when(lamportClock.getValue()).thenReturn(1);

        // Test the convertToJson method
        String jsonData = ContentServer.convertToJson(testFilePath);

        // Assert the expected JSON content
        assertEquals("{\"id\":\"value\"}", jsonData);

        // Clean up the temporary test file
        deleteTestFile(testFilePath);
    }

    @Test
    public void testCreateSocket() throws IOException {
        Socket socket = ContentServer.createSocket("example.com:80");
        assertNotNull(socket);
    }

    // Helper methods to create a temporary test file
    private void createTestFile(String filePath) {
        try (PrintWriter writer = new PrintWriter(filePath)) {
            writer.println("id: value");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Helper methods delete a temporary test file
    private void deleteTestFile(String filePath) {
        File file = new File(filePath);
        if (file.exists() && !file.isDirectory()) {
            file.delete();
        }
    }
}
