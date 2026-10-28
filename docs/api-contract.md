# Contrato da API — TestYourLuck

Base: `http://localhost:9090/api` (env `NEXT_PUBLIC_API_URL`). JSON camelCase. Autenticação: `Authorization: Bearer <token>` em tudo exceto `/auth/*` e `GET /media/{id}`.
Datas `YYYY-MM-DD`; horas `HH:mm:ss` (aceita `HH:mm` na entrada); instantes ISO-8601 UTC. Percentuais são números (`25.0`). Telefone e CPF trafegam **somente dígitos**.

## Erros
`{ "status": 409, "message": "SKU já cadastrado", "errors": { "campo": "mensagem" } }` (`errors` opcional, em 400).
400 validação · 401 token · 404 não encontrado/outro tenant · 409 conflito · 413 imagem > 5 MB · 415 formato · 422 regra de negócio.

## Auth (inalterado)
`POST /auth/signup` · `POST /auth/login` → `{ token, user }` · `GET /me` → `User = { id, name, email, company, initials }`

## Mídia
- `POST /media` (multipart, campo `file`; PNG/JPG/WEBP/SVG até 5 MB) → 201 `{ id, url, contentType, size }`
- `GET /media/{id}` → bytes (público)

## Eventos
- `GET /events` → `Event[]` · `POST /events` · `PUT /events/{id}` (`EventRequest`) → `Event` · `DELETE /events/{id}` → 204
- `EventRequest = { name, status, startDate, endDate, allDay, startTime, endTime, allowOutside, place, city, uf, region, cep, timezone, maxDraws, website, bannerMediaId }` (todos opcionais exceto `name` e `status`; 422 "A data de fim deve ser igual ou posterior ao início")
- `Event = EventRequest + { id, bannerUrl, draws }` com `status: "ATIVO"|"RASCUNHO"|"ENCERRADO"`, `allDay` padrão true, `allowOutside` padrão false, `timezone` padrão `America/Sao_Paulo`, `maxDraws` 0 = ilimitado.

## Categorias (inalterado; `stock` = soma das quantidades dos produtos)
`GET/POST/PUT/DELETE /categories` · `Category = { id, name, description, productCount, productNames, stock, chance }`

## Produtos
- `GET /products` → `Product[]` · `POST /products` · `PUT /products/{id}` · `DELETE /products/{id}`
- `ProductRequest = { name, sku, categoryId|null, weight (0–100), minStock, quantity, short?, eventIds?: string[], imageMediaId|null }` (sem `eventIds` na criação = todos os eventos; no PUT, ausente = mantém vínculos)
- `Product = { id, name, short, sku, categoryId, categoryName, weight, minStock, hue, chance, quantity, stockStatus: "OK"|"BAIXO"|"ESGOTADO", eventIds: string[], imageMediaId, imageUrl, drawn }`

## Campos personalizados
- `GET/POST /custom-fields` · `PUT/DELETE /custom-fields/{id}`
- `CustomFieldRequest = { label, key?, type: "TEXT"|"NUMBER"|"DATE"|"SELECT"|"BOOL", options?: string[], placeholder?, required?, active? }` (409 "Chave já utilizada", 400 "Chave reservada" / "Informe ao menos uma opção")
- `CustomField = { id, label, key, token, type, options, placeholder, required, active }`

## Configuração da máquina
- `GET /draw-settings` (padrões se nunca salvo) · `PUT /draw-settings`
- `DrawSettings = { style: "SLOT"|"WHEEL"|"SLIDER"|"BLOCKS", size: "P"|"M"|"G"|"F", credits (1–5), primaryColor "#RRGGBB", background: "PRETO"|"GRAFITE"|"COR", logoMode: "DEFAULT"|"CUSTOM", logoMediaId, logoUrl (só resposta), title, winMessage, askEmail, askPhone, askCpf, lights, sound, showPrizes, customFieldIds: string[] }`

## Participantes e sorteio
- `POST /participants { eventId, name, email?, phone?, cpf?, answers?: { [key]: string|boolean } }` → 201 `{ id, name, credits, creditsRemaining }`
  - 400: "Informe seu nome." · "Informe um e-mail válido." · "Informe um telefone válido." · "Informe um CPF válido." · "Preencha: <rótulo>" · "Opção inválida para <rótulo>"
  - 422: "Evento não está ativo" · "Limite de sorteios atingido" · "Fora do período do evento"
- `GET /events/{eventId}/draw-pool` → `{ eventId, eventName, eventStatus, draws, block: null|"INACTIVE"|"LIMIT"|"OUTSIDE_PERIOD", blockMessage, items: { productId, name, short, hue, imageUrl, weight, quantity, chance }[] }`
- `POST /draws { participantId }` → 201 `{ id, eventId, eventName, participantId, participantName, drawnAt, product: { id, name, short, hue, imageUrl }, eventDraws, poolCount, creditsRemaining }`
  - 422: "Sem créditos disponíveis" · "Sem estoque disponível neste evento." · mensagens de bloqueio

## Webhooks
- `GET/POST /webhooks` · `PUT/DELETE /webhooks/{id}`
- `WebhookRequest = { name, trigger: "LEAD"|"PLAY", method: "GET"|"POST"|"PUT"|"PATCH"|"DELETE", url, headers: {k,v}[], params: {k,v}[], body, eventIds: string[], active }`
- `Webhook = WebhookRequest + { id, dispatchCount, lastDispatchAt }`
- `POST /webhooks/{id}/test { eventId? }` → `DeliverySummary`
- `GET /webhook-deliveries?limit=8` → `DeliverySummary[]`
- `DeliverySummary = { id, webhookName, ok, status ("200 OK"|"500 ERRO"|"ERRO"), method, url, eventName, test, sentAt }`
- Variáveis: `lead.nome lead.email lead.telefone lead.cpf`, `campo.<key>` e `<key>`, `evento.id evento.nome evento.local`, `premio.nome premio.sku premio.categoria`, `jogo.estilo jogo.data_hora`

## Leads
- `GET /leads?q=&eventId=&page=0&size=50` → `Page<Lead>`
- `Lead = { id, participantId, name, email, phone, cpf, answers: {[key]: string}, eventId, eventName, productId, productName, productShort, productHue, drawnAt, webhookStatus: "NONE"|"OK"|"ERROR" }`
- `GET /leads/{drawId}/deliveries` → `{ id, webhookName, trigger, ok, status, method, url, sentAt, request: { method, url, headers, body }, response: { status?, body?, error? } }[]`
- `GET /leads/export?q=&eventId=` → CSV (Nome;E-mail;Telefone;CPF;<campos personalizados>;Evento;Brinde;Data;Hora)
- `Page<T> = { content, page, size, totalElements, totalPages }`

## Dashboard
`GET /dashboard?range=7|30|90` — igual à v1, exceto: `kpis.stock.value` = soma das quantidades dos produtos; `lowStock: { productName, detail ("Eletrônicos · todos os eventos"), quantity, minStock }[]`.
