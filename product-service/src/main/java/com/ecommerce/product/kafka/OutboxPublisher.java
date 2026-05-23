package com.ecommerce.product.kafka;

import com.ecommerce.product.entity.ProductOutbox;
import com.ecommerce.product.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents() {
        List<ProductOutbox> pending = outboxRepository
            .findTop100ByPublishedFalseOrderByCreatedAtAsc();

        if (pending.isEmpty()) return;

        for (ProductOutbox outbox : pending) {
            try {
                kafkaTemplate.send(
                    outbox.getEventType(),
                    outbox.getAggregateId().toString(),
                    outbox.getPayload()
                );
                outbox.setPublished(true);
            } catch (Exception e) {
                log.error("Failed to publish outbox id={} topic={}", outbox.getId(), outbox.getEventType(), e);
            }
        }

        outboxRepository.saveAll(pending);
        log.debug("Published {} outbox events", pending.stream().filter(ProductOutbox::isPublished).count());
    }
}
