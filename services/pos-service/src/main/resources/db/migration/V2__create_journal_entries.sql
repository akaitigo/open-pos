-- Journal entries for electronic journal (電子ジャーナル)
-- Immutable audit log of all POS operations

CREATE TABLE pos_schema.journal_entries (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID            NOT NULL,
    type                VARCHAR(20)     NOT NULL,
    transaction_id      UUID            REFERENCES pos_schema.transactions(id),
    staff_id            UUID            NOT NULL,
    terminal_id         UUID            NOT NULL,
    details             JSONB           NOT NULL DEFAULT '{}',
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_journal_type CHECK (type IN ('SALE', 'VOID', 'RETURN', 'SETTLEMENT'))
);

CREATE INDEX idx_journal_org ON pos_schema.journal_entries(organization_id);
CREATE INDEX idx_journal_tx ON pos_schema.journal_entries(transaction_id);
CREATE INDEX idx_journal_created ON pos_schema.journal_entries(organization_id, created_at);
CREATE INDEX idx_journal_type ON pos_schema.journal_entries(organization_id, type);
