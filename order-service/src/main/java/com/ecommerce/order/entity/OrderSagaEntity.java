package com.ecommerce.order.entity;

import com.ecommerce.order.enums.SagaState;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_saga")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderSagaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", unique = true, nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaState state;

    @Builder.Default
    @Column(name = "saga_data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String sagaData = "{}";

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
