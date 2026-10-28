package com.doulgs.testyourluck.webhook;

import com.doulgs.testyourluck.draw.Draw;
import com.doulgs.testyourluck.draw.DrawRepository;
import com.doulgs.testyourluck.event.EventRepository;
import com.doulgs.testyourluck.shared.web.NotFoundException;
import com.doulgs.testyourluck.webhook.WebhookDtos.DeliveryDetail;
import com.doulgs.testyourluck.webhook.WebhookDtos.DeliverySummary;
import com.doulgs.testyourluck.webhook.WebhookDtos.WebhookRequest;
import com.doulgs.testyourluck.webhook.WebhookDtos.WebhookResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookRepository repository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final EventRepository eventRepository;
    private final DrawRepository drawRepository;
    private final WebhookDispatcher dispatcher;

    @Transactional(readOnly = true)
    public List<WebhookResponse> list() {
        return repository.findAllByOrderByCreatedAtAsc().stream().map(WebhookService::toResponse).toList();
    }

    @Transactional
    public WebhookResponse create(WebhookRequest req) {
        Webhook w = new Webhook();
        apply(w, req);
        repository.save(w);
        return toResponse(w);
    }

    @Transactional
    public WebhookResponse update(UUID id, WebhookRequest req) {
        Webhook w = get(id);
        apply(w, req);
        return toResponse(w);
    }

    /** Exclusão lógica: os disparos continuam visíveis nos leads. */
    @Transactional
    public void delete(UUID id) {
        get(id).setDeletedAt(Instant.now());
    }

    public DeliverySummary test(UUID id, UUID eventId) {
        get(id);
        return summary(dispatcher.test(id, eventId));
    }

    @Transactional(readOnly = true)
    public List<DeliverySummary> recent(int limit) {
        return deliveryRepository.findAllByOrderBySentAtDesc(PageRequest.of(0, Math.max(1, Math.min(limit, 50))))
                .stream().map(WebhookService::summary).toList();
    }

    @Transactional(readOnly = true)
    public List<DeliveryDetail> forLead(UUID drawId) {
        Draw d = drawRepository.findById(drawId).orElseThrow(() -> new NotFoundException("Lead não encontrado"));
        return deliveryRepository.findForLead(d.getId(), d.getParticipantId()).stream()
                .map(x -> new DeliveryDetail(x.getId(), x.getWebhookName(), x.getTrigger(), x.isOk(), status(x),
                        x.getMethod(), x.getUrl(), x.getSentAt(), x.getRequest(), x.getResponse()))
                .toList();
    }

    public Webhook get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Webhook não encontrado"));
    }

    static String status(WebhookDelivery d) {
        if (d.getStatusCode() == null) {
            return "ERRO";
        }
        return d.getStatusCode() + (d.isOk() ? " OK" : " ERRO");
    }

    private static DeliverySummary summary(WebhookDelivery d) {
        return new DeliverySummary(d.getId(), d.getWebhookName(), d.isOk(), status(d), d.getMethod(), d.getUrl(),
                d.isTest() ? "teste" : d.getEventName(), d.isTest(), d.getSentAt());
    }

    private void apply(Webhook w, WebhookRequest req) {
        List<UUID> events = req.eventIds() == null ? List.of() : req.eventIds().stream().distinct().toList();
        for (UUID id : events) {
            if (!eventRepository.existsById(id)) {
                throw new NotFoundException("Evento não encontrado");
            }
        }
        w.setName(req.name().trim());
        w.setTrigger(req.trigger());
        w.setMethod(req.method());
        w.setUrl(req.url().trim());
        w.setHeaders(clean(req.headers()));
        w.setParams(clean(req.params()));
        w.setBody(req.body());
        w.setEventIds(new ArrayList<>(events));
        w.setActive(req.active() == null || req.active());
    }

    private static List<KeyValue> clean(List<KeyValue> kv) {
        return kv == null ? new ArrayList<>() : new ArrayList<>(kv.stream()
                .filter(x -> x != null && ((x.k() != null && !x.k().isBlank()) || (x.v() != null && !x.v().isBlank())))
                .map(x -> new KeyValue(x.k() == null ? "" : x.k().trim(), x.v() == null ? "" : x.v()))
                .toList());
    }

    private static WebhookResponse toResponse(Webhook w) {
        return new WebhookResponse(w.getId(), w.getName(), w.getTrigger(), w.getMethod(), w.getUrl(),
                List.copyOf(w.getHeaders()), List.copyOf(w.getParams()), w.getBody(), List.copyOf(w.getEventIds()),
                w.isActive(), w.getDispatchCount(), w.getLastDispatchAt());
    }
}
