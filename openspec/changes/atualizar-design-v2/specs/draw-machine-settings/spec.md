# Spec Delta

## Purpose

Permite que cada empresa configure a aparência e as regras de participação da máquina de sorteio (estilo de jogo, brand kit, efeitos, créditos e campos do cadastro) e valide o resultado em uma pré-visualização antes de abrir para o público.

## ADDED Requirements

### Requirement: Configuração por empresa
O sistema SHALL manter uma configuração da máquina por tenant, persistida no servidor e aplicada à pré-visualização e ao modo de jogo. Na ausência de configuração salva, MUST usar os padrões: estilo caça-níquel, tamanho M, 1 crédito, cor #F36ACB, fundo preto, logo padrão, título "Teste sua sorte", mensagem "Retire seu brinde no balcão", e-mail, telefone e CPF habilitados, luzes, som e lista de prêmios ligados.

#### Scenario: Primeiro acesso
- **WHEN** um tenant novo abre a tela de Sorteios
- **THEN** a configuração exibida é a padrão

#### Scenario: Persistência
- **WHEN** o operador altera o título para "Gire e ganhe" e recarrega a página
- **THEN** o título "Gire e ganhe" continua aplicado

#### Scenario: Isolamento
- **WHEN** o tenant A altera a cor principal
- **THEN** a configuração do tenant B não muda

### Requirement: Estilo do jogo
A tela de configuração SHALL oferecer os estilos Caça-níquel ("3 rolos com alavanca"), Roleta ("Roda da sorte com ponteiro"), Slider ("Faixa de prêmios deslizando") e Blocos ("Revele até achar 3 iguais"), destacando o selecionado.

#### Scenario: Trocar estilo
- **WHEN** o operador seleciona "Roleta"
- **THEN** a pré-visualização passa a exibir a roleta e o botão "GIRAR ROLETA"

### Requirement: Brand kit
A configuração SHALL permitir escolher o logo (padrão TestYourLuck ou logo do cliente enviado em PNG/SVG), a cor principal (paleta #F36ACB, #22d3ee, #a3e635, #fbbf24, #8b5cf6 ou cor livre), o fundo (Preto, Grafite ou Brilho da cor), o título da máquina e a mensagem do prêmio. A cor do texto sobre a cor principal MUST ser escura para cores claras e branca para cores escuras.

#### Scenario: Logo do cliente
- **WHEN** o operador escolhe "Logo do cliente" e envia uma imagem
- **THEN** a máquina exibe o logo enviado no lugar do logo padrão

#### Scenario: Cor clara
- **WHEN** a cor principal é #a3e635
- **THEN** o texto do botão principal é exibido em cor escura

#### Scenario: Cor inválida
- **WHEN** a requisição informa uma cor fora do formato #RRGGBB
- **THEN** o servidor MUST recusar com 400

### Requirement: Máquina e efeitos
A configuração SHALL permitir escolher o tamanho (P, M, G ou Tela cheia) e ligar/desligar "Efeitos de luz", "Som" e "Exibir prêmios disponíveis".

#### Scenario: Luzes desligadas
- **WHEN** "Efeitos de luz" está desligado
- **THEN** as luzes do gabinete ficam fixas, sem piscar

#### Scenario: Tela cheia
- **WHEN** o tamanho é "Tela cheia"
- **THEN** a máquina é dimensionada para ocupar a área disponível

### Requirement: Participação
A configuração SHALL definir os créditos por lead (1 a 5, com botões − e +), quais campos padrão o cadastro pede (E-mail, Telefone, CPF; nome é sempre obrigatório) e quais perguntas personalizadas ativas são exibidas, com atalhos "Gerenciar →" e "+ Criar nova pergunta".

#### Scenario: Limites de créditos
- **WHEN** os créditos valem 5 e o operador clica "+"
- **THEN** o valor permanece 5; com 1 e "−" permanece 1

#### Scenario: Selecionar pergunta
- **WHEN** o operador liga a pergunta "Cargo"
- **THEN** o formulário de cadastro passa a exibir a pergunta "Cargo"

#### Scenario: Sem perguntas ativas
- **WHEN** não há campos personalizados ativos
- **THEN** é exibido "Nenhum campo personalizado ativo."

### Requirement: Pré-visualização interativa
A tela de configuração SHALL exibir, ao lado das opções, uma pré-visualização funcional da máquina com o fundo, a cor e o estilo configurados, e o botão "Abrir sorteio", que abre o modo de jogo em tela cheia. Jogadas feitas na pré-visualização MUST seguir as mesmas regras do modo de jogo.

#### Scenario: Abrir sorteio
- **WHEN** o operador clica "Abrir sorteio"
- **THEN** o modo de jogo é aberto com o evento selecionado, sem créditos e sem participante

#### Scenario: Alteração refletida
- **WHEN** o operador muda o fundo para "Grafite"
- **THEN** a pré-visualização passa a usar o fundo grafite imediatamente
