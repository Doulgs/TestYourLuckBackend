# Spec Delta

## Purpose

Disponibiliza os participantes cadastrados nos sorteios (leads) para consulta, filtragem e exportação, protegendo dados pessoais na interface.

## ADDED Requirements

### Requirement: Listagem de leads
A página Leads SHALL listar os participantes do tenant, do mais recente para o mais antigo, com Participante (iniciais, nome e e-mail), Telefone, CPF, Evento, Brinde (com indicador de cor) e Data (dd/mm hh:mm).

#### Scenario: Listar leads
- **WHEN** o usuário abre a página Leads
- **THEN** os leads são exibidos ordenados por data decrescente

### Requirement: Mascaramento de CPF
O CPF SHALL ser exibido parcialmente na listagem no formato `000.***.***-00`; campos ausentes MUST ser exibidos como "—".

#### Scenario: CPF mascarado
- **WHEN** o lead possui CPF "123.456.789-01"
- **THEN** a listagem exibe "123.***.***-01"

### Requirement: Busca e filtro
A listagem SHALL permitir buscar por nome ou e-mail (sem diferenciar maiúsculas) ou por dígitos do CPF, e filtrar por evento ("Todos os eventos" por padrão).

#### Scenario: Buscar por CPF
- **WHEN** o usuário digita "45678"
- **THEN** são exibidos leads cujo CPF contém esses dígitos

#### Scenario: Filtrar por evento
- **WHEN** o usuário seleciona "Summit Irrah"
- **THEN** apenas leads desse evento são exibidos

### Requirement: Exportação CSV
O botão "Exportar CSV" SHALL baixar o arquivo `leads-testyourluck.csv` com os leads do filtro atual, separador `;`, valores entre aspas, BOM UTF-8 e colunas Nome, E-mail, Telefone, CPF, Evento, Brinde, Data, Hora, com CPF completo.

#### Scenario: Exportar filtrado
- **WHEN** o usuário filtra por um evento e clica "Exportar CSV"
- **THEN** o arquivo contém apenas os leads desse evento com o cabeçalho especificado

### Requirement: Paginação da API
A API de leads SHALL retornar resultados paginados (padrão 50, máximo 200 por página) aceitando os filtros de busca e evento.

#### Scenario: Página solicitada
- **WHEN** o cliente solicita a página 2 com tamanho 50
- **THEN** a API retorna até 50 leads e o total de registros
