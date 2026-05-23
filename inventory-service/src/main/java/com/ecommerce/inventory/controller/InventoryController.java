package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.dto.CreateInventoryRequest;
import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.dto.ReservationRequest;
import com.ecommerce.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{skuId}")
    public ResponseEntity<InventoryResponse> getInventory(@PathVariable UUID skuId) {
        return ResponseEntity.ok(inventoryService.getInventory(skuId));
    }

    @PostMapping
    public ResponseEntity<InventoryResponse> createInventory(
            @Valid @RequestBody CreateInventoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(inventoryService.createInventory(request));
    }

    @PostMapping("/{skuId}/reserve")
    public ResponseEntity<Map<String, String>> reserve(
            @PathVariable UUID skuId,
            @Valid @RequestBody ReservationRequest request) {
        inventoryService.reserveStock(skuId, request.getQuantity(), request.getOrderId());
        return ResponseEntity.ok(Map.of("message", "Reserved successfully"));
    }

    @PostMapping("/{skuId}/release")
    public ResponseEntity<Map<String, String>> release(
            @PathVariable UUID skuId,
            @Valid @RequestBody ReservationRequest request) {
        inventoryService.releaseStock(skuId, request.getQuantity(), request.getOrderId());
        return ResponseEntity.ok(Map.of("message", "Released successfully"));
    }

    @PostMapping("/{skuId}/restock")
    public ResponseEntity<Map<String, String>> restock(
            @PathVariable UUID skuId,
            @RequestParam int quantity) {
        inventoryService.restock(skuId, quantity);
        return ResponseEntity.ok(Map.of("message", "Restocked successfully"));
    }
}
