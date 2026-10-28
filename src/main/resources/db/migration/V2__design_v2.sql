-- TestYourLuck: design v2 (estoque por produto, participantes/créditos, campos personalizados,
-- configuração da máquina, webhooks e mídia). Toda tabela nova mantém id, tenant, tenant_owner,
-- created_at e updated_at.

-- Mídia --------------------------------------------------------------------------------------
CREATE TABLE media_files (
    id            UUID PRIMARY KEY,
    tenant        UUID         NOT NULL,
    tenant_owner  UUID         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    content_type  VARCHAR(60)  NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    original_name VARCHAR(255),
    storage_key   VARCHAR(255) NOT NULL
);
CREATE INDEX idx_media_files_tenant ON media_files (tenant);

-- Eventos ------------------------------------------------------------------------------------
ALTER TABLE events RENAME COLUMN event_date TO start_date;
ALTER TABLE events
    ADD COLUMN end_date        DATE,
    ADD COLUMN all_day         BOOLEAN     NOT NULL DEFAULT TRUE,
    ADD COLUMN start_time      TIME,
    ADD COLUMN end_time        TIME,
    ADD COLUMN allow_outside   BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN city            VARCHAR(120),
    ADD COLUMN uf              VARCHAR(2),
    ADD COLUMN region          VARCHAR(20),
    ADD COLUMN cep             VARCHAR(9),
    ADD COLUMN timezone        VARCHAR(40) NOT NULL DEFAULT 'America/Sao_Paulo',
    ADD COLUMN max_draws       INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN website         VARCHAR(255),
    ADD COLUMN banner_media_id UUID REFERENCES media_files (id);
ALTER TABLE events ADD CONSTRAINT ck_events_max_draws CHECK (max_draws >= 0);
ALTER TABLE events ADD CONSTRAINT ck_events_period CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date);
-- Eventos da v1 não tinham restrição de período: preservam o comportamento permitindo jogar fora da data.
UPDATE events SET allow_outside = TRUE;

-- Produtos: estoque único e vínculo com eventos ----------------------------------------------
ALTER TABLE products
    ADD COLUMN quantity       INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN image_media_id UUID REFERENCES media_files (id);
UPDATE products SET weight = 100 WHERE weight > 100;
ALTER TABLE products ADD CONSTRAINT ck_products_quantity CHECK (quantity >= 0);
ALTER TABLE products ADD CONSTRAINT ck_products_weight_max CHECK (weight <= 100);

CREATE TABLE product_events (
    id           UUID PRIMARY KEY,
    tenant       UUID        NOT NULL,
    tenant_owner UUID        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    product_id   UUID        NOT NULL REFERENCES products (id),
    event_id     UUID        NOT NULL REFERENCES events (id)
);
CREATE UNIQUE INDEX uq_product_events ON product_events (tenant, product_id, event_id);
CREATE INDEX idx_product_events_event ON product_events (tenant, event_id);

UPDATE products p SET quantity = COALESCE((
    SELECT SUM(s.quantity) FROM event_stocks s JOIN events e ON e.id = s.event_id AND e.deleted_at IS NULL
    WHERE s.product_id = p.id), 0);

INSERT INTO product_events (id, tenant, tenant_owner, created_at, updated_at, product_id, event_id)
SELECT gen_random_uuid(), s.tenant, s.tenant_owner, now(), now(), s.product_id, s.event_id
FROM event_stocks s JOIN events e ON e.id = s.event_id AND e.deleted_at IS NULL;

-- Produtos sem nenhum vínculo passam a participar de todos os eventos do tenant (padrão do design).
INSERT INTO product_events (id, tenant, tenant_owner, created_at, updated_at, product_id, event_id)
SELECT gen_random_uuid(), p.tenant, p.tenant_owner, now(), now(), p.id, e.id
FROM products p JOIN events e ON e.tenant = p.tenant AND e.deleted_at IS NULL
WHERE NOT EXISTS (SELECT 1 FROM product_events pe WHERE pe.product_id = p.id);

DROP TABLE event_stocks;

-- Participantes e créditos -------------------------------------------------------------------
CREATE TABLE participants (
    id              UUID PRIMARY KEY,
    tenant          UUID         NOT NULL,
    tenant_owner    UUID         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    event_id        UUID         NOT NULL REFERENCES events (id),
    name            VARCHAR(160) NOT NULL,
    email           VARCHAR(255),
    phone           VARCHAR(11),
    cpf             VARCHAR(11),
    answers         JSONB        NOT NULL DEFAULT '{}'::jsonb,
    credits_granted INTEGER      NOT NULL,
    credits_used    INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT ck_participants_credits CHECK (credits_used >= 0 AND credits_used <= credits_granted)
);
CREATE INDEX idx_participants_tenant_event ON participants (tenant, event_id);

-- Cada sorteio da v1 vira um participante com 1 crédito já utilizado (mesmo id do sorteio).
INSERT INTO participants (id, tenant, tenant_owner, created_at, updated_at, event_id, name, email, phone, cpf,
                          credits_granted, credits_used)
SELECT d.id, d.tenant, d.tenant_owner, d.created_at, d.updated_at, d.event_id, d.participant_name,
       d.participant_email, d.participant_phone, d.participant_cpf, 1, 1
FROM draws d;

ALTER TABLE draws ADD COLUMN participant_id UUID REFERENCES participants (id);
UPDATE draws SET participant_id = id;
ALTER TABLE draws ALTER COLUMN participant_id SET NOT NULL;
ALTER TABLE draws
    DROP COLUMN participant_name,
    DROP COLUMN participant_email,
    DROP COLUMN participant_phone,
    DROP COLUMN participant_cpf;
CREATE INDEX idx_draws_participant ON draws (participant_id);

-- Campos personalizados ----------------------------------------------------------------------
CREATE TABLE custom_fields (
    id           UUID PRIMARY KEY,
    tenant       UUID         NOT NULL,
    tenant_owner UUID         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    label        VARCHAR(160) NOT NULL,
    field_key    VARCHAR(32)  NOT NULL,
    field_type   VARCHAR(10)  NOT NULL,
    options      JSONB        NOT NULL DEFAULT '[]'::jsonb,
    placeholder  VARCHAR(200),
    required     BOOLEAN      NOT NULL DEFAULT FALSE,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted_at   TIMESTAMPTZ,
    CONSTRAINT ck_custom_fields_type CHECK (field_type IN ('TEXT', 'NUMBER', 'DATE', 'SELECT', 'BOOL'))
);
CREATE UNIQUE INDEX uq_custom_fields_key ON custom_fields (tenant, field_key) WHERE deleted_at IS NULL;

-- Configuração da máquina (uma por tenant) ---------------------------------------------------
CREATE TABLE draw_settings (
    id               UUID PRIMARY KEY,
    tenant           UUID         NOT NULL,
    tenant_owner     UUID         NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    style            VARCHAR(10)  NOT NULL,
    size             VARCHAR(2)   NOT NULL,
    credits          INTEGER      NOT NULL,
    primary_color    VARCHAR(7)   NOT NULL,
    background       VARCHAR(10)  NOT NULL,
    logo_mode        VARCHAR(10)  NOT NULL,
    logo_media_id    UUID REFERENCES media_files (id),
    title            VARCHAR(80)  NOT NULL,
    win_message      VARCHAR(160) NOT NULL,
    ask_email        BOOLEAN      NOT NULL,
    ask_phone        BOOLEAN      NOT NULL,
    ask_cpf          BOOLEAN      NOT NULL,
    lights           BOOLEAN      NOT NULL,
    sound            BOOLEAN      NOT NULL,
    show_prizes      BOOLEAN      NOT NULL,
    custom_field_ids JSONB        NOT NULL DEFAULT '[]'::jsonb,
    CONSTRAINT ck_draw_settings_credits CHECK (credits BETWEEN 1 AND 5)
);
CREATE UNIQUE INDEX uq_draw_settings_tenant ON draw_settings (tenant);

-- Webhooks -----------------------------------------------------------------------------------
CREATE TABLE webhooks (
    id               UUID PRIMARY KEY,
    tenant           UUID         NOT NULL,
    tenant_owner     UUID         NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    name             VARCHAR(160) NOT NULL,
    trigger_type     VARCHAR(10)  NOT NULL,
    method           VARCHAR(10)  NOT NULL,
    url              VARCHAR(2000) NOT NULL,
    headers          JSONB        NOT NULL DEFAULT '[]'::jsonb,
    params           JSONB        NOT NULL DEFAULT '[]'::jsonb,
    body             TEXT,
    event_ids        JSONB        NOT NULL DEFAULT '[]'::jsonb,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    dispatch_count   INTEGER      NOT NULL DEFAULT 0,
    last_dispatch_at TIMESTAMPTZ,
    deleted_at       TIMESTAMPTZ,
    CONSTRAINT ck_webhooks_trigger CHECK (trigger_type IN ('LEAD', 'PLAY')),
    CONSTRAINT ck_webhooks_method CHECK (method IN ('GET', 'POST', 'PUT', 'PATCH', 'DELETE'))
);
CREATE INDEX idx_webhooks_tenant ON webhooks (tenant) WHERE deleted_at IS NULL;

CREATE TABLE webhook_deliveries (
    id             UUID PRIMARY KEY,
    tenant         UUID         NOT NULL,
    tenant_owner   UUID         NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    webhook_id     UUID         NOT NULL REFERENCES webhooks (id),
    webhook_name   VARCHAR(160) NOT NULL,
    trigger_type   VARCHAR(10)  NOT NULL,
    test           BOOLEAN      NOT NULL DEFAULT FALSE,
    participant_id UUID REFERENCES participants (id),
    draw_id        UUID REFERENCES draws (id),
    event_name     VARCHAR(160),
    method         VARCHAR(10)  NOT NULL,
    url            VARCHAR(4000) NOT NULL,
    request        JSONB        NOT NULL,
    response       JSONB,
    status_code    INTEGER,
    ok             BOOLEAN      NOT NULL,
    error          VARCHAR(500),
    sent_at        TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_webhook_deliveries_recent ON webhook_deliveries (tenant, sent_at DESC);
CREATE INDEX idx_webhook_deliveries_draw ON webhook_deliveries (draw_id);
CREATE INDEX idx_webhook_deliveries_participant ON webhook_deliveries (participant_id);
