package slow.kafka.consumer.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ConsumerService {

    private final AtomicLong messageCounter = new AtomicLong(0);
    private long startTime = System.currentTimeMillis();

    @KafkaListener(topics = "test-topic", containerFactory = "kafkaListenerContainerFactory")
    public void consumeMessage(String message, Acknowledgment acknowledgement) {

        long count = messageCounter.incrementAndGet();

        // Periodically log performance
        if (count % 5000 == 0) {
            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - startTime;
            double messagesPerSecond = count * 1000.0 / elapsed;

            System.out.printf("Processed %d messages in %d ms (%.2f msg/sec) - Latest: %s%n",
                    count, elapsed, messagesPerSecond, message);
        }

        acknowledgement.acknowledge();
    }

    public void resetCounters() {
        messageCounter.set(0);
        startTime = System.currentTimeMillis();
        System.out.println("Consumer counters reset at " + LocalDateTime.now());
    }
}
