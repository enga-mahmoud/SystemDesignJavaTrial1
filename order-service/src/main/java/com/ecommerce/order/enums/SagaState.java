package com.ecommerce.order.enums;

public enum SagaState {
    PENDING,
    INVENTORY_RESERVED,
    PAYMENT_PROCESSING,
    CONFIRMED,
    INVENTORY_FAILED,
    PAYMENT_FAILED_COMPENSATING,
    CANCELLED
}
