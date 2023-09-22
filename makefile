# Define the Java compiler and options
JAVAC = javac
JAVAC_OPTIONS = -cp .json-20230618.jar

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
	$(JAVA) $(JAVAC_OPTIONS) $(GET_CLIENT) localhost:4567

run-content-server:
	$(JAVA) $(JAVAC_OPTIONS) $(CONTENT_SERVER) localhost:4567 ./weather_data.txt

run-aggregation-server:
	$(JAVA) $(JAVAC_OPTIONS) $(AGGREGATION_SERVER) 4567

clean:
	rm -f $(GET_CLIENT).class
	rm -f $(CONTENT_SERVER).class
	rm -f $(AGGREGATION_SERVER).class
