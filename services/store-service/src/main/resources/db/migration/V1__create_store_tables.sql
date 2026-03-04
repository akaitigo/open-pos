-- Store schema tables

-- Organizations (tenant root)
CREATE TABLE store_schema.organizations (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255)    NOT NULL,
    business_type   VARCHAR(20)     NOT NULL DEFAULT 'RETAIL',
    invoice_number  VARCHAR(20)     UNIQUE,
    plan            VARCHAR(20)     NOT NULL DEFAULT 'FREE',
    deleted_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_business_type CHECK (business_type IN ('RETAIL', 'RESTAURANT', 'OTHER'))
);

-- Stores
CREATE TABLE store_schema.stores (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID            NOT NULL REFERENCES store_schema.organizations(id),
    code            VARCHAR(20),
    name            VARCHAR(255)    NOT NULL,
    address         TEXT,
    phone           VARCHAR(20),
    timezone        VARCHAR(50)     NOT NULL DEFAULT 'Asia/Tokyo',
    settings        JSONB           NOT NULL DEFAULT '{}',
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_stores_code_org UNIQUE (organization_id, code)
);

CREATE INDEX idx_stores_org ON store_schema.stores(organization_id);

-- Terminals
CREATE TABLE store_schema.terminals (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID            NOT NULL REFERENCES store_schema.organizations(id),
    store_id        UUID            NOT NULL REFERENCES store_schema.stores(id),
    terminal_code   VARCHAR(20)     NOT NULL,
    name            VARCHAR(100),
    last_sync_at    TIMESTAMPTZ,
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_terminals_code_org UNIQUE (organization_id, terminal_code)
);

CREATE INDEX idx_terminals_org ON store_schema.terminals(organization_id);
CREATE INDEX idx_terminals_store ON store_schema.terminals(store_id);

-- Staff
CREATE TABLE store_schema.staff (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID            NOT NULL REFERENCES store_schema.organizations(id),
    store_id        UUID            NOT NULL REFERENCES store_schema.stores(id),
    hydra_subject   VARCHAR(255)    UNIQUE,
    name            VARCHAR(100)    NOT NULL,
    email           VARCHAR(255),
    role            VARCHAR(20)     NOT NULL DEFAULT 'CASHIER',
    pin_hash        VARCHAR(255),
    pin_failed_count INT            NOT NULL DEFAULT 0,
    pin_locked_until TIMESTAMPTZ,
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_role CHECK (role IN ('OWNER', 'MANAGER', 'CASHIER'))
);

CREATE INDEX idx_staff_org ON store_schema.staff(organization_id);
CREATE INDEX idx_staff_store ON store_schema.staff(store_id);
CREATE INDEX idx_staff_email ON store_schema.staff(email);
