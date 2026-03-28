-- Stamp Cards table
-- Customer loyalty stamp cards for reward programs
CREATE TABLE store_schema.stamp_cards (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID        NOT NULL,
    customer_id         UUID        NOT NULL,
    stamp_count         INT         NOT NULL DEFAULT 0 CHECK (stamp_count >= 0),
    max_stamps          INT         NOT NULL DEFAULT 10 CHECK (max_stamps > 0),
    reward_description  TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    issued_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    version             BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX idx_stamp_cards_org ON store_schema.stamp_cards (organization_id);
CREATE INDEX idx_stamp_cards_customer ON store_schema.stamp_cards (organization_id, customer_id);
