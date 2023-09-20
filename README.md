# Distributed-Systems-assignment-2
Sarah Telford : a1810750
# Java Client and Servers Makefile

The Makefile simplifies the compilation and execution of client and server programs. It provides targets for compiling the Java source code and running the client and server programs.

## Prerequisites

- Java Development Kit (JDK): Make sure you have a Java Development Kit (JDK) installed on your system.

## Usage

### Compilation

To compile the Java source code, use the `compile` target. This target will compile the following main classes:

- `GETClient.java`: The WebSocket client for making GET requests.
- `ContentServer.java`: The WebSocket server for serving content.
- `AggregationServer.java`: The WebSocket server for aggregating content.

To compile the code, open your terminal and navigate to the directory containing the Makefile and source code files. Then, run:

```bash
make compile
```

This will compile the Java source files using the specified options and generate the corresponding `.class` files.

### Running the GET Client

To run the WebSocket GET client, use the `run-get-client` target. This client will make GET requests to a WebSocket server. You can provide the server URL and, optionally, a station ID as command-line arguments. The server URL should be in the format "http://servername:portnumber".

Example usage:

```bash
make run-get-client
```

### Running the Content Server

To run the WebSocket Content Server, use the `run-content-server` target. This server serves content over a WebSocket connection.

Example usage:

```bash
make run-content-server
```

### Running the Aggregation Server

To run the WebSocket Aggregation Server, use the `run-aggregation-server` target. This server aggregates content received from multiple WebSocket clients.

Example usage:

```bash
make run-aggregation-server
```

### Cleaning Up

To remove the generated `.class` files, use the `clean` target.

```bash
make clean
```