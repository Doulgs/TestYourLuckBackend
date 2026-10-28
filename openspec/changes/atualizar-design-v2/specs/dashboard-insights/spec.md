# Spec Delta

## MODIFIED Requirements

### Requirement: KPIs
O Dashboard SHALL exibir três cartões: "Sorteios realizados" (total de sorteios), "Leads captados" (total de leads) e "Brindes em estoque" (soma das quantidades em estoque de todos os produtos), cada um com variação percentual em relação ao período anterior de mesma duração.

#### Scenario: Variação positiva
- **WHEN** o período atual teve 120 sorteios e o anterior 100
- **THEN** o cartão exibe o badge "+20,0%" com ícone de tendência de alta

#### Scenario: Brindes em estoque
- **WHEN** existem produtos com 148, 92 e 108 unidades
- **THEN** o cartão "Brindes em estoque" exibe 348

### Requirement: Listas de resumo
O Dashboard SHALL exibir: "Histórico recente" (5 últimos sorteios com participante, brinde, evento e hora), "Estoque baixo" (até 5 produtos com quantidade ≤ mínimo, com categoria e eventos vinculados, no formato qtd / mín, e link "Produtos →"), "Mais sorteados" (5 produtos mais sorteados com barra proporcional) e "Eventos ativos" (data, sorteios e atalho "Sortear →").

#### Scenario: Atalho de evento ativo
- **WHEN** o usuário clica em um evento da lista "Eventos ativos"
- **THEN** a tela de Sorteios abre com esse evento selecionado

#### Scenario: Links das listas
- **WHEN** o usuário clica "Produtos →" na lista "Estoque baixo"
- **THEN** a página de Produtos é exibida

#### Scenario: Linha de estoque baixo
- **WHEN** o produto "Power bank" (Eletrônicos, vinculado a todos os eventos) tem 6 unidades e mínimo 6
- **THEN** a lista exibe "Power bank", "Eletrônicos · todos os eventos" e "6 / 6"
