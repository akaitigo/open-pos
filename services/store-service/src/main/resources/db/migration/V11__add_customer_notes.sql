-- Add notes field to customers table (#1028)
ALTER TABLE store_schema.customers
    ADD COLUMN IF NOT EXISTS notes TEXT;

COMMENT ON COLUMN store_schema.customers.notes IS '顧客メモ（店舗スタッフ向けの自由記述）';
