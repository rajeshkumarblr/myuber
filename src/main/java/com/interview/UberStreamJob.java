package com.interview;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UberStreamJob {

    public static void main(String[] args) throws Exception {

        // 1. Set up the Flink Execution Environment
        // This is the "Stage" where the play happens.
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 2. Define the Kafka Source (Consumer)
        // We connect to "driver-locations" starting from the LATEST event (Real-time).
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("kafka:29092") // Note: Internal Docker DNS
                .setTopics("driver-locations")
                .setGroupId("uber-flink-group")
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 3. Create the Data Stream
        DataStream<String> rawStream = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");

        // 4. Process the Stream (The Logic)
        // We parse the JSON and just print "High Demand" drivers for now.
        ObjectMapper mapper = new ObjectMapper();

        DataStream<String> parsedStream = rawStream.map(json -> {
            try {
                JsonNode node = mapper.readTree(json);
                int driverId = node.get("driver_id").asInt();
                String status = node.get("status").asText();
                return "Driver " + driverId + " is " + status;
            } catch (Exception e) {
                return "Error parsing: " + json;
            }
        });

        // 5. Output (Sink)
        // For debugging, we print to the TaskManager logs (stdout).
        // In the next step, we will check these logs via Docker.
        parsedStream.print();

        // 6. Execute
        env.execute("Uber Real-Time Engine");
    }
}