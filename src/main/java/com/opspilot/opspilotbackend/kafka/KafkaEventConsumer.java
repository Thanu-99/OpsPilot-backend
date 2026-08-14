package com.opspilot.opspilotbackend.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaEventConsumer {

    @KafkaListener(
            topics = "opspilot-events",
            groupId = "opspilot-group"
    )
    public void consumeEvent(String event) {

        System.out.println(
                "🔥 Kafka Event Received: " + event
        );
    }
}