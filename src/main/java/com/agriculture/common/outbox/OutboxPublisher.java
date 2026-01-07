package com.agriculture.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Service for publishing outbox events to Kafka.
 * Implements transactional outbox pattern for reliable messaging.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {
    
    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    /**
     * Scheduled task to process pending outbox events.
     * Runs every 5 seconds by default (configurable via application.yml).
     */
    @Scheduled(fixedDelayString = "${agriculture.outbox.scheduler.fixed-delay:5000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc();
        
        for (OutboxEvent event : pendingEvents) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            markEventAsProcessed(event.getId());
                            log.debug("Published outbox event {} to topic {}", event.getId(), event.getTopic());
                        } else {
                            handlePublishError(event, ex);
                        }
                    });
            } catch (Exception e) {
                handlePublishError(event, e);
            }
        }
    }
    
    @Transactional
    protected void markEventAsProcessed(String eventId) {
        outboxRepository.markAsProcessed(eventId);
    }
    
    @Transactional
    protected void handlePublishError(OutboxEvent event, Throwable error) {
        event.setRetryCount(event.getRetryCount() + 1);
        event.setErrorMessage(error.getMessage());
        outboxRepository.save(event);
        log.error("Failed to publish outbox event {}: {}", event.getId(), error.getMessage());
    }
    
    /**
     * Save a new event to the outbox.
     * This should be called within the same transaction as the business logic.
     */
    @Transactional
    public void saveEvent(String aggregateId, String aggregateType, String eventType, 
                         Object payload, String topic) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            
            OutboxEvent event = OutboxEvent.builder()
                .aggregateId(aggregateId)
                .aggregateType(aggregateType)
                .eventType(eventType)
                .payload(payloadJson)
                .topic(topic)
                .processed(false)
                .createdAt(Instant.now())
                .retryCount(0)
                .build();
            
            outboxRepository.save(event);
            log.debug("Saved outbox event for aggregate {} of type {}", aggregateId, aggregateType);
        } catch (Exception e) {
            log.error("Failed to save outbox event", e);
            throw new RuntimeException("Failed to save outbox event", e);
        }
    }
}
