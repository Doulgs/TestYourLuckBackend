package com.doulgs.testyourluck.participant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface ParticipantRepository extends JpaRepository<Participant, UUID> {

    /** Consumo condicional: nunca deixa os créditos negativos, mesmo sob concorrência. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Participant p set p.creditsUsed = p.creditsUsed + 1, p.updatedAt = CURRENT_TIMESTAMP "
            + "where p.id = :id and p.creditsUsed < p.creditsGranted")
    int consumeCredit(UUID id);
}
