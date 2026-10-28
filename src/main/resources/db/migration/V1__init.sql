-- TestYourLuck: esquema inicial multitenant.
-- Toda tabela possui id, tenant, tenant_owner, created_at e updated_at.

CREATE TABLE tenants
(
    id           UUID PRIMARY KEY,
    tenant       UUID         NOT NULL,
    tenant_owner UUID         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    name         VARCHAR(160) NOT NULL
);
CREATE INDEX idx_tenants_tenant ON tenants (tenant);

CREATE TABLE users
(
    id            UUID PRIMARY KEY,
    tenant        UUID         NOT NULL,
    tenant_owner  UUID         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    name          VARCHAR(160) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL
);
CREATE UNIQUE INDEX uq_users_email ON users (lower(email));
CREATE INDEX idx_users_tenant ON users (tenant);

CREATE TABLE events
(
    id           UUID PRIMARY KEY,
    tenant       UUID         NOT NULL,
    tenant_owner UUID         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    name         VARCHAR(160) NOT NULL,
    event_date   DATE,
    place        VARCHAR(200),
    status       VARCHAR(20)  NOT NULL,
    deleted_at   TIMESTAMPTZ,
    CONSTRAINT ck_events_status CHECK (status IN ('ATIVO', 'RASCUNHO', 'ENCERRADO'))
);
CREATE INDEX idx_events_tenant ON events (tenant) WHERE deleted_at IS NULL;

CREATE TABLE categories
(
    id           UUID PRIMARY KEY,
    tenant       UUID         NOT NULL,
    tenant_owner UUID         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    name         VARCHAR(120) NOT NULL,
    description  VARCHAR(255),
    deleted_at   TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_categories_tenant_name ON categories (tenant, lower(name)) WHERE deleted_at IS NULL;

CREATE TABLE products
(
    id           UUID PRIMARY KEY,
    tenant       UUID         NOT NULL,
    tenant_owner UUID         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    name         VARCHAR(160) NOT NULL,
    short_code   VARCHAR(2)   NOT NULL,
    sku          VARCHAR(60)  NOT NULL,
    category_id  UUID REFERENCES categories (id),
    weight       INTEGER      NOT NULL,
    min_stock    INTEGER      NOT NULL,
    hue          INTEGER      NOT NULL,
    deleted_at   TIMESTAMPTZ,
    CONSTRAINT ck_products_weight CHECK (weight >= 0),
    CONSTRAINT ck_products_min_stock CHECK (min_stock >= 0)
);
CREATE UNIQUE INDEX uq_products_tenant_sku ON products (tenant, lower(sku)) WHERE deleted_at IS NULL;
CREATE INDEX idx_products_tenant ON products (tenant) WHERE deleted_at IS NULL;

CREATE TABLE event_stocks
(
    id           UUID PRIMARY KEY,
    tenant       UUID        NOT NULL,
    tenant_owner UUID        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    event_id     UUID        NOT NULL REFERENCES events (id),
    product_id   UUID        NOT NULL REFERENCES products (id),
    quantity     INTEGER     NOT NULL,
    CONSTRAINT ck_event_stocks_quantity CHECK (quantity >= 0)
);
CREATE UNIQUE INDEX uq_event_stocks ON event_stocks (tenant, event_id, product_id);

CREATE TABLE draws
(
    id                UUID PRIMARY KEY,
    tenant            UUID         NOT NULL,
    tenant_owner      UUID         NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,
    event_id          UUID         NOT NULL REFERENCES events (id),
    product_id        UUID         NOT NULL REFERENCES products (id),
    participant_name  VARCHAR(160) NOT NULL,
    participant_email VARCHAR(255),
    participant_phone VARCHAR(11),
    participant_cpf   VARCHAR(11)  NOT NULL,
    event_name        VARCHAR(160) NOT NULL,
    product_name      VARCHAR(160) NOT NULL,
    product_short     VARCHAR(2)   NOT NULL,
    product_hue       INTEGER      NOT NULL,
    drawn_at          TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_draws_tenant_drawn_at ON draws (tenant, drawn_at DESC);
CREATE INDEX idx_draws_tenant_event ON draws (tenant, event_id);
CREATE INDEX idx_draws_tenant_product ON draws (tenant, product_id);
