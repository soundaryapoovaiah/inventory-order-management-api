package microservices.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import microservices.notification.service.ProcessedEventService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class OrderCreatedNotificationConsumer {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProcessedEventService processedEventService;
    private static final Logger log =
            LoggerFactory.getLogger(OrderCreatedNotificationConsumer.class);

    public OrderCreatedNotificationConsumer(
            ProcessedEventService processedEventService
    ) {
        this.processedEventService = processedEventService;
    }

    @KafkaListener(
            topics = "order.created",
            groupId = "notification-service"
    )
    public void consumeOrderCreatedEvent(
            String payload,
            @Header(name = "eventId", required = false) byte[] eventIdHeader
    ) {

        try {

            String eventIdValue =
                    eventIdHeader == null
                            ? null
                            : new String(
                            eventIdHeader,
                            StandardCharsets.UTF_8
                    );

            JsonNode event = objectMapper.readTree(payload);

            Long orderId = event.get("orderId").asLong();
            Long customerId = event.get("customerId").asLong();
            String customerName = event.get("customerName").asText();
            String orderStatus = event.get("orderStatus").asText();
            String totalAmount = event.get("totalAmount").asText();

            /*
             * Older Kafka records may not contain eventId because
             * the header was introduced later.
             */
            if (eventIdValue == null) {

                log.info(
                        "Order notification processed eventId={} orderId={} " +
                                "customerId={} customerName={} orderStatus={} totalAmount={}",
                        eventId,
                        orderId,
                        customerId,
                        customerName,
                        orderStatus,
                        totalAmount
                );

                generateNotification(
                        null,
                        orderId,
                        customerId,
                        customerName,
                        orderStatus,
                        totalAmount
                );

                return;
            }

            UUID eventId = UUID.fromString(eventIdValue);

            boolean processed = processedEventService.processIfNew(
                    eventId,
                    "ORDER_CREATED",
                    orderId.toString(),
                    () -> generateNotification(
                            eventIdValue,
                            orderId,
                            customerId,
                            customerName,
                            orderStatus,
                            totalAmount
                    )
            );

            if (!processed) {
                log.warn(
                        "Duplicate order event skipped eventId={} orderId={}",
                        eventId,
                        orderId
                );
            }

        } catch (Exception ex) {
            throw new RuntimeException(
                    "Failed to process order.created event",
                    ex
            );
        }
    }

    private void generateNotification(
            String eventId,
            Long orderId,
            Long customerId,
            String customerName,
            String orderStatus,
            String totalAmount
    ) {

        System.out.println("==============================================");
        System.out.println("NOTIFICATION SERVICE RECEIVED ORDER EVENT");
        System.out.println("Event ID      : " + eventId);
        System.out.println("Order ID      : " + orderId);
        System.out.println("Customer ID   : " + customerId);
        System.out.println("Customer Name : " + customerName);
        System.out.println("Order Status  : " + orderStatus);
        System.out.println("Total Amount  : " + totalAmount);
        System.out.println(
                "Notification  : Order confirmation notification generated"
        );
        System.out.println("==============================================");
    }
}