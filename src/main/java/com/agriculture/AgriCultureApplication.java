package com.agriculture;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Agri-Culture modular monolith platform.
 * 
 * Modules:
 * - Procurement: Saga orchestrator with Outbox pattern
 * - Inventory: Idempotent consumer with Outbox
 * - Billing: Factory pattern for pricing
 * - Catalog: SQL + NoSQL entities
 * - Compliance: Placeholder for regulatory features
 * - Gateway: Rate limiting and API layer
 * - Blockchain: Web3j adapter for smart contracts
 * - Common: Shared utilities
 */
@SpringBootApplication
@EnableKafka
@EnableRetry
@EnableScheduling
public class AgriCultureApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgriCultureApplication.class, args);
    }
}
