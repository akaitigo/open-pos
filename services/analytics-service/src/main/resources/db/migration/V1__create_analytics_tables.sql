-- Analytics tables for daily_sales, product_sales aggregation
-- All amounts are in sen (10000 = 100 JPY)

CREATE SCHEMA IF NOT EXISTS analytics_schema;

CREATE TABLE analytics_schema.daily_sales (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    store_id UUID NOT NULL,
    date DATE NOT NULL,
    gross_amount BIGINT NOT NULL DEFAULT 0,
    net_amount BIGINT NOT NULL DEFAULT 0,
    tax_amount BIGINT NOT NULL DEFAULT 0,
    discount_amount BIGINT NOT NULL DEFAULT 0,
    transaction_count INTEGER NOT NULL DEFAULT 0,
    cash_amount BIGINT NOT NULL DEFAULT 0,
    card_amount BIGINT NOT NULL DEFAULT 0,
    qr_amount BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (organization_id, store_id, date)
);

CREATE INDEX idx_daily_sales_org_store_date ON analytics_schema.daily_sales (organization_id, store_id, date);

CREATE TABLE analytics_schema.product_sales (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    store_id UUID NOT NULL,
    date DATE NOT NULL,
    product_id UUID NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    quantity_sold INTEGER NOT NULL DEFAULT 0,
    total_amount BIGINT NOT NULL DEFAULT 0,
    cost_amount BIGINT NOT NULL DEFAULT 0,
    transaction_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (organization_id, store_id, date, product_id)
);

CREATE INDEX idx_product_sales_org_store_date ON analytics_schema.product_sales (organization_id, store_id, date);
CREATE INDEX idx_product_sales_product ON analytics_schema.product_sales (product_id);
