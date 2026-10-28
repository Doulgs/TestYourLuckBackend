package com.doulgs.testyourluck.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public interface ProductEventRepository extends JpaRepository<ProductEvent, UUID> {

    List<ProductEvent> findByProductId(UUID productId);

    @Modifying(flushAutomatically = true)
    @Query("delete from ProductEvent pe where pe.productId = :productId")
    void deleteByProductId(UUID productId);

    /** Vínculos considerando apenas eventos não excluídos. */
    @Query("select pe from ProductEvent pe, Event e where e.id = pe.eventId")
    List<ProductEvent> findAllActive();

    default Map<UUID, Set<UUID>> eventsByProduct() {
        return findAllActive().stream().collect(Collectors.groupingBy(ProductEvent::getProductId,
                Collectors.mapping(ProductEvent::getEventId, Collectors.toSet())));
    }
}
