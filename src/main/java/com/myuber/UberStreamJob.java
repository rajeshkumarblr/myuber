package com.myuber;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UberStreamJob {

    // Helper class for unified events
    public static class GeoEvent {
        public String type; // "DRIVER" or "REQUEST"
        public String gridId;
        public double lat;
        public double lon;
        public long timestamp;

        public GeoEvent() {
        }

        public GeoEvent(String type, String gridId, double lat, double lon, long timestamp) {
            this.type = type;
            this.gridId = gridId;
            this.lat = lat;
            this.lon = lon;
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return type + "@" + gridId;
        }
    }

    // Grid System: Round to 2 decimal places (approx 1.1km)
    public static String getGridId(double lat, double lon) {
        return String.format("%.2f,%.2f", lat, lon);
    }

    public static void main(String[] args) throws Exception {

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        ObjectMapper mapper = new ObjectMapper();

        // 1. Source: Drivers
        KafkaSource<String> driverSource = KafkaSource.<String>builder()
                .setBootstrapServers("localhost:9092")
                .setTopics("driver-locations")
                .setGroupId("uber-flink-group-drivers")
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStream<GeoEvent> driverStream = env
                .fromSource(driverSource, WatermarkStrategy.noWatermarks(), "Driver Source")
                .map(json -> {
                    try {
                        JsonNode node = mapper.readTree(json);
                        double lat = node.get("lat").asDouble();
                        double lon = node.get("lon").asDouble();
                        return new GeoEvent("DRIVER", getGridId(lat, lon), lat, lon, System.currentTimeMillis());
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(e -> e != null);

        // 2. Source: Requests
        KafkaSource<String> requestSource = KafkaSource.<String>builder()
                .setBootstrapServers("localhost:9092")
                .setTopics("ride-requests")
                .setGroupId("uber-flink-group-requests")
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStream<GeoEvent> requestStream = env
                .fromSource(requestSource, WatermarkStrategy.noWatermarks(), "Request Source")
                .map(json -> {
                    try {
                        JsonNode node = mapper.readTree(json);
                        double lat = node.get("lat").asDouble();
                        double lon = node.get("lon").asDouble();
                        return new GeoEvent("REQUEST", getGridId(lat, lon), lat, lon, System.currentTimeMillis());
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(e -> e != null);

        // 3. Union & Process
        DataStream<String> surgeAlerts = driverStream.union(requestStream)
                .keyBy(e -> e.gridId)
                .window(TumblingProcessingTimeWindows.of(Time.seconds(10))) // Check every 10 seconds
                .apply(new WindowFunction<GeoEvent, String, String, TimeWindow>() {
                    @Override
                    public void apply(String gridId, TimeWindow window, Iterable<GeoEvent> events,
                            Collector<String> out) {
                        int drivers = 0;
                        int requests = 0;

                        for (GeoEvent e : events) {
                            // Simple count for now. In real world we'd track unique IDs.
                            if ("DRIVER".equals(e.type)) {
                                drivers++;
                            } else {
                                requests++;
                            }
                        }

                        // Surge Logic: If Requests > Drivers * 1.5 (and at least some activity)
                        if (requests > 0 && requests > drivers * 1.5) {
                            double surgeMultiplier = Math.min(3.0, 1.0 + (double) requests / (Math.max(1, drivers)));
                            out.collect(String.format(
                                    "🔥 SURGE DETECTED in Grid [%s]! Demand: %d, Supply: %d, Multiplier: %.1fx",
                                    gridId, requests, drivers, surgeMultiplier));
                        }
                    }
                });

        surgeAlerts.print();

        env.execute("Uber Surge Pricing Engine");
    }
}