package microservices.postgresql.repository;

import jakarta.persistence.LockModeType;
import microservices.postgresql.entity.OutboxEvent;
import microservices.postgresql.enums.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query("""
            SELECT e
            FROM OutboxEvent e
            WHERE e.status = :pendingStatus
               OR (
                    e.status = :retryStatus
                    AND (
                        e.nextAttemptAt IS NULL
                        OR e.nextAttemptAt <= :now
                    )
               )
            ORDER BY e.createdAt ASC
            """)
    List<OutboxEvent> findEligibleForPublishing(
            @Param("pendingStatus") OutboxStatus pendingStatus,
            @Param("retryStatus") OutboxStatus retryStatus,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e
            FROM OutboxEvent e
            WHERE e.eventId = :eventId
            """)
    Optional<OutboxEvent> findByIdForUpdate(
            @Param("eventId") UUID eventId
    );
    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
        UPDATE OutboxEvent e
        SET e.status = :retryStatus,
            e.nextAttemptAt = :now,
            e.processingStartedAt = null,
            e.errorMessage = :errorMessage
        WHERE e.status = :processingStatus
          AND e.processingStartedAt IS NOT NULL
          AND e.processingStartedAt < :cutoff
        """)
    int recoverStaleProcessingEvents(
            @Param("processingStatus") OutboxStatus processingStatus,
            @Param("retryStatus") OutboxStatus retryStatus,
            @Param("cutoff") LocalDateTime cutoff,
            @Param("now") LocalDateTime now,
            @Param("errorMessage") String errorMessage
    );
}