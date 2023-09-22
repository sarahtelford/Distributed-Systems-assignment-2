import java.net.*;
import java.io.*;
import java.lang.Object; 

public class ContentServer
{
    private static final int MAX_INACTIVE_TIME = 30000; // 30 seconds
    BufferedReader reader = null;
	public static void main(String args[])
	{
        String serverUrl = args[0];
        String feedFilePath = args[1];
        int lamportClock = 0;

        try(BufferedReader reader = new BufferedReader(new FileReader(feedFilePath))) {
           
            String host = serverUrl.split(":")[0];
            int port = Integer.parseInt(serverUrl.split(":")[1]);

            Socket s = new Socket(host,port);
            System.out.println("socket created");
            DataInputStream inputData = new DataInputStream(s.getInputStream());
            DataOutputStream  outputData = new DataOutputStream(s.getOutputStream());
        
            String line;
            StringBuilder jsonDataBuilder = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    if ("id".equals(key)) {
                        // Check if the entry has a valid "id" field
                        if (!value.isEmpty()) {
                            if (jsonDataBuilder.length() > 0) {
                                // Append a comma to separate entries
                                jsonDataBuilder.append(",");
                            }
                            // Append the key-value pair to JSON format
                            jsonDataBuilder.append("\"").append(key).append("\":\"").append(value).append("\"");
                        } else {
                            // Log an error for entries with no "id" field
                            System.err.println("Skipping entry with no 'id' field: " + line);
                        }
                    } else {
                        // Append other fields to JSON format (ignoring markup in text fields)
                        if (jsonDataBuilder.length() > 0) {
                            // Append a comma to separate entries
                            jsonDataBuilder.append(",");
                        }
                        jsonDataBuilder.append("\"").append(key).append("\":\"").append(value).append("\"");
                    }
                }
            }

            // Wrap the collected JSON data with curly braces to form a JSON object
            String finalJsonData = "{" + jsonDataBuilder.toString() + "}";

            // Append the JSON data to the feed file
            outputData.writeUTF("PUT /weather_data.txt HTTP/1.1\r\nUser-Agent: // ATOMClient/1/0\r\nContent-Type: text/Json\r\nContent-Length: " + finalJsonData.length() + "\r\nLamport-Clock: " + lamportClock + "\r\n" + finalJsonData);
            outputData.flush();


            String serverResponse = inputData.readUTF();
            System.out.println("Server Response: " + serverResponse);

            s.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
	}
}
