package com.ecommerce;

import com.ecommerce.consumer.OrderPlacedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, OrderPlacedEvent> consumerFactory() {
        JsonDeserializer<OrderPlacedEvent> deserializer =
                new JsonDeserializer<>(OrderPlacedEvent.class);
        // Trust our own package — never trust "*" in production
        deserializer.addTrustedPackages("com.ecommerce.consumer", "com.ecommerce.event");
        deserializer.setUseTypeMapperForKey(false);

        // Wrap in ErrorHandlingDeserializer so a poison-pill message doesn't
        // crash the entire consumer thread — it's logged and skipped instead.
        ErrorHandlingDeserializer<OrderPlacedEvent> errorHandlingDeserializer =
                new ErrorHandlingDeserializer<>(deserializer);

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Manual ack — we commit the offset only after the email is sent
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                errorHandlingDeserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderPlacedEvent>
    kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, OrderPlacedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());

        // MANUAL_IMMEDIATE: offset committed right after the listener returns
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        // One concurrent thread per partition (up to 3 for our 3-partition topic)
        factory.setConcurrency(3);

        return factory;
    }
}
