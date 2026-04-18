package com.ecommerce;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    /**
     * Declaring topics as beans ensures they exist before any producer or
     * consumer starts. Kafka auto-creates topics by default, but explicit
     * declaration lets you control partition count and replication factor.
     */
    @Bean
    public NewTopic orderPlacedTopic() {
        return TopicBuilder.name("order-placed")
                .partitions(3)       // 3 partitions = 3 consumers can read in parallel
                .replicas(1)         // 1 replica is fine for local dev; use 3 in production
                .build();
    }

    @Bean
    public NewTopic orderCancelledTopic() {
        return TopicBuilder.name("order-cancelled")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
