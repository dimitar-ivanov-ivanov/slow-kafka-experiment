# slow-kafka-experiment
An experiment to see how slow I make a kafka producer/consumer app

# Stack:
 - Java 25 
 - Spring 4.0.0-M3
 - Zookeeper
 - Kafka 
 - Kafka UI (Monitoring tool for Kafka)
 - Yaak (For sending REST requests to Producer App)

# My Specs:
 - Processor: Intel(R) Core(TM) i7-14700HX (2.10 GHz), 28 Threads
 - RAM: 32 GB
 - OS: Windows 11

# Local setup:
  - Install Java, Gradle and Docker
  - Build both producer and consumer apps (gradle clean build)
  - Run docker-compose up -d to start Zookeeper And 2 Kafka brokers 
  - [TOPIC CREATION] Create topic for testing ``docker exec -it kafka1 kafka-topics --create --topic test-topic --bootstrap-server kafka1:29092,kafka2:29093 --replication-factor 2 --partitions 10``
   - verify succesful creation ``docker exec kafka-perf-testing kafka-topics --list --bootstrap-server kafka-perf-testing:9092``
  - Run both apps either through Intellij or manually
     - enter slow-kafka-experiment\slow.kafka.producer\build\libs and slow-kafka-experiment\slow.kafka.consumer\build\libs
     - java -jar slow.kafka.producer-0.0.1-SNAPSHOT.jar
     - java -jar slow.kafka.consumer-0.0.1-SNAPSHOT.jar

# Initial Kafka setup 
 - broker: 
   - 2 Kafka brokers managed by Zookeeper
   - one topic/producer/consumer 

 - topic:
   - partitions: 10 
     - A Kafka topic is split into partitions, the idea is to distribute the data in the topic, these partitions give Kafka its scalability as the consumers aim to create consumer threads equal to the number of partitions.
   - replication factor: 2 
     - This means that the leader partition will have its data replicated on the second broker.
     I'm doing this to make Kafka put resources into syncing the replica.

- producer:
  - producer is really just a loop over the desired amount of messages, for each iteration a new message is published to Kafka.
    It produces an event in Strin format, where the value is "Sending message COUNT at TIME" without any key or headers. Lack of key means that Kafka will put the event into partitions 
    round-robin style.
  
- consumer:
  - The consumer logic is a literally just increasing a counter and acknowledging that the message was consumed. For each 5K messages we make a log to track our
   concurrency: 10 -> 10 consumer threads reading from 10 partitions 

I can think of 4 levels on which we can make changes to slow processing

## Results:
 - I'm testing after 2 changes 
    - First change is on broker level
    - Second is on consumer level
 - Before each tests I run 2 warmups, one with 1k events and another with 5k events.
   There will be 3 cases that I run
    - 10k events 
    - 50k events 
    - 100k events

## Baseline
    1) 10k events   
       producer: Sent 10000 messages in 147 ms (68027.21 msg/sec) 
       consumer: Processed 10000 messages in 220 ms (45454.55 msg/sec)
    2) 50k events
       producer: Sent 50000 messages in 117 ms (427350.43 msg/sec)
       consumer: Processed 50000 messages in 131 ms (381679.39 msg/sec)
    3) 100k events
       producer: Sent 100000 messages in 193 ms (518134.72 msg/sec)
       consumer: Processed 100000 messages in 200 ms (500000.00 msg/sec)
 
## Broker config changes 
    - refer to docker compose
     1) 10k events 
        producer: Sent 10000 messages in 164 ms (60975.61 msg/sec)
        consumer: Processed 10000 messages in 405 ms (24691.36 msg/sec)
     2) 50k events
        producer: Sent 50000 messages in 205 ms (243902.44 msg/sec)
        consumer: Processed 50000 messages in 2175 ms (22988.51 msg/sec)
     3) 100k events
        producer: Sent 100000 messages in 230 ms (434782.61 msg/sec)
        consumer: Processed 100000 messages in 2505 ms (39920.16 msg/sec)

## Consumer config changes 
    - refer to slow.kafka.consumer.config.KafkaConsumerConfig
     1) 10k events 
        producer: Sent 10000 messages in 54 ms (185185.19 msg/sec)
        consumer: Processed 10000 messages in 158950 ms (62.91 msg/sec)
     2) 50k events
        producer: Sent 50000 messages in 224 ms (223214.29 msg/sec)
        consumer: Processed 50000 messages in 761824 ms (65.63 msg/sec) 
     3) 100k events       
        producer: Sent 100000 messages in 216 ms (462962.96 msg/sec)
        consumer: Processed 100000 messages in 1531620 ms (65.29 msg/sec)  

## Overall:
    10K: 45,454 → 62.91 = 722x slower
    50K: 381,679 → 65.63 = 5,817x slower
    100K: 500,000 → 65.29 = 7,658x slower