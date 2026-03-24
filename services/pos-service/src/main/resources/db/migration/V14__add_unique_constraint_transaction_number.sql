-- transaction_number の衝突を防止するためユニーク制約を追加
-- organization_id と transaction_number の組み合わせで一意性を保証
ALTER TABLE pos_schema.transactions
    ADD CONSTRAINT uq_tx_org_transaction_number
    UNIQUE (organization_id, transaction_number);
