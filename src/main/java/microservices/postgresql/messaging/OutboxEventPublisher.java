package microservices.postgresql.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import java.nio.charset.StandardCharsets;
import microservices.postgresql.entity.OutboxEvent;
import microservices.postgresql.enums.OutboxStatus;
import microservices.postgresql.repository.OutboxEventRepository;
import microservices.postgresql.service.OutboxEventProcessingService;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.ZoneOffset;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {

    private static final int BATCH_SIZE = 20;
    private static final long KAFKA_TIMEOUT_SECONDS = 5;
    private static final Duration PROCESSING_TIMEOUT =
            Duration.ofMinutes(2);
    private static final String EVENT_ID_HEADER = "eventId";

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventProcessingService processingService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        int recovered =
                processingService.recoverStaleProcessingEvents(
                        PROCESSING_TIMEOUT
                );

        if (recovered > 0) {
            log.warn(
                    "Recovered {} stale PROCESSING outbox event(s)",
                    recovered
            );
        }

        List<OutboxEvent> candidates =
                outboxEventRepository.findEligibleForPublishing(
                        OutboxStatus.PENDING,
                        OutboxStatus.RETRY,
                        LocalDateTime.now(ZoneOffset.UTC),
                        PageRequest.of(0, BATCH_SIZE)
                );

        for (OutboxEvent candidate : candidates) {

            Optional<ClaimedOutboxEvent> claimed =
                    processingService.claim(
                            candidate.getEventId()
                    );

            if (claimed.isEmpty()) {

                log.debug(
                        "Outbox event was no longer eligible eventId={}",
                        candidate.getEventId()
                );

                continue;
            }

            publish(claimed.get());
        }
    }

    private void publish(ClaimedOutboxEvent event) {

        try {

            ProducerRecord<String, String> record =
                    new ProducerRecord<>(
                            event.topic(),
                            event.eventKey(),
                            event.payload()
                    );

            record.headers().add(
                    EVENT_ID_HEADER,
                    event.eventId()
                            .toString()
                            .getBytes(StandardCharsets.UTF_8)
            );

            kafkaTemplate.send(record)
                    .get(
                            KAFKA_TIMEOUT_SECONDS,
                            TimeUnit.SECONDS
                    );

            processingService.markPublished(
                    event.eventId()
            );

            log.info(
                    "Published outbox event eventId={} topic={} key={} attempt={}",
                    event.eventId(),
                    event.topic(),
                    event.eventKey(),
                    event.attemptCount()
            );

        } catch (Exception ex) {

            processingService.recordFailure(
                    event.eventId(),
                    errorMessage(ex)
            );

            log.error(
                    "Failed to publish outbox event eventId={} topic={} key={} attempt={}",
                    event.eventId(),
                    event.topic(),
                    event.eventKey(),
                    event.attemptCount(),
                    ex
            );
        }
    }

    private String errorMessage(Exception ex) {

        if (ex.getMessage() != null) {
            return ex.getMessage();
        }

        return ex.getClass().getSimpleName();
    }
}