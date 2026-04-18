package com.ecommerce;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Provides a no-op JavaMailSender when spring.mail is not configured.
 * This lets the app start in dev mode without an SMTP server.
 * EmailService checks notification.mail.enabled before calling mailSender.send().
 */
@Configuration
public class MailConfig {

    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    public JavaMailSender noOpMailSender() {
        // Returns a sender that is never actually called (EmailService guards it)
        return new JavaMailSenderImpl();
    }
}
