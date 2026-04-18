package com.ecommerce.consumer;

import com.ecommerce.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final EmailService emailService;

    /**
     * Listens on the "order-placed" topic.
     *
     * groupId = "notification-group"
     *   — all instances of this service share the same group, so each
     *     message is processed by exactly ONE instance (competing consumers).
     *     If you scale to 3 replicas across 3 Kafka partitions, each replica
     *     processes its own partition in parallel.
     *
     * containerFactory = "kafkaListenerContainerFactory"
     *   — wired in application.yml; uses JsonDeserializer so the raw bytes
     *     are automatically converted to an OrderPlacedEvent record.
     */
    @KafkaListener(
            topics = "order-placed",
            groupId = "notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderPlaced(
            @Payload OrderPlacedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Consumed order-placed event: orderId={} partition={} offset={}",
                event.orderId(), partition, offset);

        try {
            emailService.sendOrderConfirmation(event);
        } catch (Exception ex) {
            // In production: publish to a dead-letter topic instead of swallowing
            log.error("Failed to send notification for orderId={}", event.orderId(), ex);
        }
    }

    /**
     * Separate listener for order cancellations.
     * Demonstrates that one service can consume multiple topics.
     */
    @KafkaListener(
            topics = "order-cancelled",
            groupId = "notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderCancelled(
            @Payload OrderPlacedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Order cancelled: orderId={} customer={}", event.orderId(), event.customerEmail());
        // emailService.sendCancellationNotice(event);  ← wire up when ready
    }
}
