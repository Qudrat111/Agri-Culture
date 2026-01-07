package com.agriculture.billing.service;

import com.agriculture.billing.pricing.PricingStrategy;
import com.agriculture.billing.pricing.PricingStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service for billing and pricing operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {
    
    private final PricingStrategyFactory pricingStrategyFactory;
    
    /**
     * Calculate price using specified strategy.
     */
    public BigDecimal calculatePrice(String productId, Integer quantity, String strategyName) {
        PricingStrategy strategy = pricingStrategyFactory.getStrategy(strategyName);
        BigDecimal price = strategy.calculatePrice(productId, quantity);
        
        log.info("Calculated price for product {} with {} strategy: {}", 
            productId, strategyName, price);
        
        return price;
    }
    
    /**
     * Calculate price using default strategy.
     */
    public BigDecimal calculatePrice(String productId, Integer quantity) {
        PricingStrategy strategy = pricingStrategyFactory.getDefaultStrategy();
        BigDecimal price = strategy.calculatePrice(productId, quantity);
        
        log.info("Calculated price for product {} with default strategy: {}", 
            productId, price);
        
        return price;
    }
}
