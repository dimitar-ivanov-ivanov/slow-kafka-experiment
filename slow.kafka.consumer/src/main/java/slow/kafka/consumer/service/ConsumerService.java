package slow.kafka.consumer.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ConsumerService {

    private final AtomicLong messageCounter = new AtomicLong(0);
    private long startTime = System.currentTimeMillis();

    @KafkaListener(topics = "test-topic", containerFactory = "kafkaListenerContainerFactory")
    public void consumeMessage(String message) {

        long count = messageCounter.incrementAndGet();

        // Periodically log performance
        if (count % 500 == 0) {
            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - startTime;
            double messagesPerSecond = count * 1000.0 / elapsed;

            System.out.printf("Processed %d messages in %d ms (%.2f msg/sec) - Latest: %s%n",
                    count, elapsed, messagesPerSecond, message);
        }

        // Simulate some processing time (optional - remove for pure throughput test)
        // try { Thread.sleep(1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Manual acknowledgment (if needed)
        // acknowledgment.acknowledge();
    }

    public void resetCounters() {
        messageCounter.set(0);
        startTime = System.currentTimeMillis();
        System.out.println("Consumer counters reset at " + LocalDateTime.now());
    }
}
