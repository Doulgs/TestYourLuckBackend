package com.doulgs.testyourluck.webhook;

import java.util.UUID;

/** Eventos de domínio que disparam webhooks após o commit. */
public final class WebhookTriggerEvents {

    private WebhookTriggerEvents() {
    }

    public record LeadRegistered(UUID tenant, UUID tenantOwner, UUID participantId) {
    }

    public record PlayCompleted(UUID tenant, UUID tenantOwner, UUID drawId) {
    }
}
