package com.ecommerce.order.kafka;

import com.ecommerce.order.saga.OrderSaga;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaEventConsumer {

    private final OrderSaga orderSaga;

    @KafkaListener(topics = "inventory.reserved", groupId = "order-service")
    public void onInventoryReserved(String payload, Acknowledgment ack) {
        log.info("Received inventory.reserved");
        orderSaga.onInventoryReserved(payload);
        ack.acknowledge();
    }

    @KafkaListener(topics = "inventory.failed", groupId = "order-service")
    public void onInventoryFailed(String payload, Acknowledgment ack) {
        log.info("Received inventory.failed");
        orderSaga.onInventoryFailed(payload);
        ack.acknowledge();
    }

    @KafkaListener(topics = "payment.completed", groupId = "order-service")
    public void onPaymentCompleted(String payload, Acknowledgment ack) {
        log.info("Received payment.completed");
        orderSaga.onPaymentCompleted(payload);
        ack.acknowledge();
    }

    @KafkaListener(topics = "payment.failed", groupId = "order-service")
    public void onPaymentFailed(String payload, Acknowledgment ack) {
        log.info("Received payment.failed");
        orderSaga.onPaymentFailed(payload);
        ack.acknowledge();
    }

    @KafkaListener(topics = "inventory.released", groupId = "order-service")
    public void onInventoryReleased(String payload, Acknowledgment ack) {
        log.info("Received inventory.released");
        orderSaga.onInventoryReleased(payload);
        ack.acknowledge();
    }
}
