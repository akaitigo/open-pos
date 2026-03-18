-- Add missing indexes on foreign key columns for query performance
-- Identified by Issue #373: DB schema integrity verification

-- transaction_items.product_id: referenced FK, frequently queried for product sales lookups
CREATE INDEX IF NOT EXISTS idx_tx_items_product ON pos_schema.transaction_items(product_id);

-- transaction_discounts.discount_id: optional FK reference, used in discount analytics
CREATE INDEX IF NOT EXISTS idx_tx_discounts_discount ON pos_schema.transaction_discounts(discount_id);

-- transaction_discounts.transaction_item_id: optional FK reference for item-level discounts
CREATE INDEX IF NOT EXISTS idx_tx_discounts_item ON pos_schema.transaction_discounts(transaction_item_id);
