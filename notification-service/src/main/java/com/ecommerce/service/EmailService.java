package com.ecommerce.service;

import com.ecommerce.consumer.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${notification.mail.from:orders@ecommerce.local}")
    private String fromAddress;

    @Value("${notification.mail.enabled:false}")
    private boolean mailEnabled;

    /**
     * Sends a confirmation email for a placed order.
     * When mail.enabled=false (default in dev) it only logs — no SMTP needed.
     * Set mail.enabled=true and configure spring.mail.* to send real emails.
     */
    public void sendOrderConfirmation(OrderPlacedEvent event) {
        String subject = "Order Confirmation #" + event.orderId();
        String body    = buildEmailBody(event);

        if (mailEnabled) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(event.customerEmail());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {} for order {}", event.customerEmail(), event.orderId());
        } else {
            // ── FAKE EMAIL — logged to console ──────────────────────────────
            log.info("""
                    ╔══════════════════════════════════════════════════════╗
                    ║  [FAKE EMAIL — set notification.mail.enabled=true    ║
                    ║   and configure spring.mail.* to send real emails]   ║
                    ╠══════════════════════════════════════════════════════╣
                    ║  To:      {}
                    ║  Subject: {}
                    ╠══════════════════════════════════════════════════════╣
                    {}
                    ╚══════════════════════════════════════════════════════╝
                    """,
                    event.customerEmail(), subject, body);
        }
    }

    private String buildEmailBody(OrderPlacedEvent event) {
        String itemLines = event.items().stream()
                .map(i -> String.format("  • %s x%d @ $%.2f = $%.2f",
                        i.productName(), i.quantity(),
                        i.unitPrice(), i.unitPrice().multiply(BigDecimal.valueOf(i.quantity()))))
                .collect(Collectors.joining("\n"));

        return """
                Hi %s,
                
                Thank you for your order! Here's your summary:
                
                Order ID : %s
                Placed   : %s
                
                Items:
                %s
                
                ──────────────────────────────
                Total: $%.2f
                
                We'll notify you when your order ships.
                
                — The E-Commerce Team
                """.formatted(
                event.customerName(),
                event.orderId(),
                event.placedAt(),
                itemLines,
                event.totalAmount());
    }
}
