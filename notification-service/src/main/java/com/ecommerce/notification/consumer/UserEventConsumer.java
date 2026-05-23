package com.ecommerce.notification.consumer;

import com.ecommerce.notification.service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user.registered", groupId = "notification-service")
    public void onUserRegistered(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            JsonNode event = objectMapper.readTree(record.value());
            String email = event.has("email") ? event.get("email").asText() : "unknown@example.com";
            String name  = event.has("username") ? event.get("username").asText() : "Customer";
            emailService.sendWelcomeEmail(email, name);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing user.registered event", e);
            ack.acknowledge(); // ack to avoid infinite retry on bad data
        }
    }
}
