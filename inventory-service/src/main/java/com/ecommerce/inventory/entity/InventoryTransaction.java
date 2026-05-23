package com.ecommerce.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_transaction")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    @Column(nullable = false)
    private String type; // RESERVE, RELEASE, RESTOCK, SELL

    @Column(nullable = false)
    private int quantity;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
