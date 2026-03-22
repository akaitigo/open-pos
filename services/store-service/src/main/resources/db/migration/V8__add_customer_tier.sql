-- Customer tier/segment support (#621)
ALTER TABLE store_schema.customers
    ADD COLUMN IF NOT EXISTS tier VARCHAR(20) NOT NULL DEFAULT 'REGULAR';

COMMENT ON COLUMN store_schema.customers.tier IS '顧客ランク: REGULAR, SILVER, GOLD, VIP';
