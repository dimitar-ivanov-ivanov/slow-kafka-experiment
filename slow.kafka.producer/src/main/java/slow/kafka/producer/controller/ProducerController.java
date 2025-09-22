package slow.kafka.producer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import slow.kafka.producer.services.ProducerService;

@RestController
@RequestMapping("/api/producer")
public class ProducerController {

    @Autowired
    private ProducerService producerService;

    @PostMapping("/{count}")
    public String sendBatchMessages(@PathVariable int count) {
        producerService.sendMessages(count);
        return String.format("Sent %d messages successfully!", count);
    }
}
