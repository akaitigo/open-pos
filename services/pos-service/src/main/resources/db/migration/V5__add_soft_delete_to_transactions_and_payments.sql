-- Add soft delete column to transactions and payments tables for audit log integrity.
-- Physical deletion is prohibited; void/cancel operations set status to VOIDED
-- and soft delete ensures records are never physically removed.

ALTER TABLE pos_schema.transactions
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE pos_schema.payments
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false;

-- Index for efficient filtering of non-deleted records
CREATE INDEX IF NOT EXISTS idx_tx_deleted
    ON pos_schema.transactions(deleted) WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_payments_deleted
    ON pos_schema.payments(deleted) WHERE deleted = false;
