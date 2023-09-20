# Define the Java compiler and options
JAVAC = javac
JAVAC_OPTIONS = -cp .:Java-WebSocket-1.5.4.jar

# Define the Java interpreter
JAVA = java

# Define the main classes
GET_CLIENT = GETClient
CONTENT_SERVER = ContentServer
AGGREGATION_SERVER = AggregationServer

all: compile

compile:
	$(JAVAC) $(JAVAC_OPTIONS) $(GET_CLIENT).java
	$(JAVAC) $(JAVAC_OPTIONS) $(CONTENT_SERVER).java
	$(JAVAC) $(JAVAC_OPTIONS) $(AGGREGATION_SERVER).java

run-get-client:
	$(JAVA) $(JAVAC_OPTIONS) $(GET_CLIENT)

run-content-server:
	$(JAVA) $(JAVAC_OPTIONS) $(CONTENT_SERVER)

run-aggregation-server:
	$(JAVA) $(JAVAC_OPTIONS) $(AGGREGATION_SERVER)

clean:
	rm -f $(GET_CLIENT).class
	rm -f $(CONTENT_SERVER).class
	rm -f $(AGGREGATION_SERVER).class
