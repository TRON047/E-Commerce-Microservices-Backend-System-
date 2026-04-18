package com.ecommerce.dto;

import com.ecommerce.model.Order;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {}

    // ── Request ──────────────────────────────────────────────────────────────

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ItemRequest {
        @NotNull(message = "productId is required")
        private Long productId;

        @Min(value = 1, message = "Quantity must be at least 1")
        private int quantity;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PlaceOrderRequest {
        @NotBlank @Email
        private String customerEmail;

        @NotBlank
        private String customerName;

        @NotEmpty
        private List<ItemRequest> items;
    }

    // ── Response ─────────────────────────────────────────────────────────────

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ItemResponse {
        private Long productId;
        private String productName;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OrderResponse {
        private String id;
        private String customerEmail;
        private String customerName;
        private List<ItemResponse> items;
        private BigDecimal totalAmount;
        private Order.OrderStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
