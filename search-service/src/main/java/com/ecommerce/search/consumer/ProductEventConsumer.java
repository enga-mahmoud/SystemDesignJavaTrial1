package com.ecommerce.search.consumer;

import com.ecommerce.search.document.ProductDocument;
import com.ecommerce.search.service.SearchService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Kafka consumer that listens to product lifecycle events and keeps the
 * Elasticsearch index in sync.
 *
 * <p>Topics consumed:
 * <ul>
 *   <li>{@code product.created} — index a new product document</li>
 *   <li>{@code product.updated} — overwrite the existing document (upsert via save)</li>
 *   <li>{@code product.deleted} — remove the document from the index</li>
 * </ul>
 *
 * <p>Manual acknowledgment ({@code ack-mode: manual_immediate}) is used so that
 * offset commits only happen after successful processing. Parse errors are
 * acknowledged immediately to avoid infinite retry loops on poison-pill messages.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEventConsumer {

    private final SearchService searchService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = {"product.created", "product.updated", "product.deleted"},
            groupId = "search-service"
    )
    public void onProductEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String topic = record.topic();
        log.debug("Received event from topic={} partition={} offset={}",
                topic, record.partition(), record.offset());

        try {
            JsonNode event = objectMapper.readTree(record.value());

            if ("product.deleted".equals(topic)) {
                String productId = event.get("productId").asText();
                searchService.deleteProduct(productId);
                log.info("Processed product.deleted for productId={}", productId);
            } else {
                // product.created or product.updated — index / upsert the document
                ProductDocument doc = ProductDocument.builder()
                        .id(event.get("productId").asText())
                        .name(event.get("name").asText())
                        .description(event.has("description") ? event.get("description").asText() : "")
                        .categoryId(event.has("categoryId") ? event.get("categoryId").asText() : null)
                        .categoryName(event.has("categoryName") ? event.get("categoryName").asText() : null)
                        .price(event.has("price") ? event.get("price").asDouble() : 0.0)
                        .vendorId(event.has("vendorId") ? event.get("vendorId").asText() : null)
                        .status("ACTIVE")
                        .createdAt(LocalDateTime.now())
                        .build();
                searchService.indexProduct(doc);
                log.info("Processed {} for productId={}", topic, doc.getId());
            }

            ack.acknowledge();

        } catch (Exception e) {
            // Acknowledge to avoid blocking the partition on a malformed message.
            // A dead-letter topic strategy can be added here if needed.
            log.error("Error processing product event from topic={} offset={}: {}",
                    topic, record.offset(), e.getMessage(), e);
            ack.acknowledge();
        }
    }
}
