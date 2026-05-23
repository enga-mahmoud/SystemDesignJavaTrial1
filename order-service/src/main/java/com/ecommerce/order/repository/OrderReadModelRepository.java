package com.ecommerce.order.repository;

import com.ecommerce.order.entity.OrderReadModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderReadModelRepository extends JpaRepository<OrderReadModel, UUID> {
    Page<OrderReadModel> findByUserId(UUID userId, Pageable pageable);
}
