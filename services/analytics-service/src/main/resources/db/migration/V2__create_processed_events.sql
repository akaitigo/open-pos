-- Processed events table for idempotent event handling
CREATE TABLE analytics_schema.processed_events (
    event_id        UUID            PRIMARY KEY,
    event_type      VARCHAR(50)     NOT NULL,
    processed_at    TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_processed_events_type ON analytics_schema.processed_events(event_type);
