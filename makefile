# Define the Java compiler and options
JAVAC = javac

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
	$(JAVAC) $(GET_CLIENT).java
	$(JAVAC) $(CONTENT_SERVER).java
	$(JAVAC) $(AGGREGATION_SERVER).java

run-get-client:
	$(JAVA) $(GET_CLIENT) localhost:4567

run-content-server:
	$(JAVA) $(CONTENT_SERVER) localhost:4567 ./weather_data.txt

run-aggregation-server:
	$(JAVA) $(AGGREGATION_SERVER) 4567

clean:
	rm -f $(GET_CLIENT).class
	rm -f $(CONTENT_SERVER).class
	rm -f $(AGGREGATION_SERVER).class
