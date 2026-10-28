# Design

## Context

- Estado atual (change `implementar-testyourluck`, arquivada): backend Spring Boot 4 com pacotes por domínio (`auth`, `event`, `catalog`, `stock`, `draw`, `lead`, `dashboard`), multitenancy por `@TenantId` em `BaseEntity`, sorteio com lock Redis por evento + `UPDATE ... WHERE quantity > 0`, Flyway `V1__init.sql`, 27 testes `*IT` contra o Postgres/Redis do compose. Frontend Next.js 16 com páginas client em `(app)/*`, componentes em `components/<domínio>`, `useResource` para dados e stores zustand.
- O protótipo `design/TestYourLuck.dc.html` (revisão 28/10) é a referência visual e de microcopy; regras estão nas specs desta change. Motivação: ver `proposal.md`.
- Dados existentes no banco de desenvolvimento precisam ser migrados (estoque por evento → por produto; dados do participante em `draws` → `participants`).

## Goals / Non-Goals

**Goals:**
- Créditos, prêmio, estoque e bloqueios do evento decididos no servidor; o cliente apenas anima.
- Webhooks reais, assíncronos, auditáveis e protegidos contra SSRF.
- Migração sem perda dos dados atuais e com as 5 colunas obrigatórias em toda tabela nova.

**Non-Goals:**
- Configuração da máquina diferente por evento (é uma por tenant, como no protótipo).
- Retentativa automática ou reenvio manual de webhooks.
- Armazenamento de mídia em nuvem/CDN e processamento de imagens (redimensionamento).
- Busca de endereço por CEP.

## Decisions

### D1. Estoque por produto e vínculo produto ↔ evento
- `products` ganha `quantity int ≥ 0` e `image_media_id`; nova tabela `product_events (product_id, event_id)` com as 5 colunas e unicidade `(tenant, product_id, event_id)`.
- `V2` migra: `quantity = Σ event_stocks.quantity` por produto; cria `product_events` para cada linha de `event_stocks` em evento não excluído; produtos sem estoque em nenhum evento ficam vinculados a todos os eventos (mesmo padrão do protótipo). Depois `DROP TABLE event_stocks`.
- O pool elegível passa a ser `products p JOIN product_events pe` com `p.weight > 0 AND p.quantity > 0 AND pe.event_id = :eventId`; baixa com `UPDATE products SET quantity = quantity - 1 WHERE id = ? AND quantity > 0`.
- Alternativa descartada: manter `event_stocks` e somar na leitura — contradiz o design (quantidade única editável no produto) e mantém a tela Estoque viva.

### D2. Participantes e créditos controlados pelo servidor
- Nova tabela `participants` (evento, nome, e-mail, telefone, CPF opcionais conforme configuração, `answers jsonb`, `credits_granted`, `credits_used`). `POST /api/participants` valida campos conforme `draw_settings` e `custom_fields`, checa bloqueios do evento, grava com `credits_granted = settings.credits` e dispara webhooks "Lead cadastrado".
- `POST /api/draws { participantId }` (contrato novo): sob o lock Redis do evento e em transação, consome crédito com `UPDATE participants SET credits_used = credits_used + 1 WHERE id = ? AND credits_used < credits_granted` (0 linhas → 422 "Sem créditos disponíveis"), sorteia, baixa estoque, grava `draws` e agenda webhooks "Jogada concluída".
- `draws` passa a ter `participant_id`; as colunas `participant_*` saem de `draws` após o backfill (um participante por draw existente, com `credits_granted = credits_used = 1`). Snapshot de nomes de evento/produto continua em `draws` (já decidido na v1).
- Leads = `draws JOIN participants` (uma linha por jogada). Leads KPI continua contando CPFs distintos, agora de `participants`; participantes sem CPF contam pelo id.
- Estilo blocos: o crédito e o prêmio são definidos no início do tabuleiro (uma chamada a `/draws`); a revelação é só visual — evita expor o prêmio antes do consumo do crédito.

### D3. Bloqueios do evento
- `events` ganha `end_date`, `all_day`, `start_time`, `end_time`, `allow_outside`, `city`, `uf`, `region`, `cep`, `timezone`, `max_draws`, `website`, `banner_media_id` (renomeia `event_date` → `start_date`).
- `EventRules.check(event, drawCount, Clock)` centraliza: status ATIVO, limite (`max_draws > 0 && draws >= max_draws`) e janela no `ZoneId` do evento (00:00/23:59 quando dia todo ou sem horário). Usado no cadastro do participante e em cada jogada. A tela usa `GET /events/{id}/draw-pool`, que passa a devolver `block` (`null`, `LIMIT`, `OUTSIDE_PERIOD`, `INACTIVE`) para exibir o motivo.

### D4. Configuração da máquina por tenant
- Tabela `draw_settings` com unicidade em `tenant`; colunas tipadas (estilo, tamanho, créditos 1–5, cor `#RRGGBB`, fundo, `logo_mode`, `logo_media_id`, título, mensagem, `ask_email`, `ask_phone`, `ask_cpf`, `lights`, `sound`, `show_prizes`) e `custom_field_ids jsonb`.
- `GET /api/draw-settings` devolve a linha ou os padrões (sem gravar); `PUT` faz upsert. O frontend salva com debounce (≈600 ms) a cada alteração, sem botão "Salvar", como no protótipo.
- O formulário público do totem usa o mesmo endpoint autenticado: o totem roda logado na conta da empresa (mesmo modelo da v1).

### D5. Campos personalizados
- Tabela `custom_fields` (label, key, type, `options jsonb`, placeholder, required, active, position) com unicidade `(tenant, key)` parcial para não excluídos; exclusão lógica. Chaves reservadas: prefixos `lead`, `evento`, `premio`, `jogo`, `campo`.
- Respostas em `participants.answers` (`{ "<key>": "<valor>" }`, booleanos como `"sim"`/`"nao"`), preservadas mesmo após exclusão do campo. O CSV usa os campos atuais (não excluídos) como colunas.
- Mapeamento JSON com `@JdbcTypeCode(SqlTypes.JSON)` do Hibernate 7.

### D6. Webhooks
- Tabelas `webhooks` (nome, gatilho `LEAD|PLAY`, método, URL, `headers jsonb`, `params jsonb`, body, `event_ids jsonb`, ativo, `dispatch_count`, `last_dispatch_at`, exclusão lógica) e `webhook_deliveries` (webhook_id + nome snapshot, participant_id, draw_id, evento, `test`, `request jsonb`, `response jsonb`, `status_code`, `ok`, `error`, `sent_at`).
- Disparo: o serviço publica um evento de domínio; um `@TransactionalEventListener(AFTER_COMMIT)` submete a um executor dedicado (pool limitado, fila limitada). A thread do executor recebe tenant/owner explicitamente e preenche o `TenantContext` antes de gravar o delivery (o ThreadLocal não atravessa threads).
- Template: `TemplateRenderer` substitui `{{ chave }}` pelo mapa de variáveis (lead, campos `campo.<k>` e `<k>`, evento, prêmio, jogo); valores ausentes viram "" e `"` é escapado. Body enviado como texto (JSON quando válido, com `Content-Type` dos headers).
- HTTP: `java.net.http.HttpClient` com timeout de conexão e requisição de 10 s; resposta truncada a 2 KB.
- SSRF: `WebhookTargetGuard` exige `https`, resolve o host e recusa loopback, site-local, link-local, any-local, multicast e IPv6 ULA. Flag `app.webhooks.allow-insecure-targets` (padrão `false`) liberada apenas no perfil de teste para apontar a um servidor HTTP local.
- Status por lead calculado na consulta de leads: deliveries do `draw_id` + deliveries `LEAD` do `participant_id`; `none` | `ok` | `err`.
- Endpoints: CRUD `/api/webhooks`, `POST /api/webhooks/{id}/test`, `GET /api/webhook-deliveries?limit=8`, `GET /api/leads/{drawId}/deliveries`.
- Alternativa descartada: disparar dentro da transação da jogada — acoplaria a latência do destino ao jogo e faria rollback da jogada em caso de erro externo.

### D7. Mídia
- Tabela `media_files` (content_type, size, original_name, storage_key); arquivos em `${app.storage.dir:./storage}/<tenant>/<id>`. `POST /api/media` (multipart, 5 MB, PNG/JPG/WEBP/SVG validados por magic bytes + content-type) e `GET /api/media/{id}` público (id UUID aleatório), `Cache-Control: public, max-age=31536000, immutable`, `X-Content-Type-Options: nosniff` e, para SVG, `Content-Security-Policy: default-src 'none'; style-src 'unsafe-inline'`.
- Entidades guardam `*_media_id`; o serviço valida que a mídia pertence ao tenant (o filtro `@TenantId` torna mídia alheia "não encontrada"). As respostas trazem `imageUrl`/`bannerUrl`/`logoUrl` absolutos a partir do prefixo `/api/media/`.
- `spring.servlet.multipart.max-file-size=5MB`; volume `storage/` ignorado no git.

### D8. Frontend
- Rotas: `/sorteios` (configuração + pré-visualização, com sidebar) e `/sorteios/jogar` (modo de jogo sem sidebar). `/sorteio` e `/estoque` redirecionam para `/sorteios` e `/produtos`. Componentes `components/stock` removidos.
- Motor de jogo compartilhado pela pré-visualização e pelo modo de jogo: hook `useGameSession(eventId, settings)` (participante, créditos, estado da jogada, resultado) e componentes `SlotGame` (rolos atuais), `WheelGame` (conic-gradient + rotação 5 s), `SliderGame` (faixa 5,5 s), `BlocksGame` (12 blocos), `MachineCabinet` (logo, título, painel, indicadores, botão, luzes) e `LeadForm` (campos conforme configuração + perguntas).
- Tema: `--primary`/`--tyl-pink` = `#F36ACB`, `--primary-foreground` = branco. A cor da máquina é aplicada por variáveis CSS no container (`--machine-c`, `--machine-soft`, `--machine-glow`, `--machine-on` calculado por luminância).
- UI: `RowMenu` (DropdownMenu "…"), `ModalShell` (cabeçalho com ícone, corpo `#0b0b0b`, rótulos em caixa alta, rodapé com Cancelar/Salvar), `EventMultiSelect`, `ImageDrop`, `Toggle`, `KeyValueEditor` e `TemplateEditor` (textarea com numeração de linhas e autocomplete `@`).
- Toasts continuam com sonner, posicionados no canto inferior direito como no design.

### D9. Contrato
- `docs/api-contract.md` atualizado com eventos, produtos, participantes, sorteio, configuração, campos, webhooks, mídia e leads; remoção de `/events/{id}/stock`.

## Risks / Trade-offs

- [DNS rebinding entre a checagem e a conexão] → resolver o host uma vez e conectar ao IP validado não é suportado pelo `HttpClient` com SNI; mitigação parcial: revalidar o IP resolvido imediatamente antes do envio e documentar o risco residual.
- [Disparos perdidos se o processo cair antes de executar a fila] → aceitável nesta fase (sem retentativas no escopo); o delivery só existe após a tentativa, então a ausência é visível como "Nenhum webhook disparado".
- [Totem logado expõe o painel no dispositivo do evento] → mesmo risco da v1; o modo de jogo não mostra navegação. Evoluir para token de totem restrito em change futura.
- [Migração de `draws` em bases grandes] → V2 roda em transação única; volume atual é de desenvolvimento.
- [JSONB com Hibernate 7] → coberto por testes de integração de campos personalizados e webhooks.
- [Arquivos locais em `./storage`] → não escala horizontalmente; isolado em `MediaStorage` para trocar por objeto/nuvem depois.

## Migration Plan

1. `V2__design_v2.sql`: novas colunas em `events`/`products`, tabelas novas, backfill de `product_events`/`quantity`, criação de `participants` a partir de `draws`, remoção de `event_stocks` e das colunas `participant_*` de `draws`.
2. Deploy conjunto de backend e frontend (o contrato de `/draws` muda).
3. Rollback: restaurar backup do banco anterior ao V2 e a versão anterior das duas aplicações (não há migration reversa automática).
