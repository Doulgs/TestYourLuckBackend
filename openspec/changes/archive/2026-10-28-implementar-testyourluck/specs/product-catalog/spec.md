# Spec Delta

## Purpose

Mantém o catálogo de brindes (produtos) e suas categorias, incluindo o peso que define a chance de cada brinde sair no sorteio.

## ADDED Requirements

### Requirement: Cadastro de categorias
O sistema SHALL permitir criar, editar e excluir categorias com Nome (obrigatório, único no tenant) e Descrição (opcional).

#### Scenario: Criar categoria
- **WHEN** o usuário clica "+ Nova categoria", informa "Eletrônicos" e salva
- **THEN** a categoria aparece na listagem

#### Scenario: Nome duplicado
- **WHEN** o usuário salva uma categoria com nome já existente no tenant
- **THEN** o sistema MUST responder 409 e exibir "Categoria já existe"

#### Scenario: Excluir categoria em uso
- **WHEN** o usuário exclui uma categoria que possui produtos
- **THEN** o sistema MUST recusar a exclusão com a mensagem "Categoria possui produtos vinculados"

### Requirement: Indicadores por categoria
A listagem de categorias SHALL exibir: nome e descrição ("—" se vazia), nomes dos produtos, quantidade de produtos (Itens), soma do estoque desses produtos em todos os eventos e chance somada (soma dos pesos da categoria ÷ soma de todos os pesos, em %), com busca por nome.

#### Scenario: Chance somada
- **WHEN** a categoria possui produtos com pesos 10, 8 e 2 e a soma total de pesos é 100
- **THEN** a chance somada exibida é "20.0%"

### Requirement: Cadastro de produtos
O sistema SHALL permitir criar, editar e excluir produtos com Nome (obrigatório), SKU (único no tenant), Categoria, Peso no sorteio (inteiro ≥ 0, padrão 10) e Estoque mínimo (inteiro ≥ 0, padrão 5).

#### Scenario: Criar produto
- **WHEN** o usuário preenche nome, SKU, categoria, peso e mínimo e salva
- **THEN** o produto é criado com sigla e cor gerados e aparece na listagem

#### Scenario: Peso inválido
- **WHEN** o usuário informa peso negativo ou não numérico
- **THEN** o formulário exibe erro e não salva

#### Scenario: SKU duplicado
- **WHEN** o usuário salva um produto com SKU já existente no tenant
- **THEN** o sistema MUST responder 409 e exibir "SKU já cadastrado"

### Requirement: Sigla e cor do produto
Cada produto SHALL possuir sigla de até 2 letras (quando não informada: iniciais das duas primeiras palavras do nome, ou as duas primeiras letras se o nome tiver uma palavra, em maiúsculas) e matiz de cor atribuído da sequência 200, 150, 80, 280, 30, 250, 340, 110 conforme a ordem de cadastro.

#### Scenario: Sigla gerada
- **WHEN** um produto "Fone Bluetooth" é criado sem sigla
- **THEN** a sigla é "FB"

#### Scenario: Sigla de nome com uma palavra
- **WHEN** um produto "Caneca" é criado sem sigla
- **THEN** a sigla é "CA"

### Requirement: Cálculo de chance
A chance de cada produto SHALL ser peso do produto ÷ soma dos pesos de todos os produtos do tenant, exibida com uma casa decimal e barra proporcional. O modal de produto MUST exibir a "Chance estimada" considerando o peso digitado.

#### Scenario: Chance na listagem
- **WHEN** existem produtos com pesos somando 100 e um produto tem peso 25
- **THEN** sua chance exibida é "25.0%"

#### Scenario: Chance estimada no modal
- **WHEN** o usuário digita peso 50 em um novo produto e os demais somam 100
- **THEN** o modal exibe "Chance estimada: 33.3%"

### Requirement: Listagem de produtos
A página Produtos SHALL listar Produto (sigla em círculo colorido e nome), SKU, Categoria, Peso, Chance, Estoque (soma em todos os eventos) e Ações, com busca por nome ou SKU e nota explicativa do cálculo de chance.

#### Scenario: Buscar por SKU
- **WHEN** o usuário digita "BR-004"
- **THEN** apenas o produto com esse SKU é exibido

### Requirement: Exclusão de produto
O sistema SHALL excluir produtos de forma lógica quando já houver sorteios associados, preservando o histórico de leads.

#### Scenario: Excluir produto já sorteado
- **WHEN** o usuário exclui um produto que já foi sorteado
- **THEN** o produto some do catálogo, do estoque e do sorteio, mas os leads continuam exibindo o nome do brinde
