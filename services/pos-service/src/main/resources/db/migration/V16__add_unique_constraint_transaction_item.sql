-- Prevent duplicate product rows in transaction_items (#610)
-- Same product in same transaction should be merged (quantity updated), not duplicated.
CREATE UNIQUE INDEX IF NOT EXISTS uq_tx_item_product
    ON pos_schema.transaction_items (transaction_id, product_id);
