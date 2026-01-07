package com.agriculture.billing.pricing;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Standard pricing strategy with fixed unit price.
 */
@Component
public class StandardPricingStrategy implements PricingStrategy {
    
    private static final BigDecimal STANDARD_PRICE = new BigDecimal("100.00");
    
    @Override
    public BigDecimal calculatePrice(String productId, Integer quantity) {
        return STANDARD_PRICE.multiply(BigDecimal.valueOf(quantity));
    }
    
    @Override
    public String getStrategyName() {
        return "STANDARD";
    }
}
