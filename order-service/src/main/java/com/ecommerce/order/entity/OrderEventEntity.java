package com.ecommerce.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_events",
    uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "sequence_no"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Builder.Default
    @Column(name = "occurred_at")
    private LocalDateTime occurredAt = LocalDateTime.now();
}
