package com.ecommerce.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {

    private String id;
    private String orderId;
    private BigDecimal amount;
    private String status;
    private String failureReason;
    private LocalDateTime createdAt;
}
