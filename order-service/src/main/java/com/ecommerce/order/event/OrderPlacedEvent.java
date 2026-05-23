package com.ecommerce.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedEvent implements OrderEvent {

    private UUID eventId;
    private UUID orderId;
    private UUID userId;
    private List<?> items;
    private BigDecimal total;

    @Builder.Default
    private Instant occurredAt = Instant.now();
}
