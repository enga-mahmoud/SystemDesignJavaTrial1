package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.CreateInventoryRequest;
import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.entity.InventoryTransaction;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.inventory.repository.InventoryTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(UUID skuId) {
        Inventory inv = inventoryRepository.findBySkuId(skuId)
            .orElseThrow(() -> new RuntimeException("Inventory not found for SKU: " + skuId));
        return toResponse(inv);
    }

    @Transactional
    public InventoryResponse createInventory(CreateInventoryRequest request) {
        Inventory inv = Inventory.builder()
            .skuId(request.getSkuId())
            .productId(request.getProductId())
            .quantity(request.getQuantity())
            .reservedQuantity(0)
            .build();
        inv = inventoryRepository.save(inv);
        return toResponse(inv);
    }

    @Retryable(
        retryFor = ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    @Transactional
    public void reserveStock(UUID skuId, int quantity, UUID orderId) {
        Inventory inv = inventoryRepository.findBySkuIdForUpdate(skuId)
            .orElseThrow(() -> new RuntimeException("Inventory not found for SKU: " + skuId));

        if (inv.getAvailableQuantity() < quantity) {
            throw new RuntimeException("Insufficient stock for SKU: " + skuId +
                ". Available: " + inv.getAvailableQuantity() + ", Requested: " + quantity);
        }

        inv.setReservedQuantity(inv.getReservedQuantity() + quantity);
        inventoryRepository.save(inv);

        transactionRepository.save(InventoryTransaction.builder()
            .skuId(skuId)
            .type("RESERVE")
            .quantity(quantity)
            .orderId(orderId)
            .build());

        log.info("Reserved {} units for SKU={} orderId={}", quantity, skuId, orderId);
    }

    @Retryable(
        retryFor = ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    @Transactional
    public void releaseStock(UUID skuId, int quantity, UUID orderId) {
        Inventory inv = inventoryRepository.findBySkuIdForUpdate(skuId)
            .orElseThrow(() -> new RuntimeException("Inventory not found for SKU: " + skuId));

        int newReserved = Math.max(0, inv.getReservedQuantity() - quantity);
        inv.setReservedQuantity(newReserved);
        inventoryRepository.save(inv);

        transactionRepository.save(InventoryTransaction.builder()
            .skuId(skuId)
            .type("RELEASE")
            .quantity(quantity)
            .orderId(orderId)
            .build());

        log.info("Released {} units for SKU={} orderId={}", quantity, skuId, orderId);
    }

    @Transactional
    public void restock(UUID skuId, int quantity) {
        Inventory inv = inventoryRepository.findBySkuId(skuId)
            .orElseThrow(() -> new RuntimeException("Inventory not found for SKU: " + skuId));
        inv.setQuantity(inv.getQuantity() + quantity);
        inventoryRepository.save(inv);

        transactionRepository.save(InventoryTransaction.builder()
            .skuId(skuId)
            .type("RESTOCK")
            .quantity(quantity)
            .build());
    }

    private InventoryResponse toResponse(Inventory inv) {
        return InventoryResponse.builder()
            .skuId(inv.getSkuId())
            .productId(inv.getProductId())
            .quantity(inv.getQuantity())
            .reservedQuantity(inv.getReservedQuantity())
            .availableQuantity(inv.getAvailableQuantity())
            .build();
    }
}
