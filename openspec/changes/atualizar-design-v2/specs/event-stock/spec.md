# Spec Delta

## REMOVED Requirements

### Requirement: Estoque por evento e produto
**Reason**: O design passou a controlar uma única quantidade por produto, compartilhada pelos eventos vinculados.
**Migration**: Use "Estoque do produto" e "Eventos vinculados ao produto" em `product-catalog`. A migration de banco soma as quantidades por evento em `products.quantity` e cria os vínculos a partir dos eventos que tinham estoque.

### Requirement: Visualização do estoque de um evento
**Reason**: A tela Estoque foi removida do design.
**Migration**: A quantidade e o status de estoque passam a ser exibidos na listagem de Produtos, filtrável por evento.

### Requirement: Ajuste de quantidade
**Reason**: Não há mais ajuste por evento.
**Migration**: A quantidade é editada no modal do produto ("Quantidade em estoque").

### Requirement: Status de nível de estoque
**Reason**: O status passou a ser do produto.
**Migration**: Ver "Estoque do produto" em `product-catalog` (OK, Estoque baixo, Esgotado).
