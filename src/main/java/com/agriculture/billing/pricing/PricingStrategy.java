package com.agriculture.billing.pricing;

import java.math.BigDecimal;

/**
 * Interface for pricing strategies (Strategy pattern).
 */
public interface PricingStrategy {
    
    BigDecimal calculatePrice(String productId, Integer quantity);
    
    String getStrategyName();
}
