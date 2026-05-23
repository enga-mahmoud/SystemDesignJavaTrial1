package com.ecommerce.product.service;

import com.ecommerce.product.cache.ProductCacheService;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductOutbox;
import com.ecommerce.product.enums.ProductStatus;
import com.ecommerce.product.repository.OutboxRepository;
import com.ecommerce.product.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final OutboxRepository outboxRepository;
    private final ProductCacheService cacheService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID id) {
        return cacheService.getProduct(id)
            .map(json -> {
                try {
                    return objectMapper.readValue(json, ProductResponse.class);
                } catch (Exception e) {
                    log.warn("Cache deserialization failed for id={}", id);
                    return null;
                }
            })
            .filter(p -> p != null)
            .orElseGet(() -> {
                Product product = productRepository.findByIdAndStatus(id, ProductStatus.ACTIVE)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + id));
                ProductResponse response = toResponse(product);
                try {
                    cacheService.setProduct(id, objectMapper.writeValueAsString(response));
                } catch (Exception e) {
                    log.warn("Failed to cache product id={}", id);
                }
                return response;
            });
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listProducts(UUID categoryId, String name, Pageable pageable) {
        if (name != null && !name.isBlank()) {
            if (categoryId != null) {
                return productRepository.findByCategoryIdAndStatusAndNameContainingIgnoreCase(
                    categoryId, ProductStatus.ACTIVE, name, pageable).map(this::toResponse);
            }
            return productRepository.findByStatusAndNameContainingIgnoreCase(
                ProductStatus.ACTIVE, name, pageable).map(this::toResponse);
        }
        if (categoryId != null) {
            return productRepository.findByCategoryIdAndStatus(categoryId, ProductStatus.ACTIVE, pageable)
                .map(this::toResponse);
        }
        return productRepository.findByStatus(ProductStatus.ACTIVE, pageable).map(this::toResponse);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request, UUID vendorId) {
        Product product = Product.builder()
            .name(request.getName())
            .description(request.getDescription())
            .price(request.getPrice())
            .categoryId(request.getCategoryId())
            .vendorId(vendorId)
            .status(ProductStatus.ACTIVE)
            .build();

        product = productRepository.save(product);

        writeOutbox(product, "product.created");
        log.info("Created product id={}", product.getId());
        return toResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(UUID id, ProductRequest request, UUID requesterId) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        if (request.getCategoryId() != null) {
            product.setCategoryId(request.getCategoryId());
        }

        product = productRepository.save(product);
        writeOutbox(product, "product.updated");
        cacheService.evictProduct(id);
        return toResponse(product);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        product.setStatus(ProductStatus.DELETED);
        productRepository.save(product);
        writeOutbox(product, "product.deleted");
        cacheService.evictProduct(id);
        log.info("Soft-deleted product id={}", id);
    }

    @Transactional
    public int reindexAll() {
        List<Product> active = productRepository.findByStatus(ProductStatus.ACTIVE, Pageable.unpaged()).getContent();
        for (Product p : active) {
            writeOutbox(p, "product.created");
        }
        log.info("Queued reindex outbox events for {} products", active.size());
        return active.size();
    }

    private void writeOutbox(Product product, String eventType) {
        try {
            Map<String, Object> payload = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "productId", product.getId().toString(),
                "name", product.getName(),
                "description", product.getDescription() != null ? product.getDescription() : "",
                "price", product.getPrice(),
                "categoryId", product.getCategoryId() != null ? product.getCategoryId().toString() : "",
                "vendorId", product.getVendorId().toString(),
                "status", product.getStatus().name(),
                "occurredAt", Instant.now().toString()
            );
            ProductOutbox outbox = ProductOutbox.builder()
                .aggregateId(product.getId())
                .eventType(eventType)
                .payload(objectMapper.writeValueAsString(payload))
                .build();
            outboxRepository.save(outbox);
        } catch (Exception e) {
            log.error("Failed to write outbox for product id={}", product.getId(), e);
        }
    }

    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
            .id(p.getId())
            .name(p.getName())
            .description(p.getDescription())
            .price(p.getPrice())
            .categoryId(p.getCategoryId())
            .vendorId(p.getVendorId())
            .status(p.getStatus().name())
            .createdAt(p.getCreatedAt())
            .updatedAt(p.getUpdatedAt())
            .build();
    }
}
