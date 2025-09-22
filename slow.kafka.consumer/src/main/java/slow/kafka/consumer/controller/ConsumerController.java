package slow.kafka.consumer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import slow.kafka.consumer.service.ConsumerService;

@RestController
@RequestMapping("/api/consumer")
public class ConsumerController {

    @Autowired
    private ConsumerService consumerService;

    @PostMapping("/reset")
    public String resetCounters() {
        consumerService.resetCounters();
        return "Consumer counters reset successfully!";
    }
}
