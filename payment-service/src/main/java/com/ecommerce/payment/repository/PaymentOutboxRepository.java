package com.ecommerce.payment.repository;

import com.ecommerce.payment.entity.PaymentOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, String> {

    List<PaymentOutbox> findTop100ByPublishedFalseOrderByCreatedAtAsc();
}
