package com.agriculture.billing.pricing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory for creating pricing strategies.
 * Implements Factory pattern for billing module.
 */
@Component
@RequiredArgsConstructor
public class PricingStrategyFactory {
    
    private final List<PricingStrategy> strategies;
    private final Map<String, PricingStrategy> strategyMap = new HashMap<>();
    
    /**
     * Initialize factory with available strategies.
     */
    public void init() {
        for (PricingStrategy strategy : strategies) {
            strategyMap.put(strategy.getStrategyName(), strategy);
        }
    }
    
    /**
     * Get pricing strategy by name.
     */
    public PricingStrategy getStrategy(String strategyName) {
        if (strategyMap.isEmpty()) {
            init();
        }
        
        PricingStrategy strategy = strategyMap.get(strategyName.toUpperCase());
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown pricing strategy: " + strategyName);
        }
        return strategy;
    }
    
    /**
     * Get default pricing strategy.
     */
    public PricingStrategy getDefaultStrategy() {
        return getStrategy("STANDARD");
    }
}
