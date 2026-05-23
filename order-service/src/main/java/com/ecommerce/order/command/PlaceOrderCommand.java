package com.ecommerce.order.command;

import com.ecommerce.order.dto.PlaceOrderRequest;

import java.util.UUID;

public record PlaceOrderCommand(PlaceOrderRequest request, UUID userId) {}
