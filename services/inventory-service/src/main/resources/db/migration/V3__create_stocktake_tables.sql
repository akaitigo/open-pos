-- Stocktake (棚卸し) tables

CREATE TABLE inventory_schema.stocktakes (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID            NOT NULL,
    store_id            UUID            NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'IN_PROGRESS',
    started_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    completed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_stocktake_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_stocktakes_org ON inventory_schema.stocktakes(organization_id);
CREATE INDEX idx_stocktakes_store ON inventory_schema.stocktakes(organization_id, store_id);

CREATE TABLE inventory_schema.stocktake_items (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID            NOT NULL,
    stocktake_id        UUID            NOT NULL REFERENCES inventory_schema.stocktakes(id),
    product_id          UUID            NOT NULL,
    expected_qty        INT             NOT NULL DEFAULT 0,
    actual_qty          INT             NOT NULL DEFAULT 0,
    difference          INT             NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_stocktake_item UNIQUE (stocktake_id, product_id)
);

CREATE INDEX idx_stocktake_items_stocktake ON inventory_schema.stocktake_items(stocktake_id);
CREATE INDEX idx_stocktake_items_org ON inventory_schema.stocktake_items(organization_id);
