-- Add optimistic lock version column to transactions
ALTER TABLE pos_schema.transactions ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
