package com.agriculture.inventory.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Inventory
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, String> {
    
    Optional<Inventory> findByProductId(String productId);
}
