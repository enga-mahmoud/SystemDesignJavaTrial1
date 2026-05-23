package com.ecommerce.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledEvent implements OrderEvent {

    private UUID eventId;
    private UUID orderId;
    private String reason;

    @Builder.Default
    private Instant occurredAt = Instant.now();
}
