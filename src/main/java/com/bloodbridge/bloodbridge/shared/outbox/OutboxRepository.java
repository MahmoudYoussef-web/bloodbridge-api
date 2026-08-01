package com.bloodbridge.bloodbridge.shared.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, String> {

    @Query("SELECT o FROM OutboxEvent o WHERE o.status = :status AND o.createdAt <= :threshold ORDER BY o.createdAt ASC")
    List<OutboxEvent> findPendingEvents(@Param("status") OutboxStatus status, @Param("threshold") LocalDateTime threshold);

    @Modifying
    @Query("UPDATE OutboxEvent o SET o.status = :status, o.processedAt = :now WHERE o.id = :id AND o.status = 'PENDING'")
    int markAsProcessed(@Param("id") String id, @Param("status") OutboxStatus status, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE OutboxEvent o SET o.status = 'FAILED', o.errorMessage = :error, o.retryCount = o.retryCount + 1 WHERE o.id = :id")
    int markAsFailed(@Param("id") String id, @Param("error") String error);

    long countByStatus(OutboxStatus status);
}
