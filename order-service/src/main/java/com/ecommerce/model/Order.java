package com.ecommerce.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Stored as a MongoDB document — the order and all its items live in one
 * document. No JOINs needed; reads are a single collection lookup.
 */
@Document(collection = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    private String id;   // MongoDB generates a hex ObjectId string

    private String customerEmail;
    private String customerName;

    private List<OrderItem> items;

    private BigDecimal totalAmount;

    private OrderStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum OrderStatus {
        PENDING,     // payment not yet confirmed
        CONFIRMED,   // stock reserved, Kafka event fired
        CANCELLED,   // user cancelled or stock unavailable
        SHIPPED
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private Long productId;
        private String productName;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}
