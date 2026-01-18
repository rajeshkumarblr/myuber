import json
import time
import random
from kafka import KafkaProducer

# CONFIG
KAFKA_TOPIC_DRIVERS = 'driver-locations'
KAFKA_TOPIC_REQUESTS = 'ride-requests'
BOOTSTRAP_SERVERS = 'localhost:9092'
NUM_DRIVERS = 1000
NUM_REQUESTS_PER_SEC = 500 # Simulate demand

# SAN FRANCISCO BOUNDS
LAT_MIN, LAT_MAX = 37.70, 37.81
LON_MIN, LON_MAX = -122.52, -122.37

def create_producer():
    return KafkaProducer(
        bootstrap_servers=BOOTSTRAP_SERVERS,
        value_serializer=lambda x: json.dumps(x).encode('utf-8')
    )

def generate_drivers():
    # Initialize drivers at random spots
    return [{
        'driver_id': i,
        'lat': random.uniform(LAT_MIN, LAT_MAX),
        'lon': random.uniform(LON_MIN, LON_MAX),
        'status': 'AVAILABLE'
    } for i in range(NUM_DRIVERS)]

def run_simulation():
    producer = create_producer()
    drivers = generate_drivers()
    print(f"🚀 Starting simulation for {NUM_DRIVERS} drivers and demand...")

    while True:
        # 1. Update and Send Driver Locations
        for driver in drivers:
            # Move driver slightly (simulate driving)
            driver['lat'] += random.uniform(-0.001, 0.001)
            driver['lon'] += random.uniform(-0.001, 0.001)
            driver['timestamp'] = int(time.time() * 1000)
            
            # Send to Kafka
            producer.send(KAFKA_TOPIC_DRIVERS, value=driver)
        
        # 2. Generate and Send Ride Requests (Demand)
        # Create a "Hot Area" around downtown (approx 37.7749, -122.4194)
        for _ in range(NUM_REQUESTS_PER_SEC):
            is_hot_area = random.random() < 0.3 # 30% chane to be in hot area
            
            if is_hot_area:
                 lat = random.uniform(37.77, 37.78)
                 lon = random.uniform(-122.42, -122.41)
            else:
                 lat = random.uniform(LAT_MIN, LAT_MAX)
                 lon = random.uniform(LON_MIN, LON_MAX)

            request = {
                'request_id': random.randint(100000, 999999),
                'lat': lat,
                'lon': lon,
                'timestamp': int(time.time() * 1000)
            }
            producer.send(KAFKA_TOPIC_REQUESTS, value=request)

        # Flush batch to network
        producer.flush()
        print(f"📡 Pushed {NUM_DRIVERS} driver updates and {NUM_REQUESTS_PER_SEC} ride requests...")
        time.sleep(1) # 1 second interval

if __name__ == "__main__":
    run_simulation()