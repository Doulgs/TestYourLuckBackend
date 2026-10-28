# event-stock Specification

## Purpose
Controla a quantidade disponível de cada brinde em cada evento, base para o sorteio e para os alertas de estoque baixo.

## Requirements

### Requirement: Estoque por evento e produto
O sistema SHALL manter, para cada par evento × produto do tenant, uma quantidade disponível inteira ≥ 0. Produtos sem registro de estoque em um evento MUST ser tratados como quantidade 0.

#### Scenario: Produto novo
- **WHEN** um produto é criado
- **THEN** sua quantidade disponível em todos os eventos é 0

### Requirement: Visualização do estoque de um evento
A página Estoque SHALL permitir selecionar um evento e listar todos os produtos ativos com Produto, Disponível, Mínimo, Sorteados (no evento) e Status, além de "Total disponível" e "Itens abaixo do mínimo".

#### Scenario: Trocar de evento
- **WHEN** o usuário seleciona outro evento no seletor
- **THEN** a tabela exibe as quantidades e sorteados desse evento

### Requirement: Ajuste de quantidade
O usuário SHALL poder ajustar a quantidade com os botões "−" e "+" ou digitando um número; valores negativos MUST ser convertidos para 0 e caracteres não numéricos ignorados.

#### Scenario: Incrementar
- **WHEN** o usuário clica "+" em um produto com 9 unidades
- **THEN** a quantidade passa a 10 e é persistida

#### Scenario: Decrementar no zero
- **WHEN** o usuário clica "−" em um produto com 0 unidades
- **THEN** a quantidade permanece 0

### Requirement: Status de nível de estoque
O status SHALL ser "Esgotado" quando a quantidade for 0, "Baixo" quando for maior que 0 e menor ou igual ao estoque mínimo, e "OK" caso contrário.

#### Scenario: Estoque baixo
- **WHEN** um produto tem mínimo 8 e quantidade 8
- **THEN** o status exibido é "Baixo"

#### Scenario: Esgotado
- **WHEN** a quantidade é 0
- **THEN** o status exibido é "Esgotado" e ele conta em "Itens abaixo do mínimo"
