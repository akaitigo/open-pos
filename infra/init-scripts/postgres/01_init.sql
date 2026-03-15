-- Create schemas for each service
CREATE SCHEMA IF NOT EXISTS store_schema;
CREATE SCHEMA IF NOT EXISTS product_schema;
CREATE SCHEMA IF NOT EXISTS pos_schema;
CREATE SCHEMA IF NOT EXISTS inventory_schema;
CREATE SCHEMA IF NOT EXISTS analytics_schema;
CREATE SCHEMA IF NOT EXISTS audit_schema;
CREATE SCHEMA IF NOT EXISTS hydra;

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA store_schema TO openpos;
GRANT ALL PRIVILEGES ON SCHEMA product_schema TO openpos;
GRANT ALL PRIVILEGES ON SCHEMA pos_schema TO openpos;
GRANT ALL PRIVILEGES ON SCHEMA inventory_schema TO openpos;
GRANT ALL PRIVILEGES ON SCHEMA analytics_schema TO openpos;
GRANT ALL PRIVILEGES ON SCHEMA audit_schema TO openpos;
GRANT ALL PRIVILEGES ON SCHEMA hydra TO openpos;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
