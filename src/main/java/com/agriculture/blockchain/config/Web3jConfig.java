package com.agriculture.blockchain.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;

/**
 * Configuration for Web3j and blockchain connectivity.
 */
@Configuration
@ConditionalOnProperty(prefix = "blockchain", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class Web3jConfig {
    
    private final BlockchainProperties blockchainProperties;
    
    /**
     * Create Web3j instance for Ethereum connectivity.
     */
    @Bean
    public Web3j web3j() {
        String rpcUrl = blockchainProperties.getNetwork().getRpcUrl();
        if (rpcUrl == null || rpcUrl.isEmpty() || rpcUrl.contains("YOUR_")) {
            log.warn("Blockchain RPC URL not configured. Using localhost default.");
            rpcUrl = "http://localhost:8545";
        }
        
        log.info("Connecting to blockchain network: {} at {}", 
            blockchainProperties.getNetwork().getName(), rpcUrl);
        
        return Web3j.build(new HttpService(rpcUrl));
    }
    
    /**
     * Create credentials for transaction signing.
     */
    @Bean
    public Credentials credentials() {
        String privateKey = blockchainProperties.getWallet().getPrivateKey();
        
        if (privateKey == null || privateKey.isEmpty() || privateKey.contains("YOUR_")) {
            log.warn("Private key not configured. Blockchain transactions will not work.");
            // Return dummy credentials for testing
            return Credentials.create("0x0000000000000000000000000000000000000000000000000000000000000001");
        }
        
        // Remove 0x prefix if present
        if (privateKey.startsWith("0x")) {
            privateKey = privateKey.substring(2);
        }
        
        Credentials creds = Credentials.create(privateKey);
        log.info("Loaded credentials for address: {}", creds.getAddress());
        
        return creds;
    }
    
    /**
     * Create gas provider for transactions.
     */
    @Bean
    public DefaultGasProvider gasProvider() {
        return new DefaultGasProvider();
    }
}
