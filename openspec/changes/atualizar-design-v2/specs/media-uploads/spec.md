# Spec Delta

## Purpose

Armazena e serve as imagens enviadas pelos usuários (banner de evento, foto de produto e logo do cliente), isoladas por tenant.

## ADDED Requirements

### Requirement: Envio de imagem
O sistema SHALL aceitar o envio autenticado de imagens PNG, JPG, WEBP ou SVG de até 5 MB, registrando o arquivo no tenant do usuário e retornando um identificador e a URL pública de leitura.

#### Scenario: Envio válido
- **WHEN** o usuário envia uma imagem PNG de 800 KB
- **THEN** o sistema retorna 201 com id e URL da imagem

#### Scenario: Arquivo grande
- **WHEN** o usuário envia uma imagem de 8 MB
- **THEN** o sistema MUST responder 413 com a mensagem "Imagem maior que 5 MB"

#### Scenario: Tipo não suportado
- **WHEN** o usuário envia um arquivo PDF
- **THEN** o sistema MUST responder 415 "Formato de imagem não suportado"

### Requirement: Leitura de imagem
As imagens SHALL ser servidas por URL com identificador não sequencial e não adivinhável, sem exigir token (para uso em tags de imagem e no totem), com o tipo de conteúdo original e cache longo; SVGs MUST ser servidos com política de segurança que impeça execução de scripts.

#### Scenario: Abrir imagem
- **WHEN** o navegador requisita a URL retornada no envio
- **THEN** a imagem é retornada com o content-type correto

#### Scenario: Id inexistente
- **WHEN** a URL contém um id inexistente
- **THEN** o sistema responde 404

### Requirement: Referência por tenant
Entidades (evento, produto, configuração da máquina) SHALL aceitar apenas imagens enviadas pelo próprio tenant.

#### Scenario: Imagem de outro tenant
- **WHEN** um produto é salvo com o id de imagem enviada por outro tenant
- **THEN** o sistema MUST responder 404 "Imagem não encontrada"
