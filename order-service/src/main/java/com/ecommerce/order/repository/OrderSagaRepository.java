package com.ecommerce.order.repository;

import com.ecommerce.order.entity.OrderSagaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderSagaRepository extends JpaRepository<OrderSagaEntity, UUID> {
    Optional<OrderSagaEntity> findByOrderId(UUID orderId);
}
