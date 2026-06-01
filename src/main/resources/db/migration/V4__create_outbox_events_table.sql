CREATE TABLE outbox_events (
                               event_id UUID PRIMARY KEY,
                               aggregate_type VARCHAR(100) NOT NULL,
                               aggregate_id VARCHAR(100) NOT NULL,
                               event_type VARCHAR(100) NOT NULL,
                               topic VARCHAR(100) NOT NULL,
                               event_key VARCHAR(100) NOT NULL,
                               payload TEXT NOT NULL,
                               status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               published_at TIMESTAMP NULL,
                               error_message TEXT
);

CREATE INDEX idx_outbox_events_status_created_at
    ON outbox_events(status, created_at);