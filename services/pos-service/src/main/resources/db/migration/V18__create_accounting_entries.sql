-- Structured accounting entries for journal integration (#624)

CREATE TABLE pos_schema.accounting_entries (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID            NOT NULL,
    transaction_id      UUID            NOT NULL,
    account_code        VARCHAR(20)     NOT NULL,
    account_name        VARCHAR(100)    NOT NULL,
    debit_amount        BIGINT          NOT NULL DEFAULT 0,
    credit_amount       BIGINT          NOT NULL DEFAULT 0,
    description         VARCHAR(255),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_accounting_entries_tx ON pos_schema.accounting_entries(organization_id, transaction_id);
CREATE INDEX idx_accounting_entries_account ON pos_schema.accounting_entries(organization_id, account_code);

COMMENT ON TABLE pos_schema.accounting_entries IS '構造化仕訳エントリ。会計ソフト連携の基盤。';
