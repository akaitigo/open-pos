-- Outbox events table for reliable event publishing.
-- When RabbitMQ is unavailable, events are saved here and retried by OutboxProcessor.

CREATE TABLE pos_schema.outbox_events (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type      VARCHAR(100)    NOT NULL,
    payload         TEXT            NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    sent_at         TIMESTAMPTZ,
    retry_count     INT             NOT NULL DEFAULT 0,
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

CREATE INDEX idx_outbox_status_created ON pos_schema.outbox_events(status, created_at)
    WHERE status = 'PENDING';
