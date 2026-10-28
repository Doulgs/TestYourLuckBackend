package com.doulgs.testyourluck.catalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record CategoryRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 120) String name,
            @Size(max = 255) String description) {
    }

    public record CategoryResponse(UUID id, String name, String description, int productCount,
                                   List<String> productNames, long stock, double chance) {
    }

    public record ProductRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 160) String name,
            @NotBlank(message = "Informe o SKU") @Size(max = 60) String sku,
            UUID categoryId,
            @NotNull(message = "Informe o peso") @Min(value = 0, message = "O peso deve estar entre 0 e 100")
            @Max(value = 100, message = "O peso deve estar entre 0 e 100") Integer weight,
            @NotNull(message = "Informe o estoque mínimo") @Min(value = 0, message = "O estoque mínimo deve ser maior ou igual a 0") Integer minStock,
            @JsonProperty("short") @Pattern(regexp = "^[A-Za-z0-9]{0,2}$", message = "A sigla deve ter até 2 letras") String shortCode,
            @NotNull(message = "Informe a quantidade") @Min(value = 0, message = "A quantidade deve ser maior ou igual a 0") Integer quantity,
            Set<UUID> eventIds,
            UUID imageMediaId) {
    }

    public record ProductResponse(UUID id, String name, @JsonProperty("short") String shortCode, String sku, UUID categoryId,
                                  String categoryName, int weight, int minStock, int hue, double chance,
                                  int quantity, StockStatus stockStatus, List<UUID> eventIds, UUID imageMediaId,
                                  String imageUrl, long drawn) {
    }
}
