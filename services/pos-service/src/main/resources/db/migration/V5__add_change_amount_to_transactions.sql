-- Add change_amount column for split payment overpay tracking (#376)
ALTER TABLE pos_schema.transactions
    ADD COLUMN change_amount BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN pos_schema.transactions.change_amount IS 'Overpay amount returned as cash change in split payment (sen unit)';
