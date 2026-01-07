package com.agriculture.billing.pricing;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Premium pricing strategy with higher unit price.
 */
@Component
public class PremiumPricingStrategy implements PricingStrategy {
    
    private static final BigDecimal PREMIUM_PRICE = new BigDecimal("150.00");
    
    @Override
    public BigDecimal calculatePrice(String productId, Integer quantity) {
        return PREMIUM_PRICE.multiply(BigDecimal.valueOf(quantity));
    }
    
    @Override
    public String getStrategyName() {
        return "PREMIUM";
    }
}
