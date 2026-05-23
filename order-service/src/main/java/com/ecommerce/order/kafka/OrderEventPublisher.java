package com.ecommerce.order.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishOrderPlaced(UUID orderId, UUID userId, List<?> items, BigDecimal total) {
        publish("order.placed", orderId, Map.of(
            "eventId", UUID.randomUUID().toString(),
            "orderId", orderId.toString(),
            "userId", userId.toString(),
            "items", items,
            "total", total,
            "occurredAt", Instant.now().toString()
        ));
    }

    public void publishOrderConfirmed(UUID orderId) {
        publish("order.confirmed", orderId, Map.of(
            "eventId", UUID.randomUUID().toString(),
            "orderId", orderId.toString(),
            "occurredAt", Instant.now().toString()
        ));
    }

    public void publishOrderCancelled(UUID orderId, String reason) {
        publish("order.cancelled", orderId, Map.of(
            "eventId", UUID.randomUUID().toString(),
            "orderId", orderId.toString(),
            "reason", reason,
            "occurredAt", Instant.now().toString()
        ));
    }

    public void publishInventoryReleaseRequest(UUID orderId, String reservationData) {
        try {
            Object reservations = objectMapper.readValue(reservationData, Object.class);
            publish("inventory.release-requested", orderId, Map.of(
                "eventId", UUID.randomUUID().toString(),
                "orderId", orderId.toString(),
                "reservations", reservations,
                "occurredAt", Instant.now().toString()
            ));
        } catch (Exception e) {
            log.error("Failed to publish inventory release request for orderId={}", orderId, e);
        }
    }

    private void publish(String topic, UUID key, Map<String, Object> payload) {
        try {
            kafkaTemplate.send(topic, key.toString(), objectMapper.writeValueAsString(payload));
            log.info("Published {} for key={}", topic, key);
        } catch (Exception e) {
            log.error("Failed to publish {} for key={}", topic, key, e);
        }
    }
}
