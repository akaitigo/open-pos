-- Phase 9: Product variants, bundles, time sales, receipt templates, weight/age (#133-#135, #138, #143, #148-#149)

-- #135: Add unit_type to products
ALTER TABLE product_schema.products ADD COLUMN IF NOT EXISTS unit_type VARCHAR(20) NOT NULL DEFAULT 'PIECE';
-- #138: Add age_restricted to products
ALTER TABLE product_schema.products ADD COLUMN IF NOT EXISTS age_restricted BOOLEAN NOT NULL DEFAULT false;

-- #143: Time sales
CREATE TABLE IF NOT EXISTS product_schema.time_sales (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID            NOT NULL,
    product_id      UUID            NOT NULL,
    sale_price      BIGINT          NOT NULL,
    start_time      TIMESTAMPTZ     NOT NULL,
    end_time        TIMESTAMPTZ     NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    description     TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_ts_price CHECK (sale_price >= 0)
);
CREATE INDEX IF NOT EXISTS idx_ts_org ON product_schema.time_sales(organization_id);
CREATE INDEX IF NOT EXISTS idx_ts_product ON product_schema.time_sales(product_id);

-- #133: Product variants
CREATE TABLE IF NOT EXISTS product_schema.product_variants (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID            NOT NULL,
    product_id      UUID            NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    sku             VARCHAR(100),
    price           BIGINT          NOT NULL,
    attributes      JSONB           NOT NULL DEFAULT '{}',
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_pv_price CHECK (price >= 0)
);
CREATE INDEX IF NOT EXISTS idx_pv_org ON product_schema.product_variants(organization_id);
CREATE INDEX IF NOT EXISTS idx_pv_product ON product_schema.product_variants(product_id);

-- #134: Product bundles
CREATE TABLE IF NOT EXISTS product_schema.product_bundles (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID            NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    bundle_price    BIGINT          NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    description     TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_bp_price CHECK (bundle_price >= 0)
);
CREATE INDEX IF NOT EXISTS idx_pb_org ON product_schema.product_bundles(organization_id);

CREATE TABLE IF NOT EXISTS product_schema.product_bundle_items (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID            NOT NULL,
    bundle_id       UUID            NOT NULL,
    product_id      UUID            NOT NULL,
    quantity        INT             NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_pbi_qty CHECK (quantity >= 1)
);
CREATE INDEX IF NOT EXISTS idx_pbi_bundle ON product_schema.product_bundle_items(bundle_id);

-- #149: Receipt templates
CREATE TABLE IF NOT EXISTS product_schema.receipt_templates (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID            NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    header          TEXT,
    footer          TEXT,
    logo_url        TEXT,
    show_barcode    BOOLEAN         NOT NULL DEFAULT true,
    is_default      BOOLEAN         NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_rt_org ON product_schema.receipt_templates(organization_id);
