-- Inventory schema tables

CREATE TABLE inventory_schema.stocks (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id         UUID            NOT NULL,
    store_id                UUID            NOT NULL,
    product_id              UUID            NOT NULL,
    quantity                INT             NOT NULL DEFAULT 0,
    low_stock_threshold     INT             NOT NULL DEFAULT 10,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_stock_store_product UNIQUE (organization_id, store_id, product_id),
    CONSTRAINT chk_quantity CHECK (quantity >= 0)
);

CREATE INDEX idx_stocks_org ON inventory_schema.stocks(organization_id);
CREATE INDEX idx_stocks_store ON inventory_schema.stocks(organization_id, store_id);
CREATE INDEX idx_stocks_low ON inventory_schema.stocks(organization_id, store_id) WHERE quantity <= low_stock_threshold;

CREATE TABLE inventory_schema.stock_movements (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID            NOT NULL,
    store_id            UUID            NOT NULL,
    product_id          UUID            NOT NULL,
    movement_type       VARCHAR(20)     NOT NULL,
    quantity            INT             NOT NULL,
    reference_id        VARCHAR(36),
    note                TEXT,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_movement_type CHECK (movement_type IN ('SALE', 'RETURN', 'RECEIPT', 'ADJUSTMENT', 'TRANSFER'))
);

CREATE INDEX idx_movements_org ON inventory_schema.stock_movements(organization_id);
CREATE INDEX idx_movements_store ON inventory_schema.stock_movements(organization_id, store_id);
CREATE INDEX idx_movements_product ON inventory_schema.stock_movements(organization_id, store_id, product_id);
CREATE INDEX idx_movements_ref ON inventory_schema.stock_movements(reference_id);
CREATE INDEX idx_movements_created ON inventory_schema.stock_movements(created_at);
