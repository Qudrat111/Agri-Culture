package com.agriculture.procurement.saga;

import com.agriculture.procurement.entity.PurchaseOrder;
import com.agriculture.procurement.entity.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Saga orchestrator for purchase order processing.
 * Coordinates the distributed transaction across procurement, inventory, and billing modules.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderSaga {
    
    private final PurchaseOrderRepository purchaseOrderRepository;
    
    /**
     * Start the purchase order saga.
     */
    @Transactional
    public String startSaga(PurchaseOrder order) {
        String sagaId = UUID.randomUUID().toString();
        order.setSagaId(sagaId);
        order.setStatus(PurchaseOrder.PurchaseOrderStatus.PENDING);
        
        purchaseOrderRepository.save(order);
        log.info("Started purchase order saga {} for order {}", sagaId, order.getId());
        
        return sagaId;
    }
    
    /**
     * Complete the saga successfully.
     */
    @Transactional
    public void completeSaga(String sagaId) {
        purchaseOrderRepository.findBySagaId(sagaId).ifPresent(order -> {
            order.setStatus(PurchaseOrder.PurchaseOrderStatus.COMPLETED);
            order.setUpdatedAt(Instant.now());
            purchaseOrderRepository.save(order);
            log.info("Completed purchase order saga {} for order {}", sagaId, order.getId());
        });
    }
    
    /**
     * Compensate the saga (rollback).
     */
    @Transactional
    public void compensateSaga(String sagaId, String reason) {
        purchaseOrderRepository.findBySagaId(sagaId).ifPresent(order -> {
            order.setStatus(PurchaseOrder.PurchaseOrderStatus.FAILED);
            order.setUpdatedAt(Instant.now());
            purchaseOrderRepository.save(order);
            log.warn("Compensated purchase order saga {} for order {}: {}", 
                sagaId, order.getId(), reason);
        });
    }
    
    /**
     * Update saga step status.
     */
    @Transactional
    public void updateSagaStep(String sagaId, PurchaseOrder.PurchaseOrderStatus status) {
        purchaseOrderRepository.findBySagaId(sagaId).ifPresent(order -> {
            order.setStatus(status);
            order.setUpdatedAt(Instant.now());
            purchaseOrderRepository.save(order);
            log.info("Updated saga {} step to status {}", sagaId, status);
        });
    }
}
