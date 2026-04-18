package com.ecommerce;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Fallback responses when a downstream service is unreachable.
 * The circuit breaker in application.yml forwards to these endpoints.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/products")
    public ResponseEntity<Map<String, String>> productFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "unavailable",
                        "message", "Product service is currently unavailable. Please try again later."
                ));
    }

    @GetMapping("/orders")
    public ResponseEntity<Map<String, String>> orderFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "unavailable",
                        "message", "Order service is currently unavailable. Please try again later."
                ));
    }
}
