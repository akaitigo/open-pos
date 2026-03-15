-- Add cost_price column to products for gross profit calculation (#184)
-- cost_price is in sen (銭) unit: 10000 = 100 JPY

ALTER TABLE product_schema.products ADD COLUMN cost_price BIGINT NOT NULL DEFAULT 0;
COMMENT ON COLUMN product_schema.products.cost_price IS '原価（銭単位: 10000 = 100円）';
