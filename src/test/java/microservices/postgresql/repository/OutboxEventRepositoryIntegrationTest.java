package microservices.postgresql.repository;

import microservices.postgresql.entity.OutboxEvent;
import microservices.postgresql.enums.OutboxStatus;
import microservices.postgresql.messaging.OutboxEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@Transactional
class OutboxEventRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("inventory_db")
                    .withUsername("inventory_user")
                    .withPassword("inventory_pass");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );

        registry.add(
                "spring.datasource.driver-class-name",
                postgres::getDriverClassName
        );
    }

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    /*
     * Prevent the @Scheduled publisher from consuming
     * the test records while we verify repository selection.
     */
    @MockitoBean
    private OutboxEventPublisher outboxEventPublisher;

    @Test
    void shouldReturnOnlyEventsEligibleForPublishing() {

        LocalDateTime now = LocalDateTime.now();

        OutboxEvent pendingEvent =
                createEvent(
                        "order-pending",
                        OutboxStatus.PENDING,
                        null
                );

        OutboxEvent retryDueEvent =
                createEvent(
                        "order-retry-due",
                        OutboxStatus.RETRY,
                        now.minusSeconds(10)
                );

        OutboxEvent retryFutureEvent =
                createEvent(
                        "order-retry-future",
                        OutboxStatus.RETRY,
                        now.plusMinutes(10)
                );

        OutboxEvent publishedEvent =
                createEvent(
                        "order-published",
                        OutboxStatus.PUBLISHED,
                        null
                );

        OutboxEvent deadLetterEvent =
                createEvent(
                        "order-dead-letter",
                        OutboxStatus.DEAD_LETTER,
                        null
                );

        outboxEventRepository.saveAllAndFlush(
                List.of(
                        pendingEvent,
                        retryDueEvent,
                        retryFutureEvent,
                        publishedEvent,
                        deadLetterEvent
                )
        );

        List<OutboxEvent> eligibleEvents =
                outboxEventRepository.findEligibleForPublishing(
                        OutboxStatus.PENDING,
                        OutboxStatus.RETRY,
                        now,
                        PageRequest.of(0, 20)
                );

        assertThat(eligibleEvents)
                .extracting(OutboxEvent::getEventKey)
                .containsExactlyInAnyOrder(
                        "order-pending",
                        "order-retry-due"
                );
    }

    private OutboxEvent createEvent(
            String eventKey,
            OutboxStatus status,
            LocalDateTime nextAttemptAt
    ) {

        return OutboxEvent.builder()
                .aggregateType("ORDER")
                .aggregateId(eventKey)
                .eventType("ORDER_CREATED")
                .topic("order.created")
                .eventKey(eventKey)
                .payload("""
                        {"orderId":"%s"}
                        """.formatted(eventKey))
                .status(status)
                .nextAttemptAt(nextAttemptAt)
                .build();
    }
}