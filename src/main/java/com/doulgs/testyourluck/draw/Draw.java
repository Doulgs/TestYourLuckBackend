package com.doulgs.testyourluck.draw;

import com.doulgs.testyourluck.participant.Participant;
import com.doulgs.testyourluck.shared.tenant.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma jogada (e uma linha em Leads). Guarda um snapshot dos nomes de evento e produto para que o
 * histórico continue legível após exclusões ou renomeações; os dados pessoais ficam no participante.
 */
@Getter
@Setter
@Entity
@Table(name = "draws")
public class Draw extends BaseEntity {

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "participant_id", nullable = false)
    private UUID participantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", insertable = false, updatable = false)
    private Participant participant;

    @Column(name = "event_name", nullable = false, length = 160)
    private String eventName;

    @Column(name = "product_name", nullable = false, length = 160)
    private String productName;

    @Column(name = "product_short", nullable = false, length = 2)
    private String productShort;

    @Column(name = "product_hue", nullable = false)
    private int productHue;

    @Column(name = "drawn_at", nullable = false)
    private Instant drawnAt;
}
