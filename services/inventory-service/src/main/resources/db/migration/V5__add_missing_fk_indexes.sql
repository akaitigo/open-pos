-- Add missing indexes on foreign key columns for query performance
-- Identified by Issue #373: DB schema integrity verification

-- purchase_order_items.product_id: FK reference, needed for product-level purchase history queries
CREATE INDEX IF NOT EXISTS idx_poi_product ON inventory_schema.purchase_order_items(product_id);
