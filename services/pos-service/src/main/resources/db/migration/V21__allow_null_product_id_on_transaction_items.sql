-- Issue #1138: Allow custom items (no product_id) in transaction_items
ALTER TABLE pos_schema.transaction_items ALTER COLUMN product_id DROP NOT NULL;
