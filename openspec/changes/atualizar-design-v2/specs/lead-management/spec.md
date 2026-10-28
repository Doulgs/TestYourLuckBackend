# Spec Delta

## MODIFIED Requirements

### Requirement: Listagem de leads
A página Leads SHALL listar as jogadas do tenant, da mais recente para a mais antiga, com Participante (iniciais, nome e e-mail), Telefone, CPF, Evento, Brinde (com indicador de cor) e Data (dd/mm hh:mm), além da coluna Webhook com o status dos disparos daquele lead.

#### Scenario: Listar leads
- **WHEN** o usuário abre a página Leads
- **THEN** os leads são exibidos ordenados por data decrescente

#### Scenario: Participante com vários créditos
- **WHEN** um participante cadastrado com 3 créditos joga 3 vezes
- **THEN** a listagem exibe 3 linhas, uma por jogada, com os mesmos dados do participante

### Requirement: Exportação CSV
O botão "Exportar CSV" SHALL baixar o arquivo `leads-testyourluck.csv` com os leads do filtro atual, separador `;`, valores entre aspas, BOM UTF-8 e colunas Nome, E-mail, Telefone, CPF, uma coluna por campo personalizado (com o rótulo do campo), Evento, Brinde, Data, Hora, com CPF completo.

#### Scenario: Exportar filtrado
- **WHEN** o usuário filtra por um evento e clica "Exportar CSV"
- **THEN** o arquivo contém apenas os leads desse evento com o cabeçalho especificado

#### Scenario: Campos personalizados no CSV
- **WHEN** existem os campos "Empresa" e "Cargo" e um lead respondeu "Irrah" e "Gerente"
- **THEN** o cabeçalho contém "Empresa" e "Cargo" após "CPF" e a linha do lead contém "Irrah" e "Gerente"

## ADDED Requirements

### Requirement: Status de webhook do lead
A coluna Webhook SHALL exibir um ícone cinza quando nenhum webhook foi disparado para o lead, verde quando todos os disparos tiveram sucesso e vermelho quando algum disparo falhou; o ícone MUST ter dica com "Nenhum webhook disparado", "Webhook disparado" ou "Erro no disparo".

#### Scenario: Disparo com erro
- **WHEN** um dos webhooks do lead respondeu com status 500
- **THEN** o ícone fica vermelho com a dica "Erro no disparo"

### Requirement: Detalhes dos disparos do lead
Ao clicar no ícone de webhook, o sistema SHALL abrir o modal "Disparos de webhook" com nome, evento e data do lead e, para cada disparo (do cadastro e da jogada), status, nome do webhook, método, URL, horário, botão "Copiar" e o JSON com webhook, envio, request (método, URL, headers, body) e response.

#### Scenario: Copiar JSON
- **WHEN** o usuário clica "Copiar" em um disparo
- **THEN** o JSON é copiado para a área de transferência e é exibido o toast "JSON copiado"

#### Scenario: Nenhum disparo
- **WHEN** o lead não teve disparos
- **THEN** o modal exibe "Nenhum webhook foi disparado para este lead" e "Não havia webhook ativo vinculado ao evento no momento da jogada."
