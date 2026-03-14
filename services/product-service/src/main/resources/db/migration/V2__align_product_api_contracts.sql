ALTER TABLE product_schema.products
    ADD COLUMN description TEXT;

ALTER TABLE product_schema.tax_rates
    ADD COLUMN is_default BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX idx_tax_rates_org_default
    ON product_schema.tax_rates(organization_id, is_default);

ALTER TABLE product_schema.coupons
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT true;

CREATE INDEX idx_coupons_org_active
    ON product_schema.coupons(organization_id, is_active);
