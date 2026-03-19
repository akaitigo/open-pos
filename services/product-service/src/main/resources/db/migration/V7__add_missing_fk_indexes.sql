-- Add missing indexes on foreign key columns for query performance
-- Identified by Issue #373: DB schema integrity verification

-- products.tax_rate_id: FK to tax_rates but no dedicated index
CREATE INDEX IF NOT EXISTS idx_products_tax_rate ON product_schema.products(tax_rate_id);

-- coupons.discount_id: FK to discounts but no dedicated index
CREATE INDEX IF NOT EXISTS idx_coupons_discount ON product_schema.coupons(discount_id);
