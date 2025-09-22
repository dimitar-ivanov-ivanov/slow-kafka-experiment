# slow-kafka-experiment
An experiment to see how slow I make a kafka producer/consumer app

Stack:
 Java 25 
 Spring 4.0.0-M3
 Zookeeper
 Kafka 
 Kafka UI (Monitoring tool for Kafka)

My Specs:
 Processor: Intel(R) Core(TM) i7-14700HX (2.10 GHz), 28 Threads
 RAM: 32 GB
 OS: Windows 11

Local setup:
 1. Install Java, Gradle and Docker
 2. Build both producer and consumer apps (gradle clean build)
 2. Run docker-compose up -d to start Zookeeper And Kafka 
 3. [TOPIC CREATION] Create topic for testing ``docker exec -it kafka-perf-testing kafka-topics --create --topic test-topic --bootstrap-server kafka-perf-testing:9092 --partitions 10``
   - verify succesful creation ``docker exec kafka-perf-testing kafka-topics --list --bootstrap-server kafka-perf-testing:9092``
 4. Run both apps either through Intellij or manually
     4.1 enter slow-kafka-experiment\slow.kafka.producer\build\libs and slow-kafka-experiment\slow.kafka.consumer\build\libs
     4.2 java -jar slow.kafka.producer-0.0.1-SNAPSHOT.jar
     4.3 java -jar slow.kafka.consumer-0.0.1-SNAPSHOT.jar

Initial Kafka setup 
topic:
 partitions: 10 
  A Kafka topic is split into partitions, the idea is to distribute the data in the topic, these partitions give Kafka its scalability as the consumers aim to create consumer threads equal
  to the number of partitions.
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
   - acks = all, meaning the producer waits for all partition replicas to replicate the data
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

 1) Baseline 
      1) 10k events
         producer: Sent 10000 messages in 55 ms (181818.18 msg/sec)
         consumer: Processed 10000 messages in 104 ms (96153.85 msg/sec)
      2) 50k events
         producer: Sent 50000 messages in 67 ms (746268.66 msg/sec)
         consumer: Processed 50000 messages in 229 ms (218340.61 msg/sec)
      3) 100k events:
         producer: Sent 100000 messages in 102 ms (980392.16 msg/sec)
         consumer: Processed 100000 messages in 394 ms (253807.11 msg/sec) 

 2) Reduce num.network.threads and num.io.threads to 1 
     num.network.threads
     num.io.threads

 3) Decrease buffer sizes (socket.send.buffer.bytes, socket.receive.buffer.bytes)    
     explain what the buffer is..
     socket.send.buffer.bytes
     socket.receive.buffer.bytes
    
