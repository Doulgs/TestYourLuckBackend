# Spec Delta

## MODIFIED Requirements

### Requirement: Indicadores por categoria
A listagem de categorias SHALL exibir: nome e descrição ("—" se vazia), nomes dos produtos, quantidade de produtos (Itens), soma da quantidade em estoque desses produtos e chance somada (soma dos pesos da categoria ÷ soma de todos os pesos, em %), com busca por nome.

#### Scenario: Chance somada
- **WHEN** a categoria possui produtos com pesos 10, 8 e 2 e a soma total de pesos é 100
- **THEN** a chance somada exibida é "20.0%"

#### Scenario: Estoque da categoria
- **WHEN** a categoria possui produtos com 14, 6 e 2 unidades em estoque
- **THEN** a coluna Estoque exibe 22

### Requirement: Cadastro de produtos
O sistema SHALL permitir criar, editar e excluir produtos com Imagem (opcional), Nome (obrigatório), SKU (único no tenant), Categoria, Eventos vinculados, Peso no sorteio (inteiro de 0 a 100, padrão 10), Quantidade em estoque (inteiro ≥ 0, padrão 0) e Estoque mínimo (inteiro ≥ 0, padrão 5). Novos produtos MUST vir vinculados a todos os eventos por padrão.

#### Scenario: Criar produto
- **WHEN** o usuário preenche nome, SKU, categoria, peso, quantidade e mínimo e salva
- **THEN** o produto é criado com sigla e cor gerados, vinculado aos eventos selecionados, e aparece na listagem

#### Scenario: Peso inválido
- **WHEN** o usuário informa peso negativo, maior que 100 ou não numérico
- **THEN** o sistema MUST recusar e não salvar

#### Scenario: SKU duplicado
- **WHEN** o usuário salva um produto com SKU já existente no tenant
- **THEN** o sistema MUST responder 409 e exibir "SKU já cadastrado"

#### Scenario: Peso por slider
- **WHEN** o usuário arrasta o slider de peso até 40
- **THEN** o valor exibido passa a 40 e a "Chance estimada" é recalculada

### Requirement: Listagem de produtos
A página Produtos SHALL listar Produto (imagem ou sigla em círculo colorido, e nome), SKU, Categoria, Eventos, Peso no sorteio (barra 0–100 e valor), Chance, Estoque (quantidade e status) e menu de ações, com busca por nome ou SKU, filtro por categoria, filtro por evento e nota explicativa do cálculo de chance.

#### Scenario: Buscar por SKU
- **WHEN** o usuário digita "BR-004"
- **THEN** apenas o produto com esse SKU é exibido

#### Scenario: Filtrar por evento
- **WHEN** o usuário seleciona o evento "Summit Irrah" no filtro
- **THEN** apenas produtos vinculados a esse evento são exibidos

#### Scenario: Filtrar por categoria
- **WHEN** o usuário seleciona a categoria "Eletrônicos"
- **THEN** apenas produtos dessa categoria são exibidos

#### Scenario: Texto da coluna Eventos
- **WHEN** um produto está vinculado a todos os eventos, a nenhum, a dois ou a três ou mais eventos
- **THEN** a coluna exibe respectivamente "Todos os eventos", "Nenhum", os dois nomes separados por vírgula, ou "<n> eventos"

### Requirement: Exclusão de produto
O sistema SHALL excluir produtos de forma lógica quando já houver sorteios associados, preservando o histórico de leads.

#### Scenario: Excluir produto já sorteado
- **WHEN** o usuário exclui um produto que já foi sorteado
- **THEN** o produto some do catálogo e do sorteio, mas os leads continuam exibindo o nome do brinde

## ADDED Requirements

### Requirement: Estoque do produto
Cada produto SHALL possuir uma única quantidade em estoque, compartilhada por todos os eventos vinculados, com status "Esgotado" quando 0, "Estoque baixo" quando maior que 0 e menor ou igual ao mínimo, e "OK" caso contrário.

#### Scenario: Estoque baixo
- **WHEN** um produto tem mínimo 8 e quantidade 8
- **THEN** a listagem exibe 8 e o status "Estoque baixo"

#### Scenario: Esgotado
- **WHEN** a quantidade do produto é 0
- **THEN** a listagem exibe o status "Esgotado"

#### Scenario: Quantidade negativa
- **WHEN** o usuário informa quantidade negativa
- **THEN** o sistema MUST recusar e não salvar

### Requirement: Eventos vinculados ao produto
O produto SHALL participar do sorteio apenas nos eventos vinculados. A seleção MUST oferecer a opção "Todos os eventos", que marca ou desmarca todos de uma vez.

#### Scenario: Marcar todos
- **WHEN** o usuário clica "Todos os eventos" no seletor sem nenhum evento marcado
- **THEN** todos os eventos ficam marcados e o seletor exibe o chip "Todos os eventos"

#### Scenario: Vínculo de outro tenant
- **WHEN** a requisição informa o id de um evento de outro tenant
- **THEN** o sistema MUST responder 404 "Evento não encontrado"

### Requirement: Imagem do produto
O produto SHALL aceitar uma foto (PNG, JPG ou WEBP) exibida no lugar da sigla na listagem, na máquina e no resultado do sorteio.

#### Scenario: Produto com imagem
- **WHEN** um produto possui imagem
- **THEN** o círculo da listagem exibe a imagem sem a sigla
