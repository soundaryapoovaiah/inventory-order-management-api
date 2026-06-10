package microservices.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class OrderCreatedNotificationConsumer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "order.created", groupId = "notification-service")
    public void consumeOrderCreatedEvent(String payload) {
        try {
            JsonNode event = objectMapper.readTree(payload);

            Long orderId = event.get("orderId").asLong();
            Long customerId = event.get("customerId").asLong();
            String customerName = event.get("customerName").asText();
            String orderStatus = event.get("orderStatus").asText();
            String totalAmount = event.get("totalAmount").asText();

            System.out.println("==============================================");
            System.out.println("NOTIFICATION SERVICE RECEIVED ORDER EVENT");
            System.out.println("Order ID      : " + orderId);
            System.out.println("Customer ID   : " + customerId);
            System.out.println("Customer Name : " + customerName);
            System.out.println("Order Status  : " + orderStatus);
            System.out.println("Total Amount  : " + totalAmount);
            System.out.println("Notification  : Order confirmation notification generated");
            System.out.println("==============================================");

        } catch (Exception ex) {
            throw new RuntimeException("Failed to process order.created event", ex);
        }
    }
}