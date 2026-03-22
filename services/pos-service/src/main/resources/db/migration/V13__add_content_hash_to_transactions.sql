-- 電子帳簿保存法: 取引データの真正性確保（SHA-256ハッシュ） (#597)
ALTER TABLE pos_schema.transactions
    ADD COLUMN IF NOT EXISTS content_hash VARCHAR(64);

COMMENT ON COLUMN pos_schema.transactions.content_hash IS '取引内容のSHA-256ハッシュ。電子帳簿保存法の真正性確保要件。';
