package com.agriculture.blockchain.controller;

import com.agriculture.blockchain.service.BlockchainProcurementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;

/**
 * REST controller for blockchain procurement operations.
 */
@RestController
@RequestMapping("/api/chain/orders")
@ConditionalOnProperty(prefix = "blockchain", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class BlockchainOrderController {
    
    private final BlockchainProcurementService blockchainService;
    
    /**
     * Create order on blockchain with escrow.
     */
    @PostMapping
    public ResponseEntity<BlockchainOrderResponse> createOrder(
        @RequestBody CreateBlockchainOrderRequest request
    ) {
        log.info("Creating blockchain order {} for seller {}", 
            request.orderId(), request.sellerAddress());
        
        String txHash = blockchainService.createOrder(
            request.orderId(),
            request.sellerAddress(),
            request.amount()
        );
        
        BlockchainOrderResponse response = new BlockchainOrderResponse(
            request.orderId(),
            txHash,
            "Order created on blockchain"
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Confirm order on blockchain.
     */
    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<BlockchainOrderResponse> confirmOrder(@PathVariable String orderId) {
        log.info("Confirming blockchain order {}", orderId);
        
        String txHash = blockchainService.confirmOrder(orderId);
        
        BlockchainOrderResponse response = new BlockchainOrderResponse(
            orderId,
            txHash,
            "Order confirmed on blockchain"
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Cancel order on blockchain.
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<BlockchainOrderResponse> cancelOrder(@PathVariable String orderId) {
        log.info("Cancelling blockchain order {}", orderId);
        
        String txHash = blockchainService.cancelOrder(orderId);
        
        BlockchainOrderResponse response = new BlockchainOrderResponse(
            orderId,
            txHash,
            "Order cancelled on blockchain"
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get order details from blockchain.
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<String> getOrder(@PathVariable String orderId) {
        log.info("Fetching blockchain order {}", orderId);
        
        String orderDetails = blockchainService.getOrder(orderId);
        
        return ResponseEntity.ok(orderDetails);
    }
    
    /**
     * Health check for blockchain connectivity.
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        boolean connected = blockchainService.isConnected();
        
        HealthResponse response = new HealthResponse(
            connected,
            connected ? "Blockchain connected" : "Blockchain disconnected"
        );
        
        return ResponseEntity.ok(response);
    }
    
    public record CreateBlockchainOrderRequest(
        String orderId,
        String sellerAddress,
        BigInteger amount
    ) {}
    
    public record BlockchainOrderResponse(
        String orderId,
        String transactionHash,
        String message
    ) {}
    
    public record HealthResponse(
        boolean connected,
        String message
    ) {}
}
