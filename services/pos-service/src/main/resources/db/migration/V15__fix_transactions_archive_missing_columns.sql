-- Fix: transactions_archive missing columns added in V5/V5_4/V7 (#617)
-- These columns exist in the transactions table but were omitted from V8.

ALTER TABLE pos_schema.transactions_archive
    ADD COLUMN IF NOT EXISTS change_amount  BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS table_number   VARCHAR(20),
    ADD COLUMN IF NOT EXISTS memo           TEXT,
    ADD COLUMN IF NOT EXISTS is_training    BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS customer_id    UUID;
