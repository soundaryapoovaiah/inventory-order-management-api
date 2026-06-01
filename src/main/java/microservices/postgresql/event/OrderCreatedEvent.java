package microservices.postgresql.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderCreatedEvent(
        Long orderId,
        Long customerId,
        String customerName,
        String orderStatus,
        BigDecimal totalAmount,
        Instant eventCreatedAt,
        List<Item> items
) {
    public record Item(
            Long productId,
            String productName,
            Integer quantity,
            BigDecimal unitPrice
    ) {
    }
}