package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentOutbox;
import com.ecommerce.payment.enums.PaymentStatus;
import com.ecommerce.payment.exception.ResourceNotFoundException;
import com.ecommerce.payment.repository.PaymentOutboxRepository;
import com.ecommerce.payment.repository.PaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentResponse charge(PaymentRequest request) throws JsonProcessingException {
        // Idempotency check: if payment already exists for this key, return cached result
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            Payment p = existing.get();
            log.info("Idempotent payment request detected for key: {}", request.getIdempotencyKey());
            return PaymentResponse.builder()
                    .id(p.getId())
                    .orderId(p.getOrderId())
                    .amount(p.getAmount())
                    .status(p.getStatus().name())
                    .failureReason(p.getFailureReason())
                    .createdAt(p.getCreatedAt())
                    .build();
        }

        // Simulate 95% success rate
        boolean success = Math.random() > 0.05;
        PaymentStatus status = success ? PaymentStatus.COMPLETED : PaymentStatus.FAILED;
        String failureReason = success ? null : "Simulated payment gateway decline";

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .status(status)
                .idempotencyKey(request.getIdempotencyKey())
                .failureReason(failureReason)
                .build();
        payment = paymentRepository.save(payment);

        // Write outbox record in same transaction (Outbox Pattern)
        String eventType = success ? "payment.completed" : "payment.failed";
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("paymentId", payment.getId());
        payloadMap.put("orderId", payment.getOrderId());
        payloadMap.put("userId", payment.getUserId());
        payloadMap.put("amount", payment.getAmount());
        payloadMap.put("status", status.name());
        if (failureReason != null) {
            payloadMap.put("failureReason", failureReason);
        }

        PaymentOutbox outbox = PaymentOutbox.builder()
                .aggregateId(payment.getOrderId())
                .eventType(eventType)
                .payload(objectMapper.writeValueAsString(payloadMap))
                .published(false)
                .build();
        outboxRepository.save(outbox);

        log.info("Payment {} for order {}: {}", payment.getId(), payment.getOrderId(), status);
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .status(status.name())
                .failureReason(failureReason)
                .createdAt(payment.getCreatedAt())
                .build();
    }

    public PaymentResponse getByOrderId(String orderId) {
        Payment p = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));
        return PaymentResponse.builder()
                .id(p.getId())
                .orderId(p.getOrderId())
                .amount(p.getAmount())
                .status(p.getStatus().name())
                .failureReason(p.getFailureReason())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
