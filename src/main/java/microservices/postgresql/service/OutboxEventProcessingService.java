package microservices.postgresql.service;

import lombok.RequiredArgsConstructor;
import microservices.postgresql.entity.OutboxEvent;
import microservices.postgresql.enums.OutboxStatus;
import microservices.postgresql.messaging.ClaimedOutboxEvent;
import microservices.postgresql.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.time.Duration;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class OutboxEventProcessingService {

    private final OutboxEventRepository outboxEventRepository;
    private static final int MAX_ATTEMPTS = 5;
    private static final long INITIAL_RETRY_DELAY_SECONDS = 5;
    @Transactional
    public Optional<ClaimedOutboxEvent> claim(UUID eventId) {

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        Optional<OutboxEvent> lockedEvent =
                outboxEventRepository.findByIdForUpdate(eventId);

        if (lockedEvent.isEmpty()) {
            return Optional.empty();
        }

        OutboxEvent event = lockedEvent.get();

        if (!isEligible(event, now)) {
            return Optional.empty();
        }

        event.markProcessing();

        return Optional.of(
                ClaimedOutboxEvent.from(event)
        );
    }

    private boolean isEligible(
            OutboxEvent event,
            LocalDateTime now
    ) {

        if (event.getStatus() == OutboxStatus.PENDING) {
            return true;
        }

        if (event.getStatus() == OutboxStatus.RETRY) {

            return event.getNextAttemptAt() == null
                    || !event.getNextAttemptAt().isAfter(now);
        }

        return false;
    }
    @Transactional
    public void markPublished(UUID eventId) {

        Optional<OutboxEvent> lockedEvent =
                outboxEventRepository.findByIdForUpdate(eventId);

        if (lockedEvent.isEmpty()) {
            return;
        }

        OutboxEvent event = lockedEvent.get();

        if (event.getStatus() != OutboxStatus.PROCESSING) {
            return;
        }

        event.markPublished();
    }

    @Transactional
    public void recordFailure(
            UUID eventId,
            String errorMessage
    ) {

        Optional<OutboxEvent> lockedEvent =
                outboxEventRepository.findByIdForUpdate(eventId);

        if (lockedEvent.isEmpty()) {
            return;
        }

        OutboxEvent event = lockedEvent.get();

        if (event.getStatus() != OutboxStatus.PROCESSING) {
            return;
        }

        if (event.getAttemptCount() >= MAX_ATTEMPTS) {

            event.markDeadLetter(errorMessage);
            return;
        }

        long retryDelaySeconds =
                calculateRetryDelaySeconds(
                        event.getAttemptCount()
                );

        event.scheduleRetry(
                errorMessage,
                retryDelaySeconds
        );
    }
    private long calculateRetryDelaySeconds(int attemptCount) {

        return INITIAL_RETRY_DELAY_SECONDS
                * (1L << Math.max(0, attemptCount - 1));
    }
    @Transactional
    public int recoverStaleProcessingEvents(
            Duration processingTimeout
    ) {

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        LocalDateTime cutoff =
                now.minus(processingTimeout);

        return outboxEventRepository
                .recoverStaleProcessingEvents(
                        OutboxStatus.PROCESSING,
                        OutboxStatus.RETRY,
                        cutoff,
                        now,
                        "Recovered after processing timeout"
                );
    }


}