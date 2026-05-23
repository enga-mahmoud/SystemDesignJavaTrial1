package com.ecommerce.inventory.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishInventoryReserved(UUID orderId, List<Map<String, Object>> reservations) {
        try {
            Map<String, Object> event = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "orderId", orderId.toString(),
                "reservations", reservations,
                "occurredAt", Instant.now().toString()
            );
            kafkaTemplate.send("inventory.reserved", orderId.toString(), objectMapper.writeValueAsString(event));
            log.info("Published inventory.reserved for orderId={}", orderId);
        } catch (Exception e) {
            log.error("Failed to publish inventory.reserved", e);
        }
    }

    public void publishInventoryFailed(UUID orderId, String reason) {
        try {
            Map<String, Object> event = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "orderId", orderId.toString(),
                "reason", reason,
                "occurredAt", Instant.now().toString()
            );
            kafkaTemplate.send("inventory.failed", orderId.toString(), objectMapper.writeValueAsString(event));
            log.info("Published inventory.failed for orderId={}", orderId);
        } catch (Exception e) {
            log.error("Failed to publish inventory.failed", e);
        }
    }

    public void publishInventoryReleased(UUID orderId) {
        try {
            Map<String, Object> event = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "orderId", orderId.toString(),
                "occurredAt", Instant.now().toString()
            );
            kafkaTemplate.send("inventory.released", orderId.toString(), objectMapper.writeValueAsString(event));
            log.info("Published inventory.released for orderId={}", orderId);
        } catch (Exception e) {
            log.error("Failed to publish inventory.released", e);
        }
    }
}
