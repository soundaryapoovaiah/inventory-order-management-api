package microservices.postgresql.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import microservices.postgresql.dto.OrderRequest;
import microservices.postgresql.dto.OrderResponse;
import microservices.postgresql.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import microservices.postgresql.exception.DuplicateOrderRequestException;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody OrderRequest request
    ) {
        OrderResponse response = orderService.placeOrder(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomerId(@PathVariable Long customerId) {
        return ResponseEntity.ok(orderService.getOrdersByCustomerId(customerId));
    }

    @ExceptionHandler(DuplicateOrderRequestException.class)
    public ResponseEntity<OrderResponse> handleDuplicateOrderRequest(DuplicateOrderRequestException ex) {
        OrderResponse existingOrder = orderService.getOrderById(ex.getExistingOrderId());
        return ResponseEntity.ok(existingOrder);
    }
}