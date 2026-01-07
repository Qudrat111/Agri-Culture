# Agri-Culture Platform

A modular monolith backend for agricultural procurement with blockchain integration, built with Spring Boot 3 and Java 21.

## Architecture Overview

This platform implements a DDD-inspired modular monolith with the following key modules:

### Modules

1. **Procurement** - Saga orchestrator for purchase orders with Outbox pattern
2. **Inventory** - Idempotent consumer with retry logic for inventory management
3. **Billing** - Factory pattern implementation for flexible pricing strategies
4. **Catalog** - Polyglot persistence (PostgreSQL + MongoDB) for product data
5. **Compliance** - Placeholder for regulatory and certification features
6. **Gateway** - Rate limiting (429 responses) with Bucket4j and Redis
7. **Blockchain** - Web3j adapter for Ethereum smart contract integration
8. **Common** - Shared utilities, Outbox pattern, idempotency, events

### Key Patterns & Technologies

- **Saga Orchestration** - Distributed transaction coordination
- **Outbox Pattern** - Reliable event publishing to Kafka
- **CQRS-Ready Events** - Domain events for event-driven architecture
- **Idempotency** - Redis-backed duplicate message detection
- **Rate Limiting** - Token bucket algorithm with 429 responses
- **Retry Logic** - Spring Retry with configurable backoff
- **Polyglot Persistence** - PostgreSQL (transactional) + MongoDB (flexible schema)
- **Blockchain Integration** - Hardhat + Web3j for on-chain escrow

## Prerequisites

- **Java 17+** - Required for Spring Boot 3
- **Maven 3.9+** - Build tool
- **Docker & Docker Compose** - For infrastructure services
- **Node.js 18+** - For Hardhat smart contract development

## Quick Start

### 1. Start Infrastructure Services

```bash
docker compose up -d
```

This starts:
- PostgreSQL (port 5432)
- MongoDB (port 27017)
- Redis (port 6379)
- Kafka + Zookeeper (ports 9092, 2181)
- OpenTelemetry Collector (ports 4317, 4318)

Wait for all services to be healthy:
```bash
docker compose ps
```

### 2. Build the Application

```bash
mvn clean install
```

This will:
- Compile Java sources
- Run tests
- Package the application

### 3. Run the Spring Boot Application

```bash
mvn spring-boot:run
```

Or run the JAR directly:
```bash
java -jar target/agri-culture-platform-1.0.0.jar
```

The application will start on `http://localhost:8080`

### 4. Verify the Application

Check application health:
```bash
curl http://localhost:8080/actuator/health
```

## API Endpoints

### Purchase Orders (`/api/pos`)

Create a purchase order (emits Kafka event via Outbox):
```bash
curl -X POST http://localhost:8080/api/pos \
  -H "Content-Type: application/json" \
  -d '{
    "supplierId": "supplier-123",
    "productId": "product-456",
    "quantity": 100,
    "unitPrice": 50.00
  }'
```

Get purchase order:
```bash
curl http://localhost:8080/api/pos/{orderId}
```

### Pricing (`/api/pricing`)

Calculate price with strategy (Factory pattern):
```bash
# Standard pricing
curl -X POST http://localhost:8080/api/pricing/calculate \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "product-123",
    "quantity": 10,
    "strategy": "STANDARD"
  }'

# Bulk pricing (with discounts)
curl -X POST http://localhost:8080/api/pricing/calculate \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "product-123",
    "quantity": 100,
    "strategy": "BULK"
  }'

# Premium pricing
curl -X POST http://localhost:8080/api/pricing/calculate \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "product-123",
    "quantity": 10,
    "strategy": "PREMIUM"
  }'
```

### Blockchain Orders (`/api/chain/orders`)

Create order on blockchain with escrow:
```bash
curl -X POST http://localhost:8080/api/chain/orders \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "order-789",
    "sellerAddress": "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb",
    "amount": 1000000000000000000
  }'
```

Confirm order:
```bash
curl -X POST http://localhost:8080/api/chain/orders/{orderId}/confirm
```

Cancel order (triggers refund):
```bash
curl -X POST http://localhost:8080/api/chain/orders/{orderId}/cancel
```

Get order from blockchain:
```bash
curl http://localhost:8080/api/chain/orders/{orderId}
```

Check blockchain connectivity:
```bash
curl http://localhost:8080/api/chain/orders/health
```

### Rate Limiting

The API implements rate limiting at `/api/*` endpoints. Default: 100 requests per minute per IP.

When limit exceeded, returns:
```json
{
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Please try again later."
}
```
HTTP Status: `429 Too Many Requests`

## Hardhat Smart Contract

### Setup

Install dependencies (if not done):
```bash
npm install
```

### Compile Contract

```bash
npm run compile
```

This compiles `hardhat/contracts/ProcurementOrder.sol`

### Run Tests

```bash
npm test
```

### Coverage Report

Generate test coverage (targeting >=80%):
```bash
npm run test:coverage
```

View coverage report:
```bash
open hardhat/coverage/index.html
```

### Deploy to Local Network

Start Hardhat local node:
```bash
npx hardhat node
```

In another terminal, deploy:
```bash
npm run deploy:local
```

### Deploy to Sepolia Testnet

1. Set environment variables:
```bash
export SEPOLIA_RPC_URL="https://sepolia.infura.io/v3/YOUR_INFURA_PROJECT_ID"
export PRIVATE_KEY="your_private_key_here"
export ETHERSCAN_API_KEY="your_etherscan_api_key"
```

2. Deploy:
```bash
npm run deploy:sepolia
```

3. Update `src/main/resources/application-blockchain.yml` with the deployed contract address.

## Blockchain Configuration

Configure blockchain settings in `application-blockchain.yml`:

```yaml
blockchain:
  enabled: true
  network:
    name: sepolia
    rpc-url: https://sepolia.infura.io/v3/YOUR_INFURA_PROJECT_ID
    chain-id: 11155111
  wallet:
    private-key: 0xYOUR_PRIVATE_KEY_HERE
  contracts:
    procurement-order:
      address: 0xYOUR_DEPLOYED_CONTRACT_ADDRESS
```

**Important Security Notes:**
- Never commit real private keys to version control
- Use environment variables in production: `${BLOCKCHAIN_PRIVATE_KEY}`
- Keep Infura/Alchemy API keys secure

### Running with Blockchain Profile

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=blockchain
```

Or set environment variable:
```bash
export SPRING_PROFILES_ACTIVE=blockchain
java -jar target/agri-culture-platform-1.0.0.jar
```

## Configuration

### Application Properties

Main configuration: `src/main/resources/application.yml`
Blockchain config: `src/main/resources/application-blockchain.yml`

Key settings:
- Database: PostgreSQL connection
- MongoDB: Connection settings
- Kafka: Bootstrap servers and topics
- Redis: Cache and rate limit settings
- Outbox: Scheduler configuration
- Rate Limiting: Token bucket parameters
- Retry: Max attempts and backoff

### Environment Variables

Override settings via environment variables:
```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/agriculture
export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export BLOCKCHAIN_NETWORK_RPC_URL=https://sepolia.infura.io/v3/YOUR_PROJECT_ID
export BLOCKCHAIN_WALLET_PRIVATE_KEY=0xYOUR_PRIVATE_KEY
```

## Development

### Project Structure

```
agri-culture-platform/
├── src/main/java/com/agriculture/
│   ├── procurement/          # Purchase orders, Saga orchestrator
│   ├── inventory/            # Inventory management, idempotent consumer
│   ├── billing/              # Pricing strategies, factory pattern
│   ├── catalog/              # Product catalog (SQL + NoSQL)
│   ├── compliance/           # Regulatory placeholder
│   ├── gateway/              # Rate limiting filter
│   ├── blockchain/           # Web3j adapter and controllers
│   ├── common/               # Outbox, idempotency, events
│   └── api/                  # REST controllers
├── hardhat/
│   ├── contracts/            # Solidity smart contracts
│   ├── test/                 # Smart contract tests
│   └── scripts/              # Deployment scripts
├── docker-compose.yml        # Infrastructure services
└── pom.xml                   # Maven dependencies
```

### Adding New Modules

1. Create package under `com.agriculture`
2. Follow DDD patterns (entities, repositories, services)
3. Use Outbox pattern for events
4. Implement idempotency for consumers
5. Add REST controllers under appropriate package

### Testing

Run Java tests:
```bash
mvn test
```

Run Hardhat tests:
```bash
npm test
```

Run with coverage:
```bash
npm run test:coverage
```

## Monitoring & Observability

### OpenTelemetry

The application includes OpenTelemetry configuration stubs for:
- Distributed tracing
- Metrics collection
- Log aggregation

Collector endpoint: `http://localhost:4317` (gRPC) or `http://localhost:4318` (HTTP)

View Prometheus metrics: `http://localhost:8889`

### Logging

Application logs include:
- Request/response logging
- Saga execution tracking
- Outbox publishing status
- Kafka consumer processing
- Blockchain transaction hashes

Log level configuration in `application.yml`

## Production Considerations

1. **Security**
   - Use secrets management (e.g., AWS Secrets Manager, HashiCorp Vault)
   - Enable HTTPS/TLS
   - Implement authentication/authorization
   - Rotate blockchain private keys regularly

2. **Scalability**
   - Scale horizontally behind load balancer
   - Use dedicated Kafka cluster
   - Consider read replicas for PostgreSQL
   - Implement caching strategies

3. **Reliability**
   - Configure Kafka retention policies
   - Set up database backups
   - Monitor Outbox processing lag
   - Implement circuit breakers

4. **Blockchain**
   - Use dedicated RPC nodes (not public endpoints)
   - Implement gas price optimization
   - Set up event monitoring and alerting
   - Handle chain reorganizations

## Troubleshooting

### Docker Services Not Starting

```bash
docker compose down -v
docker compose up -d
```

### Kafka Connection Issues

Check Kafka is running:
```bash
docker compose logs kafka
```

### Database Connection Errors

Verify PostgreSQL:
```bash
docker compose exec postgres psql -U agriuser -d agriculture
```

### Blockchain Connection Failed

1. Check RPC URL is correct
2. Verify network connectivity
3. Ensure private key is valid
4. Check contract address is deployed

### Rate Limit Testing

Use a tool like Apache Bench to test:
```bash
ab -n 200 -c 10 http://localhost:8080/api/pos/test-id
```

## License

ISC

## Support

For issues and questions, please open a GitHub issue.
