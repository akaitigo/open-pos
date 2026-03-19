-- Phase 9: 売上目標管理、商品アラート

-- #214 売上目標
CREATE TABLE analytics_schema.sales_targets (
    id                UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id   UUID            NOT NULL,
    store_id          UUID,
    target_month      DATE            NOT NULL,
    target_amount     BIGINT          NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_sales_target_org_store_month UNIQUE (organization_id, store_id, target_month)
);

CREATE INDEX idx_sales_targets_org ON analytics_schema.sales_targets(organization_id);

-- #207 商品アラート
CREATE TABLE analytics_schema.product_alerts (
    id                UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id   UUID            NOT NULL,
    product_id        UUID            NOT NULL,
    alert_type        VARCHAR(20)     NOT NULL,
    description       TEXT            NOT NULL,
    is_read           BOOLEAN         NOT NULL DEFAULT false,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_alert_type CHECK (alert_type IN ('TRENDING', 'DECLINING', 'ANOMALY'))
);

CREATE INDEX idx_product_alerts_org ON analytics_schema.product_alerts(organization_id);
CREATE INDEX idx_product_alerts_product ON analytics_schema.product_alerts(product_id);

COMMENT ON TABLE analytics_schema.sales_targets IS '売上目標管理';
COMMENT ON TABLE analytics_schema.product_alerts IS '商品アラート（売れ筋変動・在庫異常）';
