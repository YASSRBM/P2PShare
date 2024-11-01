# Compiler
CXX = g++
CXXFLAGS = -Wall -Wextra -g3 -O0 -std=c++11 -lpthread

# Directories
SRC_DIR = src
TRACKER_DIR = $(SRC_DIR)/Tracker

# Source and object files
SRCS = $(TRACKER_DIR)/handle_tracker.cpp $(TRACKER_DIR)/tracker.cpp
OBJS = $(SRCS:.cpp=.o)

# Java files
JAVADIR = $(SRC_DIR)/peer
JAVAC = javac
JAVA = java
JAVASRCS = $(wildcard $(JAVADIR)/*.java)
JAVACLASSPATH = $(JAVADIR)

# Targets
all: tracker peer

# Compile C++ source files
%.o: %.cpp
	$(CXX) $(CXXFLAGS) -c $< -o $@

tracker: $(OBJS)
	$(CXX) $(CXXFLAGS) $(SRCS) -o tracker

# Compile Java files
peer: $(JAVASRCS)
	$(JAVAC) -d $(JAVADIR) -cp $(JAVACLASSPATH) $(JAVASRCS)

# Clean up generated files
clean:
	rm -f $(OBJS) tracker
	rm -rf $(JAVADIR)/*.class
	rm -rf $(JAVADIR)/peer
	rm -rf tstHandlegetfile tstHandleLook

# Run commands
run-tracker: tracker
	./tracker

run-peer: peer
	$(JAVA) -cp $(JAVACLASSPATH) peer.Peer

.PHONY: all clean run-tracker run-peer
