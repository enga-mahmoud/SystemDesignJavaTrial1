package com.ecommerce.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class InventoryResponse {
    private UUID skuId;
    private UUID productId;
    private int quantity;
    private int reservedQuantity;
    private int availableQuantity;
}
