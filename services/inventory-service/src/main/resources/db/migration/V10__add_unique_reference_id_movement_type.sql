-- Add partial unique index for idempotency: prevent duplicate adjustments for the same
-- reference + movement type + store + product combination.
-- Only applies when reference_id IS NOT NULL (manual adjustments without reference_id are allowed).
CREATE UNIQUE INDEX IF NOT EXISTS idx_stock_movements_reference_idempotency
    ON inventory_schema.stock_movements (reference_id, movement_type, store_id, product_id)
    WHERE reference_id IS NOT NULL;
