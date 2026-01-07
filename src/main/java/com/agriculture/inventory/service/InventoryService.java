package com.agriculture.inventory.service;

import com.agriculture.common.outbox.OutboxPublisher;
import com.agriculture.inventory.entity.Inventory;
import com.agriculture.inventory.entity.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service for managing inventory with retry logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
    
    private final InventoryRepository inventoryRepository;
    private final OutboxPublisher outboxPublisher;
    
    private static final String INVENTORY_TOPIC = "inventory-events";
    
    /**
     * Reserve inventory for a purchase order.
     * Includes retry logic for transient failures.
     */
    @Transactional
    @Retryable(
        maxAttemptsExpression = "${agriculture.retry.max-attempts:3}",
        backoff = @Backoff(
            delayExpression = "${agriculture.retry.backoff.delay:1000}",
            multiplierExpression = "${agriculture.retry.backoff.multiplier:2.0}"
        )
    )
    public void reserveInventory(String productId, Integer quantity, String orderId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        
        inventory.reserve(quantity);
        inventoryRepository.save(inventory);
        
        // Publish event via outbox
        InventoryReservedEvent event = new InventoryReservedEvent(productId, quantity, orderId);
        outboxPublisher.saveEvent(
            productId,
            "Inventory",
            "InventoryReserved",
            event,
            INVENTORY_TOPIC
        );
        
        log.info("Reserved {} units of product {} for order {}", quantity, productId, orderId);
    }
    
    /**
     * Release reserved inventory (compensation).
     */
    @Transactional
    public void releaseInventory(String productId, Integer quantity, String orderId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        
        inventory.release(quantity);
        inventoryRepository.save(inventory);
        
        log.info("Released {} units of product {} for order {}", quantity, productId, orderId);
    }
    
    /**
     * Initialize inventory for a product.
     */
    @Transactional
    public Inventory initializeInventory(String productId, Integer initialQuantity) {
        Inventory inventory = Inventory.builder()
            .productId(productId)
            .availableQuantity(initialQuantity)
            .reservedQuantity(0)
            .createdAt(Instant.now())
            .build();
        
        return inventoryRepository.save(inventory);
    }
    
    /**
     * Event for inventory reservation
     */
    public record InventoryReservedEvent(String productId, Integer quantity, String orderId) {}
}
