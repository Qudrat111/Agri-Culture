package com.agriculture.api.controller;

import com.agriculture.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * REST controller for pricing operations using factory pattern.
 */
@RestController
@RequestMapping("/api/pricing")
@RequiredArgsConstructor
@Slf4j
public class PricingController {
    
    private final BillingService billingService;
    
    /**
     * Calculate price using specified pricing strategy.
     */
    @PostMapping("/calculate")
    public ResponseEntity<PriceResponse> calculatePrice(
        @RequestBody PriceRequest request
    ) {
        log.info("Calculating price for product {} with strategy {}", 
            request.productId(), request.strategy());
        
        BigDecimal price = billingService.calculatePrice(
            request.productId(),
            request.quantity(),
            request.strategy()
        );
        
        PriceResponse response = new PriceResponse(
            request.productId(),
            request.quantity(),
            request.strategy(),
            price
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Calculate price using default strategy.
     */
    @PostMapping("/calculate/default")
    public ResponseEntity<PriceResponse> calculateDefaultPrice(
        @RequestBody DefaultPriceRequest request
    ) {
        log.info("Calculating price for product {} with default strategy", 
            request.productId());
        
        BigDecimal price = billingService.calculatePrice(
            request.productId(),
            request.quantity()
        );
        
        PriceResponse response = new PriceResponse(
            request.productId(),
            request.quantity(),
            "STANDARD",
            price
        );
        
        return ResponseEntity.ok(response);
    }
    
    public record PriceRequest(
        String productId,
        Integer quantity,
        String strategy
    ) {}
    
    public record DefaultPriceRequest(
        String productId,
        Integer quantity
    ) {}
    
    public record PriceResponse(
        String productId,
        Integer quantity,
        String strategy,
        BigDecimal price
    ) {}
}
