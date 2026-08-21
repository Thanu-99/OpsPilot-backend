package com.opspilot.opspilotbackend.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaEventProducer {

    private static final String TOPIC = "opspilot-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final boolean enabled;

    public KafkaEventProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${opspilot.kafka.enabled:false}") boolean enabled) {

        this.kafkaTemplate = kafkaTemplate;
        this.enabled = enabled;
    }

    public void sendEvent(String event) {
        if (!enabled) {
            return;
        }

        kafkaTemplate.send(TOPIC, event);
    }
}
