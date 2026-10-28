package com.doulgs.testyourluck.catalog;

import com.doulgs.testyourluck.shared.tenant.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Vínculo produto ↔ evento: o produto só entra no sorteio dos eventos vinculados. */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "product_events")
public class ProductEvent extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    public ProductEvent(UUID productId, UUID eventId) {
        this.productId = productId;
        this.eventId = eventId;
    }
}
