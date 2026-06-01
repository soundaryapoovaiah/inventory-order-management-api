package microservices.postgresql.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import microservices.postgresql.entity.OutboxEvent;
import microservices.postgresql.repository.OutboxEventRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents =
                outboxEventRepository.findTop20ByStatusOrderByCreatedAtAsc("PENDING");

        for (OutboxEvent event : pendingEvents) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getEventKey(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);

                event.markPublished();

                log.info("Published outbox event eventId={} topic={} key={}",
                        event.getEventId(), event.getTopic(), event.getEventKey());

            } catch (Exception ex) {
                event.markFailed(ex.getMessage());

                log.error("Failed to publish outbox event eventId={} topic={} key={}",
                        event.getEventId(), event.getTopic(), event.getEventKey(), ex);
            }
        }
    }
}