-- Add soft delete column to products
ALTER TABLE product_schema.products ADD COLUMN deleted_at TIMESTAMPTZ;

-- Add optimistic lock version column to products
ALTER TABLE product_schema.products ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Index for soft-deleted product filtering
CREATE INDEX idx_products_deleted ON product_schema.products(organization_id, deleted_at) WHERE deleted_at IS NULL;
