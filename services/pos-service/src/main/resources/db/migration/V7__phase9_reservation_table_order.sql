-- Phase 9: 予約注文、テーブルオーダー、値引き理由コード

-- #193 予約注文
CREATE TABLE pos_schema.reservations (
    id                UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id   UUID            NOT NULL,
    store_id          UUID            NOT NULL,
    customer_name     VARCHAR(255),
    customer_phone    VARCHAR(20),
    items             JSONB           NOT NULL DEFAULT '[]',
    reserved_until    TIMESTAMPTZ     NOT NULL,
    status            VARCHAR(20)     NOT NULL DEFAULT 'RESERVED',
    note              TEXT,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_reservation_status CHECK (status IN ('RESERVED', 'FULFILLED', 'CANCELLED', 'EXPIRED'))
);

CREATE INDEX idx_reservations_org ON pos_schema.reservations(organization_id);
CREATE INDEX idx_reservations_store ON pos_schema.reservations(store_id);
CREATE INDEX idx_reservations_status ON pos_schema.reservations(status);

-- #200 テーブルオーダー: transactions テーブルに table_number を追加
ALTER TABLE pos_schema.transactions ADD COLUMN table_number VARCHAR(20);

-- #216 値引き理由コード
CREATE TABLE pos_schema.discount_reasons (
    id                UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id   UUID            NOT NULL,
    code              VARCHAR(20)     NOT NULL,
    description       VARCHAR(255)    NOT NULL,
    is_active         BOOLEAN         NOT NULL DEFAULT true,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_discount_reason_code_org UNIQUE (organization_id, code)
);

CREATE INDEX idx_discount_reasons_org ON pos_schema.discount_reasons(organization_id);

COMMENT ON TABLE pos_schema.reservations IS '予約注文・取り置き管理';
COMMENT ON TABLE pos_schema.discount_reasons IS '値引き理由コードマスタ';
