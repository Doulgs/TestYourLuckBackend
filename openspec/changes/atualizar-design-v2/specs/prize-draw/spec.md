# Spec Delta

## MODIFIED Requirements

### Requirement: Formulário do participante
Quando não há créditos, o totem SHALL exibir sobre a máquina o formulário de cadastro com o título da máquina, o subtítulo "Preencha seus dados para liberar <n> crédito(s)", Nome completo (sempre obrigatório), os campos E-mail, Telefone e CPF que estiverem habilitados na configuração e as perguntas personalizadas selecionadas, com os botões "Liberar créditos" e "Cancelar".

#### Scenario: Dados válidos
- **WHEN** o participante preenche os campos exigidos e clica "Liberar créditos"
- **THEN** o lead é registrado, o formulário é ocultado e os créditos configurados ficam disponíveis

#### Scenario: Dados inválidos
- **WHEN** o participante clica "Liberar créditos" com algum campo exigido inválido
- **THEN** é exibida a mensagem de erro do primeiro campo inválido e nenhum crédito é liberado

#### Scenario: Nome vazio
- **WHEN** o participante clica "Liberar créditos" sem nome
- **THEN** é exibida a mensagem "Informe seu nome." e nenhum crédito é liberado

#### Scenario: Campos habilitados inválidos
- **WHEN** o e-mail habilitado é inválido, o telefone habilitado tem menos de 10 dígitos ou o CPF habilitado não tem 11 dígitos
- **THEN** é exibida respectivamente "Informe um e-mail válido.", "Informe um telefone válido." ou "Informe um CPF válido."

#### Scenario: Campo desabilitado
- **WHEN** o CPF está desabilitado na configuração
- **THEN** o formulário não exibe o campo CPF e o servidor aceita o cadastro sem CPF

#### Scenario: Pergunta obrigatória sem resposta
- **WHEN** uma pergunta personalizada obrigatória (exceto sim/não) está vazia
- **THEN** é exibida a mensagem "Preencha: <rótulo>"

#### Scenario: Cancelar
- **WHEN** o participante clica "Cancelar"
- **THEN** o formulário fecha sem registrar o lead

### Requirement: Seleção de evento
A tela de configuração de sorteios SHALL exibir um seletor com os eventos do tenant; a pré-visualização e o modo de jogo MUST usar os produtos vinculados e as regras do evento selecionado.

#### Scenario: Trocar evento
- **WHEN** o operador seleciona outro evento
- **THEN** os indicadores e os prêmios exibidos passam a refletir os produtos vinculados a esse evento, e o jogo em andamento é reiniciado

### Requirement: Sorteio ponderado no servidor
O resultado SHALL ser definido pelo servidor, sorteando aleatoriamente entre os produtos ativos vinculados ao evento com peso > 0 e quantidade em estoque > 0, com probabilidade proporcional ao peso. O cliente MUST NOT definir o brinde.

#### Scenario: Produto sem estoque fica de fora
- **WHEN** um produto vinculado tem peso 30 mas quantidade 0
- **THEN** ele MUST NOT ser sorteado e as chances são recalculadas entre os demais

#### Scenario: Produto não vinculado fica de fora
- **WHEN** um produto com estoque não está vinculado ao evento selecionado
- **THEN** ele MUST NOT ser sorteado nesse evento

#### Scenario: Sem estoque no evento
- **WHEN** nenhum produto vinculado ao evento tem peso > 0 e quantidade > 0
- **THEN** o servidor MUST responder 422 e a máquina exibe "SEM ESTOQUE NESTE EVENTO"

#### Scenario: Evento não ativo
- **WHEN** o evento selecionado não tem status Ativo
- **THEN** o servidor MUST responder 422 com a mensagem "Evento não está ativo"

### Requirement: Baixa de estoque e registro atômicos
Cada jogada SHALL, de forma atômica, consumir 1 crédito do participante, decrementar em 1 a quantidade em estoque do produto sorteado e registrar a jogada com participante, evento, produto e data/hora. Jogadas simultâneas MUST NOT resultar em quantidade negativa nem em créditos negativos.

#### Scenario: Baixa de estoque
- **WHEN** uma jogada resulta em "Caneca" com 44 unidades
- **THEN** a quantidade passa a 43, o participante perde 1 crédito e a jogada aparece em Leads

#### Scenario: Concorrência no último item
- **WHEN** duas jogadas simultâneas disputam o único item restante de um produto
- **THEN** no máximo uma delas recebe esse produto e a quantidade final é 0

#### Scenario: Sem créditos
- **WHEN** uma jogada é solicitada para um participante sem créditos restantes
- **THEN** o servidor MUST responder 422 com a mensagem "Sem créditos disponíveis" e nada é registrado

### Requirement: Experiência da slot machine
No estilo caça-níquel, a máquina SHALL exibir 3 rolos com imagem ou sigla e nome dos produtos, painel de status, luzes, alavanca e botão principal. Ao jogar, os rolos MUST parar em sequência (2s, 2,6s, 3,2s) exibindo o brinde sorteado na linha central.

#### Scenario: Girar
- **WHEN** o participante com crédito clica em "GIRAR" ou puxa a alavanca
- **THEN** o botão exibe "· · ·", o painel exibe "BOA SORTE" e os rolos param no brinde sorteado

#### Scenario: Giro em andamento
- **WHEN** uma jogada está em andamento ou o resultado está aberto e o botão é clicado
- **THEN** nenhuma nova jogada é iniciada

#### Scenario: Sem créditos
- **WHEN** o participante sem créditos clica no botão "CADASTRAR PARA JOGAR"
- **THEN** o formulário de cadastro é aberto

### Requirement: Indicadores e prêmios da máquina
A máquina SHALL exibir os indicadores "CRÉDITOS" (marcadores e número de créditos restantes), "BRINDES EM JOGO" (produtos elegíveis) e "JOGADAS NO EVENTO" (jogadas do evento), e, quando habilitado na configuração, a lista de prêmios elegíveis abaixo da máquina. O painel de status MUST exibir, nesta ordem de prioridade: bloqueio do evento, "SEM ESTOQUE NESTE EVENTO", "BOA SORTE" durante a jogada, "TEMOS UM GANHADOR" após o resultado, "CADASTRE-SE PARA LIBERAR O JOGO" sem créditos, ou "<n> CRÉDITO(S) · <instrução do estilo>".

#### Scenario: Painel com brindes
- **WHEN** o evento possui 6 produtos elegíveis
- **THEN** o indicador "BRINDES EM JOGO" exibe 6 e a lista de prêmios exibe os 6 produtos

#### Scenario: Painel com créditos
- **WHEN** o participante tem 2 créditos no estilo roleta
- **THEN** o painel exibe "2 CRÉDITOS · GIRE A ROLETA"

#### Scenario: Sem cadastro
- **WHEN** não há créditos e o evento tem estoque
- **THEN** o painel exibe "CADASTRE-SE PARA LIBERAR O JOGO"

### Requirement: Som
A máquina SHALL emitir efeitos sonoros ao liberar créditos, ao jogar, durante a animação e ao exibir o resultado, quando o som estiver habilitado na configuração; o modo de jogo MUST ter um botão para ativar/desativar o som.

#### Scenario: Silenciar
- **WHEN** o operador desativa o som
- **THEN** nenhum efeito sonoro é emitido nas próximas jogadas

### Requirement: Resultado do sorteio
Após a animação, a máquina SHALL exibir um modal com "PARABÉNS, <PRIMEIRO NOME>", imagem ou sigla e nome do brinde, e "<mensagem do prêmio> · <evento>". Se restarem créditos, o botão MUST ser "Jogar novamente (<n>)" e manter o participante; caso contrário, "Próximo participante", que limpa os dados do participante.

#### Scenario: Próximo participante
- **WHEN** não há créditos restantes e o operador clica "Próximo participante"
- **THEN** o modal fecha e os dados do participante são limpos

#### Scenario: Jogar novamente
- **WHEN** restam 2 créditos e o participante clica "Jogar novamente (2)"
- **THEN** o modal fecha e a máquina fica pronta para nova jogada do mesmo participante

## ADDED Requirements

### Requirement: Créditos por lead
O cadastro do participante SHALL liberar a quantidade de créditos definida na configuração (1 a 5), controlada pelo servidor; cada jogada consome 1 crédito.

#### Scenario: Créditos liberados
- **WHEN** a configuração define 3 créditos e o participante conclui o cadastro
- **THEN** o servidor registra o participante com 3 créditos e a máquina exibe 3 marcadores acesos

### Requirement: Bloqueios do evento
O servidor SHALL recusar cadastros e jogadas quando o evento atingiu o limite de sorteios (limite > 0) ou quando o momento atual, no timezone do evento, está fora do período (início às 00:00 ou hora de início; fim às 23:59 ou hora de fim) e o evento não permite sorteios fora da data. A máquina MUST exibir o motivo no painel.

#### Scenario: Limite atingido
- **WHEN** o evento tem limite 50 e já possui 50 jogadas
- **THEN** o servidor MUST responder 422 "Limite de sorteios atingido" e o painel exibe "LIMITE DE SORTEIOS ATINGIDO"

#### Scenario: Fora do período
- **WHEN** o evento termina ontem e não permite sorteios fora da data
- **THEN** o servidor MUST responder 422 "Fora do período do evento" e o painel exibe "FORA DO PERÍODO DO EVENTO"

#### Scenario: Fora do período permitido
- **WHEN** o evento termina ontem e permite sorteios fora da data
- **THEN** a jogada é aceita

### Requirement: Estilo roleta
No estilo roleta, a máquina SHALL exibir uma roda com 8 a 12 segmentos coloridos formados pelos produtos elegíveis (repetidos quando há menos de 8), ponteiro no topo e centro clicável; ao jogar, a roda MUST girar por 5 segundos e parar com o brinde sorteado sob o ponteiro.

#### Scenario: Girar roleta
- **WHEN** o participante com crédito clica "GIRAR ROLETA" ou no centro da roda
- **THEN** a roda gira e para em um segmento do brinde sorteado pelo servidor

### Requirement: Estilo slider
No estilo slider, a máquina SHALL exibir uma faixa horizontal de cartões de prêmios com um marcador central; ao jogar, a faixa MUST deslizar por 5,5 segundos e parar com o cartão do brinde sorteado sob o marcador, destacado na cor principal.

#### Scenario: Sortear no slider
- **WHEN** o participante com crédito clica "SORTEAR"
- **THEN** a faixa desliza e o cartão do brinde sorteado fica destacado sob o marcador

### Requirement: Estilo blocos
No estilo blocos, a máquina SHALL exibir 12 blocos fechados com "?"; ao iniciar, consome 1 crédito e distribui 3 blocos do brinde sorteado e 9 de outros produtos. O participante revela blocos até encontrar os 3 do brinde sorteado, quando o resultado MUST ser exibido e todos os blocos revelados.

#### Scenario: Revelar blocos
- **WHEN** o participante revela blocos e encontra o terceiro bloco do brinde sorteado
- **THEN** todos os blocos são revelados, os vencedores ficam destacados e o resultado é exibido

#### Scenario: Progresso
- **WHEN** o participante encontrou 1 dos 3 blocos vencedores
- **THEN** o painel exibe "ENCONTRE 3 IGUAIS · 1/3"
