-- Phase 9: Customer, Points, Gift Cards, Attendance, Shifts, Notifications, Currency (#140-#142, #170-#174)

-- #170: Add currency to organizations
ALTER TABLE store_schema.organizations ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'JPY';

-- #140: Customers
CREATE TABLE IF NOT EXISTS store_schema.customers (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID            NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    email           VARCHAR(255),
    phone           VARCHAR(20),
    points          BIGINT          NOT NULL DEFAULT 0,
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_customers_org ON store_schema.customers(organization_id);
CREATE INDEX IF NOT EXISTS idx_customers_email ON store_schema.customers(email);

-- #141: Point transactions
CREATE TABLE IF NOT EXISTS store_schema.point_transactions (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID            NOT NULL,
    customer_id     UUID            NOT NULL,
    points          BIGINT          NOT NULL,
    type            VARCHAR(20)     NOT NULL DEFAULT 'EARN',
    transaction_id  UUID,
    description     TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_pt_type CHECK (type IN ('EARN', 'REDEEM'))
);
CREATE INDEX IF NOT EXISTS idx_pt_org ON store_schema.point_transactions(organization_id);
CREATE INDEX IF NOT EXISTS idx_pt_customer ON store_schema.point_transactions(customer_id);

-- #142: Gift cards
CREATE TABLE IF NOT EXISTS store_schema.gift_cards (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID            NOT NULL,
    code            VARCHAR(50)     NOT NULL UNIQUE,
    balance         BIGINT          NOT NULL DEFAULT 0,
    initial_balance BIGINT          NOT NULL DEFAULT 0,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_gc_status CHECK (status IN ('ACTIVE', 'USED', 'EXPIRED')),
    CONSTRAINT chk_gc_balance CHECK (balance >= 0)
);
CREATE INDEX IF NOT EXISTS idx_gc_org ON store_schema.gift_cards(organization_id);

-- #171: Attendances
CREATE TABLE IF NOT EXISTS store_schema.attendances (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID            NOT NULL,
    staff_id        UUID            NOT NULL,
    store_id        UUID            NOT NULL,
    date            DATE            NOT NULL,
    clock_in        TIMESTAMPTZ     NOT NULL,
    clock_out       TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_attendance_staff_date UNIQUE (organization_id, staff_id, date)
);
CREATE INDEX IF NOT EXISTS idx_att_org ON store_schema.attendances(organization_id);

-- #172: Shifts
CREATE TABLE IF NOT EXISTS store_schema.shifts (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID            NOT NULL,
    staff_id        UUID            NOT NULL,
    store_id        UUID            NOT NULL,
    date            DATE            NOT NULL,
    start_time      TIME            NOT NULL,
    end_time        TIME            NOT NULL,
    note            TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_shifts_org ON store_schema.shifts(organization_id);
CREATE INDEX IF NOT EXISTS idx_shifts_store_date ON store_schema.shifts(store_id, date);

-- #174: Notifications
CREATE TABLE IF NOT EXISTS store_schema.notifications (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID            NOT NULL,
    type            VARCHAR(50)     NOT NULL,
    title           VARCHAR(255)    NOT NULL,
    message         TEXT            NOT NULL,
    is_read         BOOLEAN         NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_notifications_org ON store_schema.notifications(organization_id);
CREATE INDEX IF NOT EXISTS idx_notifications_read ON store_schema.notifications(organization_id, is_read);
