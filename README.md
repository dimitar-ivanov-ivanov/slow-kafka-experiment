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
 producer is really just a loop over the desired amount of messages, for each iteration a new message is published to Kafka.
 It produces an event in Strin format, where the value is "Sending message COUNT at TIME" without any key or headers. Lack of key means that Kafka will put the event into partitions 
 round-robin style.
 acks = 1 -> 
  When acks=1 , producers consider messages as "written successfully" when the message was acknowledged by only the leader.
  Leader response is requested, but replication is not a guarantee as it happens in the background. If an ack is not received, the producer may retry the request. If the leader broker goes offline unexpectedly but replicas haven’t replicated the data yet, we have a data loss.
  
consumer:
 The consumer logic is a literally just increasing a counter and acknowledging that the message was consumed. For each 5K messages we make a log to track our
 concurrency: 10 -> 10 consumer threads reading from 10 partitions 

I can think of 4 levels on which we can make changes to slow processing

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
     1) 10k events 
        producer: Sent 10000 messages in 164 ms (60975.61 msg/sec)
        consumer: Processed 10000 messages in 405 ms (24691.36 msg/sec)
     2) 50k events
        producer: Sent 50000 messages in 205 ms (243902.44 msg/sec)
        consumer: Processed 50000 messages in 2175 ms (22988.51 msg/sec)
     3) 100k events
        producer: Sent 100000 messages in 230 ms (434782.61 msg/sec)
        consumer: Processed 100000 messages in 2505 ms (39920.16 msg/sec)

 3) Consumer config changes 
     1) 10k events 
        producer: Sent 10000 messages in 54 ms (185185.19 msg/sec)
        consumer: Processed 10000 messages in 158950 ms (62.91 msg/sec)
     2) 50k events
        producer: Sent 50000 messages in 224 ms (223214.29 msg/sec)
        consumer: Processed 50000 messages in 761824 ms (65.63 msg/sec) 
     3) 100k events       
        producer: Sent 100000 messages in 216 ms (462962.96 msg/sec)
        consumer: Processed 100000 messages in 1531620 ms (65.29 msg/sec) 
 4) Enabling XML changes 
     1) 10k events
     2) 50k events
     3) 100k events        

Overall:
10K: 45,454 → 62.91 = 🚨 722x slower
50K: 381,679 → 65.63 = 🚨 5,817x slower
100K: 500,000 → 65.29 = 🚨 7,658x slower

  Honestly during the warmup this started so slow I though I completely broke it for a second.
  And when I saw that the warmup took 41 seconds I knew this was going to be good.

 4) Topic 
   - Increase segments  