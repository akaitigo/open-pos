-- Add optimistic lock version column to stocks
ALTER TABLE inventory_schema.stocks ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Purchase orders table
CREATE TABLE inventory_schema.purchase_orders (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID            NOT NULL,
    store_id        UUID            NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    supplier_name   VARCHAR(255)    NOT NULL,
    note            TEXT,
    ordered_at      TIMESTAMPTZ,
    received_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_po_status CHECK (status IN ('DRAFT', 'ORDERED', 'RECEIVED', 'CANCELLED'))
);

CREATE INDEX idx_po_org ON inventory_schema.purchase_orders(organization_id);
CREATE INDEX idx_po_store ON inventory_schema.purchase_orders(organization_id, store_id);
CREATE INDEX idx_po_status ON inventory_schema.purchase_orders(organization_id, status);

-- Purchase order items table
CREATE TABLE inventory_schema.purchase_order_items (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID        NOT NULL,
    purchase_order_id   UUID        NOT NULL REFERENCES inventory_schema.purchase_orders(id),
    product_id          UUID        NOT NULL,
    ordered_quantity    INT         NOT NULL,
    received_quantity   INT         NOT NULL DEFAULT 0,
    unit_cost           BIGINT      NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_poi_ordered_qty CHECK (ordered_quantity > 0),
    CONSTRAINT chk_poi_received_qty CHECK (received_quantity >= 0)
);

CREATE INDEX idx_poi_po ON inventory_schema.purchase_order_items(purchase_order_id);
CREATE INDEX idx_poi_org ON inventory_schema.purchase_order_items(organization_id);
