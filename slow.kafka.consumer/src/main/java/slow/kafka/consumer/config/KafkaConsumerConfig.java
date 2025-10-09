package slow.kafka.consumer.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import slow.kafka.consumer.util.XmlDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "performance-test-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // tried changing the message format to xml but it got faster....
        //props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, XmlDeserializer.class);

        // Essential settings
        //props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);

        // process one message at a time (vs 500 default)
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);

        // disable auto-commit and commit manually (should be slower)
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // reduce fetch size to minimum (more network calls)
        props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 1); // 1 byte vs 50MB default

        // increase fetch wait time (slower polling)
        // actually blocks when the broker can't get enough data for FETCH_MAX_BYTES_CONFIG
        // but we're constantly sending events and max bytes = 1, so this will never block the consumer
        //props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 5000); // 5s vs 500ms default

        // smaller receive buffer
        props.put(ConsumerConfig.RECEIVE_BUFFER_CONFIG, 1); // 1 byte vs 64KB default

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        //factory.setConcurrency(10);
        // 1 thread will now switch context between 10 partitions
        factory.setConcurrency(1);
        factory.setAutoStartup(true);
        // use manual offset commits to decrease performance
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        // 1 second acknowledgement delay
        // Useless config, only wors for ack mode = COUNT or TIME
        //factory.getContainerProperties().setAckTime(1000);

        // individual message processing
        factory.setBatchListener(false);
        return factory;
    }
}
