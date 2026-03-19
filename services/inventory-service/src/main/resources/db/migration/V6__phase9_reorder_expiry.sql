-- Phase 9: 在庫自動発注、在庫有効期限管理

-- #189 在庫自動発注: stocks テーブルに発注点・発注数量を追加
ALTER TABLE inventory_schema.stocks ADD COLUMN reorder_point INT NOT NULL DEFAULT 0;
ALTER TABLE inventory_schema.stocks ADD COLUMN reorder_quantity INT NOT NULL DEFAULT 0;

-- #199 在庫ロット（消費期限/賞味期限管理）
CREATE TABLE inventory_schema.stock_lots (
    id                UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id   UUID            NOT NULL,
    store_id          UUID            NOT NULL,
    product_id        UUID            NOT NULL,
    lot_number        VARCHAR(100),
    quantity          INT             NOT NULL DEFAULT 0,
    expiry_date       DATE,
    received_at       TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_stock_lots_org ON inventory_schema.stock_lots(organization_id);
CREATE INDEX idx_stock_lots_store_product ON inventory_schema.stock_lots(store_id, product_id);
CREATE INDEX idx_stock_lots_expiry ON inventory_schema.stock_lots(expiry_date);

COMMENT ON TABLE inventory_schema.stock_lots IS '在庫ロット（消費期限/賞味期限管理）';
