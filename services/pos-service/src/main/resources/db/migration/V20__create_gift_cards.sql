-- Gift Cards table
-- Prepaid store-value cards for customer loyalty and gift purchases
-- All monetary amounts in sen (BIGINT: 10000 = 100 JPY)
CREATE TABLE pos_schema.gift_cards (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID        NOT NULL,
    code            VARCHAR(19) NOT NULL,
    initial_amount  BIGINT      NOT NULL CHECK (initial_amount > 0),
    balance         BIGINT      NOT NULL DEFAULT 0 CHECK (balance >= 0),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    issued_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_gift_cards_org_code UNIQUE (organization_id, code)
);

CREATE INDEX idx_gift_cards_org ON pos_schema.gift_cards (organization_id);
CREATE INDEX idx_gift_cards_status ON pos_schema.gift_cards (organization_id, status);
