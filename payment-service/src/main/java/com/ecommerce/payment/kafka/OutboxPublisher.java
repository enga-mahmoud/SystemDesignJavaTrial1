package com.ecommerce.payment.kafka;

import com.ecommerce.payment.entity.PaymentOutbox;
import com.ecommerce.payment.repository.PaymentOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final PaymentOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents() {
        List<PaymentOutbox> pending = outboxRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc();
        for (PaymentOutbox outbox : pending) {
            try {
                kafkaTemplate.send(outbox.getEventType(), outbox.getAggregateId(), outbox.getPayload())
                        .get(5, TimeUnit.SECONDS);
                outbox.setPublished(true);
            } catch (Exception e) {
                log.error("Failed to publish outbox event id={}, eventType={}: {}",
                        outbox.getId(), outbox.getEventType(), e.getMessage(), e);
            }
        }
        if (!pending.isEmpty()) {
            outboxRepository.saveAll(pending);
            log.debug("Published {} payment outbox events", pending.size());
        }
    }
}
