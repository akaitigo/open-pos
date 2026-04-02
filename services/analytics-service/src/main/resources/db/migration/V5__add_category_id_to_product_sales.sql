-- Add category_id column to product_sales for accurate category-level grouping (#1141)
-- Previously grouped by category_name only, which could merge distinct categories with the same name
ALTER TABLE analytics_schema.product_sales
    ADD COLUMN category_id UUID;

-- Update index to include category_id for efficient grouping queries
DROP INDEX IF EXISTS analytics_schema.idx_product_sales_category;
CREATE INDEX idx_product_sales_category ON analytics_schema.product_sales (organization_id, store_id, date, category_id, category_name);
