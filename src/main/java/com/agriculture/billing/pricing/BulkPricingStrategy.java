package com.agriculture.billing.pricing;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Bulk pricing strategy with tiered discounts.
 */
@Component
public class BulkPricingStrategy implements PricingStrategy {
    
    private static final BigDecimal BASE_PRICE = new BigDecimal("100.00");
    private static final BigDecimal DISCOUNT_TIER1 = new BigDecimal("0.90"); // 10% off for 10+ units
    private static final BigDecimal DISCOUNT_TIER2 = new BigDecimal("0.85"); // 15% off for 50+ units
    private static final BigDecimal DISCOUNT_TIER3 = new BigDecimal("0.80"); // 20% off for 100+ units
    
    @Override
    public BigDecimal calculatePrice(String productId, Integer quantity) {
        BigDecimal baseTotal = BASE_PRICE.multiply(BigDecimal.valueOf(quantity));
        
        if (quantity >= 100) {
            return baseTotal.multiply(DISCOUNT_TIER3);
        } else if (quantity >= 50) {
            return baseTotal.multiply(DISCOUNT_TIER2);
        } else if (quantity >= 10) {
            return baseTotal.multiply(DISCOUNT_TIER1);
        }
        
        return baseTotal;
    }
    
    @Override
    public String getStrategyName() {
        return "BULK";
    }
}
