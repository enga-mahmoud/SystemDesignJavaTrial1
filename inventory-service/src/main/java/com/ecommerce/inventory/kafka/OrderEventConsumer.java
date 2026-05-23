package com.ecommerce.inventory.kafka;

import com.ecommerce.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final InventoryService inventoryService;
    private final InventoryEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.placed", groupId = "inventory-service")
    public void handleOrderPlaced(String payload, Acknowledgment ack) {
        try {
            JsonNode event = objectMapper.readTree(payload);
            String orderId = event.get("orderId").asText();
            JsonNode items = event.get("items");

            List<Map<String, Object>> reservations = new ArrayList<>();
            List<Map<String, Object>> failedItems = new ArrayList<>();

            for (JsonNode item : items) {
                UUID skuId = UUID.fromString(item.get("skuId").asText());
                int quantity = item.get("quantity").asInt();
                try {
                    inventoryService.reserveStock(skuId, quantity, UUID.fromString(orderId));
                    reservations.add(Map.of("skuId", skuId.toString(), "quantity", quantity));
                } catch (Exception e) {
                    log.warn("Failed to reserve skuId={} qty={} for orderId={}: {}", skuId, quantity, orderId, e.getMessage());
                    failedItems.add(Map.of("skuId", skuId.toString(), "quantity", quantity, "reason", e.getMessage()));
                }
            }

            if (failedItems.isEmpty()) {
                eventPublisher.publishInventoryReserved(UUID.fromString(orderId), reservations);
            } else {
                // Compensate: release any successful reservations
                for (Map<String, Object> r : reservations) {
                    try {
                        inventoryService.releaseStock(
                            UUID.fromString(r.get("skuId").toString()),
                            (int) r.get("quantity"),
                            UUID.fromString(orderId)
                        );
                    } catch (Exception ex) {
                        log.error("Compensation failed for skuId={}", r.get("skuId"), ex);
                    }
                }
                eventPublisher.publishInventoryFailed(UUID.fromString(orderId), "INSUFFICIENT_STOCK");
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing order.placed: {}", payload, e);
            ack.acknowledge(); // Acknowledge to avoid infinite retry loop; use DLQ in production
        }
    }

    @KafkaListener(topics = "inventory.release-requested", groupId = "inventory-service")
    public void handleReleaseRequested(String payload, Acknowledgment ack) {
        try {
            JsonNode event = objectMapper.readTree(payload);
            String orderId = event.get("orderId").asText();
            JsonNode reservations = event.get("reservations");

            for (JsonNode reservation : reservations) {
                UUID skuId = UUID.fromString(reservation.get("skuId").asText());
                int quantity = reservation.get("quantity").asInt();
                inventoryService.releaseStock(skuId, quantity, UUID.fromString(orderId));
            }

            eventPublisher.publishInventoryReleased(UUID.fromString(orderId));
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing inventory.release-requested", e);
            ack.acknowledge();
        }
    }
}
