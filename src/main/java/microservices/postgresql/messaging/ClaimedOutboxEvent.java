package microservices.postgresql.messaging;

import microservices.postgresql.entity.OutboxEvent;

import java.util.UUID;

public record ClaimedOutboxEvent(
        UUID eventId,
        String topic,
        String eventKey,
        String payload,
        int attemptCount
) {

    public static ClaimedOutboxEvent from(OutboxEvent event) {
        return new ClaimedOutboxEvent(
                event.getEventId(),
                event.getTopic(),
                event.getEventKey(),
                event.getPayload(),
                event.getAttemptCount()
        );
    }
}