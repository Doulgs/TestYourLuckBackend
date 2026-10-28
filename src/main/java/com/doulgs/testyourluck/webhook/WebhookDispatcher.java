package com.doulgs.testyourluck.webhook;

import com.doulgs.testyourluck.catalog.Category;
import com.doulgs.testyourluck.catalog.CategoryRepository;
import com.doulgs.testyourluck.catalog.Product;
import com.doulgs.testyourluck.catalog.ProductRepository;
import com.doulgs.testyourluck.draw.Draw;
import com.doulgs.testyourluck.draw.DrawRepository;
import com.doulgs.testyourluck.event.Event;
import com.doulgs.testyourluck.event.EventRepository;
import com.doulgs.testyourluck.field.CustomField;
import com.doulgs.testyourluck.field.CustomFieldRepository;
import com.doulgs.testyourluck.participant.Participant;
import com.doulgs.testyourluck.participant.ParticipantRepository;
import com.doulgs.testyourluck.settings.DrawSettingsService;
import com.doulgs.testyourluck.webhook.WebhookEnums.Trigger;
import com.doulgs.testyourluck.webhook.WebhookSender.Outcome;
import com.doulgs.testyourluck.webhook.WebhookVariables.EventData;
import com.doulgs.testyourluck.webhook.WebhookVariables.Lead;
import com.doulgs.testyourluck.webhook.WebhookVariables.Prize;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Executa os disparos: carrega o contexto (lead, evento, prêmio), resolve as variáveis, envia fora de
 * transação e grava o registro do disparo. Requer TenantContext preenchido pelo chamador.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDispatcher {

    record Context(UUID participantId, UUID drawId, String eventName, UUID eventId, Map<String, String> vars) {
    }

    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final ParticipantRepository participantRepository;
    private final DrawRepository drawRepository;
    private final EventRepository eventRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CustomFieldRepository customFieldRepository;
    private final DrawSettingsService settingsService;
    private final WebhookSender sender;
    private final TransactionTemplate tx;

    public void dispatchLead(UUID participantId) {
        Context ctx = tx.execute(s -> participantRepository.findById(participantId)
                .map(p -> context(p, null)).orElse(null));
        dispatchMatching(Trigger.LEAD, ctx);
    }

    public void dispatchPlay(UUID drawId) {
        Context ctx = tx.execute(s -> drawRepository.findById(drawId)
                .map(d -> context(participantRepository.findById(d.getParticipantId()).orElseThrow(), d))
                .orElse(null));
        dispatchMatching(Trigger.PLAY, ctx);
    }

    /** Disparo de teste com dados de exemplo; o registro é marcado como teste. */
    public WebhookDelivery test(UUID webhookId, UUID eventId) {
        record Prepared(Webhook webhook, Map<String, String> vars, String eventName) {
        }
        Prepared prepared = tx.execute(s -> {
            Webhook w = webhookRepository.findById(webhookId).orElseThrow();
            UUID evId = eventId != null ? eventId : w.getEventIds().stream().findFirst().orElse(null);
            Event ev = evId == null ? null : eventRepository.findById(evId).orElse(null);
            if (ev == null) {
                ev = eventRepository.findAll().stream().findFirst().orElse(null);
            }
            Product product = productRepository.findAllByOrderByCreatedAtAsc().stream().findFirst().orElse(null);
            Lead lead = new Lead("Maria Teste", "maria@exemplo.com", "11999990000", "12345678900", Map.of());
            Map<String, String> vars = WebhookVariables.build(lead, fields(), eventData(ev), prize(product),
                    settingsService.current().getStyle(), Instant.now(), true);
            return new Prepared(w, vars, ev == null ? null : ev.getName());
        });
        Outcome outcome = sender.send(prepared.webhook(), prepared.vars());
        return record(prepared.webhook(), outcome, null, null, prepared.eventName(), true);
    }

    private void dispatchMatching(Trigger trigger, Context ctx) {
        if (ctx == null) {
            return;
        }
        List<Webhook> hooks = tx.execute(s -> webhookRepository.findAllByOrderByCreatedAtAsc().stream()
                .filter(w -> w.isActive() && w.getTrigger() == trigger && w.getEventIds().contains(ctx.eventId()))
                .toList());
        for (Webhook w : hooks) {
            try {
                Outcome outcome = sender.send(w, ctx.vars());
                record(w, outcome, ctx.participantId(), ctx.drawId(), ctx.eventName(), false);
            } catch (RuntimeException ex) {
                log.warn("Falha ao registrar disparo do webhook {}: {}", w.getId(), ex.getMessage());
            }
        }
    }

    private WebhookDelivery record(Webhook w, Outcome o, UUID participantId, UUID drawId, String eventName, boolean test) {
        return tx.execute(s -> {
            WebhookDelivery d = new WebhookDelivery();
            d.setWebhookId(w.getId());
            d.setWebhookName(w.getName());
            d.setTrigger(w.getTrigger());
            d.setTest(test);
            d.setParticipantId(participantId);
            d.setDrawId(drawId);
            d.setEventName(eventName);
            d.setMethod(o.method());
            d.setUrl(o.url().length() > 4000 ? o.url().substring(0, 4000) : o.url());
            d.setRequest(o.request());
            d.setResponse(o.response());
            d.setStatusCode(o.statusCode());
            d.setOk(o.ok());
            d.setError(o.error() == null ? null : o.error().substring(0, Math.min(500, o.error().length())));
            d.setSentAt(Instant.now());
            deliveryRepository.save(d);
            webhookRepository.registerDispatch(w.getId(), d.getSentAt());
            return d;
        });
    }

    private Context context(Participant p, Draw d) {
        Event ev = eventRepository.findById(p.getEventId()).orElse(null);
        Product product = d == null ? null : productRepository.findById(d.getProductId()).orElse(null);
        Prize prize = d == null ? null : product != null ? prize(product) : new Prize(d.getProductName(), null, null);
        Lead lead = new Lead(p.getName(), p.getEmail(), p.getPhone(), p.getCpf(), p.getAnswers());
        Map<String, String> vars = WebhookVariables.build(lead, fields(), eventData(ev), prize,
                settingsService.current().getStyle(), d == null ? Instant.now() : d.getDrawnAt(), false);
        String eventName = ev != null ? ev.getName() : d != null ? d.getEventName() : null;
        return new Context(p.getId(), d == null ? null : d.getId(), eventName, p.getEventId(), vars);
    }

    private List<CustomField> fields() {
        return customFieldRepository.findAllByOrderByCreatedAtAsc();
    }

    private static EventData eventData(Event ev) {
        return ev == null ? null : new EventData(ev.getId(), ev.getName(), ev.getPlace());
    }

    private Prize prize(Product p) {
        if (p == null) {
            return null;
        }
        String category = p.getCategoryId() == null ? null
                : categoryRepository.findById(p.getCategoryId()).map(Category::getName).orElse(null);
        return new Prize(p.getName(), p.getSku(), category);
    }
}
