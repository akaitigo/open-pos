-- Product schema tables
-- All monetary values are BIGINT in sen (銭) unit: 10000 = 100 JPY

-- Categories (hierarchical via parent_id)
CREATE TABLE product_schema.categories (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID            NOT NULL,
    parent_id       UUID            REFERENCES product_schema.categories(id),
    name            VARCHAR(100)    NOT NULL,
    color           VARCHAR(7),
    icon            VARCHAR(50),
    display_order   INT             NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_categories_org ON product_schema.categories(organization_id);
CREATE INDEX idx_categories_parent ON product_schema.categories(parent_id);

-- Tax rates
CREATE TABLE product_schema.tax_rates (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID            NOT NULL,
    name            VARCHAR(50)     NOT NULL,
    rate            DECIMAL(5,4)    NOT NULL,
    tax_type        VARCHAR(20)     NOT NULL DEFAULT 'STANDARD',
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_tax_type CHECK (tax_type IN ('STANDARD', 'REDUCED'))
);

CREATE INDEX idx_tax_rates_org ON product_schema.tax_rates(organization_id);

-- Products
CREATE TABLE product_schema.products (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID            NOT NULL,
    category_id     UUID            REFERENCES product_schema.categories(id),
    tax_rate_id     UUID            REFERENCES product_schema.tax_rates(id),
    name            VARCHAR(255)    NOT NULL,
    barcode         VARCHAR(100),
    sku             VARCHAR(100),
    price           BIGINT          NOT NULL,
    image_url       TEXT,
    display_order   INT             NOT NULL DEFAULT 0,
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_products_barcode_org UNIQUE (organization_id, barcode),
    CONSTRAINT uq_products_sku_org UNIQUE (organization_id, sku),
    CONSTRAINT chk_price_positive CHECK (price >= 0)
);

CREATE INDEX idx_products_org ON product_schema.products(organization_id);
CREATE INDEX idx_products_category ON product_schema.products(category_id);
CREATE INDEX idx_products_barcode ON product_schema.products(organization_id, barcode);
CREATE INDEX idx_products_active ON product_schema.products(organization_id, is_active);

-- Discounts
CREATE TABLE product_schema.discounts (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID            NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    discount_type   VARCHAR(20)     NOT NULL,
    value           BIGINT          NOT NULL,
    applies_to      VARCHAR(20)     NOT NULL DEFAULT 'TRANSACTION',
    valid_from      TIMESTAMPTZ,
    valid_until     TIMESTAMPTZ,
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_discount_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    CONSTRAINT chk_applies_to CHECK (applies_to IN ('TRANSACTION', 'PRODUCT')),
    CONSTRAINT chk_value_positive CHECK (value > 0)
);

CREATE INDEX idx_discounts_org ON product_schema.discounts(organization_id);
CREATE INDEX idx_discounts_active ON product_schema.discounts(organization_id, is_active);

-- Coupons
CREATE TABLE product_schema.coupons (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID            NOT NULL,
    discount_id     UUID            NOT NULL REFERENCES product_schema.discounts(id),
    code            VARCHAR(50)     NOT NULL UNIQUE,
    max_uses        INT,
    used_count      INT             NOT NULL DEFAULT 0,
    valid_from      TIMESTAMPTZ,
    valid_until     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_used_count CHECK (used_count >= 0)
);

CREATE INDEX idx_coupons_org ON product_schema.coupons(organization_id);
CREATE INDEX idx_coupons_code ON product_schema.coupons(code);
