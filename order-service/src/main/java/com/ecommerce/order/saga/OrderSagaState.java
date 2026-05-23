package com.ecommerce.order.saga;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSagaState {

    private List<Reservation> reservations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Reservation {
        private String skuId;
        private int quantity;
    }
}
