-- Store operating hours and closed dates (#623)

CREATE TABLE store_schema.store_operating_hours (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID            NOT NULL,
    store_id        UUID            NOT NULL,
    day_of_week     INT             NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    open_time       TIME            NOT NULL,
    close_time      TIME            NOT NULL,
    is_closed       BOOLEAN         NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_operating_hours UNIQUE (organization_id, store_id, day_of_week)
);

CREATE TABLE store_schema.store_closed_dates (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID            NOT NULL,
    store_id        UUID            NOT NULL,
    closed_date     DATE            NOT NULL,
    reason          VARCHAR(255),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_closed_date UNIQUE (organization_id, store_id, closed_date)
);

CREATE INDEX idx_operating_hours_store ON store_schema.store_operating_hours(organization_id, store_id);
CREATE INDEX idx_closed_dates_store ON store_schema.store_closed_dates(organization_id, store_id, closed_date);
