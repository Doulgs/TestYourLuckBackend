package com.doulgs.testyourluck.webhook;

import com.doulgs.testyourluck.shared.tenant.TenantContext;
import com.doulgs.testyourluck.webhook.WebhookTriggerEvents.LeadRegistered;
import com.doulgs.testyourluck.webhook.WebhookTriggerEvents.PlayCompleted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/** Agenda os disparos somente após o commit, em executor próprio, sem atrasar o jogo. */
@Slf4j
@Component
public class WebhookListener {

    private final WebhookDispatcher dispatcher;
    private final TaskExecutor executor;

    public WebhookListener(WebhookDispatcher dispatcher, @Qualifier("webhookExecutor") TaskExecutor executor) {
        this.dispatcher = dispatcher;
        this.executor = executor;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onLead(LeadRegistered e) {
        submit(e.tenant(), e.tenantOwner(), () -> dispatcher.dispatchLead(e.participantId()));
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onPlay(PlayCompleted e) {
        submit(e.tenant(), e.tenantOwner(), () -> dispatcher.dispatchPlay(e.drawId()));
    }

    /** O ThreadLocal de tenant não atravessa threads: é preenchido explicitamente na tarefa. */
    private void submit(UUID tenant, UUID owner, Runnable task) {
        try {
            executor.execute(() -> {
                TenantContext.set(tenant, owner);
                try {
                    task.run();
                } catch (RuntimeException ex) {
                    log.warn("Falha ao disparar webhooks: {}", ex.getMessage());
                } finally {
                    TenantContext.clear();
                }
            });
        } catch (RuntimeException ex) {
            log.warn("Fila de webhooks cheia, disparo descartado: {}", ex.getMessage());
        }
    }
}
