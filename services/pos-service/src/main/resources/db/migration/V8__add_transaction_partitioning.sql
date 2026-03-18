-- Transaction partitioning by month for data archiving (#210)
-- NOTE: PostgreSQL partitioning requires table recreation; this migration
-- creates a partitioned archive table for historical data.
-- Active transactions remain in the original table.

CREATE TABLE pos_schema.transactions_archive (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    store_id            UUID            NOT NULL,
    terminal_id         UUID            NOT NULL,
    staff_id            UUID            NOT NULL,
    transaction_number  VARCHAR(30)     NOT NULL,
    type                VARCHAR(20)     NOT NULL,
    status              VARCHAR(20)     NOT NULL,
    client_id           VARCHAR(36),
    subtotal            BIGINT          NOT NULL DEFAULT 0,
    tax_total           BIGINT          NOT NULL DEFAULT 0,
    discount_total      BIGINT          NOT NULL DEFAULT 0,
    total               BIGINT          NOT NULL DEFAULT 0,
    completed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now()
) PARTITION BY RANGE (created_at);

-- Create partitions for the current and next months
CREATE TABLE pos_schema.transactions_archive_default
    PARTITION OF pos_schema.transactions_archive DEFAULT;

CREATE INDEX idx_tx_archive_org ON pos_schema.transactions_archive(organization_id);
CREATE INDEX idx_tx_archive_created ON pos_schema.transactions_archive(organization_id, created_at);
CREATE INDEX idx_tx_archive_total ON pos_schema.transactions_archive(organization_id, total);
