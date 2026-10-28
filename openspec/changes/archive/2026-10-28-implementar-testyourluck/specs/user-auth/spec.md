# Spec Delta

## Purpose

Permite que empresas criem sua conta no TestYourLuck e que usuários se autentiquem com e-mail e senha para acessar o painel do próprio tenant.

## ADDED Requirements

### Requirement: Cadastro de conta
O sistema SHALL permitir criar uma conta informando nome completo, empresa, e-mail e senha (mínimo 8 caracteres). O cadastro MUST criar um novo tenant para a empresa e o usuário proprietário dele, e retornar uma sessão autenticada.

#### Scenario: Cadastro válido
- **WHEN** o visitante envia nome, empresa, e-mail inédito e senha com 8+ caracteres
- **THEN** o sistema cria o tenant e o usuário, retorna um token de acesso e o frontend redireciona para o Dashboard

#### Scenario: E-mail já cadastrado
- **WHEN** o visitante envia um e-mail que já possui conta
- **THEN** o sistema MUST responder 409 com a mensagem "E-mail já cadastrado" e não criar registros

#### Scenario: Dados inválidos
- **WHEN** algum campo obrigatório está vazio, o e-mail é inválido ou a senha tem menos de 8 caracteres
- **THEN** o formulário MUST exibir a mensagem de erro no campo correspondente e não enviar a requisição

### Requirement: Login com e-mail e senha
O sistema SHALL autenticar o usuário por e-mail e senha e retornar um token de acesso com validade limitada contendo o usuário, o tenant e o tenant_owner.

#### Scenario: Credenciais válidas
- **WHEN** o usuário envia e-mail e senha corretos
- **THEN** o sistema retorna o token e os dados do usuário, e o frontend redireciona para o Dashboard

#### Scenario: Credenciais inválidas
- **WHEN** o usuário envia e-mail inexistente ou senha incorreta
- **THEN** o sistema MUST responder 401 com a mensagem genérica "E-mail ou senha inválidos"

### Requirement: Proteção da API por token
Todos os endpoints da API, exceto cadastro, login, health e documentação, SHALL exigir um token válido.

#### Scenario: Requisição sem token
- **WHEN** uma requisição a um endpoint protegido não contém token ou contém token expirado/inválido
- **THEN** o sistema MUST responder 401

### Requirement: Usuário corrente
O sistema SHALL expor os dados do usuário autenticado (nome, e-mail, empresa e iniciais) para exibição no menu do usuário.

#### Scenario: Consulta do usuário corrente
- **WHEN** o frontend consulta o usuário corrente com token válido
- **THEN** o sistema retorna nome, e-mail e empresa do usuário

### Requirement: Logout
O sistema SHALL permitir que o usuário encerre a sessão pelo item "Sair" do menu do usuário, descartando o token no cliente e retornando à tela de login.

#### Scenario: Sair
- **WHEN** o usuário clica em "Sair"
- **THEN** o token é removido do cliente e a tela de login é exibida

### Requirement: Login social fora de escopo
O botão "Entrar com Google" e o link "Esqueceu sua senha?" SHALL ser exibidos conforme o design, sem efetuar autenticação.

#### Scenario: Clique em Entrar com Google
- **WHEN** o usuário clica em "Entrar com Google"
- **THEN** o sistema exibe um aviso de que a funcionalidade estará disponível em breve e não autentica o usuário
