package com.ecommerce.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Inbound DTO — what the client sends when creating/updating a product.
 * Never expose the JPA entity directly; this decouples the API contract
 * from the database schema.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    private BigDecimal price;

    @NotNull
    @Min(0)
    private Integer stockQuantity;

    @NotBlank
    private String sku;
}
