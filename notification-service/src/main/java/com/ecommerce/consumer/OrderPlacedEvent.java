package com.ecommerce.consumer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Mirror of the event published by Order Service.
 *
 * In a production system, extract this to a shared Maven module:
 *   <artifactId>ecommerce-events</artifactId>
 * Both order-service and notification-service depend on it.
 * Using Avro + Confluent Schema Registry is the enterprise-grade approach.
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
