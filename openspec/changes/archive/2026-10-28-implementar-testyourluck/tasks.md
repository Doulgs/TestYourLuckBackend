# Tasks

## 1. Backend — fundação multitenant

- [x] 1.1 Criar `V1__init.sql` com `tenants`, `users`, `events`, `categories`, `products`, `event_stocks` e `draws`, todas com `id`, `tenant`, `tenant_owner`, `created_at`, `updated_at` (não nulos), índices e unicidades por tenant; verificar que o Flyway aplica a migration ao subir o backend contra o Postgres do compose
- [x] 1.2 Implementar `BaseEntity` (`@TenantId`, `tenantOwner`, timestamps), `TenantContext` e `CurrentTenantIdentifierResolver` com tenant root; verificar com teste de integração que inserts recebem tenant/tenant_owner do contexto e ignoram valor enviado
- [x] 1.3 Implementar tratamento global de erros (`{ status, message, errors }`) e exceções de domínio (NotFound 404, Conflict 409, BusinessRule 422); verificar com teste de controller
- [x] 1.4 Configurar CORS por `app.cors.allowed-origins` e springdoc em `/swagger-ui.html`; verificar que `/actuator/health` e Swagger respondem sem token

## 2. Backend — autenticação

- [x] 2.1 Implementar entidades `Tenant`/`User`, `JwtService` (HS256, claims tenant/tenantOwner/name/email), filtro JWT que popula `SecurityContext` e `TenantContext`, e `SecurityFilterChain` stateless; verificar que endpoint protegido sem token retorna 401
- [x] 2.2 Implementar `POST /api/auth/signup` (cria tenant + usuário proprietário, BCrypt, 409 para e-mail duplicado), `POST /api/auth/login` (401 genérico) e `GET /api/me`; verificar com testes de integração de cadastro, login válido/inválido e e-mail duplicado

## 3. Backend — eventos, catálogo e estoque

- [x] 3.1 Implementar CRUD `/api/events` com status (ATIVO/RASCUNHO/ENCERRADO), contagem de sorteios na listagem e exclusão lógica; verificar com teste de integração incluindo isolamento entre dois tenants (404 em acesso cruzado)
- [x] 3.2 Implementar CRUD `/api/categories` com indicadores (itens, nomes, estoque, chance somada), unicidade de nome (409) e recusa de exclusão com produtos (422); verificar com testes de integração
- [x] 3.3 Implementar CRUD `/api/products` com validação (peso/mínimo ≥ 0), sigla gerada, matiz pela sequência do design, SKU único por tenant (409), estoque total e exclusão lógica; verificar com testes de integração (incluindo mesmo SKU em tenants distintos)
- [x] 3.4 Implementar `GET /api/events/{id}/stock` (todos os produtos ativos, quantidade 0 quando ausente, sorteados no evento, status OK/Baixo/Esgotado) e `PUT /api/events/{id}/stock/{productId}` (quantidade ≥ 0); verificar com testes de integração das regras de status

## 4. Backend — sorteio, leads e dashboard

- [x] 4.1 Implementar `RedisLock` (SET NX PX + liberação compare-and-delete, fallback sem Redis) e `DrawService` (elegíveis com peso>0 e estoque>0, sorteio ponderado com `SecureRandom`, update condicional, snapshot de nomes, 422 sem estoque/evento inativo) em `POST /api/draws`; verificar com testes de integração de baixa de estoque, exclusão de produto sem estoque e concorrência no último item (threads paralelas, quantidade final 0)
- [x] 4.2 Implementar `GET /api/leads` paginado (q por nome/e-mail/dígitos do CPF, eventId, padrão 50, máx. 200) e `GET /api/leads/export` (CSV `;`, aspas, BOM, colunas Nome…Hora); verificar com testes de integração de filtro e formato do CSV
- [x] 4.3 Implementar `GET /api/dashboard?range=7|30|90` (KPIs com variação vs. período anterior, buckets 7×1/10×3/13×7, top 2 eventos, leads, histórico, estoque baixo de eventos ativos, mais sorteados, eventos ativos) com cache Redis 60s e invalidação nas escritas; verificar com teste de integração que um sorteio altera o total após invalidação
- [x] 4.4 Executar `./mvnw verify` e garantir todos os testes verdes; documentar endpoints e variáveis em `backend/README.md`

## 5. Frontend — fundação

- [x] 5.1 Configurar tema escuro em `globals.css` (paleta do design, destaque `oklch(0.72 0.2 340)`), fontes Geist/Geist Mono, metadata pt-BR, `Toaster` (sonner) e ajustar componentes shadcn; verificar com `npm run build`
- [x] 5.2 Criar `lib/api.ts` (fetch com Authorization, normalização de erros, logout em 401), tipos de domínio, utilitários (máscaras CPF/telefone, formatação de data, cor por matiz, sigla) e schemas zod; verificar com `npx tsc --noEmit` e `npm run lint`
- [x] 5.3 Criar stores zustand (`auth` persistido, `ui` com sidebar/som, `totem` com evento selecionado) e hooks de dados; verificar com `npx tsc --noEmit`

## 6. Frontend — autenticação e layout

- [x] 6.1 Implementar telas `(auth)/login` e `(auth)/cadastro` fiéis ao design com RHF + zod, erros por campo, botão Google com toast "em breve" e redirecionamento ao Dashboard; verificar manualmente cadastro e login contra a API local
- [x] 6.2 Implementar layout `(app)` com guarda de sessão, sidebar recolhível/responsiva (248/68px, overlay < 860px), grupos de navegação com ícones lucide, breadcrumb, menu do usuário (Conta, Notificações, Sair) e modo totem sem sidebar em `/sorteio`; verificar navegação e logout no navegador

## 7. Frontend — cadastros

- [x] 7.1 Implementar página Eventos (tabela, busca, badges de status, modal RHF+zod criar/editar, exclusão com confirmação, Sortear → `/sorteio?evento=`); verificar CRUD contra a API
- [x] 7.2 Implementar página Categorias (tabela com indicadores, busca, modal, exclusão com mensagem da API); verificar CRUD contra a API
- [x] 7.3 Implementar página Produtos (tabela com sigla colorida, chance com barra, estoque total, busca, modal com placeholder de foto e "Chance estimada", nota explicativa); verificar CRUD e cálculo de chance
- [x] 7.4 Implementar página Estoque (seletor de evento, −/+/digitação persistindo, status, totais); verificar ajustes e status no navegador

## 8. Frontend — sorteio, leads e dashboard

- [x] 8.1 Implementar página Sorteio: barra superior (← Painel, seletor de evento, som), slot machine (3 rolos, luzes, alavanca, painel de status, indicadores, botão GIRAR), formulário do participante com máscaras/validação, chamada a `POST /api/draws`, animação até o brinde retornado, sons Web Audio, modal de resultado e "Próximo participante"; verificar um giro completo e a baixa de estoque
- [x] 8.2 Implementar página Leads (tabela, CPF mascarado, busca debounced, filtro por evento, Exportar CSV via `/api/leads/export`); verificar busca, filtro e download
- [x] 8.3 Implementar Dashboard (seletor de período, 3 KPIs, gráfico de área SVG, barras de leads, 4 listas com links e atalho Sortear, botão Abrir sorteio); verificar troca de período e links
- [x] 8.4 Executar `npm run lint`, `npx tsc --noEmit` e `npm run build` sem erros; atualizar `frontend/README.md` com scripts e variáveis

## 9. Integração

- [x] 9.1 Subir `docker compose up -d`, backend e frontend; executar fluxo ponta a ponta (cadastro → categoria → produto → evento ativo → estoque → sorteio → lead → dashboard) e confirmar isolamento com um segundo tenant

## Workflow follow-up

- Arquivar a change com `/opsx:archive` após revisão.
