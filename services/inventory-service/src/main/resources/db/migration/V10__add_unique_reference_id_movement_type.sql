-- Add partial unique index for idempotency: prevent duplicate (reference_id, movement_type) entries
-- Only applies when reference_id IS NOT NULL (manual adjustments without reference_id are allowed to be duplicated)
CREATE UNIQUE INDEX IF NOT EXISTS idx_stock_movements_reference_idempotency
    ON inventory_schema.stock_movements (reference_id, movement_type)
    WHERE reference_id IS NOT NULL;
