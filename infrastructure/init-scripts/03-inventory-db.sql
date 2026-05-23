CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS inventory (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku_id            UUID UNIQUE NOT NULL,
    product_id        UUID NOT NULL,
    quantity          INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    version           BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_reserved CHECK (reserved_quantity >= 0),
    CONSTRAINT chk_available CHECK (quantity >= reserved_quantity)
);

CREATE TABLE IF NOT EXISTS inventory_transaction (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku_id     UUID NOT NULL,
    type       VARCHAR(50) NOT NULL,
    quantity   INT NOT NULL,
    order_id   UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
