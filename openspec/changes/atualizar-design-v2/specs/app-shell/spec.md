# Spec Delta

## MODIFIED Requirements

### Requirement: Sidebar de navegação
O painel SHALL exibir uma sidebar com o grupo "Visão geral" (Dashboard, Sorteios, Leads), o grupo "Cadastros" (Eventos, Categorias, Produtos, Campos personalizados) e o grupo "Integrações" (Webhooks), destacando o item da página atual.

#### Scenario: Navegar pela sidebar
- **WHEN** o usuário clica em "Produtos"
- **THEN** a página de Produtos é exibida e o item "Produtos" fica destacado

#### Scenario: Grupo Integrações
- **WHEN** o usuário clica em "Webhooks" no grupo "Integrações"
- **THEN** a página de Webhooks é exibida

#### Scenario: Estoque fora do menu
- **WHEN** o usuário abre a sidebar
- **THEN** não existe item "Estoque"

### Requirement: Breadcrumb
Toda página do painel, exceto o modo de jogo do sorteio, SHALL exibir no cabeçalho o breadcrumb "TestYourLuck › <nome da página>".

#### Scenario: Breadcrumb do estoque
- **WHEN** o usuário acessa o endereço antigo da página de Estoque
- **THEN** ele é redirecionado para Produtos e o cabeçalho exibe "TestYourLuck › Produtos"

#### Scenario: Breadcrumb de campos personalizados
- **WHEN** o usuário está na página de Campos personalizados
- **THEN** o cabeçalho exibe "TestYourLuck › Campos personalizados"

#### Scenario: Breadcrumb da configuração de sorteios
- **WHEN** o usuário está na tela de configuração de sorteios
- **THEN** o cabeçalho exibe "TestYourLuck › Sorteios"

### Requirement: Modo totem sem sidebar
O modo de jogo do sorteio SHALL ser exibido sem sidebar e sem breadcrumb, ocupando a tela para uso em totem, com botão "← Configurações" que volta à tela de configuração de sorteios.

#### Scenario: Abrir sorteio
- **WHEN** o usuário está no modo de jogo
- **THEN** a sidebar e o breadcrumb não são exibidos e há um botão "← Configurações" para voltar à tela de Sorteios

#### Scenario: Abrir modo de jogo
- **WHEN** o usuário clica "Abrir sorteio" na tela de configuração
- **THEN** a sidebar e o breadcrumb não são exibidos e há um botão "← Configurações"

#### Scenario: Tela de configuração com sidebar
- **WHEN** o usuário navega para "Sorteios"
- **THEN** a tela de configuração é exibida com sidebar e breadcrumb

## ADDED Requirements

### Requirement: Menu de ações por linha
As listagens de Eventos, Produtos, Categorias, Campos personalizados e Webhooks SHALL exibir as ações de cada linha em um menu aberto por um botão "…" na última coluna; ações destrutivas MUST aparecer em vermelho.

#### Scenario: Abrir menu de ações
- **WHEN** o usuário clica no botão "…" de um produto
- **THEN** é exibido um menu com "Editar" e "Excluir", este em vermelho

#### Scenario: Fechar menu
- **WHEN** o menu está aberto e o usuário clica fora dele
- **THEN** o menu fecha sem executar ação
