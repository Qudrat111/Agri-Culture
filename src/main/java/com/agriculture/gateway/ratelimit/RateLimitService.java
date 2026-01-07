package com.agriculture.gateway.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Rate limiting service using Bucket4j and Redis.
 * Implements token bucket algorithm for API rate limiting.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {
    
    private final LettuceConnectionFactory redisConnectionFactory;
    
    @Value("${agriculture.rate-limit.default-capacity:100}")
    private long defaultCapacity;
    
    @Value("${agriculture.rate-limit.default-refill-tokens:100}")
    private long defaultRefillTokens;
    
    @Value("${agriculture.rate-limit.default-refill-period:60}")
    private long defaultRefillPeriodSeconds;
    
    private ProxyManager<String> proxyManager;
    
    /**
     * Initialize the proxy manager for distributed rate limiting.
     */
    private synchronized ProxyManager<String> getProxyManager() {
        if (proxyManager == null) {
            // Create Redis client from connection factory
            String host = redisConnectionFactory.getHostName();
            int port = redisConnectionFactory.getPort();
            RedisClient redisClient = RedisClient.create("redis://" + host + ":" + port);
            StatefulRedisConnection<byte[], byte[]> connection = 
                redisClient.connect(ByteArrayCodec.INSTANCE);
            
            proxyManager = LettuceBasedProxyManager.builderFor(connection)
                .build();
        }
        return proxyManager;
    }
    
    /**
     * Resolve bucket for a given key (e.g., user ID, IP address).
     */
    public Bucket resolveBucket(String key) {
        Supplier<BucketConfiguration> configSupplier = () -> {
            Bandwidth limit = Bandwidth.builder()
                .capacity(defaultCapacity)
                .refillGreedy(defaultRefillTokens, Duration.ofSeconds(defaultRefillPeriodSeconds))
                .build();
            
            return BucketConfiguration.builder()
                .addLimit(limit)
                .build();
        };
        
        return getProxyManager().builder().build(key, configSupplier);
    }
    
    /**
     * Check if request is allowed for given key.
     */
    public boolean isAllowed(String key) {
        Bucket bucket = resolveBucket(key);
        boolean allowed = bucket.tryConsume(1);
        
        if (!allowed) {
            log.warn("Rate limit exceeded for key: {}", key);
        }
        
        return allowed;
    }
}
