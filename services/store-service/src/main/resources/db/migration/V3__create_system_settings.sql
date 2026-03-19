-- System settings (テナントレベル設定管理)
-- Key-value store for organization-wide configuration

CREATE TABLE store_schema.system_settings (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID            NOT NULL REFERENCES store_schema.organizations(id),
    key                 VARCHAR(100)    NOT NULL,
    value               TEXT            NOT NULL DEFAULT '',
    description         VARCHAR(500),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_setting_key_org UNIQUE (organization_id, key)
);

CREATE INDEX idx_settings_org ON store_schema.system_settings(organization_id);

-- Default settings
INSERT INTO store_schema.system_settings (organization_id, key, value, description)
SELECT id, 'receipt_header', '', 'レシートヘッダー文言'
FROM store_schema.organizations
ON CONFLICT DO NOTHING;

INSERT INTO store_schema.system_settings (organization_id, key, value, description)
SELECT id, 'receipt_footer', 'ありがとうございました', 'レシートフッター文言'
FROM store_schema.organizations
ON CONFLICT DO NOTHING;

INSERT INTO store_schema.system_settings (organization_id, key, value, description)
SELECT id, 'currency', 'JPY', '通貨コード（ISO 4217）'
FROM store_schema.organizations
ON CONFLICT DO NOTHING;

INSERT INTO store_schema.system_settings (organization_id, key, value, description)
SELECT id, 'timezone', 'Asia/Tokyo', 'デフォルトタイムゾーン'
FROM store_schema.organizations
ON CONFLICT DO NOTHING;
