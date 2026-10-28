# Spec Delta

## Purpose

Permite que a empresa faça perguntas extras ao participante antes de jogar e use as respostas como variáveis em webhooks e na exportação de leads.

## ADDED Requirements

### Requirement: Listagem de campos personalizados
A página Campos personalizados SHALL listar os campos do tenant com Campo (rótulo e texto de ajuda ou "—"), Variável (`{{chave}}`), Tipo, Opções (apenas para seleção, senão "—"), Obrigatório (Sim/Não), Ativo (interruptor) e menu de ações (Editar, Ativar/Desativar, Excluir), com busca por rótulo ou chave e a nota "Campos ativos aparecem no cadastro do sorteio e podem ser usados como variáveis nos webhooks."

#### Scenario: Buscar campo
- **WHEN** o usuário digita "cargo"
- **THEN** apenas campos cujo rótulo ou chave contém "cargo" são exibidos

#### Scenario: Desativar pelo interruptor
- **WHEN** o usuário desliga o interruptor "Ativo" do campo "Empresa"
- **THEN** o campo é salvo como inativo e deixa de aparecer no cadastro do sorteio

### Requirement: Cadastro de campo personalizado
O sistema SHALL permitir criar e editar campos com Pergunta/rótulo (obrigatório), Texto de ajuda, Chave da variável, Tipo (Texto curto, Número, Data, Seleção, Sim / Não), Opções (para Seleção) e Obrigatório. Novos campos MUST ser criados ativos e incluídos nas perguntas exibidas pela máquina.

#### Scenario: Chave gerada pelo rótulo
- **WHEN** o usuário digita o rótulo "Aceito receber novidades" sem editar a chave
- **THEN** a chave é preenchida com "aceito_receber_novidades" e o rodapé exibe "Variável: {{aceito_receber_novidades}}"

#### Scenario: Chave normalizada
- **WHEN** o usuário digita "Área de Atuação" na chave
- **THEN** a chave é normalizada para "area_de_atuacao" (minúsculas, sem acentos, até 32 caracteres)

#### Scenario: Chave duplicada
- **WHEN** o usuário salva um campo com chave já usada no tenant
- **THEN** o sistema MUST responder 409 "Chave já utilizada"

#### Scenario: Chave reservada
- **WHEN** a chave começa com "lead", "evento", "premio", "jogo" ou "campo"
- **THEN** o sistema MUST recusar com a mensagem "Chave reservada"

#### Scenario: Opções de seleção
- **WHEN** o tipo é Seleção
- **THEN** o modal exibe a lista de opções numeradas com remoção e "+ Adicionar mais opções", e opções vazias são descartadas ao salvar

#### Scenario: Seleção sem opções
- **WHEN** o usuário salva um campo de Seleção sem nenhuma opção preenchida
- **THEN** o sistema MUST recusar com a mensagem "Informe ao menos uma opção"

### Requirement: Exclusão de campo personalizado
O sistema SHALL permitir excluir um campo; respostas já registradas em leads MUST ser preservadas.

#### Scenario: Excluir campo respondido
- **WHEN** o usuário exclui o campo "Cargo" que já tem respostas
- **THEN** o campo sai da listagem e do cadastro, e as respostas antigas continuam no registro do lead

### Requirement: Resposta no cadastro do participante
Cada campo exibido no cadastro SHALL usar o controle do seu tipo (texto, número, data, lista de seleção ou interruptor sim/não) e o servidor MUST validar as respostas: obrigatórios preenchidos (exceto sim/não), número numérico, data válida e valor de seleção entre as opções.

#### Scenario: Opção inválida
- **WHEN** o cadastro envia para "Cargo" um valor fora das opções
- **THEN** o servidor MUST recusar com 400

#### Scenario: Sim/Não
- **WHEN** o participante deixa "Aceito receber novidades" desligado
- **THEN** a resposta é registrada como "nao"
