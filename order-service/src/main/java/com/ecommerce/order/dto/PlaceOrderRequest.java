package com.ecommerce.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class PlaceOrderRequest {
    @NotEmpty
    private List<OrderItemDto> items;

    @Data
    public static class OrderItemDto {
        @NotNull
        private UUID skuId;
        @NotNull
        private UUID productId;
        private int quantity;
        private BigDecimal unitPrice;
    }
}
