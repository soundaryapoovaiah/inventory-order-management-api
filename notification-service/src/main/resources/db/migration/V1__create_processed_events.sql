CREATE SCHEMA IF NOT EXISTS notification;

CREATE TABLE notification.processed_events (
                                               event_id UUID PRIMARY KEY,
                                               event_type VARCHAR(100) NOT NULL,
                                               aggregate_id VARCHAR(100),
                                               processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_processed_events_processed_at
    ON notification.processed_events(processed_at);