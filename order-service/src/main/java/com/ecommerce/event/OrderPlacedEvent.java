package com.ecommerce.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The message published to the "order-placed" Kafka topic.
 *
 * Using a Java record keeps this immutable and concise.
 * In a real system you'd put this in a shared library or use Avro/Schema Registry
 * so both producer and consumer use the exact same contract.
 *
 * This same class is copied into notification-service — in a real project,
 * extract it to a shared `events` Maven module instead.
 */
public record OrderPlacedEvent(
        String orderId,
        String customerEmail,
        String customerName,
        List<OrderItem> items,
        BigDecimal totalAmount,
        LocalDateTime placedAt
) {
    public record OrderItem(
            Long productId,
            String productName,
            int quantity,
            BigDecimal unitPrice
    ) {}
}
