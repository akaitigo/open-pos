-- Audit log table for recording important operations
CREATE TABLE store_schema.audit_logs (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID            NOT NULL,
    staff_id        UUID,
    action          VARCHAR(50)     NOT NULL,
    entity_type     VARCHAR(50)     NOT NULL,
    entity_id       VARCHAR(255),
    details         JSONB           NOT NULL DEFAULT '{}',
    ip_address      VARCHAR(45),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_org ON store_schema.audit_logs(organization_id);
CREATE INDEX idx_audit_logs_staff ON store_schema.audit_logs(staff_id);
CREATE INDEX idx_audit_logs_action ON store_schema.audit_logs(action);
CREATE INDEX idx_audit_logs_entity ON store_schema.audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created ON store_schema.audit_logs(created_at);

COMMENT ON TABLE store_schema.audit_logs IS 'Audit trail for important operations (create, update, delete)';
