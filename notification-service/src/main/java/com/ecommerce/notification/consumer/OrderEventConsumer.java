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
public class OrderEventConsumer {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = {"order.confirmed", "order.cancelled", "payment.completed", "payment.failed"},
            groupId = "notification-service"
    )
    public void onOrderEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            String topic = record.topic();
            JsonNode event = objectMapper.readTree(record.value());

            // Use a fallback email since notification-service has no user DB.
            // In production this field would be populated by the upstream service.
            String userEmail = event.has("userEmail")
                    ? event.get("userEmail").asText()
                    : "customer@example.com";
            String orderId = event.has("orderId") ? event.get("orderId").asText() : "unknown";

            switch (topic) {
                case "order.confirmed" -> {
                    String amount = event.has("totalAmount")
                            ? event.get("totalAmount").asText() : "0.00";
                    emailService.sendOrderConfirmedEmail(userEmail, orderId, amount);
                }
                case "order.cancelled" -> emailService.sendOrderCancelledEmail(userEmail, orderId);
                case "payment.completed" -> {
                    String amount = event.has("amount")
                            ? event.get("amount").asText() : "0.00";
                    emailService.sendPaymentCompletedEmail(userEmail, orderId, amount);
                }
                case "payment.failed" -> {
                    String reason = event.has("failureReason")
                            ? event.get("failureReason").asText() : "Unknown reason";
                    emailService.sendPaymentFailedEmail(userEmail, orderId, reason);
                }
                default -> log.warn("Unknown topic: {}", topic);
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing event from topic {}", record.topic(), e);
            ack.acknowledge(); // ack to avoid infinite retry on bad data
        }
    }
}
