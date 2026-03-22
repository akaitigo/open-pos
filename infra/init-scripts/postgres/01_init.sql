-- Create schemas for each service
CREATE SCHEMA IF NOT EXISTS store_schema;
CREATE SCHEMA IF NOT EXISTS product_schema;
CREATE SCHEMA IF NOT EXISTS pos_schema;
CREATE SCHEMA IF NOT EXISTS inventory_schema;
CREATE SCHEMA IF NOT EXISTS analytics_schema;
CREATE SCHEMA IF NOT EXISTS audit_schema;
CREATE SCHEMA IF NOT EXISTS hydra;

-- Grant permissions (backward compatible: openpos user has all for dev)
GRANT ALL PRIVILEGES ON SCHEMA store_schema TO openpos;
GRANT ALL PRIVILEGES ON SCHEMA product_schema TO openpos;
GRANT ALL PRIVILEGES ON SCHEMA pos_schema TO openpos;
GRANT ALL PRIVILEGES ON SCHEMA inventory_schema TO openpos;
GRANT ALL PRIVILEGES ON SCHEMA analytics_schema TO openpos;
GRANT ALL PRIVILEGES ON SCHEMA audit_schema TO openpos;
GRANT ALL PRIVILEGES ON SCHEMA hydra TO openpos;

-- Service-specific users (for production least-privilege)
-- Each service should only access its own schema.
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'openpos_store') THEN
        CREATE ROLE openpos_store WITH LOGIN PASSWORD 'openpos_dev';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'openpos_product') THEN
        CREATE ROLE openpos_product WITH LOGIN PASSWORD 'openpos_dev';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'openpos_pos') THEN
        CREATE ROLE openpos_pos WITH LOGIN PASSWORD 'openpos_dev';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'openpos_inventory') THEN
        CREATE ROLE openpos_inventory WITH LOGIN PASSWORD 'openpos_dev';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'openpos_analytics') THEN
        CREATE ROLE openpos_analytics WITH LOGIN PASSWORD 'openpos_dev';
    END IF;
END
$$;

GRANT ALL PRIVILEGES ON SCHEMA store_schema TO openpos_store;
GRANT ALL PRIVILEGES ON SCHEMA product_schema TO openpos_product;
GRANT ALL PRIVILEGES ON SCHEMA pos_schema TO openpos_pos;
GRANT ALL PRIVILEGES ON SCHEMA inventory_schema TO openpos_inventory;
GRANT ALL PRIVILEGES ON SCHEMA analytics_schema TO openpos_analytics;
GRANT ALL PRIVILEGES ON SCHEMA audit_schema TO openpos_pos;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
