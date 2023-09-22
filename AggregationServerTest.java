import static org.junit.Assert.*;
import java.util.Set;
import org.junit.Test;
import org.junit.Before;
import org.mockito.Mockito;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.json.JSONObject;
import java.net.InetSocketAddress;
 

public class AggregationServerTest {

    private AggregationServer aggregationServer;

    @Before
    public void setUp() {
        aggregationServer = new AggregationServer(new InetSocketAddress(8888));
        aggregationServer.start();
    }

    @Test
    public void testIsGetOrPutRequest() {
        assertTrue(aggregationServer.isGetOrPutRequest("GET /weather?station=123 HTTP/1.1\r\n\r\n"));
        assertTrue(aggregationServer.isGetOrPutRequest("put /weather?station=456 HTTP/1.1\r\n\r\n"));
        assertFalse(aggregationServer.isGetOrPutRequest("POST /data HTTP/1.1\r\n\r\n"));
    }

    @Test
    public void testProcessDataValid() {
        String validJson = "{\"id\":\"123\",\"air_temp\":15.5,\"wind_spd_kt\":10}";
        assertTrue(aggregationServer.processData(validJson));
    }

    @Test
    public void testProcessDataMissingFields() {
        String invalidJson = "{\"id\":\"123\",\"dewpt\":8.5}";
        assertFalse(aggregationServer.processData(invalidJson));
    }

    @Test
    public void testProcessDataInvalidJson() {
        String invalidJson = "invalid-json";
        assertFalse(aggregationServer.processData(invalidJson));
    }

    @Test
    public void testStoreData() {
        aggregationServer.storeData("{\"id\":\"456\",\"air_temp\":20.0,\"wind_spd_kt\":12}");
        JSONObject jsonData = aggregationServer.getJsonData();
        assertTrue(jsonData.has("456"));
    }

    @Test
    public void testRemoveInactiveServers() {
        WebSocket mockWebSocket1 = Mockito.mock(WebSocket.class);
        WebSocket mockWebSocket2 = Mockito.mock(WebSocket.class);

        long currentTime = System.currentTimeMillis();
        aggregationServer.getServerLastActiveTime().put(mockWebSocket1, currentTime - 31000); // Inactive for 31 seconds
        aggregationServer.getServerLastActiveTime().put(mockWebSocket2, currentTime - 25000); // Inactive for 25 seconds

        aggregationServer.removeInactiveServers();

        assertFalse(aggregationServer.getServerLastActiveTime().containsKey(mockWebSocket1));
        assertTrue(aggregationServer.getServerLastActiveTime().containsKey(mockWebSocket2));
    }
}
