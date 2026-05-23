package com.ecommerce.product.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductRequest {
    @NotBlank
    private String name;

    private String description;

    @NotNull @DecimalMin("0.01")
    private BigDecimal price;

    private UUID categoryId;
}
