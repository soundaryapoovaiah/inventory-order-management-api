package microservices.postgresql.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import microservices.postgresql.entity.CustomerOrder;
import microservices.postgresql.entity.OrderItem;
import microservices.postgresql.entity.OutboxEvent;
import microservices.postgresql.event.OrderCreatedEvent;
import microservices.postgresql.repository.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topics.order-created}")
    private String orderCreatedTopic;

    public void saveOrderCreatedEvent(CustomerOrder order) {
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

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("ORDER")
                    .aggregateId(String.valueOf(order.getOrderId()))
                    .eventType("ORDER_CREATED")
                    .topic(orderCreatedTopic)
                    .eventKey(String.valueOf(order.getOrderId()))
                    .payload(payload)
                    .status("PENDING")
                    .build();

            outboxEventRepository.save(outboxEvent);

            log.info("Saved outbox event for orderId={} topic={}",
                    order.getOrderId(), orderCreatedTopic);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize order.created outbox event", e);
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