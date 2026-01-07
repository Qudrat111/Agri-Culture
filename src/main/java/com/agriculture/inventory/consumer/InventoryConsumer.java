package com.agriculture.inventory.consumer;

import com.agriculture.common.idempotency.IdempotencyService;
import com.agriculture.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Idempotent Kafka consumer for inventory operations.
 * Processes purchase order events and reserves inventory.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryConsumer {
    
    private final InventoryService inventoryService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;
    
    /**
     * Listen to purchase order events and reserve inventory.
     * Implements idempotency to prevent duplicate processing.
     */
    @KafkaListener(
        topics = "purchase-orders",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumePurchaseOrderEvent(
        @Payload String message,
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        @Header(KafkaHeaders.OFFSET) Long offset,
        Acknowledgment acknowledgment
    ) {
        // Generate idempotency key from topic + partition + offset
        String idempotencyKey = "purchase-orders-" + key + "-" + offset;
        
        try {
            // Check if already processed
            if (idempotencyService.isProcessed(idempotencyKey)) {
                log.info("Message already processed, skipping: {}", idempotencyKey);
                acknowledgment.acknowledge();
                return;
            }
            
            // Try to acquire processing lock
            if (!idempotencyService.tryAcquireLock(idempotencyKey, 300)) {
                log.warn("Could not acquire lock for message: {}", idempotencyKey);
                return; // Don't acknowledge, will retry
            }
            
            // Parse event
            JsonNode event = objectMapper.readTree(message);
            String orderId = event.get("orderId").asText();
            String productId = event.get("productId").asText();
            Integer quantity = event.get("quantity").asInt();
            
            // Process inventory reservation
            inventoryService.reserveInventory(productId, quantity, orderId);
            
            // Mark as processed
            idempotencyService.markAsProcessed(idempotencyKey);
            
            // Acknowledge message
            acknowledgment.acknowledge();
            
            log.info("Successfully processed purchase order event for order {}", orderId);
            
        } catch (Exception e) {
            log.error("Error processing purchase order event: {}", e.getMessage(), e);
            // Don't acknowledge - message will be retried
        }
    }
}
