# Proposal

## Why

O design do TestYourLuck evoluiu (`design/TestYourLuck.dc.html`, revisão de 28/10/2026): a máquina passou a ser configurável por cliente (estilos de jogo, brand kit, créditos por lead), o cadastro do participante ganhou perguntas personalizadas e os leads precisam ser enviados a outros sistemas via webhooks. O modelo de estoque também mudou de "por evento" para "por produto com eventos vinculados", simplificando a operação. A aplicação atual reflete a primeira versão e precisa ser atualizada para não divergir do produto desenhado.

## What Changes

- **Visual e navegação**: cor de destaque `#F36ACB` com texto branco; ações das tabelas em menu "…"; modais com cabeçalho de ícone; toast. Menu: "Sorteios" substitui "Sorteio", **Estoque sai do menu**, entra "Campos personalizados" em Cadastros e o grupo "Integrações" com "Webhooks".
- **Eventos**: banner, data de início e fim, "dia todo" e horários, permitir sorteios fora do período, local, CEP, cidade, UF, região (derivada da UF), timezone, limite de sorteios (0 = ilimitado) e website. Tabela com período, local, sorteios "x / limite" e regra de período.
- **Produtos**: imagem, eventos vinculados (multi-seleção com "Todos os eventos"), peso de 0 a 100 com slider, **quantidade em estoque única por produto**, estoque mínimo, filtros por categoria e evento, status de estoque na listagem.
- **BREAKING — Estoque por evento removido**: a tela Estoque e os endpoints `/events/{id}/stock` deixam de existir; dados existentes são migrados (quantidade do produto = soma das quantidades por evento; vínculos = eventos onde havia estoque).
- **Sorteios**: tela de configuração com pré-visualização interativa e modo de jogo em tela cheia; estilos caça-níquel, roleta, slider e blocos; brand kit (logo padrão ou do cliente, cor principal, fundo, título, mensagem do prêmio); tamanho, luzes, som e lista de prêmios; créditos por lead (1 a 5); campos do cadastro (e-mail, telefone e CPF opcionais; nome sempre obrigatório) e perguntas personalizadas; bloqueio por limite de sorteios e fora do período. O servidor continua decidindo o prêmio e passa a controlar os créditos.
- **Campos personalizados**: cadastro de perguntas extras (texto, número, data, seleção, sim/não) com chave de variável, obrigatoriedade e ativação.
- **Webhooks**: cadastro de integrações com gatilho (lead cadastrado ou jogada concluída), eventos vinculados, método, URL, headers, query params e body com variáveis `{{...}}` (autocomplete com `@`), importação de cURL, ativar/desativar, teste de disparo, contador e últimos disparos. Disparo real pelo backend com registro de request e response.
- **Leads**: coluna com status do webhook e modal com os disparos (JSON copiável); CSV inclui as respostas dos campos personalizados.
- **Dashboard**: "Estoque baixo" e "Brindes em estoque" passam a usar o estoque dos produtos.
- **Uploads de imagem** para banner de evento, foto de produto e logo do cliente.

## Capabilities

### New Capabilities
- `draw-machine-settings`: configuração da máquina por empresa (estilo, brand kit, tamanho, efeitos, créditos por lead, campos do cadastro e perguntas exibidas) e pré-visualização.
- `custom-fields`: perguntas personalizadas do cadastro do participante e suas chaves de variável.
- `webhooks`: integrações HTTP disparadas por cadastro de lead ou jogada, com templates de variáveis, teste e histórico de disparos.
- `media-uploads`: envio e exibição de imagens (banner, produto, logo) isoladas por tenant.

### Modified Capabilities
- `app-shell`: novos grupos de navegação, menu de ações por linha, modo de jogo separado da tela de configuração.
- `event-management`: novos campos do evento (período, horários, endereço, timezone, limite, website, banner) e nova listagem.
- `product-catalog`: imagem, eventos vinculados, peso 0–100, estoque por produto, filtros e status de estoque.
- `event-stock`: capability retirada (substituída pelo estoque do produto).
- `prize-draw`: créditos por lead, cadastro configurável, estilos de jogo, bloqueios do evento, pool baseado em produtos vinculados.
- `lead-management`: status e detalhes dos disparos de webhook; CSV com campos personalizados.
- `dashboard-insights`: indicadores de estoque baseados nos produtos.

## Impact

- **Backend**: migration `V2` (novos campos em `events` e `products`; tabelas `product_events`, `participants`, `custom_fields`, `draw_settings`, `webhooks`, `webhook_deliveries`, `media_files`; migração e remoção de `event_stocks`; `draws` passa a referenciar `participants`). Novos endpoints `/participants`, `/draw-settings`, `/custom-fields`, `/webhooks`, `/media`; `/draws` muda de contrato. Cliente HTTP para webhooks e armazenamento de arquivos em disco.
- **Frontend**: telas novas (Sorteios config/jogo, Campos personalizados, Webhooks), remoção de Estoque, modais e listagens refeitos, componentes de jogo (roleta, slider, blocos).
- **Contrato**: `docs/api-contract.md` atualizado. Clientes do contrato antigo de estoque/sorteio quebram (só o próprio frontend).
- **Fora de escopo**: busca automática de endereço por CEP (apenas máscara), configuração da máquina diferente por evento, reenvio manual de webhooks com erro, retentativas automáticas, armazenamento de arquivos em nuvem.
