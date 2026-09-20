package microservices.notification.service;

import microservices.notification.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(
        properties = {
                "spring.kafka.listener.auto-startup=false"
        }
)
class ProcessedEventServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("inventory_db")
                    .withUsername("inventory_user")
                    .withPassword("inventory_pass");

    @DynamicPropertySource
    static void configureDatabase(
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
    }
    //noinspection SpringJavaInjectionPointsAutowiringInspection
    @Autowired
    private ProcessedEventService processedEventService;

    //noinspection SpringJavaInjectionPointsAutowiringInspection
    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void cleanDatabase() {
        processedEventRepository.deleteAll();
    }

    @Test
    void shouldProcessSameEventOnlyOnce() {

        UUID eventId = UUID.randomUUID();

        AtomicInteger processingCount =
                new AtomicInteger(0);

        boolean firstDelivery =
                processedEventService.processIfNew(
                        eventId,
                        "ORDER_CREATED",
                        "16",
                        processingCount::incrementAndGet
                );

        boolean duplicateDelivery =
                processedEventService.processIfNew(
                        eventId,
                        "ORDER_CREATED",
                        "16",
                        processingCount::incrementAndGet
                );

        assertThat(firstDelivery)
                .isTrue();

        assertThat(duplicateDelivery)
                .isFalse();

        assertThat(processingCount.get())
                .isEqualTo(1);

        assertThat(processedEventRepository.count())
                .isEqualTo(1);

        assertThat(
                processedEventRepository.existsById(eventId)
        ).isTrue();
    }

    @Test
    void shouldAllowRetryWhenProcessingFails() {

        UUID eventId = UUID.randomUUID();

        assertThatThrownBy(() ->
                processedEventService.processIfNew(
                        eventId,
                        "ORDER_CREATED",
                        "17",
                        () -> {
                            throw new IllegalStateException(
                                    "Simulated notification failure"
                            );
                        }
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Simulated notification failure");

        /*
         * Because processing failed, the transaction should
         * also roll back the processed_events INSERT.
         */
        assertThat(
                processedEventRepository.existsById(eventId)
        ).isFalse();

        AtomicInteger processingCount =
                new AtomicInteger(0);

        /*
         * Simulate Kafka delivering the event again.
         */
        boolean retryProcessed =
                processedEventService.processIfNew(
                        eventId,
                        "ORDER_CREATED",
                        "17",
                        processingCount::incrementAndGet
                );

        assertThat(retryProcessed)
                .isTrue();

        assertThat(processingCount.get())
                .isEqualTo(1);

        assertThat(
                processedEventRepository.existsById(eventId)
        ).isTrue();

        assertThat(processedEventRepository.count())
                .isEqualTo(1);
    }
    @Test
    void shouldProcessOnlyOnceWhenSameEventArrivesConcurrently() throws Exception {

        UUID eventId = UUID.randomUUID();

        AtomicInteger processingCount = new AtomicInteger(0);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch readyLatch =
                new CountDownLatch(2);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        try {

            var task = (java.util.concurrent.Callable<Boolean>) () -> {

                readyLatch.countDown();

                startLatch.await();

                return processedEventService.processIfNew(
                        eventId,
                        "ORDER_CREATED",
                        "18",
                        () -> {

                            processingCount.incrementAndGet();

                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException(ex);
                            }
                        }
                );
            };

            Future<Boolean> firstFuture =
                    executor.submit(task);

            Future<Boolean> secondFuture =
                    executor.submit(task);

            assertThat(
                    readyLatch.await(
                            5,
                            TimeUnit.SECONDS
                    )
            ).isTrue();

            // Release both threads at almost the same time
            startLatch.countDown();

            boolean firstResult =
                    firstFuture.get(
                            10,
                            TimeUnit.SECONDS
                    );

            boolean secondResult =
                    secondFuture.get(
                            10,
                            TimeUnit.SECONDS
                    );

            List<Boolean> results =
                    List.of(
                            firstResult,
                            secondResult
                    );

            assertThat(results)
                    .containsExactlyInAnyOrder(
                            true,
                            false
                    );

            assertThat(processingCount.get())
                    .isEqualTo(1);

            assertThat(
                    processedEventRepository.count()
            ).isEqualTo(1);

            assertThat(
                    processedEventRepository.existsById(eventId)
            ).isTrue();

        } finally {

            executor.shutdownNow();
        }
    }
}