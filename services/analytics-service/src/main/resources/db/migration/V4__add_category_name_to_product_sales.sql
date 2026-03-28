-- Add category_name column to product_sales for category-level aggregation
ALTER TABLE analytics_schema.product_sales
    ADD COLUMN category_name VARCHAR(200) NOT NULL DEFAULT '';

CREATE INDEX idx_product_sales_category ON analytics_schema.product_sales (organization_id, store_id, date, category_name);
