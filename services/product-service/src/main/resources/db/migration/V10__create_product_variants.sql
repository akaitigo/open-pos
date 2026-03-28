-- Extend product_variants table with barcode, display_order columns
-- and add missing FK constraint + additional indexes
-- V5 already created the table; this migration adds fields needed for the variant API

ALTER TABLE product_schema.product_variants
    ADD COLUMN IF NOT EXISTS barcode VARCHAR(100),
    ADD COLUMN IF NOT EXISTS display_order INT NOT NULL DEFAULT 0;

-- Add FK to parent product (cascade delete when product is removed)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_product_variants_product'
          AND table_schema = 'product_schema'
    ) THEN
        ALTER TABLE product_schema.product_variants
            ADD CONSTRAINT fk_product_variants_product
            FOREIGN KEY (product_id) REFERENCES product_schema.products(id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_product_variants_product ON product_schema.product_variants (product_id);
CREATE INDEX IF NOT EXISTS idx_product_variants_sku ON product_schema.product_variants (organization_id, sku);
CREATE INDEX IF NOT EXISTS idx_product_variants_barcode ON product_schema.product_variants (organization_id, barcode);
