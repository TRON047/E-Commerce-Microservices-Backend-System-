package com.ecommerce;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * @LoadBalanced tells Spring to intercept lb:// URIs and resolve them
     * through Eureka + Spring Cloud LoadBalancer. Without this annotation,
     * lb://product-service would fail with an unknown host error.
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}
