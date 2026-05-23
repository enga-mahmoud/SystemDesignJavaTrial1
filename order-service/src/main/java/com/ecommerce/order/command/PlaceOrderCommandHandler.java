package com.ecommerce.order.command;

import com.ecommerce.order.dto.PlaceOrderRequest;
import com.ecommerce.order.entity.OrderEventEntity;
import com.ecommerce.order.entity.OrderReadModel;
import com.ecommerce.order.entity.OrderSagaEntity;
import com.ecommerce.order.enums.OrderStatus;
import com.ecommerce.order.enums.SagaState;
import com.ecommerce.order.kafka.OrderEventPublisher;
import com.ecommerce.order.repository.OrderEventRepository;
import com.ecommerce.order.repository.OrderReadModelRepository;
import com.ecommerce.order.repository.OrderSagaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceOrderCommandHandler {

    private final OrderEventRepository eventRepository;
    private final OrderReadModelRepository readModelRepository;
    private final OrderSagaRepository sagaRepository;
    private final OrderEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public UUID handle(PlaceOrderRequest request, UUID userId) {
        UUID orderId = UUID.randomUUID();

        BigDecimal total = request.getItems().stream()
            .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 1. Persist domain event to event store
        Map<String, Object> eventPayload = Map.of(
            "eventId", UUID.randomUUID().toString(),
            "orderId", orderId.toString(),
            "userId", userId.toString(),
            "items", request.getItems(),
            "total", total,
            "occurredAt", Instant.now().toString()
        );

        try {
            OrderEventEntity event = OrderEventEntity.builder()
                .orderId(orderId)
                .eventType("OrderPlacedEvent")
                .payload(objectMapper.writeValueAsString(eventPayload))
                .sequenceNo(1)
                .build();
            eventRepository.save(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist order event", e);
        }

        // 2. Initialize saga state machine
        OrderSagaEntity saga = OrderSagaEntity.builder()
            .orderId(orderId)
            .state(SagaState.PENDING)
            .sagaData("{}")
            .build();
        sagaRepository.save(saga);

        // 3. Initialize CQRS read model projection
        try {
            OrderReadModel readModel = OrderReadModel.builder()
                .id(orderId)
                .userId(userId)
                .status(OrderStatus.PENDING)
                .total(total)
                .itemsJson(objectMapper.writeValueAsString(request.getItems()))
                .build();
            readModelRepository.save(readModel);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize read model", e);
        }

        // 4. Publish Kafka event (triggers inventory-service and payment-service consumers)
        eventPublisher.publishOrderPlaced(orderId, userId, request.getItems(), total);

        log.info("Order placed successfully orderId={} userId={} total={}", orderId, userId, total);
        return orderId;
    }
}
