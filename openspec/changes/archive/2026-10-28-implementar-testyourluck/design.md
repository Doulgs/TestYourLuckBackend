# Design

## Context

- Projeto greenfield. Já existem: `docker-compose.yml` na raiz (`postgres:15` e `redis:7`), `backend/` gerado pelo Spring Initializr (Spring Boot 4.1.2, Java 21, Web MVC, JPA, Flyway, Security, Redis, Validation, Actuator, Lombok, jjwt 0.13, springdoc 3.2) com `application.yml` por perfil, e `frontend/` (Next.js 16.4 App Router com `src/`, React 19, Tailwind 4, shadcn/ui estilo `radix-nova`, zustand, react-hook-form, @hookform/resolvers, zod, tailwind-merge).
- O protótipo `design/TestYourLuck.dc.html` define layout, cores, textos e regras de cálculo (chance, status de estoque, máscaras, animação dos rolos). Ele é a referência visual; regras de negócio estão nas specs.
- Requisitos de comportamento: ver `specs/*/spec.md`. Motivação: ver `proposal.md`.

## Goals / Non-Goals

**Goals:**
- Isolamento multitenant aplicado pela camada de persistência, sem depender de cada desenvolvedor lembrar de filtrar por tenant.
- Sorteio correto sob concorrência (sem estoque negativo, sem brinde escolhido pelo cliente).
- Frontend fiel ao design com componentes reutilizáveis (shadcn/ui) e tema escuro único.

**Non-Goals:**
- Multitenancy por schema/banco separado.
- SSR de dados autenticados (o painel é client-side; não há cookie de sessão HttpOnly nesta fase).
- Testes E2E de navegador; a cobertura fica em testes de serviço/integração no backend e verificação de build/lint/tipos no frontend.

## Decisions

### D1. Multitenancy por coluna discriminadora com `@TenantId` do Hibernate
- Toda entidade estende `BaseEntity` (`@MappedSuperclass`) com `id UUID`, `tenant UUID` anotado com `@TenantId`, `tenantOwner UUID`, `createdAt` e `updatedAt` (`@CreationTimestamp`/`@UpdateTimestamp`, `timestamptz`).
- `CurrentTenantIdentifierResolver` lê o `TenantContext` (ThreadLocal preenchido pelo filtro JWT). O Hibernate adiciona `tenant = ?` automaticamente em todas as consultas e preenche `tenant` em inserts. Para operações sem usuário (login/cadastro) o resolver retorna um tenant "root" (`isRoot` = true), que desativa o filtro apenas nesses fluxos.
- `tenant_owner` é preenchido em `@PrePersist` a partir do `TenantContext`.
- Tabela `tenants`: o próprio registro tem `tenant = id` e `tenant_owner = id do usuário proprietário`. Tabela `users`: `tenant` da empresa e `tenant_owner` do proprietário.
- Alternativas: `@Filter` + aspecto (exige ativação manual por sessão, fácil de esquecer); schema por tenant (custo operacional alto). `@TenantId` dá garantia por padrão.
- Valores de tenant vindos do cliente nunca são mapeados nos DTOs de entrada.

### D2. Identificadores e esquema
- PKs `uuid` geradas pela aplicação (`GenerationType.UUID`). Migrations Flyway versionadas em `backend/src/main/resources/db/migration` (`V1__init.sql`).
- Tabelas: `tenants`, `users`, `events`, `categories`, `products`, `event_stocks`, `draws`. Todas com as 5 colunas obrigatórias.
- Unicidades por tenant: `users(email)` global (login por e-mail), `categories(tenant, lower(name))`, `products(tenant, sku)` parcial `where deleted_at is null`, `event_stocks(tenant, event_id, product_id)`.
- Exclusão lógica (`deleted_at`) em `events`, `products` e `categories` com `@SQLRestriction("deleted_at is null")`. Simplifica a regra "excluir mantendo histórico" sem bifurcar por existência de sorteios.

### D3. Lead = registro de sorteio (`draws`)
- Cada sorteio grava participante (nome, e-mail, telefone, CPF só dígitos), `event_id`, `product_id` e um snapshot de `event_name`, `product_name`, `product_short`, `product_hue`, além de `drawn_at`.
- O snapshot garante que leads continuem legíveis após exclusão lógica/renomeação. Alternativa (join sempre) quebraria com exclusões.
- CPF é armazenado completo (necessário para o CSV) e mascarado apenas na apresentação.

### D4. Sorteio concorrente: lock Redis + update condicional
- `POST /api/draws` recebe `eventId` e dados do participante; o serviço:
  1. Adquire lock Redis `draw-lock:{tenant}:{eventId}` (`SET NX PX 5000`, token aleatório, liberação via script compare-and-delete) com espera curta (até 3s).
  2. Em transação, carrega estoques elegíveis (`quantity > 0`, produto ativo com `weight > 0`) e sorteia com `SecureRandom` ponderado pelo peso.
  3. Executa `UPDATE event_stocks SET quantity = quantity - 1 WHERE id = ? AND quantity > 0`; se 0 linhas, remove o candidato e repete.
  4. Persiste o `draw` e invalida o cache do dashboard do tenant.
- O update condicional é a garantia final (funciona mesmo se o Redis estiver indisponível — nesse caso o lock é ignorado com log de aviso). Alternativa `SELECT ... FOR UPDATE` puro também funcionaria; o lock evita contenção e retries no banco em totens com alto fluxo.

### D5. Autenticação JWT stateless
- `POST /api/auth/signup` e `POST /api/auth/login` retornam `{ token, user }`. Senhas com `BCrypt`. Token HS256 (jjwt) com claims `sub` (userId), `tenant`, `tenantOwner`, `name`, `email`; validade configurável (`app.security.jwt.expiration-minutes`).
- Filtro `OncePerRequestFilter` valida o token, popula `SecurityContext` e `TenantContext`, e limpa o contexto no `finally`.
- CORS configurado por `app.cors.allowed-origins`. Erros padronizados via `@RestControllerAdvice` no formato `{ status, message, errors? }` (`ProblemDetail`-like).

### D6. Dashboard calculado no servidor com cache Redis
- `GET /api/dashboard?range=7|30|90` retorna KPIs (com variação vs. período anterior), séries de sorteios (top 2 eventos), série de leads, histórico recente, estoque baixo, mais sorteados e eventos ativos.
- Buckets: 7 → 7×1 dia; 30 → 10×3 dias; 90 → 13×7 dias (como no protótipo), terminando no dia atual (fuso `America/Sao_Paulo`).
- Cache via `RedisTemplate` com chave `dashboard:{tenant}:{range}`, TTL 60s; serviços de escrita chamam `DashboardCache.evict(tenant)`. Alternativa `@Cacheable` foi descartada para manter a chave por tenant explícita.

### D7. API REST
- Prefixo `/api`. Recursos: `/auth/*`, `/me`, `/events`, `/categories`, `/products`, `/events/{id}/stock` (GET lista; `PUT /events/{id}/stock/{productId}` com `{quantity}`), `/draws` (POST), `/leads` (GET paginado `q`, `eventId`, `page`, `size`) e `/leads/export` (CSV completo do filtro), `/dashboard`.
- Respostas de listagem de eventos/produtos/categorias já incluem agregados exibidos na tela (sorteios por evento, estoque total por produto, indicadores por categoria) para evitar N+1 no cliente.
- Busca textual das listagens de eventos/produtos/categorias é feita no cliente (volumes pequenos); leads têm busca no servidor.

### D8. Frontend: App Router + componentes client
- Rotas: `(auth)/login`, `(auth)/cadastro`, `(app)/dashboard`, `(app)/sorteio`, `(app)/leads`, `(app)/eventos`, `(app)/categorias`, `(app)/produtos`, `(app)/estoque`; `/` redireciona conforme sessão.
- Estado: `zustand` para sessão (`persist` em localStorage), UI (sidebar, preferência de som) e seleção de evento do totem. Dados de domínio via hooks simples sobre um cliente `fetch` (`lib/api.ts`) que injeta `Authorization`, trata 401 (logout) e normaliza erros.
- Formulários com `react-hook-form` + `zodResolver`; schemas zod em `lib/validation`. Componentes shadcn/ui (`Button`, `Input`, `Select`, `Dialog`, `DropdownMenu`, `Badge`, `Sonner`) estilizados com os tokens do design.
- Tema: variáveis CSS em `globals.css` mapeando a paleta do protótipo (fundo `#000/#0a0a0a`, cartões `#111`, bordas `#222`, destaque `oklch(0.72 0.2 340)`), fontes Geist/Geist Mono via `next/font`.
- Gráficos em SVG próprio (curva Catmull-Rom → Bézier como no protótipo) para evitar dependência extra.
- Slot machine: componente dedicado com rolos em `translateY` + `transition` cúbica, luzes dirigidas por um `tick` de 140ms e sons via Web Audio API. O cliente chama a API primeiro e anima até o produto retornado.

## Risks / Trade-offs

- [Token em localStorage é suscetível a XSS] → CSP padrão do Next, nenhum `dangerouslySetInnerHTML`, validade curta; evoluir para cookie HttpOnly em mudança futura.
- [`@TenantId` + tenant "root" pode vazar dados se usado fora de auth] → root só é retornado quando não há usuário autenticado, e endpoints de domínio exigem autenticação; teste de integração cobre isolamento.
- [Hibernate 7/Spring Boot 4 são versões recentes] → manter configuração mínima e cobrir com testes de integração contra o Postgres do compose.
- [Redis indisponível] → lock vira no-op com log e o update condicional preserva a consistência; cache do dashboard faz fallback para cálculo direto.
- [Agregações do dashboard em tabelas grandes] → índices em `draws(tenant, drawn_at)` e `draws(tenant, event_id)`; cache de 60s.

## Migration Plan

- Primeira versão: `docker compose up -d`, Flyway aplica `V1__init.sql` no start do backend. Rollback = derrubar volume em ambiente de desenvolvimento. Não há dados legados.
