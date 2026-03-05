-- POS schema tables
-- All monetary values are BIGINT in sen (銭) unit: 10000 = 100 JPY

CREATE TABLE pos_schema.transactions (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID            NOT NULL,
    store_id            UUID            NOT NULL,
    terminal_id         UUID            NOT NULL,
    staff_id            UUID            NOT NULL,
    transaction_number  VARCHAR(30)     NOT NULL,
    type                VARCHAR(20)     NOT NULL DEFAULT 'SALE',
    status              VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    client_id           VARCHAR(36),
    subtotal            BIGINT          NOT NULL DEFAULT 0,
    tax_total           BIGINT          NOT NULL DEFAULT 0,
    discount_total      BIGINT          NOT NULL DEFAULT 0,
    total               BIGINT          NOT NULL DEFAULT 0,
    completed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_tx_type CHECK (type IN ('SALE', 'RETURN', 'VOID')),
    CONSTRAINT chk_tx_status CHECK (status IN ('DRAFT', 'COMPLETED', 'VOIDED')),
    CONSTRAINT chk_subtotal_positive CHECK (subtotal >= 0),
    CONSTRAINT chk_total_positive CHECK (total >= 0),
    CONSTRAINT uq_tx_client_id_org UNIQUE (organization_id, client_id)
);

CREATE INDEX idx_tx_org ON pos_schema.transactions(organization_id);
CREATE INDEX idx_tx_store ON pos_schema.transactions(organization_id, store_id);
CREATE INDEX idx_tx_status ON pos_schema.transactions(organization_id, status);
CREATE INDEX idx_tx_created ON pos_schema.transactions(organization_id, created_at);

CREATE TABLE pos_schema.transaction_items (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID            NOT NULL,
    transaction_id      UUID            NOT NULL REFERENCES pos_schema.transactions(id),
    product_id          UUID            NOT NULL,
    product_name        VARCHAR(255)    NOT NULL,
    unit_price          BIGINT          NOT NULL,
    quantity            INT             NOT NULL DEFAULT 1,
    tax_rate_name       VARCHAR(50)     NOT NULL,
    tax_rate            VARCHAR(10)     NOT NULL,
    is_reduced_tax      BOOLEAN         NOT NULL DEFAULT false,
    subtotal            BIGINT          NOT NULL,
    tax_amount          BIGINT          NOT NULL,
    total               BIGINT          NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_item_qty CHECK (quantity >= 1),
    CONSTRAINT chk_item_subtotal CHECK (subtotal >= 0)
);

CREATE INDEX idx_tx_items_tx ON pos_schema.transaction_items(transaction_id);
CREATE INDEX idx_tx_items_org ON pos_schema.transaction_items(organization_id);

CREATE TABLE pos_schema.payments (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID            NOT NULL,
    transaction_id      UUID            NOT NULL REFERENCES pos_schema.transactions(id),
    method              VARCHAR(20)     NOT NULL,
    amount              BIGINT          NOT NULL,
    received            BIGINT,
    change              BIGINT,
    reference           VARCHAR(255),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_payment_method CHECK (method IN ('CASH', 'CREDIT_CARD', 'QR_CODE')),
    CONSTRAINT chk_payment_amount CHECK (amount >= 0)
);

CREATE INDEX idx_payments_tx ON pos_schema.payments(transaction_id);
CREATE INDEX idx_payments_org ON pos_schema.payments(organization_id);

CREATE TABLE pos_schema.transaction_discounts (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id         UUID            NOT NULL,
    transaction_id          UUID            NOT NULL REFERENCES pos_schema.transactions(id),
    discount_id             UUID,
    name                    VARCHAR(100)    NOT NULL,
    discount_type           VARCHAR(20)     NOT NULL,
    value                   VARCHAR(50)     NOT NULL,
    amount                  BIGINT          NOT NULL,
    transaction_item_id     UUID,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_disc_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    CONSTRAINT chk_disc_amount CHECK (amount >= 0)
);

CREATE INDEX idx_tx_discounts_tx ON pos_schema.transaction_discounts(transaction_id);
CREATE INDEX idx_tx_discounts_org ON pos_schema.transaction_discounts(organization_id);

CREATE TABLE pos_schema.tax_summaries (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID            NOT NULL,
    transaction_id      UUID            NOT NULL REFERENCES pos_schema.transactions(id),
    tax_rate_name       VARCHAR(50)     NOT NULL,
    tax_rate            VARCHAR(10)     NOT NULL,
    is_reduced          BOOLEAN         NOT NULL DEFAULT false,
    taxable_amount      BIGINT          NOT NULL,
    tax_amount          BIGINT          NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_taxable_amount CHECK (taxable_amount >= 0),
    CONSTRAINT chk_tax_amount CHECK (tax_amount >= 0)
);

CREATE INDEX idx_tax_summaries_tx ON pos_schema.tax_summaries(transaction_id);
CREATE INDEX idx_tax_summaries_org ON pos_schema.tax_summaries(organization_id);
