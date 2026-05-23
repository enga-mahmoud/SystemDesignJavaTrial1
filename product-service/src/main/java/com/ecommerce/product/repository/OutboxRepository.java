package com.ecommerce.product.repository;

import com.ecommerce.product.entity.ProductOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<ProductOutbox, UUID> {
    List<ProductOutbox> findTop100ByPublishedFalseOrderByCreatedAtAsc();
}
