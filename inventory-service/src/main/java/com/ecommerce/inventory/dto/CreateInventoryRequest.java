package com.ecommerce.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateInventoryRequest {
    @NotNull
    private UUID skuId;
    @NotNull
    private UUID productId;
    @Min(0)
    private int quantity;
}
