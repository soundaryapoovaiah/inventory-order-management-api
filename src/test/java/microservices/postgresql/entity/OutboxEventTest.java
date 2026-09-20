package microservices.postgresql.entity;

import microservices.postgresql.enums.OutboxStatus;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OutboxEventTest {

    @Test
    void shouldMarkPendingEventAsProcessing() {

        OutboxEvent event = OutboxEvent.builder()
                .status(OutboxStatus.PENDING)
                .attemptCount(0)
                .build();

        event.markProcessing();

        assertEquals(
                OutboxStatus.PROCESSING,
                event.getStatus()
        );

        assertEquals(
                1,
                event.getAttemptCount()
        );

        assertNotNull(
                event.getLastAttemptAt()
        );

        assertNotNull(
                event.getProcessingStartedAt()
        );

        assertNull(
                event.getNextAttemptAt()
        );
    }


    @Test
    void shouldScheduleRetryAfterFailure() {

        OutboxEvent event = OutboxEvent.builder()
                .status(OutboxStatus.PROCESSING)
                .attemptCount(1)
                .build();

        event.scheduleRetry(
                "Kafka unavailable",
                5
        );

        assertEquals(
                OutboxStatus.RETRY,
                event.getStatus()
        );

        assertEquals(
                "Kafka unavailable",
                event.getErrorMessage()
        );

        assertNotNull(
                event.getNextAttemptAt()
        );

        assertNull(
                event.getProcessingStartedAt()
        );
    }


    @Test
    void shouldMarkEventAsPublished() {

        OutboxEvent event = OutboxEvent.builder()
                .status(OutboxStatus.PROCESSING)
                .attemptCount(2)
                .processingStartedAt(
                        LocalDateTime.now().minusSeconds(5)
                )
                .nextAttemptAt(
                        LocalDateTime.now().plusSeconds(10)
                )
                .errorMessage("Previous Kafka error")
                .build();

        event.markPublished();

        assertEquals(
                OutboxStatus.PUBLISHED,
                event.getStatus()
        );

        assertNotNull(
                event.getPublishedAt()
        );

        assertNull(
                event.getProcessingStartedAt()
        );

        assertNull(
                event.getNextAttemptAt()
        );

        assertNull(
                event.getErrorMessage()
        );
    }


    @Test
    void shouldMoveEventToDeadLetter() {

        OutboxEvent event = OutboxEvent.builder()
                .status(OutboxStatus.PROCESSING)
                .attemptCount(5)
                .build();

        event.markDeadLetter(
                "Maximum retry attempts exceeded"
        );

        assertEquals(
                OutboxStatus.DEAD_LETTER,
                event.getStatus()
        );

        assertEquals(
                "Maximum retry attempts exceeded",
                event.getErrorMessage()
        );

        assertNull(
                event.getNextAttemptAt()
        );

        assertNull(
                event.getProcessingStartedAt()
        );
    }
}