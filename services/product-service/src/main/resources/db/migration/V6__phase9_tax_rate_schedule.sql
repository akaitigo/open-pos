-- Phase 9: 税率変更スケジューリング

-- #219 税率変更スケジュール
CREATE TABLE product_schema.tax_rate_schedules (
    id                UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id   UUID            NOT NULL,
    tax_rate_id       UUID            NOT NULL,
    new_rate          DECIMAL(5,4)    NOT NULL,
    effective_date    DATE            NOT NULL,
    applied           BOOLEAN         NOT NULL DEFAULT false,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_tax_rate_schedules_org ON product_schema.tax_rate_schedules(organization_id);
CREATE INDEX idx_tax_rate_schedules_date ON product_schema.tax_rate_schedules(effective_date, applied);

COMMENT ON TABLE product_schema.tax_rate_schedules IS '税率変更スケジュール（自動適用）';
