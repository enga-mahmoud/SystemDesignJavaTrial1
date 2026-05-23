package com.ecommerce.order.repository;

import com.ecommerce.order.entity.OrderEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderEventRepository extends JpaRepository<OrderEventEntity, UUID> {
    List<OrderEventEntity> findByOrderIdOrderBySequenceNoAsc(UUID orderId);

    @Query("SELECT COALESCE(MAX(e.sequenceNo), 0) FROM OrderEventEntity e WHERE e.orderId = :orderId")
    int findMaxSequenceNo(UUID orderId);
}
