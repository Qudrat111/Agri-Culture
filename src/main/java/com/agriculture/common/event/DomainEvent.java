package com.agriculture.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events in the system.
 * Supports CQRS and event-driven architecture patterns.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class DomainEvent {
    
    private String eventId = UUID.randomUUID().toString();
    private Instant timestamp = Instant.now();
    private String eventType;
    
    public DomainEvent(String eventType) {
        this.eventType = eventType;
    }
    
    public abstract String getAggregateId();
}
