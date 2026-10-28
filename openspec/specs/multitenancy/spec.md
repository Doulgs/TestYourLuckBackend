# multitenancy Specification

## Purpose
Garante que cada empresa (tenant) enxergue e altere somente os próprios dados, padronizando colunas de identificação, tenant e auditoria em todas as tabelas do sistema.

## Requirements

### Requirement: Colunas obrigatórias em todas as tabelas
Toda tabela persistida pelo sistema SHALL possuir as colunas `id` (identificador único), `tenant` (inquilino dono do registro), `tenant_owner` (proprietário do inquilino), `created_at` (data/hora de criação) e `updated_at` (data/hora da última atualização), todas não nulas.

#### Scenario: Tabela criada por migration
- **WHEN** qualquer migration de banco cria uma nova tabela
- **THEN** a tabela MUST conter `id`, `tenant`, `tenant_owner`, `created_at` e `updated_at` como colunas não nulas

#### Scenario: Registro inserido
- **WHEN** um registro é inserido em qualquer tabela
- **THEN** `created_at` e `updated_at` MUST ser preenchidos com o instante da inserção

#### Scenario: Registro atualizado
- **WHEN** um registro existente é alterado
- **THEN** `updated_at` MUST ser atualizado com o instante da alteração e `created_at` MUST permanecer inalterado

### Requirement: Resolução do tenant pela sessão autenticada
O sistema SHALL determinar o `tenant` e o `tenant_owner` de cada requisição autenticada exclusivamente a partir do token do usuário, ignorando qualquer valor de tenant enviado no corpo, query ou cabeçalhos da requisição.

#### Scenario: Tenant enviado pelo cliente é ignorado
- **WHEN** uma requisição autenticada envia um campo `tenant` diferente do tenant do token
- **THEN** o registro MUST ser gravado com o tenant do token

### Requirement: Isolamento de leitura e escrita entre tenants
Toda consulta, alteração e exclusão SHALL considerar apenas registros do tenant da requisição. Registros de outro tenant MUST ser tratados como inexistentes.

#### Scenario: Listagem isolada
- **WHEN** o usuário do tenant A lista eventos
- **THEN** a resposta MUST conter apenas eventos do tenant A

#### Scenario: Acesso direto a registro de outro tenant
- **WHEN** o usuário do tenant A solicita, altera ou exclui pelo id um registro do tenant B
- **THEN** o sistema MUST responder 404 (não encontrado) sem alterar o registro

#### Scenario: Unicidade por tenant
- **WHEN** dois tenants diferentes cadastram um produto com o mesmo SKU
- **THEN** ambos os cadastros MUST ser aceitos
