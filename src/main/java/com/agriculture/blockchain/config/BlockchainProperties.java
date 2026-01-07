package com.agriculture.blockchain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for blockchain integration.
 */
@Configuration
@ConfigurationProperties(prefix = "blockchain")
@Data
public class BlockchainProperties {
    
    private boolean enabled = true;
    private Network network = new Network();
    private Wallet wallet = new Wallet();
    private Contracts contracts = new Contracts();
    private Transaction transaction = new Transaction();
    private Polling polling = new Polling();
    private Events events = new Events();
    
    @Data
    public static class Network {
        private String name = "sepolia";
        private String rpcUrl;
        private Long chainId = 11155111L;
    }
    
    @Data
    public static class Wallet {
        private String privateKey;
        private String keystorePath;
        private String keystorePassword;
    }
    
    @Data
    public static class Contracts {
        private ProcurementOrderContract procurementOrder = new ProcurementOrderContract();
    }
    
    @Data
    public static class ProcurementOrderContract {
        private String address;
        private Long deploymentBlock = 0L;
    }
    
    @Data
    public static class Transaction {
        private Long gasPriceGwei;
        private Long gasLimitDeploy = 3000000L;
        private Long gasLimitInteract = 500000L;
        private Integer timeoutSeconds = 120;
    }
    
    @Data
    public static class Polling {
        private Long receiptIntervalMs = 3000L;
        private Integer maxAttempts = 40;
    }
    
    @Data
    public static class Events {
        private boolean enabled = true;
        private Integer confirmationBlocks = 3;
        private Long pollingIntervalMs = 5000L;
    }
}
