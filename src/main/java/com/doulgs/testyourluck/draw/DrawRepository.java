package com.doulgs.testyourluck.draw;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public interface DrawRepository extends JpaRepository<Draw, UUID>, JpaSpecificationExecutor<Draw> {

    long countByEventId(UUID eventId);

    long countByEventIdAndProductId(UUID eventId, UUID productId);

    @Query("select d.eventId, count(d) from Draw d group by d.eventId")
    List<Object[]> countGroupedByEvent();

    @Query("select d.productId, count(d) from Draw d group by d.productId")
    List<Object[]> countGroupedByProduct();

    @Query("select d.productId, count(d) from Draw d where d.eventId = :eventId group by d.productId")
    List<Object[]> countGroupedByProductForEvent(UUID eventId);

    /** Leads distintos: CPF quando informado, senão o próprio participante. */
    @Query("select count(distinct coalesce(p.cpf, cast(p.id as string))) from Draw d join d.participant p")
    long countDistinctLeads();

    @Query("select d from Draw d join fetch d.participant where d.drawnAt >= :from order by d.drawnAt")
    List<Draw> findDrawnSince(Instant from);

    @Query("select d from Draw d join fetch d.participant order by d.drawnAt desc")
    List<Draw> findRecent(Pageable pageable);

    default Map<UUID, Long> countByEvent() {
        return toMap(countGroupedByEvent());
    }

    default Map<UUID, Long> countByProduct() {
        return toMap(countGroupedByProduct());
    }

    default Map<UUID, Long> countByProductForEvent(UUID eventId) {
        return toMap(countGroupedByProductForEvent(eventId));
    }

    private static Map<UUID, Long> toMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(r -> (UUID) r[0], r -> (Long) r[1]));
    }
}
