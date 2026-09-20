package microservices.postgresql.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import microservices.postgresql.enums.OutboxStatus;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "topic", nullable = false, length = 100)
    private String topic;

    @Column(name = "event_key", nullable = false, length = 100)
    private String eventKey;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OutboxStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @PrePersist
    void prePersist() {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (status == null) {
            status = OutboxStatus.PENDING;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneOffset.UTC);
        }
        if (attemptCount < 0) {
            attemptCount = 0;
        }
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now(ZoneOffset.UTC);
        this.processingStartedAt = null;
        this.nextAttemptAt = null;
        this.errorMessage = null;
    }

    public void markProcessing() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        this.status = OutboxStatus.PROCESSING;
        this.attemptCount++;
        this.lastAttemptAt = now;
        this.processingStartedAt = now;
        this.nextAttemptAt = null;
    }

    public void scheduleRetry(String errorMessage, long delaySeconds) {
        this.status = OutboxStatus.RETRY;
        this.errorMessage = errorMessage;
        this.nextAttemptAt = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(delaySeconds);
        this.processingStartedAt = null;
    }

    public void markDeadLetter(String errorMessage) {
        this.status = OutboxStatus.DEAD_LETTER;
        this.errorMessage = errorMessage;
        this.nextAttemptAt = null;
        this.processingStartedAt = null;
    }

}