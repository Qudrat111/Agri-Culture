# Security Considerations

This document outlines security considerations and best practices for the Agri-Culture platform.

## ⚠️ Current Implementation Status

**This is a development/demonstration implementation.** Before deploying to production, address all items in the Production Security Checklist below.

## Known Security Issues in Current Implementation

### 1. Hardcoded Credentials

**Issue**: Database and service credentials are hardcoded in configuration files.

**Files Affected**:
- `src/main/resources/application.yml` (PostgreSQL, MongoDB passwords)
- `src/main/resources/application-blockchain.yml` (placeholder private key)
- `docker-compose.yml` (service passwords)

**Production Fix**:
```yaml
# Use environment variables
spring:
  datasource:
    password: ${DB_PASSWORD}
  data:
    mongodb:
      password: ${MONGO_PASSWORD}

blockchain:
  wallet:
    private-key: ${BLOCKCHAIN_PRIVATE_KEY}
```

**Recommended Solutions**:
- Use environment variables for all secrets
- Use secrets management services (AWS Secrets Manager, HashiCorp Vault, Azure Key Vault)
- Use Spring Cloud Config Server with encryption
- Never commit real credentials to version control

### 2. Missing Input Validation

**Issue**: API endpoints lack validation annotations.

**Files Affected**:
- `PurchaseOrderController.java`
- `PricingController.java`
- `BlockchainOrderController.java`

**Production Fix**:
```java
public record CreatePurchaseOrderRequest(
    @NotBlank String supplierId,
    @NotBlank String productId,
    @Positive Integer quantity,
    @Positive BigDecimal unitPrice
) {}
```

**Required Dependencies**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### 3. Generic Exception Handling

**Issue**: Services throw generic `RuntimeException` instead of specific exceptions.

**Production Fix**:
```java
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String orderId) {
        super("Purchase order not found: " + orderId);
    }
}

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getMessage()));
    }
}
```

### 4. No Authentication/Authorization

**Issue**: All endpoints are publicly accessible.

**Production Fix**:
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/pos/**").hasRole("BUYER")
                .requestMatchers("/api/chain/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
            .build();
    }
}
```

### 5. Transaction Boundary Issues

**Issue**: `@Transactional` on scheduled methods may cause issues.

**File**: `OutboxPublisher.java`

**Production Fix**:
```java
@Scheduled(fixedDelayString = "${agriculture.outbox.scheduler.fixed-delay:5000}")
public void publishPendingEvents() {
    List<OutboxEvent> events = outboxRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc();
    
    for (OutboxEvent event : events) {
        processEventInNewTransaction(event);
    }
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
protected void processEventInNewTransaction(OutboxEvent event) {
    // Process single event in isolated transaction
}
```

## Production Security Checklist

### Infrastructure Security

- [ ] Enable TLS/HTTPS for all services
- [ ] Configure firewall rules to restrict database access
- [ ] Use private VPC for service communication
- [ ] Enable database encryption at rest
- [ ] Configure Redis authentication and ACLs
- [ ] Use Kafka SASL/SSL for secure communication
- [ ] Implement network segmentation

### Application Security

- [ ] Add Spring Security with OAuth2/JWT
- [ ] Implement role-based access control (RBAC)
- [ ] Add request validation with Bean Validation
- [ ] Implement rate limiting per user (not just per IP)
- [ ] Add CORS configuration
- [ ] Enable CSRF protection
- [ ] Implement request signing for sensitive operations
- [ ] Add SQL injection protection (use prepared statements)
- [ ] Sanitize all user inputs
- [ ] Implement audit logging

### Secrets Management

- [ ] Externalize all credentials to environment variables
- [ ] Use secrets management service (Vault, AWS Secrets Manager)
- [ ] Rotate database passwords regularly
- [ ] Rotate blockchain private keys
- [ ] Use separate credentials for each environment
- [ ] Never commit secrets to version control
- [ ] Implement secret scanning in CI/CD

### Blockchain Security

- [ ] Use dedicated RPC nodes (not public endpoints)
- [ ] Implement transaction signing on server-side
- [ ] Add gas price limits to prevent excessive costs
- [ ] Implement transaction monitoring and alerting
- [ ] Use hardware wallets for production private keys
- [ ] Implement multi-signature wallets for high-value operations
- [ ] Add contract upgrade mechanisms
- [ ] Conduct smart contract security audit
- [ ] Test on testnet extensively before mainnet deployment

### API Security

- [ ] Add API key authentication
- [ ] Implement request throttling
- [ ] Add request/response logging for audit
- [ ] Validate all input data
- [ ] Implement output encoding
- [ ] Add security headers (CSP, HSTS, X-Frame-Options)
- [ ] Disable unnecessary HTTP methods
- [ ] Implement API versioning
- [ ] Add OpenAPI/Swagger with authentication

### Data Security

- [ ] Encrypt sensitive data at rest
- [ ] Encrypt sensitive data in transit
- [ ] Implement data masking for logs
- [ ] Add PII data handling procedures
- [ ] Implement data retention policies
- [ ] Add GDPR compliance features (if applicable)
- [ ] Implement secure data deletion
- [ ] Add backup encryption

### Monitoring & Incident Response

- [ ] Set up security monitoring and alerting
- [ ] Implement intrusion detection
- [ ] Add anomaly detection for transactions
- [ ] Create incident response plan
- [ ] Set up log aggregation and analysis
- [ ] Implement security event correlation
- [ ] Add vulnerability scanning
- [ ] Conduct regular security assessments
- [ ] Implement disaster recovery plan

### Development Security

- [ ] Add dependency vulnerability scanning
- [ ] Implement SAST (Static Application Security Testing)
- [ ] Add DAST (Dynamic Application Security Testing)
- [ ] Conduct code reviews with security focus
- [ ] Use secure coding guidelines
- [ ] Implement secret scanning in Git
- [ ] Add container image scanning
- [ ] Use signed commits

## Security Best Practices

### 1. Principle of Least Privilege

Grant minimum necessary permissions:
- Database users with specific permissions
- Service accounts with limited scopes
- API keys with restricted endpoints
- Blockchain accounts with minimal funds

### 2. Defense in Depth

Implement multiple security layers:
- Network firewall
- Application firewall (WAF)
- Input validation
- Output encoding
- Authentication
- Authorization
- Rate limiting
- Encryption

### 3. Secure Development Lifecycle

- Security requirements in design phase
- Threat modeling before implementation
- Security code reviews
- Automated security testing in CI/CD
- Penetration testing before release
- Security monitoring in production
- Regular security updates

### 4. Blockchain-Specific Security

- Test extensively on testnets
- Use battle-tested libraries (OpenZeppelin)
- Implement circuit breakers for critical functions
- Add pause functionality for emergencies
- Use time locks for critical operations
- Implement access control carefully
- Test with various attack scenarios
- Monitor for unusual on-chain activity

## Compliance Considerations

### GDPR (if applicable)

- Implement data subject rights (access, deletion)
- Add privacy policy and consent management
- Implement data portability
- Add data breach notification procedures
- Document data processing activities
- Implement privacy by design

### PCI DSS (if handling payments)

- Use tokenization for payment data
- Implement secure payment processing
- Add cardholder data protection
- Regular security testing
- Maintain audit trails

### SOC 2 (for SaaS)

- Implement access controls
- Add logging and monitoring
- Document security procedures
- Regular security assessments
- Incident response procedures

## Resources

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [Smart Contract Security Best Practices](https://consensys.github.io/smart-contract-best-practices/)
- [CWE/SANS Top 25](https://cwe.mitre.org/top25/)
- [NIST Cybersecurity Framework](https://www.nist.gov/cyberframework)

## Contact

For security issues, please report to: security@agriculture.example.com

**Do not report security vulnerabilities in public GitHub issues.**

---

**Important**: This security document should be regularly reviewed and updated as threats evolve and new security features are implemented.
