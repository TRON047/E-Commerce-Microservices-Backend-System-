package com.ecommerce;

import com.ecommerce.consumer.OrderPlacedEvent;
import com.ecommerce.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Full integration test — spins up an embedded Kafka broker (no Docker needed),
 * publishes a real message, and asserts the consumer calls EmailService.
 *
 * @EmbeddedKafka replaces the real Kafka broker during tests.
 */
@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {"order-placed", "order-cancelled"},
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9093",
                "port=9093"
        }
)
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "notification.mail.enabled=false"
})
@DirtiesContext   // reset Spring context after test to avoid port conflicts
class NotificationIntegrationTest {

    @Autowired
    private KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @SpyBean   // real bean, but we can verify calls were made on it
    private EmailService emailService;

    @Test
    void whenOrderPlacedEventPublished_thenEmailServiceIsCalled() throws InterruptedException {
        OrderPlacedEvent event = new OrderPlacedEvent(
                "order-abc-123",
                "alice@example.com",
                "Alice Smith",
                List.of(new OrderPlacedEvent.OrderItem(
                        1L, "Laptop", 1, new BigDecimal("999.99")
                )),
                new BigDecimal("999.99"),
                LocalDateTime.now()
        );

        kafkaTemplate.send("order-placed", event.orderId(), event);

        // timeout(5000) = wait up to 5 seconds for the async consumer to process
        verify(emailService, timeout(5000).times(1))
                .sendOrderConfirmation(any(OrderPlacedEvent.class));
    }
}
