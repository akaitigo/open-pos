-- Analytics schema tables

CREATE TABLE analytics_schema.daily_sales (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID            NOT NULL,
    store_id            UUID            NOT NULL,
    sale_date           DATE            NOT NULL,
    total_sales         BIGINT          NOT NULL DEFAULT 0,
    net_sales           BIGINT          NOT NULL DEFAULT 0,
    tax_amount          BIGINT          NOT NULL DEFAULT 0,
    discount_amount     BIGINT          NOT NULL DEFAULT 0,
    transaction_count   INT             NOT NULL DEFAULT 0,
    voided_count        INT             NOT NULL DEFAULT 0,
    returned_count      INT             NOT NULL DEFAULT 0,
    cash_amount         BIGINT          NOT NULL DEFAULT 0,
    card_amount         BIGINT          NOT NULL DEFAULT 0,
    qr_amount           BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_daily_sales_org_store_date UNIQUE (organization_id, store_id, sale_date)
);

CREATE INDEX idx_daily_sales_org ON analytics_schema.daily_sales(organization_id);
CREATE INDEX idx_daily_sales_store_date ON analytics_schema.daily_sales(organization_id, store_id, sale_date);

CREATE TABLE analytics_schema.product_sales (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID            NOT NULL,
    store_id            UUID            NOT NULL,
    product_id          UUID            NOT NULL,
    product_name        VARCHAR(255)    NOT NULL,
    sale_date           DATE            NOT NULL,
    quantity_sold       INT             NOT NULL DEFAULT 0,
    total_amount        BIGINT          NOT NULL DEFAULT 0,
    average_price       BIGINT          NOT NULL DEFAULT 0,
    transaction_count   INT             NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_product_sales_org_store_product_date UNIQUE (organization_id, store_id, product_id, sale_date)
);

CREATE INDEX idx_product_sales_org ON analytics_schema.product_sales(organization_id);
CREATE INDEX idx_product_sales_store_date ON analytics_schema.product_sales(organization_id, store_id, sale_date);
CREATE INDEX idx_product_sales_product ON analytics_schema.product_sales(organization_id, store_id, product_id);

CREATE TABLE analytics_schema.hourly_sales (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID            NOT NULL,
    store_id            UUID            NOT NULL,
    sale_date           DATE            NOT NULL,
    hour                INT             NOT NULL,
    total_sales         BIGINT          NOT NULL DEFAULT 0,
    transaction_count   INT             NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_hourly_sales_org_store_date_hour UNIQUE (organization_id, store_id, sale_date, hour),
    CONSTRAINT chk_hour CHECK (hour >= 0 AND hour <= 23)
);

CREATE INDEX idx_hourly_sales_org ON analytics_schema.hourly_sales(organization_id);
CREATE INDEX idx_hourly_sales_store_date ON analytics_schema.hourly_sales(organization_id, store_id, sale_date);
