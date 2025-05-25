Dual-Connection File Transfer System

A Java-based client–server application that accelerates large-file downloads by splitting transfer over two TCP connections: one reads from the file’s start, the other from its end. The download completes when both streams meet.

Features
	•	Parallel Transfer: Two concurrent connections fetching from opposite ends.
	•	Automatic Merge Point Detection: Streams detect when they overlap and terminate cleanly.
	•	Resumable Downloads: Supports restarting interrupted transfers without re-downloading completed segments.
	•	Cross-Platform Scripts & Docker: Includes run.sh/run.bat and Dockerfiles for easy setup.

Installation

# Clone repository
git clone <repo_url>
cd dual-file-transfer

# Build Java sources
javac -d out src/*.java

Usage

Running the Server

# Linux/macOS
./run.sh server <port> <shared-directory>

# Windows
run.bat server <port> <shared-directory>

Running the Client

# Linux/macOS
./run.sh client <server-host> <port> <file-name> <output-path>

# Windows
run.bat client <server-host> <port> <file-name> <output-path>

Using Docker

# Start containers
./docker_start.sh

# Stop containers
./docker_stop.sh