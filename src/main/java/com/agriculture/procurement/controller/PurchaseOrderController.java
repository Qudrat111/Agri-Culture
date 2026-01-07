package com.agriculture.procurement.controller;

import com.agriculture.procurement.entity.PurchaseOrder;
import com.agriculture.procurement.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * REST controller for purchase order operations.
 */
@RestController
@RequestMapping("/api/pos")
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderController {
    
    private final PurchaseOrderService purchaseOrderService;
    
    /**
     * Create a new purchase order.
     * Emits event to Kafka via Outbox pattern.
     */
    @PostMapping
    public ResponseEntity<PurchaseOrderResponse> createPurchaseOrder(
        @RequestBody CreatePurchaseOrderRequest request
    ) {
        log.info("Creating purchase order for supplier {} and product {}", 
            request.supplierId(), request.productId());
        
        PurchaseOrder order = purchaseOrderService.createPurchaseOrder(
            request.supplierId(),
            request.productId(),
            request.quantity(),
            request.unitPrice()
        );
        
        PurchaseOrderResponse response = new PurchaseOrderResponse(
            order.getId(),
            order.getSupplierId(),
            order.getProductId(),
            order.getQuantity(),
            order.getUnitPrice(),
            order.getTotalAmount(),
            order.getStatus().name(),
            order.getSagaId()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get purchase order by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderResponse> getPurchaseOrder(@PathVariable String id) {
        PurchaseOrder order = purchaseOrderService.getPurchaseOrder(id);
        
        PurchaseOrderResponse response = new PurchaseOrderResponse(
            order.getId(),
            order.getSupplierId(),
            order.getProductId(),
            order.getQuantity(),
            order.getUnitPrice(),
            order.getTotalAmount(),
            order.getStatus().name(),
            order.getSagaId()
        );
        
        return ResponseEntity.ok(response);
    }
    
    public record CreatePurchaseOrderRequest(
        String supplierId,
        String productId,
        Integer quantity,
        BigDecimal unitPrice
    ) {}
    
    public record PurchaseOrderResponse(
        String id,
        String supplierId,
        String productId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        String status,
        String sagaId
    ) {}
}
