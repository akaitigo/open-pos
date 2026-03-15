-- Phase 9: Suppliers, Stock transfers (#144, #145)

-- #144: Suppliers
CREATE TABLE IF NOT EXISTS inventory_schema.suppliers (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID            NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    contact_person  VARCHAR(100),
    email           VARCHAR(255),
    phone           VARCHAR(20),
    address         TEXT,
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_suppliers_org ON inventory_schema.suppliers(organization_id);

-- #145: Stock transfers
CREATE TABLE IF NOT EXISTS inventory_schema.stock_transfers (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID            NOT NULL,
    from_store_id   UUID            NOT NULL,
    to_store_id     UUID            NOT NULL,
    items           JSONB           NOT NULL DEFAULT '[]',
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    note            TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_st_status CHECK (status IN ('PENDING', 'APPROVED', 'COMPLETED', 'CANCELLED'))
);
CREATE INDEX IF NOT EXISTS idx_st_org ON inventory_schema.stock_transfers(organization_id);
