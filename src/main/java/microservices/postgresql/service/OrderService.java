package microservices.postgresql.service;

import lombok.RequiredArgsConstructor;
import microservices.postgresql.dto.OrderItemRequest;
import microservices.postgresql.dto.OrderItemResponse;
import microservices.postgresql.dto.OrderRequest;
import microservices.postgresql.dto.OrderResponse;
import microservices.postgresql.entity.Customer;
import microservices.postgresql.entity.CustomerOrder;
import microservices.postgresql.entity.OrderItem;
import microservices.postgresql.entity.Product;
import microservices.postgresql.repository.CustomerOrderRepository;
import microservices.postgresql.repository.CustomerRepository;
import microservices.postgresql.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import microservices.postgresql.exception.DuplicateOrderRequestException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    @Transactional
    public OrderResponse placeOrder(OrderRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            customerOrderRepository.findByIdempotencyKey(idempotencyKey)
                    .ifPresent(existingOrder -> {
                        throw new DuplicateOrderRequestException(existingOrder.getOrderId());
                    });
        }
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException(
                        "Customer not found with id: " + request.getCustomerId()
                ));
        CustomerOrder order = CustomerOrder.builder()
                .customer(customer)
                .idempotencyKey(idempotencyKey)
                .orderStatus("PLACED")
                .totalAmount(BigDecimal.ZERO)
                .build();

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        List<OrderItemRequest> sortedItems = request.getItems()
                .stream()
                .sorted(Comparator.comparing(OrderItemRequest::getProductId))
                .toList();

        for (OrderItemRequest itemRequest : sortedItems) {
            Product product = productRepository.findByIdForUpdate(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + itemRequest.getProductId()));

            if (product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new RuntimeException(
                        "Insufficient stock for product: " + product.getName()
                                + ". Available: " + product.getStockQuantity()
                                + ", Requested: " + itemRequest.getQuantity()
                );
            }

            product.setStockQuantity(product.getStockQuantity() - itemRequest.getQuantity());

            BigDecimal lineTotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(lineTotal);
        }


        order.setOrderItems(orderItems);
        order.setTotalAmount(totalAmount);

        CustomerOrder savedOrder = customerOrderRepository.save(order);

        return mapToResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerId(Long customerId) {
        return customerOrderRepository.findByCustomerCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private OrderResponse mapToResponse(CustomerOrder order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems()
                .stream()
                .map(this::mapItemToResponse)
                .toList();

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .customerId(order.getCustomer().getCustomerId())
                .customerName(order.getCustomer().getName())
                .orderStatus(order.getOrderStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();
    }

    private OrderItemResponse mapItemToResponse(OrderItem orderItem) {
        BigDecimal lineTotal = orderItem.getUnitPrice()
                .multiply(BigDecimal.valueOf(orderItem.getQuantity()));

        return OrderItemResponse.builder()
                .orderItemId(orderItem.getOrderItemId())
                .productId(orderItem.getProduct().getProductId())
                .productName(orderItem.getProduct().getName())
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .lineTotal(lineTotal)
                .build();
    }
}