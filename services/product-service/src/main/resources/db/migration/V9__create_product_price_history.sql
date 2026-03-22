-- Product price history table (#620)
-- Tracks price changes for audit trail and historical reporting.

CREATE TABLE product_schema.product_price_history (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID            NOT NULL,
    product_id      UUID            NOT NULL,
    old_price       BIGINT          NOT NULL,
    new_price       BIGINT          NOT NULL,
    old_cost_price  BIGINT,
    new_cost_price  BIGINT,
    changed_by      UUID,
    reason          VARCHAR(255),
    effective_date  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_price_history_product ON product_schema.product_price_history(organization_id, product_id);
CREATE INDEX idx_price_history_date ON product_schema.product_price_history(organization_id, effective_date);
