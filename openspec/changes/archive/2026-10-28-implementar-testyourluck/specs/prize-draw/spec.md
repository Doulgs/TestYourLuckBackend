# Spec Delta

## Purpose

Executa o sorteio de brindes em modo totem: coleta os dados do participante, sorteia de forma ponderada e confiável no servidor, baixa o estoque e apresenta o resultado com a experiência de slot machine.

## ADDED Requirements

### Requirement: Formulário do participante
Antes de cada giro, o totem SHALL exibir sobre a máquina o formulário "Teste sua sorte" com Nome completo, E-mail, Telefone e CPF. Nome e CPF com 11 dígitos MUST ser obrigatórios; e-mail, quando informado, MUST ser válido.

#### Scenario: Dados válidos
- **WHEN** o participante informa nome e CPF com 11 dígitos e clica "Continuar"
- **THEN** o formulário é ocultado e a máquina fica disponível para girar

#### Scenario: Dados inválidos
- **WHEN** o participante clica "Continuar" sem nome ou com CPF incompleto
- **THEN** é exibida a mensagem "Informe nome e CPF válidos para girar." e a máquina não é liberada

### Requirement: Máscaras de entrada
O CPF SHALL ser formatado como `000.000.000-00` e o telefone como `(00) 00000-0000` durante a digitação, aceitando apenas dígitos.

#### Scenario: Máscara de telefone
- **WHEN** o participante digita "11987654321"
- **THEN** o campo exibe "(11) 98765-4321"

### Requirement: Seleção de evento
O totem SHALL exibir um seletor com os eventos do tenant; o sorteio MUST usar o estoque do evento selecionado.

#### Scenario: Trocar evento
- **WHEN** o operador seleciona outro evento
- **THEN** os indicadores e os prêmios exibidos passam a refletir o estoque desse evento

### Requirement: Sorteio ponderado no servidor
O resultado SHALL ser definido pelo servidor, sorteando aleatoriamente entre os produtos ativos do evento com peso > 0 e quantidade > 0, com probabilidade proporcional ao peso. O cliente MUST NOT definir o brinde.

#### Scenario: Produto sem estoque fica de fora
- **WHEN** um produto tem peso 30 mas quantidade 0 no evento
- **THEN** ele MUST NOT ser sorteado e as chances são recalculadas entre os demais

#### Scenario: Sem estoque no evento
- **WHEN** nenhum produto do evento tem peso > 0 e quantidade > 0
- **THEN** o servidor MUST responder 422 e o totem exibe "Sem estoque disponível neste evento."

#### Scenario: Evento não ativo
- **WHEN** o evento selecionado não tem status Ativo
- **THEN** o servidor MUST responder 422 com a mensagem "Evento não está ativo"

### Requirement: Baixa de estoque e registro atômicos
Cada sorteio SHALL, de forma atômica, decrementar em 1 a quantidade do produto sorteado no evento e registrar o lead com nome, e-mail, telefone, CPF, evento, produto e data/hora. Sorteios simultâneos no mesmo evento MUST NOT resultar em quantidade negativa.

#### Scenario: Baixa de estoque
- **WHEN** um sorteio resulta em "Caneca" com 44 unidades no evento
- **THEN** a quantidade passa a 43 e um novo lead é registrado

#### Scenario: Concorrência no último item
- **WHEN** dois sorteios simultâneos disputam o único item restante de um produto
- **THEN** no máximo um deles recebe esse produto e a quantidade final é 0

### Requirement: Experiência da slot machine
A máquina SHALL exibir 3 rolos com sigla e nome dos produtos, painel de status, luzes animadas, alavanca e botão "GIRAR". Ao girar, os rolos MUST parar em sequência (2s, 2,6s, 3,2s) exibindo o brinde sorteado na linha central.

#### Scenario: Girar
- **WHEN** o participante clica em "GIRAR" ou puxa a alavanca
- **THEN** o botão exibe "GIRANDO...", o painel exibe "BOA SORTE" e os rolos param no brinde sorteado

#### Scenario: Giro em andamento
- **WHEN** um giro está em andamento e o botão é clicado novamente
- **THEN** nenhum novo sorteio é iniciado

### Requirement: Indicadores e prêmios da máquina
O totem SHALL exibir "BRINDES EM JOGO" (produtos elegíveis), "GIROS NO EVENTO" (sorteios do evento) e a lista "PRÊMIOS DESTA MÁQUINA" com os produtos elegíveis.

#### Scenario: Painel com brindes
- **WHEN** o evento possui 6 produtos elegíveis
- **THEN** o painel exibe "6 BRINDES EM JOGO · PUXE A ALAVANCA"

### Requirement: Som
O totem SHALL emitir efeitos sonoros ao girar, a cada parada de rolo e ao exibir o resultado, com botão para ativar/desativar o som.

#### Scenario: Silenciar
- **WHEN** o operador desativa o som
- **THEN** nenhum efeito sonoro é emitido nos próximos giros

### Requirement: Resultado do sorteio
Após a parada dos rolos, o totem SHALL exibir um modal com "PARABÉNS, <PRIMEIRO NOME>", sigla e nome do brinde, e "Retire seu brinde no balcão · <evento>". O botão "Próximo participante" MUST limpar os dados e reabrir o formulário.

#### Scenario: Próximo participante
- **WHEN** o operador clica "Próximo participante"
- **THEN** o modal fecha, os campos do participante ficam vazios e o formulário é exibido novamente
