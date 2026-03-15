-- Add index for electronic bookkeeping search capability (#125)
-- Supports search by amount range for 電子帳簿保存法 compliance

CREATE INDEX idx_tx_total ON pos_schema.transactions(organization_id, total);
CREATE INDEX idx_tx_completed ON pos_schema.transactions(organization_id, completed_at)
    WHERE completed_at IS NOT NULL;

-- Add document retention period config reference
COMMENT ON TABLE pos_schema.transactions IS '取引テーブル: 電子帳簿保存法により確定済取引は7年間保持必須';
COMMENT ON TABLE pos_schema.journal_entries IS '電子ジャーナル: 確定後の変更不可（不変ログ）';
