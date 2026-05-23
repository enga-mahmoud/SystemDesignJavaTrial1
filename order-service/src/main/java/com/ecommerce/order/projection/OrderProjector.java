package com.ecommerce.order.projection;

import com.ecommerce.order.entity.OrderEventEntity;
import com.ecommerce.order.entity.OrderReadModel;
import com.ecommerce.order.enums.OrderStatus;
import com.ecommerce.order.repository.OrderEventRepository;
import com.ecommerce.order.repository.OrderReadModelRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderProjector {

    private final OrderEventRepository eventRepository;
    private final OrderReadModelRepository readModelRepository;
    private final ObjectMapper objectMapper;

    /**
     * Replays all events for an order and returns the projected read model.
     * Used for time-travel debugging and building new projections from history.
     */
    @Transactional(readOnly = true)
    public OrderReadModel project(UUID orderId) {
        List<OrderEventEntity> events = eventRepository.findByOrderIdOrderBySequenceNoAsc(orderId);
        OrderReadModel model = new OrderReadModel();
        model.setId(orderId);

        for (OrderEventEntity event : events) {
            applyEvent(model, event);
        }

        return model;
    }

    private void applyEvent(OrderReadModel model, OrderEventEntity event) {
        try {
            JsonNode payload = objectMapper.readTree(event.getPayload());
            switch (event.getEventType()) {
                case "OrderPlacedEvent" -> {
                    model.setUserId(UUID.fromString(payload.get("userId").asText()));
                    model.setStatus(OrderStatus.PENDING);
                    model.setTotal(new BigDecimal(payload.get("total").asText()));
                    model.setItemsJson(payload.get("items").toString());
                }
                case "InventoryReservedEvent" -> model.setStatus(OrderStatus.PENDING);
                case "OrderConfirmedEvent" -> model.setStatus(OrderStatus.CONFIRMED);
                case "OrderCancelledEvent" -> model.setStatus(OrderStatus.CANCELLED);
                default -> log.debug("Skipping unknown event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Failed to apply event {} for orderId={}", event.getEventType(), model.getId(), e);
        }
    }
}
