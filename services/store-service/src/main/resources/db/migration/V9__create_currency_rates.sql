-- Currency exchange rates for multi-currency support (#628)

CREATE TABLE store_schema.currency_rates (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID            NOT NULL,
    from_currency   VARCHAR(3)      NOT NULL,
    to_currency     VARCHAR(3)      NOT NULL,
    rate            DECIMAL(18,8)   NOT NULL,
    effective_date  DATE            NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_currency_rate UNIQUE (organization_id, from_currency, to_currency, effective_date)
);

CREATE INDEX idx_currency_rates_lookup ON store_schema.currency_rates(organization_id, from_currency, to_currency, effective_date DESC);
