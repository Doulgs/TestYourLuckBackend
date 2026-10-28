package com.doulgs.testyourluck.webhook;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

    List<WebhookDelivery> findAllByOrderBySentAtDesc(Pageable pageable);

    /** Disparos de um lead: os da jogada e os do cadastro do participante. */
    @Query("select d from WebhookDelivery d where d.drawId = :drawId "
            + "or (d.participantId = :participantId and d.trigger = com.doulgs.testyourluck.webhook.WebhookEnums.Trigger.LEAD) "
            + "order by d.sentAt")
    List<WebhookDelivery> findForLead(UUID drawId, UUID participantId);

    @Query("select d from WebhookDelivery d where d.drawId in :drawIds "
            + "or (d.participantId in :participantIds and d.trigger = com.doulgs.testyourluck.webhook.WebhookEnums.Trigger.LEAD)")
    List<WebhookDelivery> findForLeads(Collection<UUID> drawIds, Collection<UUID> participantIds);
}
