-- Add organization_id column to outbox_events for tenant isolation.
-- Existing rows (if any) are cleaned up since they are transient retry data.

DELETE FROM pos_schema.outbox_events WHERE status = 'PENDING';

ALTER TABLE pos_schema.outbox_events
    ADD COLUMN organization_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000';

-- Remove the default after backfill
ALTER TABLE pos_schema.outbox_events
    ALTER COLUMN organization_id DROP DEFAULT;

CREATE INDEX idx_outbox_organization_id ON pos_schema.outbox_events(organization_id);
