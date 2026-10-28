# Spec Delta

## Purpose

Permite cadastrar e manter os eventos onde os sorteios acontecem; cada evento possui estoque e histórico de sorteios próprios.

## ADDED Requirements

### Requirement: Listagem de eventos
A página Eventos SHALL listar os eventos do tenant com colunas Nome, Data (dd/mm/aaaa), Local, Status, Sorteios (quantidade de sorteios realizados) e Ações (Sortear, Editar, Excluir).

#### Scenario: Listar eventos
- **WHEN** o usuário abre a página Eventos
- **THEN** são exibidos os eventos do tenant com a contagem de sorteios de cada um

### Requirement: Busca de eventos
A listagem SHALL permitir filtrar eventos por texto contido no nome ou no local, sem diferenciar maiúsculas e minúsculas.

#### Scenario: Buscar por local
- **WHEN** o usuário digita "curitiba" na busca
- **THEN** apenas eventos cujo nome ou local contém "curitiba" são exibidos

### Requirement: Criação e edição de evento
O sistema SHALL permitir criar e editar eventos por modal com Nome (obrigatório), Data, Status (Ativo, Rascunho ou Encerrado; padrão Rascunho) e Local.

#### Scenario: Criar evento
- **WHEN** o usuário clica "+ Novo evento", preenche o nome e salva
- **THEN** o evento é criado com 0 sorteios e aparece na listagem

#### Scenario: Nome vazio
- **WHEN** o usuário tenta salvar sem nome
- **THEN** o modal exibe erro no campo Nome e nada é salvo

#### Scenario: Editar evento
- **WHEN** o usuário clica "Editar", altera o status para Encerrado e salva
- **THEN** a listagem exibe o novo status

### Requirement: Status visual do evento
O status SHALL ser exibido como badge: Ativo em verde, Encerrado em vermelho e Rascunho em cinza.

#### Scenario: Badge de evento ativo
- **WHEN** um evento tem status Ativo
- **THEN** o badge é exibido com fundo verde escuro e texto verde

### Requirement: Exclusão de evento
O sistema SHALL permitir excluir um evento após confirmação. Eventos com sorteios registrados MUST ser excluídos de forma lógica, preservando leads e histórico.

#### Scenario: Excluir evento sem sorteios
- **WHEN** o usuário confirma a exclusão de um evento sem sorteios
- **THEN** o evento e seu estoque deixam de existir

#### Scenario: Excluir evento com sorteios
- **WHEN** o usuário confirma a exclusão de um evento com sorteios
- **THEN** o evento some das listagens, mas os leads permanecem consultáveis com o nome do evento

### Requirement: Atalho para sorteio
O botão "Sortear" SHALL abrir a página de Sorteio com o evento já selecionado.

#### Scenario: Sortear a partir da lista
- **WHEN** o usuário clica "Sortear" no evento "Expo Tech 2026"
- **THEN** a página de Sorteio abre com "Expo Tech 2026" selecionado
