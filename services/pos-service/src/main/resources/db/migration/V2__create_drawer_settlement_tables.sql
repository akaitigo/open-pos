-- Drawer (cash management) and Settlement (register closing) tables
-- All monetary values are BIGINT in sen (銭) unit: 10000 = 100 JPY

CREATE TABLE pos_schema.drawers (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID            NOT NULL,
    store_id            UUID            NOT NULL,
    terminal_id         UUID            NOT NULL,
    opening_amount      BIGINT          NOT NULL DEFAULT 0,
    current_amount      BIGINT          NOT NULL DEFAULT 0,
    is_open             BOOLEAN         NOT NULL DEFAULT false,
    opened_at           TIMESTAMPTZ,
    closed_at           TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_drawer_opening CHECK (opening_amount >= 0),
    CONSTRAINT chk_drawer_current CHECK (current_amount >= 0)
);

CREATE INDEX idx_drawers_org ON pos_schema.drawers(organization_id);
CREATE INDEX idx_drawers_terminal ON pos_schema.drawers(organization_id, store_id, terminal_id);

CREATE TABLE pos_schema.settlements (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID            NOT NULL,
    store_id            UUID            NOT NULL,
    terminal_id         UUID            NOT NULL,
    staff_id            UUID            NOT NULL,
    cash_expected       BIGINT          NOT NULL DEFAULT 0,
    cash_actual         BIGINT          NOT NULL DEFAULT 0,
    difference          BIGINT          NOT NULL DEFAULT 0,
    settled_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_settlements_org ON pos_schema.settlements(organization_id);
CREATE INDEX idx_settlements_store ON pos_schema.settlements(organization_id, store_id);
CREATE INDEX idx_settlements_settled ON pos_schema.settlements(organization_id, settled_at);
