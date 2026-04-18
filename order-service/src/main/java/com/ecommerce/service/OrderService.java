package com.ecommerce.service;

import com.ecommerce.dto.Dtos;
import com.ecommerce.event.OrderPlacedEvent;
import com.ecommerce.model.Order;
import com.ecommerce.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final String ORDER_PLACED_TOPIC = "order-placed";

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    // lb:// URI — Spring Cloud LoadBalancer resolves "product-service" via Eureka
    private final WebClient.Builder webClientBuilder;

    // ── Place Order ──────────────────────────────────────────────────────────

    public Dtos.OrderResponse placeOrder(Dtos.PlaceOrderRequest request) {
        log.info("Placing order for customer={}", request.getCustomerEmail());

        List<Order.OrderItem> resolvedItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        // 1. For each requested item, call Product Service to get price
        //    and reserve stock. This is a synchronous call — if the product
        //    service is down the order fails fast (circuit breaker in Gateway
        //    handles retries at the edge).
        for (Dtos.ItemRequest itemReq : request.getItems()) {
            ProductDetails product = fetchProductDetails(itemReq.getProductId());

            // 2. Reserve stock (atomic SQL UPDATE in Product Service)
            boolean reserved = reserveStock(itemReq.getProductId(), itemReq.getQuantity());
            if (!reserved) {
                throw new IllegalStateException(
                        "Insufficient stock for product id=" + itemReq.getProductId() +
                        " (requested " + itemReq.getQuantity() + " units)");
            }

            BigDecimal subtotal = product.price().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            resolvedItems.add(Order.OrderItem.builder()
                    .productId(itemReq.getProductId())
                    .productName(product.name())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(product.price())
                    .subtotal(subtotal)
                    .build());

            total = total.add(subtotal);
        }

        // 3. Persist the order in MongoDB
        Order order = Order.builder()
                .customerEmail(request.getCustomerEmail())
                .customerName(request.getCustomerName())
                .items(resolvedItems)
                .totalAmount(total)
                .status(Order.OrderStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Order saved = orderRepository.save(order);
        log.info("Order saved id={}", saved.getId());

        // 4. Publish Kafka event — fire-and-forget with callback logging
        publishOrderPlacedEvent(saved);

        return mapToResponse(saved);
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    public List<Dtos.OrderResponse> getOrdersByEmail(String email) {
        return orderRepository.findByCustomerEmail(email)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public Dtos.OrderResponse getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
    }

    public Dtos.OrderResponse cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        if (order.getStatus() == Order.OrderStatus.SHIPPED) {
            throw new IllegalStateException("Cannot cancel a shipped order.");
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        return mapToResponse(orderRepository.save(order));
    }

    // ── Product Service integration ──────────────────────────────────────────

    private ProductDetails fetchProductDetails(Long productId) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri("lb://product-service/api/products/{id}", productId)
                    .retrieve()
                    .bodyToMono(ProductDetails.class)
                    .block();  // blocking is fine in a traditional servlet thread
        } catch (WebClientResponseException.NotFound e) {
            throw new EntityNotFoundException("Product not found: " + productId);
        } catch (Exception e) {
            log.error("Failed to fetch product id={}", productId, e);
            throw new RuntimeException("Product service unavailable. Please try again.");
        }
    }

    private boolean reserveStock(Long productId, int quantity) {
        try {
            Map<?, ?> result = webClientBuilder.build()
                    .patch()
                    .uri("lb://product-service/api/products/{id}/reserve?quantity={q}",
                            productId, quantity)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return Boolean.TRUE.equals(result != null ? result.get("reserved") : false);
        } catch (WebClientResponseException e) {
            log.warn("Stock reservation failed for productId={}: {}", productId, e.getStatusCode());
            return false;
        }
    }

    // ── Kafka publishing ─────────────────────────────────────────────────────

    private void publishOrderPlacedEvent(Order order) {
        OrderPlacedEvent event = new OrderPlacedEvent(
                order.getId(),
                order.getCustomerEmail(),
                order.getCustomerName(),
                order.getItems().stream()
                        .map(i -> new OrderPlacedEvent.OrderItem(
                                i.getProductId(), i.getProductName(),
                                i.getQuantity(), i.getUnitPrice()))
                        .toList(),
                order.getTotalAmount(),
                order.getCreatedAt()
        );

        // sendAsync returns a CompletableFuture — we attach callbacks to log
        // success/failure without blocking the request thread.
        CompletableFuture<SendResult<String, OrderPlacedEvent>> future =
                kafkaTemplate.send(ORDER_PLACED_TOPIC, order.getId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Published order-placed event orderId={} partition={} offset={}",
                        order.getId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                // In production: send to a dead-letter topic or retry queue
                log.error("Failed to publish order-placed event for orderId={}", order.getId(), ex);
            }
        });
    }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private Dtos.OrderResponse mapToResponse(Order o) {
        List<Dtos.ItemResponse> items = o.getItems().stream()
                .map(i -> Dtos.ItemResponse.builder()
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .subtotal(i.getSubtotal())
                        .build())
                .toList();

        return Dtos.OrderResponse.builder()
                .id(o.getId())
                .customerEmail(o.getCustomerEmail())
                .customerName(o.getCustomerName())
                .items(items)
                .totalAmount(o.getTotalAmount())
                .status(o.getStatus())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }

    /** Local record — mirrors the fields we need from ProductResponse. */
    private record ProductDetails(Long id, String name, BigDecimal price, Integer stockQuantity) {}
}
