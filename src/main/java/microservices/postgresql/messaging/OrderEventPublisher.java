package microservices.postgresql.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import microservices.postgresql.entity.CustomerOrder;
import microservices.postgresql.entity.OrderItem;
import microservices.postgresql.event.OrderCreatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topics.order-created}")
    private String orderCreatedTopic;

    public void publishOrderCreated(CustomerOrder order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getOrderId(),
                order.getCustomer().getCustomerId(),
                order.getCustomer().getName(),
                order.getOrderStatus(),
                order.getTotalAmount(),
                Instant.now(),
                mapItems(order.getOrderItems())
        );

        try {
            String payload = objectMapper.writeValueAsString(event);
            String key = String.valueOf(order.getOrderId());

            kafkaTemplate.send(orderCreatedTopic, key, payload)
                    .get(5, TimeUnit.SECONDS);

            log.info("Published Kafka event topic={} key={} payload={}",
                    orderCreatedTopic, key, payload);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize order.created event", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish order.created Kafka event", e);
        }
    }

    private List<OrderCreatedEvent.Item> mapItems(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(item -> new OrderCreatedEvent.Item(
                        item.getProduct().getProductId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice()
                ))
                .toList();
    }
}