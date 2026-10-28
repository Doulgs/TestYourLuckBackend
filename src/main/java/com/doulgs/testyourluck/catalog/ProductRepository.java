package com.doulgs.testyourluck.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findAllByOrderByCreatedAtAsc();

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, UUID id);

    boolean existsByCategoryId(UUID categoryId);

    /** Produtos elegíveis no evento: vinculados, com peso > 0 e estoque > 0. */
    @Query("select p from Product p, ProductEvent pe where pe.productId = p.id and pe.eventId = :eventId "
            + "and p.weight > 0 and p.quantity > 0 order by p.createdAt")
    List<Product> findEligible(UUID eventId);

    /** Baixa condicional: nunca deixa o estoque negativo, mesmo sob concorrência. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Product p set p.quantity = p.quantity - 1, p.updatedAt = CURRENT_TIMESTAMP where p.id = :id and p.quantity > 0")
    int decrementIfAvailable(UUID id);

    @Query("select coalesce(sum(p.quantity), 0) from Product p")
    long totalQuantity();
}
