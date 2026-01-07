package com.agriculture.common.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service for ensuring idempotent message processing.
 * Uses Redis to track processed message IDs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";
    
    /**
     * Check if a message has already been processed.
     * 
     * @param messageId Unique message identifier
     * @return true if already processed, false otherwise
     */
    public boolean isProcessed(String messageId) {
        String key = IDEMPOTENCY_KEY_PREFIX + messageId;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }
    
    /**
     * Mark a message as processed.
     * 
     * @param messageId Unique message identifier
     * @param ttlSeconds Time-to-live in seconds
     */
    public void markAsProcessed(String messageId, long ttlSeconds) {
        String key = IDEMPOTENCY_KEY_PREFIX + messageId;
        redisTemplate.opsForValue().set(key, "processed", Duration.ofSeconds(ttlSeconds));
        log.debug("Marked message {} as processed with TTL {} seconds", messageId, ttlSeconds);
    }
    
    /**
     * Mark a message as processed with default TTL (1 hour).
     * 
     * @param messageId Unique message identifier
     */
    public void markAsProcessed(String messageId) {
        markAsProcessed(messageId, 3600);
    }
    
    /**
     * Try to acquire processing lock for a message.
     * Returns true if lock acquired (message not yet processed).
     * 
     * @param messageId Unique message identifier
     * @param ttlSeconds Time-to-live in seconds
     * @return true if lock acquired, false if already locked/processed
     */
    public boolean tryAcquireLock(String messageId, long ttlSeconds) {
        String key = IDEMPOTENCY_KEY_PREFIX + messageId;
        Boolean success = redisTemplate.opsForValue()
            .setIfAbsent(key, "processing", Duration.ofSeconds(ttlSeconds));
        return Boolean.TRUE.equals(success);
    }
}
