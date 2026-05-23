package com.ecommerce.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("user-service", r -> r
                .path("/api/auth/**", "/api/users/**")
                .filters(f -> f
                    .circuitBreaker(c -> c
                        .setName("user-service-cb")
                        .setFallbackUri("forward:/fallback/user")))
                .uri("lb://user-service"))
            .route("product-service", r -> r
                .path("/api/products/**")
                .filters(f -> f
                    .circuitBreaker(c -> c
                        .setName("product-service-cb")
                        .setFallbackUri("forward:/fallback/product")))
                .uri("lb://product-service"))
            .route("inventory-service", r -> r
                .path("/api/inventory/**")
                .filters(f -> f
                    .circuitBreaker(c -> c
                        .setName("inventory-service-cb")
                        .setFallbackUri("forward:/fallback/inventory")))
                .uri("lb://inventory-service"))
            .route("order-service", r -> r
                .path("/api/orders/**")
                .filters(f -> f
                    .circuitBreaker(c -> c
                        .setName("order-service-cb")
                        .setFallbackUri("forward:/fallback/order")))
                .uri("lb://order-service"))
            .route("payment-service", r -> r
                .path("/api/payments/**")
                .filters(f -> f
                    .circuitBreaker(c -> c
                        .setName("payment-service-cb")
                        .setFallbackUri("forward:/fallback/payment")))
                .uri("lb://payment-service"))
            .route("search-service", r -> r
                .path("/api/search/**")
                .filters(f -> f
                    .circuitBreaker(c -> c
                        .setName("search-service-cb")
                        .setFallbackUri("forward:/fallback/search")))
                .uri("lb://search-service"))
            .build();
    }
}
