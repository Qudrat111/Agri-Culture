package com.agriculture.inventory.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Inventory entity for tracking product stock.
 */
@Entity
@Table(name = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false, unique = true)
    private String productId;
    
    @Column(nullable = false)
    private Integer availableQuantity;
    
    @Column(nullable = false)
    private Integer reservedQuantity;
    
    @Builder.Default
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
    
    @Column
    private Instant updatedAt;
    
    public void reserve(Integer quantity) {
        if (availableQuantity < quantity) {
            throw new IllegalStateException("Insufficient inventory");
        }
        availableQuantity -= quantity;
        reservedQuantity += quantity;
        updatedAt = Instant.now();
    }
    
    public void release(Integer quantity) {
        if (reservedQuantity < quantity) {
            throw new IllegalStateException("Invalid release quantity");
        }
        reservedQuantity -= quantity;
        availableQuantity += quantity;
        updatedAt = Instant.now();
    }
}
