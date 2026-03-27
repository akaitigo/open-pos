-- Add idempotency_key column for finalize transaction deduplication (#955)
ALTER TABLE pos_schema.transactions
    ADD COLUMN idempotency_key VARCHAR(128);

CREATE UNIQUE INDEX idx_transactions_idempotency_key
    ON pos_schema.transactions (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
