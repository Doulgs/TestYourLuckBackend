# Spec Delta

## MODIFIED Requirements

### Requirement: Listagem de eventos
A página Eventos SHALL listar os eventos do tenant com colunas Evento (miniatura do banner, nome e website ou "Sem website"), Período (datas e horário ou "Dia todo"), Local (cidade / UF e local), Status, Sorteios ("realizados / limite", com "∞" quando ilimitado, e a regra de período) e menu de ações (Abrir sorteio, Editar, Excluir).

#### Scenario: Listar eventos
- **WHEN** o usuário abre a página Eventos
- **THEN** são exibidos os eventos do tenant com a contagem de sorteios de cada um

#### Scenario: Período de vários dias
- **WHEN** um evento vai de 27/10/2026 a 30/10/2026 e é "dia todo"
- **THEN** a coluna Período exibe "27/10 – 30/10/2026" e "Dia todo"

#### Scenario: Limite e regra
- **WHEN** um evento tem 142 sorteios, limite 500 e não permite sorteios fora da data
- **THEN** a coluna Sorteios exibe "142 / 500" e "Só no período"

#### Scenario: Sorteios ilimitados
- **WHEN** um evento tem limite 0 e permite sorteios fora da data
- **THEN** a coluna Sorteios exibe "<realizados> / ∞" e "Fora da data: permitido"

### Requirement: Criação e edição de evento
O sistema SHALL permitir criar e editar eventos por modal com Banner, Nome (obrigatório), Status (padrão Rascunho), Data de início, Data de fim, Dia todo (padrão ligado), Hora de início e fim (visíveis quando não é dia todo), Permitir sorteios fora da data (padrão desligado), Local, CEP, Cidade, Estado, Região, Timezone (padrão America/Sao_Paulo), Sorteios disponíveis (inteiro ≥ 0, 0 = ilimitado) e Website.

#### Scenario: Criar evento
- **WHEN** o usuário clica "+ Novo evento", preenche o nome e salva
- **THEN** o evento é criado com 0 sorteios e aparece na listagem

#### Scenario: Nome vazio
- **WHEN** o usuário tenta salvar sem nome
- **THEN** o modal exibe erro no campo Nome e nada é salvo

#### Scenario: Editar evento
- **WHEN** o usuário abre "Editar" no menu, altera o status para Encerrado e salva
- **THEN** a listagem exibe o novo status

#### Scenario: Data de fim anterior ao início
- **WHEN** o usuário informa data de fim anterior à data de início
- **THEN** o sistema MUST recusar com a mensagem "A data de fim deve ser igual ou posterior ao início"

#### Scenario: Região pela UF
- **WHEN** o usuário seleciona o estado "MG"
- **THEN** a região é preenchida com "Sudeste", podendo ser alterada

#### Scenario: Máscara de CEP
- **WHEN** o usuário digita "04329900" no CEP
- **THEN** o campo exibe "04329-900"

#### Scenario: Dica de limite
- **WHEN** o campo "Sorteios disponíveis" vale 0
- **THEN** o modal exibe "0 = sorteios ilimitados"; com valor 300 exibe "Limite de 300 sorteios neste evento"

### Requirement: Exclusão de evento
O sistema SHALL permitir excluir um evento após confirmação. Eventos com sorteios registrados MUST ser excluídos de forma lógica, preservando leads e histórico.

#### Scenario: Excluir evento sem sorteios
- **WHEN** o usuário confirma a exclusão de um evento sem sorteios
- **THEN** o evento deixa de existir e sai dos vínculos de produtos e webhooks

#### Scenario: Excluir evento com sorteios
- **WHEN** o usuário confirma a exclusão de um evento com sorteios
- **THEN** o evento some das listagens, mas os leads permanecem consultáveis com o nome do evento

### Requirement: Atalho para sorteio
A ação "Abrir sorteio" do menu do evento e o atalho "Sortear →" do Dashboard SHALL abrir a tela de configuração de sorteios com o evento já selecionado.

#### Scenario: Sortear a partir da lista
- **WHEN** o usuário escolhe "Abrir sorteio" no menu do evento "Expo Tech 2026"
- **THEN** a tela de Sorteios abre com "Expo Tech 2026" selecionado

## ADDED Requirements

### Requirement: Banner do evento
O evento SHALL aceitar uma imagem de banner (PNG, JPG ou WEBP, recomendada 1600 × 600) enviada no modal, exibida como miniatura na listagem, com opção de trocar a imagem.

#### Scenario: Enviar banner
- **WHEN** o usuário envia uma imagem no campo "Banner do evento" e salva
- **THEN** a miniatura do banner aparece na linha do evento

#### Scenario: Formato inválido
- **WHEN** o usuário envia um arquivo que não é PNG, JPG ou WEBP
- **THEN** o sistema MUST recusar o envio com a mensagem "Formato de imagem não suportado"
