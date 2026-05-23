package com.ecommerce.user.kafka;

import com.ecommerce.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishUserRegistered(User user) {
        try {
            Map<String, Object> event = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "userId", user.getId().toString(),
                "email", user.getEmail(),
                "occurredAt", Instant.now().toString()
            );
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("user.registered", user.getId().toString(), payload);
            log.info("Published user.registered for userId={}", user.getId());
        } catch (Exception e) {
            log.error("Failed to publish user.registered event for userId={}", user.getId(), e);
        }
    }
}
