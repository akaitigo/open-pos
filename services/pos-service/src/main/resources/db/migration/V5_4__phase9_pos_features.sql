-- Phase 9: Transaction memo, training mode, customer_id (#137, #139, #140)

ALTER TABLE pos_schema.transactions ADD COLUMN IF NOT EXISTS memo TEXT;
ALTER TABLE pos_schema.transactions ADD COLUMN IF NOT EXISTS is_training BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE pos_schema.transactions ADD COLUMN IF NOT EXISTS customer_id UUID;

-- Index for training mode exclusion from reports
CREATE INDEX IF NOT EXISTS idx_tx_training ON pos_schema.transactions(organization_id, is_training) WHERE is_training = false;
