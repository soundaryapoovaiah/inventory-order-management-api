package microservices.postgresql.enums;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    RETRY,
    PUBLISHED,
    DEAD_LETTER
}