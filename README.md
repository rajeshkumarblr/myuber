# Uber Real-Time Analytics Engine

A high-throughput real-time streaming application built with **Apache Flink** and **Kafka** to process simulated Uber driver data. This project demonstrates a modern data engineering stack with a focus on observability (Prometheus/Grafana) and fault tolerance.

## 🏗 Architecture

The pipeline consists of the following components:

1.  **Data Generator (Python)**: Simulates thousands of drivers moving across San Francisco, emitting location events to Kafka.
2.  **Message Broker (Kafka)**: Buffers raw events in the `driver-locations` topic.
3.  **Stream Processing (Flink)**:
    *   Consumes JSON events from Kafka.
    *   Deserializes and parses data.
    *   Filters/Maps events (e.g., identifying available drivers).
4.  **Observability**:
    *   **Prometheus**: Scrapes metrics from Flink TaskManagers.
    *   **Grafana**: Visualizes throughput, latency, and job health.

## 🚀 Getting Started

### Prerequisites
*   Docker & Docker Compose
*   Java 11 or 17
*   Maven 3.x
*   Python 3.x

### 1. Start the Infrastructure
Spin up Kafka, Zookeeper, Flink, Prometheus, and Grafana:
```bash
docker-compose up -d
```

### 2. Build the Flink Job
Compile the Java application into a JAR file:
```bash
mvn clean package
```
*Output: `target/uber-flink-engine-1.0-SNAPSHOT.jar`*

### 3. Submit the Job
Submit the JAR to the Flink cluster:
```bash
# Copy JAR to JobManager container
docker cp target/uber-flink-engine-1.0-SNAPSHOT.jar flink-jobmanager-new:/tmp/job.jar

# Submit Job
docker exec flink-jobmanager-new flink run -d -c com.interview.UberStreamJob /tmp/job.jar
```

### 4. Start the Data Simulator
Generate real-time traffic:
```bash
# Create venv and install dependencies
python3 -m venv .venv
source .venv/bin/activate
pip install kafka-python

# Run simulator
python driver_simulator.py
```

## 📊 Dashboards & Observability

| Service | URL | Description |
|---------|-----|-------------|
| **Flink Dashboard** | [http://localhost:8081](http://localhost:8081) | Job management, backpressure monitoring, logs. |
| **Grafana** | [http://localhost:3000](http://localhost:3000) | Visual metrics. (User/Pass: `admin`/`admin`) |
| **Prometheus** | [http://localhost:9090](http://localhost:9090) | Raw metrics query engine. |

## 🛠 Tech Stack
*   **Language**: Java 11 (Flink), Python (Simulator)
*   **Streaming**: Apache Flink 1.17
*   **Messaging**: Apache Kafka 7.4
*   **Monitoring**: Prometheus, Grafana
*   **Containerization**: Docker

## 📂 Project Structure
```
├── src/main/java/.../UberStreamJob.java  # Main Flink Application Logic
├── driver_simulator.py                   # Python Traffic Generator
├── docker-compose.yaml                   # Infrastructure Definition
├── pom.xml                               # Maven Build Configuration
├── prometheus.yml                        # Metrics Configuration
└── schema.sql                            # Database Schema (if applicable)
```
