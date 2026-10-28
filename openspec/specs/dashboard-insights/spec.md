# dashboard-insights Specification

## Purpose
Oferece ao tenant uma visão consolidada de sorteios, leads e estoque, com indicadores por período e atalhos para as áreas de gestão.

## Requirements

### Requirement: Seleção de período
O Dashboard SHALL oferecer os períodos "Últimos 3 meses", "Últimos 30 dias" (padrão) e "Últimos 7 dias", que determinam as séries dos gráficos.

#### Scenario: Trocar período
- **WHEN** o usuário seleciona "Últimos 7 dias"
- **THEN** os gráficos passam a exibir 7 pontos diários e o subtítulo "Últimos 7 dias"

### Requirement: KPIs
O Dashboard SHALL exibir três cartões: "Sorteios realizados" (total de sorteios), "Leads captados" (total de leads) e "Brindes em estoque" (soma das quantidades em todos os eventos), cada um com variação percentual em relação ao período anterior de mesma duração.

#### Scenario: Variação positiva
- **WHEN** o período atual teve 120 sorteios e o anterior 100
- **THEN** o cartão exibe o badge "+20,0%" com ícone de tendência de alta

### Requirement: Gráfico de sorteios realizados
O Dashboard SHALL exibir um gráfico de área com a quantidade de sorteios por intervalo do período para os dois eventos com mais sorteios, com legenda, rótulos de data (dd/mm) e total do período no subtítulo.

#### Scenario: Agrupamento por período
- **WHEN** o período é "Últimos 30 dias"
- **THEN** a série possui 10 pontos agrupando 3 dias cada

### Requirement: Gráfico de novos leads
O Dashboard SHALL exibir um gráfico de barras com a quantidade de leads por intervalo do período, destacando a barra mais recente, e o total de cadastros no subtítulo.

#### Scenario: Total de cadastros
- **WHEN** o período possui 85 leads
- **THEN** o subtítulo exibe "<período> · 85 cadastros"

### Requirement: Listas de resumo
O Dashboard SHALL exibir: "Histórico recente" (5 últimos sorteios com participante, brinde, evento e hora), "Estoque baixo" (até 5 itens com quantidade ≤ mínimo em eventos ativos, no formato qtd / mín), "Mais sorteados" (5 produtos mais sorteados com barra proporcional) e "Eventos ativos" (data, sorteios e atalho "Sortear →").

#### Scenario: Atalho de evento ativo
- **WHEN** o usuário clica em um evento da lista "Eventos ativos"
- **THEN** a página de Sorteio abre com esse evento selecionado

#### Scenario: Links das listas
- **WHEN** o usuário clica "Estoque →"
- **THEN** a página de Estoque é exibida

### Requirement: Acesso rápido ao sorteio
O Dashboard SHALL exibir o botão "Abrir sorteio" que leva à página de Sorteio.

#### Scenario: Abrir sorteio
- **WHEN** o usuário clica "Abrir sorteio"
- **THEN** a página de Sorteio é exibida

### Requirement: Atualidade dos dados
Os agregados do Dashboard MAY ser servidos de cache por no máximo 60 segundos e SHALL ser invalidados quando ocorrer sorteio, ajuste de estoque ou alteração de eventos e produtos do tenant.

#### Scenario: Após um sorteio
- **WHEN** um sorteio é concluído e o Dashboard é recarregado
- **THEN** o total de sorteios reflete o novo sorteio
