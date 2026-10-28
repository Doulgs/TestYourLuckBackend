package com.doulgs.testyourluck.webhook;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WebhookRepository extends JpaRepository<Webhook, UUID> {

    List<Webhook> findAllByOrderByCreatedAtAsc();

    @Modifying
    @Query("update Webhook w set w.dispatchCount = w.dispatchCount + 1, w.lastDispatchAt = :at where w.id = :id")
    void registerDispatch(UUID id, Instant at);
}
