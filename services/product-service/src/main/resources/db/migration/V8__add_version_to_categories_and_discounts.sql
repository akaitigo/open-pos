-- #495: カテゴリと割引テーブルに楽観ロック用 version カラムを追加
-- products テーブルは V4 で追加済み

ALTER TABLE product_schema.categories ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE product_schema.discounts ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
