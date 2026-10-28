# Tasks

## 1. Backend — migração e mídia

- [x] 1.1 Criar `V2__design_v2.sql` (novas colunas de `events`/`products`; tabelas `product_events`, `participants`, `custom_fields`, `draw_settings`, `webhooks`, `webhook_deliveries`, `media_files` com as 5 colunas obrigatórias; backfill de quantidade/vínculos e de participantes; remoção de `event_stocks` e das colunas `participant_*` de `draws`); verificar com o teste de colunas obrigatórias do `MultitenancyIT` e aplicando a migration no banco de desenvolvimento com dados da v1
- [x] 1.2 Implementar `media` (`MediaFile`, `MediaStorage` em disco, `POST /api/media` com limite de 5 MB e validação de tipo, `GET /api/media/{id}` público com cache/nosniff/CSP para SVG) e o resolvedor de mídia por tenant; verificar com `MediaIT` (envio válido, 413, 415, leitura, 404 e imagem de outro tenant)

## 2. Backend — eventos e produtos

- [x] 2.1 Atualizar `Event`/DTOs/serviço com período, horários, regra fora da data, endereço, timezone, limite, website e banner, validação de datas, e `EventRules` (inativo, limite, fora do período no timezone do evento); verificar com `EventStockIT` renomeado para `EventIT` e testes unitários de `EventRules`
- [x] 2.2 Atualizar produtos (quantidade, imagem, peso 0–100, eventos vinculados com padrão "todos", validação de evento do tenant), categorias (estoque = soma das quantidades) e remover o módulo `stock`; verificar com `CatalogIT` atualizado (vínculos, filtros por evento, status de estoque, peso > 100 recusado)

## 3. Backend — configuração, campos e sorteio

- [x] 3.1 Implementar `custom-fields` (CRUD, chave normalizada/única/reservada, opções obrigatórias para seleção, exclusão lógica) e `draw-settings` (GET com padrões, PUT com validação de cor, créditos 1–5, ids de campos do tenant); verificar com `CustomFieldIT` e `DrawSettingsIT` (incluindo isolamento entre tenants)
- [x] 3.2 Implementar `participants` (`POST /api/participants` validando campos habilitados, respostas por tipo e bloqueios do evento, gravando créditos) e o novo `POST /api/draws { participantId }` (consumo condicional de crédito, pool por produtos vinculados, baixa de estoque do produto, bloqueios) e `draw-pool` com `block`; verificar com `DrawIT` atualizado (créditos, sem créditos, não vinculado fora, limite, fora do período, concorrência no último item)

## 4. Backend — webhooks, leads e dashboard

- [x] 4.1 Implementar webhooks (CRUD, `TemplateRenderer`, `WebhookTargetGuard`, disparo assíncrono após commit para gatilhos LEAD/PLAY com registro de delivery e contador, teste de disparo, últimos disparos); verificar com `WebhookIT` usando servidor HTTP local (substituição de variáveis, gatilhos, inativo/outro evento, erro 500, URL http/loopback recusada) e testes unitários do renderer
- [x] 4.2 Atualizar leads (join com participantes, status de webhook por linha, `GET /api/leads/{drawId}/deliveries`, CSV com colunas dos campos personalizados) e dashboard (estoque dos produtos, estoque baixo com categoria/eventos); verificar com `LeadIT` e `DashboardIT` atualizados
- [x] 4.3 Atualizar `docs/api-contract.md` e `backend/README.md`, e executar `./mvnw verify` com todos os testes verdes

## 5. Frontend — fundação visual

- [x] 5.1 Atualizar tema para `#F36ACB` com texto branco, criar `ModalShell`, `RowMenu`, `Toggle`, `ImageDrop`, `EventMultiSelect`, tipos/validações/cliente de mídia e posicionar toasts no canto inferior direito; verificar com `npx tsc --noEmit` e `npm run lint`
- [x] 5.2 Atualizar navegação (Sorteios, Campos personalizados, Integrações › Webhooks), breadcrumbs, redirecionamentos de `/estoque` e `/sorteio` e remover a página Estoque; verificar navegação no navegador

## 6. Frontend — cadastros

- [x] 6.1 Refazer Eventos (listagem com banner/período/local/sorteios x limite/regra e menu "…"; modal com banner, período, dia todo, horários, regra, endereço com máscara de CEP e região pela UF, timezone, limite com dica, website); verificar criação/edição no navegador contra a API
- [x] 6.2 Refazer Produtos (imagem, eventos vinculados, slider de peso com chance estimada, quantidade e mínimo; listagem com filtros por categoria e evento, coluna Eventos e status de estoque) e Categorias (novo modal e menu); verificar no navegador
- [x] 6.3 Implementar Campos personalizados (listagem com interruptor e menu; modal com chave automática, tipos, opções e obrigatório); verificar no navegador

## 7. Frontend — sorteios

- [x] 7.1 Implementar a tela de configuração `/sorteios` (evento, estilo, brand kit com upload de logo e cor livre, máquina e efeitos, participação com créditos/campos/perguntas) com salvamento automático e pré-visualização; verificar persistência após recarregar
- [x] 7.2 Implementar o motor `useGameSession`, `MachineCabinet`, `LeadForm` e os estilos caça-níquel, roleta, slider e blocos, o resultado com "Jogar novamente (n)" e o modo de jogo `/sorteios/jogar` com som; verificar uma jogada completa em cada estilo, créditos > 1 e bloqueios exibidos no painel

## 8. Frontend — integrações e leads

- [x] 8.1 Implementar Webhooks (listagem com interruptor/contador/menu, modal com abas Headers/Params/Body, `TemplateEditor` com autocomplete `@`, importação de cURL, teste de disparo e "Últimos disparos"); verificar no navegador com um destino de teste
- [x] 8.2 Atualizar Leads (coluna Webhook, modal de disparos com copiar JSON) e Dashboard (estoque baixo baseado em produtos, link "Produtos →"); verificar no navegador
- [x] 8.3 Executar `npm run lint`, `npx tsc --noEmit` e `npm run build` sem erros e atualizar `frontend/README.md`

## 9. Integração

- [x] 9.1 Com compose, backend e frontend rodando: configurar a máquina, criar campo personalizado e webhook, cadastrar participante com 2 créditos, jogar duas vezes, conferir estoque, leads (status do webhook e modal) e dashboard, e confirmar o isolamento com um segundo tenant

## Workflow follow-up

- Arquivar a change com `/opsx:archive` após revisão.
