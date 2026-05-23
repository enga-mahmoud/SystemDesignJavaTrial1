package com.ecommerce.payment.kafka;

import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.service.PaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "order.placed",
            groupId = "payment-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOrderPlaced(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            log.info("Received order.placed event: key={}, offset={}", record.key(), record.offset());
            JsonNode event = objectMapper.readTree(record.value());

            PaymentRequest request = new PaymentRequest();
            request.setOrderId(event.get("orderId").asText());
            request.setUserId(event.get("userId").asText());
            request.setAmount(new BigDecimal(event.get("totalAmount").asText()));
            // Use orderId as idempotency key — guarantees one payment attempt per order
            request.setIdempotencyKey("order-" + event.get("orderId").asText());

            paymentService.charge(request);
            ack.acknowledge();
            log.info("Successfully processed order.placed event for orderId={}", request.getOrderId());
        } catch (Exception e) {
            log.error("Failed to process order.placed event at offset={}: {}", record.offset(), e.getMessage(), e);
            // Do NOT acknowledge — message will be retried based on consumer config
        }
    }
}
