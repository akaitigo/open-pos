-- #327 GDPR / 個人情報保護対応: 同意管理テーブル

CREATE TABLE store_schema.data_processing_consents (
    id                UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id   UUID            NOT NULL REFERENCES store_schema.organizations(id),
    consent_type      VARCHAR(50)     NOT NULL,
    granted           BOOLEAN         NOT NULL DEFAULT false,
    granted_at        TIMESTAMPTZ,
    revoked_at        TIMESTAMPTZ,
    granted_by        UUID,
    policy_version    VARCHAR(20)     NOT NULL DEFAULT '1.0',
    ip_address        VARCHAR(45),
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_consent_org_type UNIQUE (organization_id, consent_type)
);

CREATE INDEX idx_consent_org ON store_schema.data_processing_consents(organization_id);
CREATE INDEX idx_consent_type ON store_schema.data_processing_consents(consent_type);

COMMENT ON TABLE store_schema.data_processing_consents IS 'GDPR データ処理同意管理';
COMMENT ON COLUMN store_schema.data_processing_consents.consent_type IS '同意種別: DATA_PROCESSING, MARKETING, ANALYTICS';
COMMENT ON COLUMN store_schema.data_processing_consents.granted IS '同意ステータス（true=同意済み, false=拒否/撤回）';
COMMENT ON COLUMN store_schema.data_processing_consents.granted_at IS '同意付与日時';
COMMENT ON COLUMN store_schema.data_processing_consents.revoked_at IS '同意撤回日時';
COMMENT ON COLUMN store_schema.data_processing_consents.policy_version IS 'プライバシーポリシーのバージョン番号';
