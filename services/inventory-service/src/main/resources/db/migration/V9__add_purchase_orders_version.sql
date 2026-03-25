-- Add optimistic lock version column to purchase_orders
ALTER TABLE inventory_schema.purchase_orders ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
