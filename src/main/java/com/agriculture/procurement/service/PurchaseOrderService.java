package com.agriculture.procurement.service;

import com.agriculture.common.outbox.OutboxPublisher;
import com.agriculture.procurement.entity.PurchaseOrder;
import com.agriculture.procurement.entity.PurchaseOrderRepository;
import com.agriculture.procurement.saga.PurchaseOrderCreatedEvent;
import com.agriculture.procurement.saga.PurchaseOrderSaga;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Service for managing purchase orders with Saga orchestration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderService {
    
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderSaga saga;
    private final OutboxPublisher outboxPublisher;
    
    private static final String PURCHASE_ORDER_TOPIC = "purchase-orders";
    
    /**
     * Create a new purchase order.
     * Starts saga and publishes event via outbox pattern.
     */
    @Transactional
    public PurchaseOrder createPurchaseOrder(String supplierId, String productId, 
                                            Integer quantity, BigDecimal unitPrice) {
        // Calculate total
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        
        // Create order
        PurchaseOrder order = PurchaseOrder.builder()
            .supplierId(supplierId)
            .productId(productId)
            .quantity(quantity)
            .unitPrice(unitPrice)
            .totalAmount(totalAmount)
            .createdAt(Instant.now())
            .build();
        
        // Start saga
        String sagaId = saga.startSaga(order);
        
        // Create event
        PurchaseOrderCreatedEvent event = new PurchaseOrderCreatedEvent(
            order.getId(),
            supplierId,
            productId,
            quantity,
            totalAmount,
            sagaId
        );
        
        // Save to outbox for reliable publishing
        outboxPublisher.saveEvent(
            order.getId(),
            "PurchaseOrder",
            "PurchaseOrderCreated",
            event,
            PURCHASE_ORDER_TOPIC
        );
        
        log.info("Created purchase order {} with saga {}", order.getId(), sagaId);
        return order;
    }
    
    /**
     * Get purchase order by ID.
     */
    public PurchaseOrder getPurchaseOrder(String id) {
        return purchaseOrderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Purchase order not found: " + id));
    }
}
