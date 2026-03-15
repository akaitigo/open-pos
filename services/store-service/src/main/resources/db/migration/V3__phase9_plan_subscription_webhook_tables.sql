-- Phase 9: プラン・課金、Webhook、お気に入り商品テーブル

-- #176 プランマスタ
CREATE TABLE store_schema.plans (
    id                UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    name              VARCHAR(100)    NOT NULL,
    max_stores        INT             NOT NULL DEFAULT 1,
    max_terminals     INT             NOT NULL DEFAULT 2,
    max_products      INT             NOT NULL DEFAULT 100,
    monthly_price     BIGINT          NOT NULL DEFAULT 0,
    is_active         BOOLEAN         NOT NULL DEFAULT true,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT now()
);

-- #176 サブスクリプション
CREATE TABLE store_schema.subscriptions (
    id                UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id   UUID            NOT NULL REFERENCES store_schema.organizations(id),
    plan_id           UUID            NOT NULL REFERENCES store_schema.plans(id),
    status            VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    start_date        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    end_date          TIMESTAMPTZ,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_subscription_status CHECK (status IN ('ACTIVE', 'CANCELLED', 'EXPIRED', 'TRIAL'))
);

CREATE INDEX idx_subscriptions_org ON store_schema.subscriptions(organization_id);
CREATE INDEX idx_subscriptions_plan ON store_schema.subscriptions(plan_id);

-- #188 お気に入り商品
CREATE TABLE store_schema.favorite_products (
    id                UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id   UUID            NOT NULL REFERENCES store_schema.organizations(id),
    staff_id          UUID            NOT NULL REFERENCES store_schema.staff(id),
    product_id        UUID            NOT NULL,
    sort_order        INT             NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_favorite_staff_product UNIQUE (staff_id, product_id)
);

CREATE INDEX idx_favorite_products_staff ON store_schema.favorite_products(staff_id);
CREATE INDEX idx_favorite_products_org ON store_schema.favorite_products(organization_id);

-- #203 Webhook
CREATE TABLE store_schema.webhooks (
    id                UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id   UUID            NOT NULL REFERENCES store_schema.organizations(id),
    url               VARCHAR(2048)   NOT NULL,
    events            JSONB           NOT NULL DEFAULT '[]',
    secret            VARCHAR(255)    NOT NULL,
    is_active         BOOLEAN         NOT NULL DEFAULT true,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_webhooks_org ON store_schema.webhooks(organization_id);
CREATE INDEX idx_webhooks_active ON store_schema.webhooks(organization_id, is_active);

COMMENT ON TABLE store_schema.plans IS 'マルチテナント プランマスタ';
COMMENT ON TABLE store_schema.subscriptions IS 'テナントのサブスクリプション管理';
COMMENT ON TABLE store_schema.favorite_products IS 'スタッフ別お気に入り商品';
COMMENT ON TABLE store_schema.webhooks IS 'Webhook連携設定';
