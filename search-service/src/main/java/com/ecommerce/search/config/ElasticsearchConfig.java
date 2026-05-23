package com.ecommerce.search.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Application-level Jackson and Elasticsearch configuration.
 *
 * <p>A {@link Primary} {@link ObjectMapper} bean is registered here so that:
 * <ul>
 *   <li>Java 8 date/time types ({@link java.time.LocalDateTime}, etc.) are serialised as
 *       ISO-8601 strings rather than epoch-millisecond arrays.</li>
 *   <li>The same mapper is reused by the Kafka consumer ({@link com.ecommerce.search.consumer.ProductEventConsumer})
 *       for deserialising incoming JSON event payloads.</li>
 * </ul>
 */
@Configuration
public class ElasticsearchConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
