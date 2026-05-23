package com.ecommerce.order.controller;

import com.ecommerce.order.command.PlaceOrderCommandHandler;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.PlaceOrderRequest;
import com.ecommerce.order.query.GetOrderQueryHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final PlaceOrderCommandHandler commandHandler;
    private final GetOrderQueryHandler queryHandler;

    @PostMapping
    public ResponseEntity<Map<String, String>> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        UUID uid = userId != null ? UUID.fromString(userId) : UUID.randomUUID();
        UUID orderId = commandHandler.handle(request, uid);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(Map.of("orderId", orderId.toString(), "status", "PENDING"));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(queryHandler.getOrder(orderId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<OrderResponse>> getUserOrders(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(queryHandler.getUserOrders(
            userId, PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(queryHandler.getUserOrders(
            UUID.fromString(userId), PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }
}
