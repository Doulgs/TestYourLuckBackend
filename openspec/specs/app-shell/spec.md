# app-shell Specification

## Purpose
Define a estrutura visual e de navegação do painel autenticado do TestYourLuck, incluindo sidebar, breadcrumb, menu do usuário e proteção de rotas.

## Requirements

### Requirement: Telas de autenticação
As telas de Login e Cadastro SHALL seguir o design: imagem de fundo com gradiente, logo no topo e formulário alinhado à esquerda, com alternância entre "Cadastre-se" e "Entrar".

#### Scenario: Alternar para cadastro
- **WHEN** o visitante clica em "Cadastre-se" na tela de login
- **THEN** a tela de Cadastro é exibida

### Requirement: Proteção de rotas
As páginas do painel SHALL ser acessíveis somente com sessão autenticada; páginas de autenticação MUST redirecionar usuários já autenticados ao Dashboard.

#### Scenario: Acesso sem sessão
- **WHEN** um visitante sem token acessa uma página do painel
- **THEN** ele é redirecionado para a tela de login

#### Scenario: Token rejeitado pela API
- **WHEN** a API responde 401 a uma requisição do painel
- **THEN** a sessão é encerrada e o usuário é levado à tela de login

### Requirement: Sidebar de navegação
O painel SHALL exibir uma sidebar com o grupo "Visão geral" (Dashboard, Sorteio, Leads) e o grupo "Cadastros" (Eventos, Categorias, Produtos, Estoque), destacando o item da página atual.

#### Scenario: Navegar pela sidebar
- **WHEN** o usuário clica em "Produtos"
- **THEN** a página de Produtos é exibida e o item "Produtos" fica destacado

### Requirement: Sidebar recolhível e responsiva
A sidebar SHALL alternar entre expandida (logo e rótulos, 248px) e recolhida (apenas ícones, 68px) ao clicar no logo. Em telas menores que 860px ela MUST iniciar recolhida e, quando expandida, sobrepor o conteúdo com fundo escurecido.

#### Scenario: Recolher no desktop
- **WHEN** o usuário clica no logo da sidebar expandida em tela ≥ 860px
- **THEN** a sidebar passa a exibir apenas ícones e o ícone da marca

#### Scenario: Fechar no mobile
- **WHEN** a sidebar está expandida em tela < 860px e o usuário clica no fundo escurecido
- **THEN** a sidebar recolhe

### Requirement: Breadcrumb
Toda página do painel, exceto Sorteio, SHALL exibir no cabeçalho o breadcrumb "TestYourLuck › <nome da página>".

#### Scenario: Breadcrumb do estoque
- **WHEN** o usuário está na página de Estoque
- **THEN** o cabeçalho exibe "TestYourLuck › Estoque"

### Requirement: Menu do usuário
O rodapé da sidebar SHALL exibir avatar com iniciais, nome e e-mail do usuário; ao clicar, MUST abrir um menu com Conta, Notificações e Sair.

#### Scenario: Abrir e fechar menu
- **WHEN** o usuário clica no bloco do usuário e depois fora do menu
- **THEN** o menu abre e em seguida fecha

### Requirement: Modo totem sem sidebar
A página de Sorteio SHALL ser exibida sem sidebar e sem cabeçalho, ocupando a tela para uso em totem.

#### Scenario: Abrir sorteio
- **WHEN** o usuário navega para Sorteio
- **THEN** a sidebar e o breadcrumb não são exibidos e há um botão "← Painel" para voltar ao Dashboard

### Requirement: Feedback de operações
Operações de criação, edição, exclusão e erros da API SHALL exibir notificação breve (toast) ao usuário.

#### Scenario: Erro da API
- **WHEN** uma operação falha com mensagem de erro da API
- **THEN** a mensagem é exibida em um toast de erro
