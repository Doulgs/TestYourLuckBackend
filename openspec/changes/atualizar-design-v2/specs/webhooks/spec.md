# Spec Delta

## Purpose

Envia os dados de leads e jogadas para sistemas externos (CRM, automação de marketing) por requisições HTTP configuráveis com variáveis, com teste e histórico de disparos.

## ADDED Requirements

### Requirement: Listagem de webhooks
A página Webhooks SHALL listar os webhooks do tenant com Nome (e "Último disparo às hh:mm:ss" ou "Ainda não disparado"), Gatilho, Endpoint (método colorido e URL), Eventos (chips ou "Nenhum evento"), Disparos (contador), Ativo (interruptor) e menu de ações (Testar disparo, Editar, Ativar/Desativar, Excluir), com busca por nome ou URL.

#### Scenario: Buscar webhook
- **WHEN** o usuário digita "crm"
- **THEN** apenas webhooks cujo nome ou URL contém "crm" são exibidos

#### Scenario: Cor do método
- **WHEN** um webhook usa o método POST
- **THEN** o selo do método é exibido em amarelo; GET em verde, PUT em azul, PATCH em roxo e DELETE em vermelho

### Requirement: Configuração de webhook
O sistema SHALL permitir criar e editar webhooks com Nome (obrigatório), Eventos vinculados, Gatilho ("Lead cadastrado" ou "Jogada concluída"), Método (POST, PUT, PATCH, GET, DELETE), URL (obrigatória), Headers, Query params e Body, em abas, e rodapé com o método e a URL final com os params. Novos webhooks MUST vir com o evento selecionado no sorteio, gatilho "Jogada concluída", header `Content-Type: application/json` e body de exemplo com nome, e-mail e prêmio.

#### Scenario: Salvar sem URL
- **WHEN** o usuário salva sem nome ou sem URL
- **THEN** é exibido "Informe nome e URL" e nada é salvo

#### Scenario: Prévia da URL
- **WHEN** a URL é "https://api.exemplo.com/v1/leads" e há o param "origem=totem"
- **THEN** o rodapé exibe "https://api.exemplo.com/v1/leads?origem=totem"

### Requirement: Variáveis de template
URL, valores de headers e params e o body SHALL aceitar variáveis `{{chave}}` dos grupos Lead (lead.nome, lead.email, lead.telefone, lead.cpf), Campos personalizados (`campo.<chave>` ou `<chave>`), Evento (evento.id, evento.nome, evento.local), Prêmio (premio.nome, premio.sku, premio.categoria) e Jogo (jogo.estilo, jogo.data_hora em ISO-8601). Variáveis sem valor MUST ser substituídas por texto vazio e aspas no valor MUST ser escapadas.

#### Scenario: Substituição no body
- **WHEN** o body contém `"nome": "{{lead.nome}}"` e o lead se chama Ana
- **THEN** a requisição enviada contém `"nome": "Ana"`

#### Scenario: Gatilho de lead sem prêmio
- **WHEN** um webhook de "Lead cadastrado" usa `{{premio.nome}}`
- **THEN** a variável é enviada vazia

### Requirement: Autocomplete de variáveis
Ao digitar `@` no body ou em um valor de header/param, o editor SHALL abrir uma lista filtrável das variáveis com rótulo, grupo e token, navegável por setas, confirmada com Enter ou Tab e fechada com Esc; ao escolher, o `@termo` MUST ser substituído pelo token `{{chave}}`.

#### Scenario: Inserir variável
- **WHEN** o usuário digita "@email" no body e pressiona Enter
- **THEN** o texto "@email" é substituído por "{{lead.email}}"

#### Scenario: Nenhuma variável
- **WHEN** o termo digitado após `@` não corresponde a nenhuma variável
- **THEN** a lista exibe "Nenhuma variável encontrada"

### Requirement: Importar cURL
O botão "Colar cURL" SHALL abrir um modal onde o usuário cola um comando cURL; ao importar, o sistema MUST preencher método, URL, query params, headers e body (JSON formatado quando válido), assumindo POST quando houver body sem método.

#### Scenario: Importação completa
- **WHEN** o usuário importa `curl -X PUT 'https://api.x.com/v1/leads?src=tyl' -H 'Authorization: Bearer abc' -d '{"a":1}'`
- **THEN** o método é PUT, a URL é "https://api.x.com/v1/leads", o param é "src=tyl", o header é "Authorization: Bearer abc" e o body é o JSON formatado

#### Scenario: cURL sem URL
- **WHEN** o texto colado não contém URL
- **THEN** é exibido o toast "Não encontrei uma URL no cURL" e nada muda

### Requirement: Disparo automático
O servidor SHALL disparar, de forma assíncrona e sem atrasar o jogo, cada webhook ativo vinculado ao evento cujo gatilho corresponda: "Lead cadastrado" ao concluir o cadastro e "Jogada concluída" a cada jogada. Cada disparo MUST registrar webhook, lead, jogada (quando houver), horário, request resolvido (método, URL, headers, body), status HTTP ou erro e resposta resumida, e incrementar o contador do webhook.

#### Scenario: Jogada concluída
- **WHEN** ocorre uma jogada no evento "Expo Tech 2026" e há um webhook ativo de "Jogada concluída" vinculado a ele
- **THEN** a requisição é enviada, o disparo é registrado e o contador do webhook aumenta em 1

#### Scenario: Webhook inativo ou de outro evento
- **WHEN** o webhook está inativo ou não vinculado ao evento da jogada
- **THEN** nenhuma requisição é enviada

#### Scenario: Erro no destino
- **WHEN** o destino responde 500 ou não responde em 10 segundos
- **THEN** o disparo é registrado como erro com a mensagem correspondente, sem afetar a jogada

### Requirement: Segurança do destino
O servidor SHALL aceitar apenas URLs `https` e MUST recusar o envio a hosts que resolvam para endereços de loopback, rede privada ou link-local, registrando o disparo como erro "URL inválida ou não permitida". Valores de headers MUST ser exibidos como cadastrados apenas para usuários do próprio tenant.

#### Scenario: URL sem HTTPS
- **WHEN** um webhook com URL "http://api.exemplo.com" é disparado
- **THEN** o disparo é registrado como erro "URL inválida ou não permitida" e nada é enviado

#### Scenario: Host interno
- **WHEN** a URL aponta para "https://127.0.0.1/hook"
- **THEN** o disparo é registrado como erro e nada é enviado

### Requirement: Testar disparo
A ação "Testar disparo" SHALL enviar imediatamente o webhook com dados de exemplo (lead "Maria Teste", primeiro produto do catálogo, evento selecionado e "(rótulo)" para campos personalizados), registrar o disparo como teste e exibir toast com o resultado.

#### Scenario: Teste bem-sucedido
- **WHEN** o usuário testa o webhook "CRM — novo lead" e o destino responde 200
- **THEN** é exibido o toast "Webhook "CRM — novo lead" disparado" e o disparo aparece em "Últimos disparos"

### Requirement: Últimos disparos
A página Webhooks SHALL exibir o quadro "Últimos disparos" com os 8 disparos mais recentes do tenant (status, nome, método, URL sem protocolo, evento ou "teste", horário), ou "Nenhum disparo ainda. Faça uma jogada ou use "Testar"." quando vazio.

#### Scenario: Lista vazia
- **WHEN** nenhum webhook foi disparado
- **THEN** é exibida a mensagem de lista vazia

### Requirement: Ativação e exclusão
O usuário SHALL poder ativar/desativar um webhook pelo interruptor ou pelo menu e excluí-lo após confirmação; o histórico de disparos dos leads MUST ser preservado após a exclusão.

#### Scenario: Excluir webhook com disparos
- **WHEN** o usuário exclui um webhook que já disparou
- **THEN** ele sai da listagem e os disparos continuam visíveis nos leads
