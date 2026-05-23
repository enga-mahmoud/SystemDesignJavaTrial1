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
public class OrderConfirmedEvent implements OrderEvent {

    private UUID eventId;
    private UUID orderId;

    @Builder.Default
    private Instant occurredAt = Instant.now();
}
