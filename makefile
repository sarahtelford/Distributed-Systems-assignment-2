# Define the Java compiler and options
JAVAC = javac
JAVAC_OPTIONS = -cp .:./lib/json-20230618.jar

# Define the Java interpreter
JAVA = java

# Define the main classes
GET_CLIENT = GETClient
CONTENT_SERVER = ContentServer
AGGREGATION_SERVER = AggregationServer
LAMPORT_CLOCK = LamportClock

all: compile

compile:
	$(JAVAC) $(JAVAC_OPTIONS) $(LAMPORT_CLOCK).java
	$(JAVAC) $(JAVAC_OPTIONS) $(GET_CLIENT).java
	$(JAVAC) $(JAVAC_OPTIONS) $(CONTENT_SERVER).java
	$(JAVAC) $(JAVAC_OPTIONS) $(AGGREGATION_SERVER).java

run-get-client:
	$(JAVA) $(JAVAC_OPTIONS) $(GET_CLIENT) $(URL)

run-content-server:
	$(JAVA) $(JAVAC_OPTIONS) $(CONTENT_SERVER) $(URL) $(LOCATION)

run-aggregation-server:
	$(JAVA) $(JAVAC_OPTIONS) $(AGGREGATION_SERVER) $(PORT)

clean:
	rm -f $(GET_CLIENT).class
	rm -f $(CONTENT_SERVER).class
	rm -f $(AGGREGATION_SERVER).class
	rm -f $(LAMPORT_CLOCK).class