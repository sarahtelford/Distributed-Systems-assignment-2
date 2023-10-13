# Distributed Systems Assignment 2
**Student:** Sarah Telford (a1810750)

## Content Server
The Content Server is responsible for sending weather data to another server over a socket connection. It reads weather data from a feed file, converts it to JSON format, and sends it to the destination server using HTTP PUT requests. The class handles retries in case of connection failures and keeps track of the last active time for each socket.

### Key Features
- Converts weather data from a feed file into JSON format.
- Retries sending data to the server in case of failures.
- Responds with appropriate HTTP status codes based on the success or failure of data transmission.

## Aggregation Server
The Aggregation Server acts as a server for receiving data from multiple content servers, processing requests, and managing client connections. It is designed to aggregate and manage weather data provided by content servers.

### Key Features
- Collects weather data from various sources for centraliszed management.
- Handles incoming client connections concurrently, ensuring data integrity.
- Organises and stores weather data in the data/ directory.
- Automatically removes outdated data (data from old sockets/data not recived in the last 20 messages) to maintain data accuracy.
- Processes GET and PUT requests, facilitating data retrieval and submission.
- Provides detailed error responses and status codes.

## GET Client
The GETClient is a client application for retrieving weather data from the aggregation server. It sends HTTP GET requests and processes the server's responses. The GET Client will only return weather data that has been recved in the last 30 seconds. If no data has been recived in this timeframe a 

```
HTTP/1.1 404 Not Found

No weather data available.
```

will be returned. 

### Key Features
- Sends a HTTP GET requests to a content server, allowing the retrieval of weather data stored on the server.
- Implements a heartbeat mechanism to maintain the connection with the server. 
- Uses a Lamport clock to timestamp its requests to ensure that requests are ordered correctly
- The client processes and formats the server's responses for easier readability.

# Using the Makefile
The Makefile simplifies the compilation and execution of client and server programs. It provides targets for compiling the Java source code and running the client and server programs.

### Prerequisites
- Java Development Kit (JDK) installed.
- JSON library (json-20230618.jar) available in a 'lib' directory in your project folder.

#### Compile Java Code
- To compile the Java code, run the following command:
  ```
  make compile
  ```

  This will compile the main Java classes using the `javac` command. The classes to be compiled are specified in the `compile` target.

#### Run GET Client
- Commands should be run in its own terminal to independently control the client's interactions with the server.
- To run the GETClient program, use the following command:
  ```
  make run-get-client URL=<URL>
  ```

  Replace `<URL>` with the URL you want to use when running the GETClient. Example:
  ```
  make run-get-client URL=http://example.com:8080/data
#### Run Content Server
- Commands should be run in its own terminal.
- To run the ContentServer program, use the following command:
  ```
  make run-content-server URL=<URL> LOCATION=<LOCATION>
  ```

  Replace `<URL>` with the URL and port number you want to use when running the ContentServer. Replace `<LOCATION>` with the location of your data file. Example:
  ```
  make run-content-server URL=localhost:4567 LOCATION=./data/weather_data.txt
#### Run Aggregation Server
- - Commands should be run in its own terminal.
- To run the AggregationServer program, use the following command:
  ```
  make run-aggregation-server PORT=<PORT>
  ```

  Replace `<PORT>` with the port number you want to use when running the AggregationServer. Example:
  ```
  make run-aggregation-server PORT=4567
### Clean Compiled Files
- To clean up the compiled class files, you can run:
  ```
  make clean
  ```

# Other notes
Tried to implement heartbeat from GETclient but was having an issue that the aggregation server was hanging when a heartbeat was sent. I have left the code related code in the client and aggregation server, but it is never utilised. 