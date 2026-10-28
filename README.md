# TestYourLuck API

Java 21 · Spring Boot 4 · Lombok · JPA/Hibernate 7 · Flyway · PostgreSQL 15 · Redis 7 · JWT.

## Executando

```bash
docker compose up -d          # neste diretório (backend/): PostgreSQL 15 + Redis 7
./gradlew bootRun             # http://localhost:9090 com o profile dev
```

Swagger (dev/stage): http://localhost:9090/swagger-ui.html · Health: `/actuator/health`

## Profiles

Toda a configuração está em `src/main/resources/application.yml`, um documento por profile:

| Profile | Uso | Observações |
|---|---|---|
| `production` | **padrão** do jar/Docker | Swagger desligado; exige `JWT_SECRET` |
| `stage` | homologação | Swagger ligado; exige `JWT_SECRET` |
| `dev` | desenvolvimento local | logs coloridos e compactos, `ddl-auto: none`, segredo JWT de desenvolvimento |
| `test` | suíte automatizada | banco `testyourluck_test`, Redis database 1, destinos HTTP locais liberados para webhooks |

`./gradlew bootRun` usa `dev` por padrão; outro profile: `./gradlew bootRun -Pprofile=stage`.
O jar usa `production` quando nenhum profile é informado; para escolher outro:
`java -jar build/libs/testyourluck-0.0.1-SNAPSHOT.jar --spring.profiles.active=stage`.

> Se o seu shell exporta `SPRING_PROFILES_ACTIVE` para outro projeto (ex.: `development`), ela vale também
> para o jar deste projeto. Rode com `--spring.profiles.active=...` ou `env -u SPRING_PROFILES_ACTIVE java -jar ...`.

## Build e testes

```bash
./gradlew build               # compila, testes unitários e de integração, gera o jar
./gradlew test                # apenas testes unitários (*Test)
./gradlew integrationTest     # apenas testes de integração (*IT)
./gradlew bootJar             # apenas o jar executável em build/libs/
```

Os testes de integração (`*IT`) usam o Postgres e o Redis do docker-compose, no banco
`testyourluck_test` e no Redis database 1. Se o banco não existir, crie com:
`docker exec tyl-postgres psql -U tyl -d postgres -c "CREATE DATABASE testyourluck_test"`.
Relatório de cobertura (JaCoCo): `build/reports/jacoco/test/html/index.html`.

## Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5432` / `testyourluck` | PostgreSQL |
| `DB_USER` / `DB_PASSWORD` | `tyl` / `tyl` | Credenciais |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis |
| `SPRING_PROFILES_ACTIVE` | `production` | Profile ativo (`production`, `stage`, `dev`) |
| `SERVER_PORT` | `9090` | Porta HTTP |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4000` | Origens do frontend (lista separada por vírgula) |
| `JWT_SECRET` | — (dev/test têm valor de desenvolvimento) | Base64, mínimo 256 bits — **obrigatório em production e stage** |
| `JWT_EXPIRATION_MINUTES` | `720` | Validade do token |
| `PUBLIC_BASE_URL` | `http://localhost:9090` | Prefixo das URLs públicas de imagens (`/api/media/{id}`) |
| `STORAGE_DIR` | `./storage` | Diretório dos arquivos enviados (separados por tenant) |

## Arquitetura

Pacotes por domínio em `com.doulgs.testyourluck`:

- `shared.tenant` — `BaseEntity` (`id`, `tenant` com `@TenantId`, `tenant_owner`, `created_at`, `updated_at`), `TenantContext` e resolver do Hibernate. O filtro JWT popula o contexto; o Hibernate aplica `tenant = ?` em todas as consultas e preenche o tenant nos inserts.
- `auth` — cadastro (cria tenant + usuário proprietário), login, `/api/me`, JWT e Spring Security.
- `event` — eventos (período, horários, timezone, limite de sorteios, banner) e `EventRules` (inativo, limite, fora do período).
- `catalog` — categorias e produtos com estoque único por produto e vínculo com eventos (`product_events`). Exclusão lógica (`deleted_at`).
- `field` — campos personalizados (chave de variável normalizada, tipos, opções).
- `settings` — configuração da máquina por tenant (estilo, brand kit, créditos, campos do cadastro).
- `participant` — cadastro do lead no totem; libera créditos controlados pelo servidor.
- `draw` — jogada: lock Redis por evento, consumo condicional de crédito, sorteio ponderado e baixa condicional do estoque do produto.
- `webhook` — integrações HTTP: templates `{{variavel}}`, disparo assíncrono após commit (executor dedicado), proteção SSRF (só HTTPS, sem rede interna; `app.webhooks.allow-insecure-targets` libera apenas em teste) e registro de cada disparo.
- `media` — upload (PNG/JPG/WEBP/SVG até 5 MB, validado pelos bytes) e leitura pública por id aleatório, com cache e CSP para SVG.
- `lead` — consulta paginada (jogadas + participante + status de webhook) e exportação CSV com campos personalizados.
- `dashboard` — agregados por período com cache Redis (60s) invalidado após commit das escritas.

Contrato completo dos endpoints: [`docs/api-contract.md`](docs/api-contract.md).
