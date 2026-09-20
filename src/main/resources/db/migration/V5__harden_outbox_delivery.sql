ALTER TABLE outbox_events
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN next_attempt_at TIMESTAMP NULL,
    ADD COLUMN last_attempt_at TIMESTAMP NULL,
    ADD COLUMN processing_started_at TIMESTAMP NULL;


-- Events marked FAILED by the previous implementation should become
-- eligible for retry when the new retry mechanism is deployed.
UPDATE outbox_events
SET status = 'RETRY',
    next_attempt_at = CURRENT_TIMESTAMP
WHERE status = 'FAILED';


-- Restrict outbox events to supported lifecycle states.
ALTER TABLE outbox_events
    ADD CONSTRAINT chk_outbox_events_status
        CHECK (
            status IN (
                       'PENDING',
                       'PROCESSING',
                       'RETRY',
                       'PUBLISHED',
                       'DEAD_LETTER'
                )
            );


-- Retry counts should never be negative.
ALTER TABLE outbox_events
    ADD CONSTRAINT chk_outbox_events_attempt_count
        CHECK (attempt_count >= 0);


-- Replace the original status/created_at index with an index
-- better suited for pending and retryable delivery work.
DROP INDEX IF EXISTS idx_outbox_events_status_created_at;

CREATE INDEX idx_outbox_events_delivery_queue
    ON outbox_events(status, next_attempt_at, created_at);


-- Supports recovery of events that were claimed for processing
-- but abandoned because an application instance crashed.
CREATE INDEX idx_outbox_events_processing_started_at
    ON outbox_events(processing_started_at)
    WHERE status = 'PROCESSING';