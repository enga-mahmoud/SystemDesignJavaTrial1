CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS categories (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name      VARCHAR(255) NOT NULL,
    parent_id UUID REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS products (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(500) NOT NULL,
    description TEXT,
    price       NUMERIC(12,2) NOT NULL,
    category_id UUID REFERENCES categories(id),
    vendor_id   UUID NOT NULL,
    status      VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS product_outbox (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id UUID NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload      JSONB NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published    BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_outbox_unpublished
    ON product_outbox(published, created_at) WHERE published = false;

INSERT INTO categories (id, name) VALUES
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'Electronics'),
    ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'Clothing'),
    ('c3d4e5f6-a7b8-9012-cdef-123456789012', 'Books')
ON CONFLICT DO NOTHING;
