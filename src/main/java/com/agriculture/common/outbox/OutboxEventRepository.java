package com.agriculture.common.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Outbox events
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    
    List<OutboxEvent> findTop100ByProcessedFalseOrderByCreatedAtAsc();
    
    @Modifying
    @Query("UPDATE OutboxEvent o SET o.processed = true, o.processedAt = CURRENT_TIMESTAMP WHERE o.id = :id")
    void markAsProcessed(String id);
}
