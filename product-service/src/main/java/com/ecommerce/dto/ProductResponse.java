package com.ecommerce.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Outbound DTO — what the API returns.
 * Keeps internal details (like JPA auditing fields) private.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String sku;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
