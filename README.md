# slow-kafka-experiment
An experiment to see how slow I make a kafka producer/consumer app

Stack:
 Java 25 
 Spring 4.0.0-M3
 Zookeeper
 Kafka 
 Kafka UI (Monitoring tool for Kafka)
 Yaak (For sending REST requests to Producer App)

My Specs:
 Processor: Intel(R) Core(TM) i7-14700HX (2.10 GHz), 28 Threads
 RAM: 32 GB
 OS: Windows 11

Local setup:
 1. Install Java, Gradle and Docker
 2. Build both producer and consumer apps (gradle clean build)
 2. Run docker-compose up -d to start Zookeeper And 2 Kafka brokers 
 3. [TOPIC CREATION] Create topic for testing ``docker exec -it kafka1 kafka-topics --create --topic test-topic --bootstrap-server kafka1:29092,kafka2:29093 --replication-factor 2 --partitions 10``
   - verify succesful creation ``docker exec kafka-perf-testing kafka-topics --list --bootstrap-server kafka-perf-testing:9092``
 4. Run both apps either through Intellij or manually
     4.1 enter slow-kafka-experiment\slow.kafka.producer\build\libs and slow-kafka-experiment\slow.kafka.consumer\build\libs
     4.2 java -jar slow.kafka.producer-0.0.1-SNAPSHOT.jar
     4.3 java -jar slow.kafka.consumer-0.0.1-SNAPSHOT.jar

Initial Kafka setup 
broker: 2 Kafka brokers managed by Zookeeper

topic:
 partitions: 10 
  A Kafka topic is split into partitions, the idea is to distribute the data in the topic, these partitions give Kafka its scalability as the consumers aim to create consumer threads equal
  to the number of partitions.
 replication factor: 2 
  This means that the leader partition will have its data replicated on the second broker.
  I'm doing this to make Kafka put resources into syncing the replica.

producer:
 acks = 1 -> 
  When acks=1 , producers consider messages as "written successfully" when the message was acknowledged by only the leader.
  Leader response is requested, but replication is not a guarantee as it happens in the background. If an ack is not received, the producer may retry the request. If the leader broker goes offline unexpectedly but replicas haven’t replicated the data yet, we have a data loss.
  
consumer:
 concurrency: 10 -> 10 consumer threads reading from 10 partitions 

I can think of 4 levels on which we can make changes to slow processing

1) Broker
   - Reduce num.network.threads and num.io.threads to 1
   - Decrease buffer sizes (socket.send.buffer.bytes, socket.receive.buffer.bytes)
   - Force frequent flushes (log.flush.interval.messages=1)
2) Topic
   - increase number of segments for topic 
   - decrease message retention (increases overhead for deleting messages)
3) Consumer 
   - set max.poll.records = 1
   - disable auto-commit and do manual acknolwedgements
4) Producer
   - Reduce batch.size to 1
   - Set linger.ms=0 (no batching)
   - Add compression.type=gzip
   - every message has the same message id 
      - message id goes through hash function and the messages with the same id go into the same partition
      - this essentially makes consumption single-threaded, also the leftover consumer threads are wasting resources and not doing anything
    
Results:
 Before each tests I run 2 warmups, one with 1k events and another with 5k events.
 There will be 3 cases that I run 
  1) 10k events
  2) 50k events
  3) 100k events
  4) 1 million events

 1) Baseline 
  1) 10k events   
     producer: Sent 10000 messages in 147 ms (68027.21 msg/sec) 
     consumer: Processed 10000 messages in 220 ms (45454.55 msg/sec)
  2) 50k events
     producer: Sent 50000 messages in 117 ms (427350.43 msg/sec)
     consumer: Processed 50000 messages in 131 ms (381679.39 msg/sec)
  3) 100k events
     producer: Sent 100000 messages in 193 ms (518134.72 msg/sec)
     consumer: Processed 100000 messages in 200 ms (500000.00 msg/sec) 
  4) 1 million events
     producer: Sent 1000000 messages in 1470 ms (680272.11 msg/sec)
     consumer: Processed 1000000 messages in 1481 ms (675219.45 msg/sec)

 2) Broker config changes 
      # reducing these threads intentially for perf testing
      KAFKA_NUM_NETWORK_THREADS: 1
      KAFKA_NUM_IO_THREADS: 1
      KAFKA_SOCKET_SEND_BUFFER_BYTES: 1024          # Tiny send buffer (default ~102KB)
      KAFKA_SOCKET_RECEIVE_BUFFER_BYTES: 1024       # Tiny receive buffer (default ~102KB)
      KAFKA_SOCKET_REQUEST_MAX_BYTES: 1048576       # Reduced max request size
      KAFKA_LOG_FLUSH_INTERVAL_MESSAGES: 1          # Flush after EVERY message
      KAFKA_LOG_FLUSH_INTERVAL_MS: 10               # Also flush every 10ms
      KAFKA_LOG_SEGMENT_BYTES: 1048576              # Small log segments (1MB)
      # Additional slow settings
      KAFKA_REPLICA_FETCH_MIN_BYTES: 1              # Minimum fetch size
      KAFKA_REPLICA_FETCH_WAIT_MAX_MS: 100          # Reduce wait time
      KAFKA_LOG_ROLL_MS: 60000                      # Roll logs frequently (1 minute)
      KAFKA_LOG_RETENTION_CHECK_INTERVAL_MS: 30000  # Check retention frequently

     1) 10k events 
        producer: Sent 10000 messages in 33 ms (303030.30 msg/sec)
        consumer: Processed 10000 messages in 210 ms (47619.05 msg/sec)
     2) 50k events
        producer: Sent 50000 messages in 98 ms (510204.08 msg/sec)
        consumer: Processed 50000 messages in 1015 ms (49261.08 msg/sec)
     3) 100k events
        producer: Sent 100000 messages in 144 ms (694444.44 msg/sec)
        consumer: Processed 100000 messages in 1724 ms (58004.64 msg/sec)
     4) 1 million events   
        producer: Sent 1000000 messages in 11606 ms (86162.33 msg/sec)
        consumer: Processed 1000000 messages in 20913 ms (47817.15 msg/sec)

 3) Consumer
    - Max poll record = 1
    - Manual acknowledgement with delay
    - fetching data byte by byte 
    - having only 1 thread read 10 partitions and switch context 
  1) 10k events
     producer: Sent 10000 messages in 17 ms (588235.29 msg/sec)
     consumer: Processed 10000 messages in 100905 ms (99.10 msg/sec) - Latest: Sending batch message 69304 at 2025-09-23T05:38:21.468162600
  2) 50k events
     producer: Sent 50000 messages in 65 ms (769230.77 msg/sec)
     consumer: Processed 50000 messages in 507967 ms (98.43 msg/sec)
  3) 100k events
     producer: Sent 100000 messages in 112 ms (892857.14 msg/sec)
     consumer: Processed 100000 messages in 1015010 ms (98.52 msg/sec)
  4) 1 million events

  Honestly during the warmup this started so slow I though I completely broke it for a second.
  And when I saw that the warmup took 41 seconds I knew this was going to be good.

 4) Topic 
   - Increase segments  