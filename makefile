# Define the Java compiler and options
JAVAC = javac
JAVAC_OPTIONS = -cp .:Java-WebSocket-1.5.4.jar:slf4j-api-2.0.9.jar:slf4j-simple-2.0.9.jar:json-20230618.jar

# Define the Java interpreter
JAVA = java

# Define the main classes
GET_CLIENT = GETClient
CONTENT_SERVER = ContentServer
AGGREGATION_SERVER = AggregationServer

# Define the default port number
DEFAULT_PORT = 4567

all: compile

compile:
	$(JAVAC) $(JAVAC_OPTIONS) $(GET_CLIENT).java
	$(JAVAC) $(JAVAC_OPTIONS) $(CONTENT_SERVER).java
	$(JAVAC) $(JAVAC_OPTIONS) $(AGGREGATION_SERVER).java

run-get-client:
	$(JAVA) $(JAVAC_OPTIONS) $(GET_CLIENT) ws://localhost:8887/

run-content-server:
	$(JAVA) $(JAVAC_OPTIONS) $(CONTENT_SERVER) ws://localhost:8888/ weather_data.txt

run-aggregation-server:
    @read -p "Enter port number (default is $(DEFAULT_PORT)): " port; \
    if [ -z "$$port" ]; then \
        port=$(DEFAULT_PORT); \
    fi; \
	$(JAVA) $(JAVAC_OPTIONS) $(AGGREGATION_SERVER) $$port

clean:
	rm -f $(GET_CLIENT).class
	rm -f $(CONTENT_SERVER).class
	rm -f $(AGGREGATION_SERVER).class
