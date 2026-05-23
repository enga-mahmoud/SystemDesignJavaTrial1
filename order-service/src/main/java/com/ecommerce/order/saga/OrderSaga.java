package com.ecommerce.order.saga;

import com.ecommerce.order.entity.OrderEventEntity;
import com.ecommerce.order.entity.OrderReadModel;
import com.ecommerce.order.entity.OrderSagaEntity;
import com.ecommerce.order.enums.OrderStatus;
import com.ecommerce.order.enums.SagaState;
import com.ecommerce.order.kafka.OrderEventPublisher;
import com.ecommerce.order.repository.OrderEventRepository;
import com.ecommerce.order.repository.OrderReadModelRepository;
import com.ecommerce.order.repository.OrderSagaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderSaga {

    private final OrderSagaRepository sagaRepository;
    private final OrderEventRepository eventRepository;
    private final OrderReadModelRepository readModelRepository;
    private final OrderEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public void onInventoryReserved(String eventPayload) {
        try {
            JsonNode event = objectMapper.readTree(eventPayload);
            UUID orderId = UUID.fromString(event.get("orderId").asText());

            OrderSagaEntity saga = sagaRepository.findByOrderId(orderId).orElse(null);
            if (saga == null || saga.getState() != SagaState.PENDING) {
                log.warn("Saga not in PENDING state for orderId={}, current={}", orderId, saga != null ? saga.getState() : "null");
                return;
            }

            // Store reservation data for potential compensation
            saga.setSagaData(event.get("reservations").toString());
            saga.setState(SagaState.INVENTORY_RESERVED);
            sagaRepository.save(saga);

            // Append event to event store
            appendEvent(orderId, "InventoryReservedEvent", eventPayload);

            log.info("Saga advanced to INVENTORY_RESERVED for orderId={}", orderId);
        } catch (Exception e) {
            log.error("Error processing inventory.reserved", e);
        }
    }

    @Transactional
    public void onInventoryFailed(String eventPayload) {
        try {
            JsonNode event = objectMapper.readTree(eventPayload);
            UUID orderId = UUID.fromString(event.get("orderId").asText());

            OrderSagaEntity saga = sagaRepository.findByOrderId(orderId).orElse(null);
            if (saga == null || saga.getState() != SagaState.PENDING) return;

            saga.setState(SagaState.CANCELLED);
            sagaRepository.save(saga);

            appendEvent(orderId, "OrderCancelledEvent",
                buildCancelPayload(orderId, "INVENTORY_FAILED"));

            updateReadModel(orderId, OrderStatus.CANCELLED);
            eventPublisher.publishOrderCancelled(orderId, "Insufficient inventory");

            log.info("Order cancelled (inventory failed) orderId={}", orderId);
        } catch (Exception e) {
            log.error("Error processing inventory.failed", e);
        }
    }

    @Transactional
    public void onPaymentCompleted(String eventPayload) {
        try {
            JsonNode event = objectMapper.readTree(eventPayload);
            UUID orderId = UUID.fromString(event.get("orderId").asText());

            OrderSagaEntity saga = sagaRepository.findByOrderId(orderId).orElse(null);
            if (saga == null || saga.getState() != SagaState.INVENTORY_RESERVED) {
                log.warn("Saga not in INVENTORY_RESERVED state for orderId={}", orderId);
                return;
            }

            saga.setState(SagaState.CONFIRMED);
            sagaRepository.save(saga);

            appendEvent(orderId, "OrderConfirmedEvent", eventPayload);
            updateReadModel(orderId, OrderStatus.CONFIRMED);
            eventPublisher.publishOrderConfirmed(orderId);

            log.info("Order confirmed orderId={}", orderId);
        } catch (Exception e) {
            log.error("Error processing payment.completed", e);
        }
    }

    @Transactional
    public void onPaymentFailed(String eventPayload) {
        try {
            JsonNode event = objectMapper.readTree(eventPayload);
            UUID orderId = UUID.fromString(event.get("orderId").asText());

            OrderSagaEntity saga = sagaRepository.findByOrderId(orderId).orElse(null);
            if (saga == null || saga.getState() != SagaState.INVENTORY_RESERVED) {
                log.warn("Saga not in INVENTORY_RESERVED state for orderId={}", orderId);
                return;
            }

            // Start compensation: release inventory
            saga.setState(SagaState.PAYMENT_FAILED_COMPENSATING);
            sagaRepository.save(saga);

            // Publish compensation request with reservation data
            String reservationData = saga.getSagaData();
            eventPublisher.publishInventoryReleaseRequest(orderId, reservationData);

            log.info("Compensation started (payment failed) orderId={}", orderId);
        } catch (Exception e) {
            log.error("Error processing payment.failed", e);
        }
    }

    @Transactional
    public void onInventoryReleased(String eventPayload) {
        try {
            JsonNode event = objectMapper.readTree(eventPayload);
            UUID orderId = UUID.fromString(event.get("orderId").asText());

            OrderSagaEntity saga = sagaRepository.findByOrderId(orderId).orElse(null);
            if (saga == null || saga.getState() != SagaState.PAYMENT_FAILED_COMPENSATING) {
                return;
            }

            saga.setState(SagaState.CANCELLED);
            sagaRepository.save(saga);

            appendEvent(orderId, "OrderCancelledEvent",
                buildCancelPayload(orderId, "PAYMENT_FAILED"));

            updateReadModel(orderId, OrderStatus.CANCELLED);
            eventPublisher.publishOrderCancelled(orderId, "Payment failed");

            log.info("Order cancelled (compensation complete) orderId={}", orderId);
        } catch (Exception e) {
            log.error("Error processing inventory.released", e);
        }
    }

    private void appendEvent(UUID orderId, String eventType, String payload) {
        int nextSeq = eventRepository.findMaxSequenceNo(orderId) + 1;
        OrderEventEntity entity = OrderEventEntity.builder()
            .orderId(orderId)
            .eventType(eventType)
            .payload(payload)
            .sequenceNo(nextSeq)
            .build();
        eventRepository.save(entity);
    }

    private void updateReadModel(UUID orderId, OrderStatus status) {
        readModelRepository.findById(orderId).ifPresent(model -> {
            model.setStatus(status);
            readModelRepository.save(model);
        });
    }

    private String buildCancelPayload(UUID orderId, String reason) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                "eventId", UUID.randomUUID().toString(),
                "orderId", orderId.toString(),
                "reason", reason,
                "occurredAt", Instant.now().toString()
            ));
        } catch (Exception e) {
            return "{}";
        }
    }
}
