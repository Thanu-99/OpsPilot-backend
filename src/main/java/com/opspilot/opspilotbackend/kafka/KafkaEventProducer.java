package com.opspilot.opspilotbackend.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaEventProducer {

    private static final String TOPIC = "opspilot-events";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaEventProducer(
            KafkaTemplate<String, String> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEvent(String event) {
        kafkaTemplate.send(TOPIC, event);
    }
}