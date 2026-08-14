package com.opspilot.opspilotbackend.kafka;

import com.opspilot.opspilotbackend.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaNotificationConsumer {

    private final NotificationService notificationService;

    public KafkaNotificationConsumer(
            NotificationService notificationService) {

        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = "opspilot-events",
            groupId = "opspilot-notification-group"
    )
    public void consumeEvent(String event) {

        System.out.println("🔥 Notification Consumer Received: " + event);

        if (event.startsWith("LOW_STOCK:")) {

            String[] parts = event.split(":");

            if (parts.length < 5) {
                System.out.println(
                        "⚠️ Invalid LOW_STOCK event: " + event
                );
                return;
            }

            Long userId = Long.parseLong(parts[1]);
            String productName = parts[3];
            String quantity = parts[4];

            notificationService.createNotification(
                    userId,
                    "LOW_STOCK",
                    "Low Stock Alert",
                    "Product " + productName
                            + " has low stock. Current quantity: "
                            + quantity
            );

            System.out.println(
                    "🔔 Low stock notification created for user: "
                            + userId
            );
        }
    }
}

