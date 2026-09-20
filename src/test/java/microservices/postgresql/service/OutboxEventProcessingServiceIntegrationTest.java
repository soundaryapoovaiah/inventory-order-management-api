package microservices.postgresql.service;

import microservices.postgresql.entity.OutboxEvent;
import microservices.postgresql.enums.OutboxStatus;
import microservices.postgresql.messaging.ClaimedOutboxEvent;
import microservices.postgresql.messaging.OutboxEventPublisher;
import microservices.postgresql.repository.OutboxEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.time.LocalDateTime;
import java.util.Optional;
import java.time.Duration;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties = "spring.docker.compose.enabled=false")
class OutboxEventProcessingServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("inventory_db")
                    .withUsername("inventory_user")
                    .withPassword("inventory_pass");

    @DynamicPropertySource
    static void configurePostgres(
            DynamicPropertyRegistry registry
    ) {

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

    @Autowired
    private OutboxEventProcessingService processingService;

    @MockitoBean
    private OutboxEventPublisher outboxEventPublisher;

    @AfterEach
    void cleanup() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void shouldAllowOnlyOneClaimForSameEvent() {

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType("ORDER")
                .aggregateId("order-100")
                .eventType("ORDER_CREATED")
                .topic("order.created")
                .eventKey("order-100")
                .payload("""
                        {"orderId":"order-100"}
                        """)
                .status(OutboxStatus.PENDING)
                .build();

        OutboxEvent saved =
                outboxEventRepository.saveAndFlush(event);

        Optional<ClaimedOutboxEvent> firstClaim =
                processingService.claim(saved.getEventId());

        Optional<ClaimedOutboxEvent> secondClaim =
                processingService.claim(saved.getEventId());

        assertThat(firstClaim).isPresent();

        assertThat(secondClaim).isEmpty();

        OutboxEvent persisted =
                outboxEventRepository
                        .findById(saved.getEventId())
                        .orElseThrow();

        assertThat(persisted.getStatus())
                .isEqualTo(OutboxStatus.PROCESSING);

        assertThat(persisted.getAttemptCount())
                .isEqualTo(1);

        assertThat(persisted.getLastAttemptAt())
                .isNotNull();

        assertThat(persisted.getProcessingStartedAt())
                .isNotNull();
    }

    @Test
    void shouldMarkClaimedEventAsPublished() {

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType("ORDER")
                .aggregateId("order-200")
                .eventType("ORDER_CREATED")
                .topic("order.created")
                .eventKey("order-200")
                .payload("""
                    {"orderId":"order-200"}
                    """)
                .status(OutboxStatus.PENDING)
                .build();

        OutboxEvent saved =
                outboxEventRepository.saveAndFlush(event);

        processingService.claim(saved.getEventId());

        processingService.markPublished(
                saved.getEventId()
        );

        OutboxEvent persisted =
                outboxEventRepository
                        .findById(saved.getEventId())
                        .orElseThrow();

        assertThat(persisted.getStatus())
                .isEqualTo(OutboxStatus.PUBLISHED);

        assertThat(persisted.getPublishedAt())
                .isNotNull();

        assertThat(persisted.getProcessingStartedAt())
                .isNull();

        assertThat(persisted.getErrorMessage())
                .isNull();
    }

    @Test
    void shouldScheduleRetryAfterPublishFailure() {

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType("ORDER")
                .aggregateId("order-300")
                .eventType("ORDER_CREATED")
                .topic("order.created")
                .eventKey("order-300")
                .payload("""
                    {"orderId":"order-300"}
                    """)
                .status(OutboxStatus.PENDING)
                .build();

        OutboxEvent saved =
                outboxEventRepository.saveAndFlush(event);

        processingService.claim(saved.getEventId());

        LocalDateTime beforeFailure =
                LocalDateTime.now(ZoneOffset.UTC);

        processingService.recordFailure(
                saved.getEventId(),
                "Kafka unavailable"
        );

        OutboxEvent persisted =
                outboxEventRepository
                        .findById(saved.getEventId())
                        .orElseThrow();

        assertThat(persisted.getStatus())
                .isEqualTo(OutboxStatus.RETRY);

        assertThat(persisted.getAttemptCount())
                .isEqualTo(1);

        assertThat(persisted.getErrorMessage())
                .isEqualTo("Kafka unavailable");

        assertThat(persisted.getNextAttemptAt())
                .isNotNull();

        assertThat(persisted.getNextAttemptAt())
                .isAfterOrEqualTo(
                        beforeFailure.plusSeconds(5)
                );

        assertThat(persisted.getProcessingStartedAt())
                .isNull();
    }

    @Test
    void shouldMoveEventToDeadLetterAfterMaximumAttempts() {

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType("ORDER")
                .aggregateId("order-400")
                .eventType("ORDER_CREATED")
                .topic("order.created")
                .eventKey("order-400")
                .payload("""
                    {"orderId":"order-400"}
                    """)
                .status(OutboxStatus.PENDING)
                .attemptCount(4)
                .build();

        OutboxEvent saved =
                outboxEventRepository.saveAndFlush(event);

        // Claim increments attemptCount: 4 -> 5
        processingService.claim(saved.getEventId());

        processingService.recordFailure(
                saved.getEventId(),
                "Kafka still unavailable"
        );

        OutboxEvent persisted =
                outboxEventRepository
                        .findById(saved.getEventId())
                        .orElseThrow();

        assertThat(persisted.getStatus())
                .isEqualTo(OutboxStatus.DEAD_LETTER);

        assertThat(persisted.getAttemptCount())
                .isEqualTo(5);

        assertThat(persisted.getErrorMessage())
                .isEqualTo("Kafka still unavailable");

        assertThat(persisted.getNextAttemptAt())
                .isNull();

        assertThat(persisted.getProcessingStartedAt())
                .isNull();
    }
    @Test
    void shouldRecoverStaleProcessingEventForRetry() {

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType("ORDER")
                .aggregateId("order-500")
                .eventType("ORDER_CREATED")
                .topic("order.created")
                .eventKey("order-500")
                .payload("""
                    {"orderId":"order-500"}
                    """)
                .status(OutboxStatus.PROCESSING)
                .attemptCount(1)
                .processingStartedAt(
                        LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5)
                )
                .build();

        OutboxEvent saved =
                outboxEventRepository.saveAndFlush(event);

        int recovered =
                processingService.recoverStaleProcessingEvents(
                        Duration.ofMinutes(2)
                );

        assertThat(recovered)
                .isEqualTo(1);

        OutboxEvent persisted =
                outboxEventRepository
                        .findById(saved.getEventId())
                        .orElseThrow();

        assertThat(persisted.getStatus())
                .isEqualTo(OutboxStatus.RETRY);

        assertThat(persisted.getNextAttemptAt())
                .isNotNull();

        assertThat(persisted.getProcessingStartedAt())
                .isNull();

        assertThat(persisted.getErrorMessage())
                .isEqualTo(
                        "Recovered after processing timeout"
                );

        assertThat(persisted.getAttemptCount())
                .isEqualTo(1);
    }
    @Test
    void shouldNotRecoverRecentlyProcessingEvent() {

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType("ORDER")
                .aggregateId("order-600")
                .eventType("ORDER_CREATED")
                .topic("order.created")
                .eventKey("order-600")
                .payload("""
                    {"orderId":"order-600"}
                    """)
                .status(OutboxStatus.PROCESSING)
                .attemptCount(1)
                .processingStartedAt(
                        LocalDateTime.now(ZoneOffset.UTC).minusSeconds(10)
                )
                .build();

        OutboxEvent saved =
                outboxEventRepository.saveAndFlush(event);

        int recovered =
                processingService.recoverStaleProcessingEvents(
                        Duration.ofMinutes(2)
                );

        assertThat(recovered)
                .isZero();

        OutboxEvent persisted =
                outboxEventRepository
                        .findById(saved.getEventId())
                        .orElseThrow();

        assertThat(persisted.getStatus())
                .isEqualTo(OutboxStatus.PROCESSING);

        assertThat(persisted.getProcessingStartedAt())
                .isNotNull();
    }
}