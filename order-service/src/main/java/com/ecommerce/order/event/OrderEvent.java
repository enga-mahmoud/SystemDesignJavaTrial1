package com.ecommerce.order.event;

import java.time.Instant;
import java.util.UUID;

public interface OrderEvent {
    UUID getEventId();
    UUID getOrderId();
    Instant getOccurredAt();
}
