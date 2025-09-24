package slow.kafka.producer.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ProducerService {

    private static final String TOPIC = "test-topic";

    // We'll use Jmeter and have multiple threads send requests
    // best to have this to make sure we have no race conditions for the message count
    private final AtomicLong messageCounter = new AtomicLong(0);

    @Autowired
    private KafkaTemplate<String, String> template;

    @Autowired
    private RestTemplate restTemplate;

    public void sendMessages(int count) {
        resetConsumerCounters();
        long start = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            String message = String.format("Sending message %d at %s",
                    messageCounter.incrementAndGet(), LocalDateTime.now());

            template.send(TOPIC, message);
        }

        long end = System.currentTimeMillis();
        double messagesPerSecond = (double) (count * 1000) / (end - start);
        IO.println(String.format("Sent %d messages in %d ms (%.2f msg/sec)",
                count,
                (end - start),
                messagesPerSecond));
    }

    private void resetConsumerCounters() {
        try {
            IO.println("Resetting consumer counters...");
            String response = restTemplate.postForObject(
                     "http://localhost:8081/api/consumer/reset",
                    null,
                    String.class
            );
            IO.println("Consumer reset response: " + response);
        } catch (Exception e) {
            IO.println("Failed to reset consumer counters: " + e.getMessage());
        }
    }
}
