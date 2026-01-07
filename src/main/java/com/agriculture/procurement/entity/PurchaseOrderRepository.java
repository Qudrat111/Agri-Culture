package com.agriculture.procurement.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Purchase Orders
 */
@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, String> {
    
    Optional<PurchaseOrder> findBySagaId(String sagaId);
}
