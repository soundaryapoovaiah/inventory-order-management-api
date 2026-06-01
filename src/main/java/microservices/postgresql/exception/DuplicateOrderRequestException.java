package microservices.postgresql.exception;

import lombok.Getter;

@Getter
public class DuplicateOrderRequestException extends RuntimeException {

    private final Long existingOrderId;

    public DuplicateOrderRequestException(Long existingOrderId) {
        super("Duplicate order request. Returning existing order with id: " + existingOrderId);
        this.existingOrderId = existingOrderId;
    }
}