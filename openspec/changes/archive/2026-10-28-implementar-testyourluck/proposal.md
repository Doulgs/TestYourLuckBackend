# Proposal

## Why

O TestYourLuck é um novo produto para empresas sortearem brindes em eventos por meio de uma "slot machine" em modo totem, capturando leads (nome, e-mail, telefone e CPF) e controlando o estoque de brindes por evento. Hoje existe apenas o protótipo navegável (`design/TestYourLuck.dc.html`) com dados simulados no navegador; é preciso transformá-lo em uma aplicação real, multitenant, com backend persistente e regras de sorteio confiáveis no servidor.

## What Changes

- Criação da API REST (Java 21 + Spring Boot 4 + Lombok) com PostgreSQL 15 (Flyway) e Redis 7, executando infraestrutura via `docker-compose.yml` na raiz.
- Modelo multitenant: toda tabela possui `id`, `tenant`, `tenant_owner`, `created_at` e `updated_at`; o tenant é resolvido a partir do JWT e aplicado automaticamente em leituras e escritas.
- Cadastro de conta (cria o tenant da empresa e o usuário proprietário) e login com e-mail/senha emitindo JWT. Botão "Entrar com Google" apenas visual (fora de escopo).
- Layout autenticado com sidebar recolhível (responsiva), breadcrumb e menu do usuário (Conta, Notificações, Sair).
- CRUD de Eventos (nome, data, local, status Ativo/Rascunho/Encerrado) com busca.
- CRUD de Categorias (nome, descrição) com indicadores agregados (produtos, itens, estoque, chance somada).
- CRUD de Produtos (nome, SKU, categoria, peso no sorteio, estoque mínimo, sigla e matiz de cor) com cálculo de chance (`peso ÷ soma dos pesos`).
- Gestão de Estoque por evento (quantidade por produto, incremento/decremento, status OK/Baixo/Esgotado, totais).
- Sorteio em modo totem: formulário do participante com máscaras e validação, slot machine com 3 rolos, alavanca, luzes e som; sorteio ponderado executado no servidor apenas entre produtos com peso > 0 e estoque > 0 no evento, com baixa de estoque transacional e lock distribuído no Redis; modal de resultado.
- Leads: listagem com busca (nome, e-mail, CPF), filtro por evento, CPF mascarado e exportação CSV.
- Dashboard: KPIs, gráfico de sorteios por período (7/30/90 dias) para os dois principais eventos, novos leads por período, histórico recente, estoque baixo, mais sorteados e eventos ativos; agregados em cache no Redis.
- Frontend Next.js 16 (App Router) + React 19 + Tailwind 4 + shadcn/ui + Zustand + react-hook-form + Zod, fiel ao design (tema escuro, cor de destaque `oklch(0.72 0.2 340)`, fontes Geist/Geist Mono).

## Capabilities

### New Capabilities
- `multitenancy`: isolamento de dados por tenant, colunas obrigatórias de auditoria/tenant em todas as tabelas e resolução do tenant a partir do token.
- `user-auth`: cadastro de conta/empresa, login com e-mail e senha, emissão e validação de JWT, logout e dados do usuário corrente.
- `app-shell`: layout autenticado (sidebar recolhível e responsiva, navegação, breadcrumb, menu do usuário) e proteção de rotas no frontend.
- `event-management`: cadastro, edição, exclusão, busca e status de eventos.
- `product-catalog`: cadastro de categorias e produtos, pesos de sorteio, cálculo de chance e indicadores por categoria.
- `event-stock`: estoque de cada produto por evento, ajustes de quantidade e status de nível de estoque.
- `prize-draw`: fluxo do totem (dados do participante), sorteio ponderado no servidor, baixa de estoque, registro do resultado e experiência da slot machine.
- `lead-management`: listagem, busca, filtro por evento, mascaramento de CPF e exportação CSV dos participantes sorteados.
- `dashboard-insights`: KPIs, séries temporais de sorteios e leads, listas de resumo e cache dos agregados.

### Modified Capabilities
<!-- Nenhuma: projeto novo, sem specs existentes. -->

## Impact

- **Código novo**: `backend/` (API Spring Boot, migrations Flyway, segurança JWT, Redis) e `frontend/` (Next.js, componentes shadcn/ui, stores Zustand, formulários RHF + Zod).
- **APIs**: novos endpoints REST sob `/api` (`/auth`, `/events`, `/categories`, `/products`, `/events/{id}/stock`, `/draws`, `/leads`, `/dashboard`).
- **Dependências**: jjwt, springdoc-openapi, Flyway, Spring Data Redis, Spring Security; no frontend, zustand, react-hook-form, @hookform/resolvers, zod, tailwind-merge, shadcn/ui (radix), lucide-react, sonner.
- **Infra**: containers `postgres:15` e `redis:7` via Docker Compose.
- **Fora de escopo**: login com Google, recuperação de senha ("Esqueceu sua senha?" apenas visual), telas de Conta e Notificações (itens do menu sem destino funcional), upload de foto do produto (placeholder visual), convite de múltiplos usuários por tenant.
