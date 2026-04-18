package com.ecommerce.controller;

import com.ecommerce.dto.Dtos;
import com.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * POST /api/orders
     * Full flow: validate → fetch product prices → reserve stock →
     * save to MongoDB → publish Kafka event → return order.
     */
    @PostMapping
    public ResponseEntity<Dtos.OrderResponse> placeOrder(
            @Valid @RequestBody Dtos.PlaceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.placeOrder(request));
    }

    // GET /api/orders/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Dtos.OrderResponse> getOrder(@PathVariable String id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    // GET /api/orders?email=alice@example.com
    @GetMapping
    public ResponseEntity<List<Dtos.OrderResponse>> getOrdersByEmail(
            @RequestParam String email) {
        return ResponseEntity.ok(orderService.getOrdersByEmail(email));
    }

    // PATCH /api/orders/{id}/cancel
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Dtos.OrderResponse> cancelOrder(@PathVariable String id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }
}
