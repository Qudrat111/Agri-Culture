package com.agriculture.procurement.saga;

import com.agriculture.common.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Event emitted when a purchase order is created.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderCreatedEvent extends DomainEvent {
    
    private String orderId;
    private String supplierId;
    private String productId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String sagaId;
    
    public PurchaseOrderCreatedEvent(String orderId, String supplierId, String productId, 
                                    Integer quantity, BigDecimal totalAmount, String sagaId) {
        super("PurchaseOrderCreated");
        this.orderId = orderId;
        this.supplierId = supplierId;
        this.productId = productId;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.sagaId = sagaId;
    }
    
    @Override
    public String getAggregateId() {
        return orderId;
    }
}
