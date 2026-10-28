package com.doulgs.testyourluck.lead;

import com.doulgs.testyourluck.draw.Draw;
import com.doulgs.testyourluck.draw.DrawRepository;
import com.doulgs.testyourluck.field.CustomField;
import com.doulgs.testyourluck.field.CustomFieldRepository;
import com.doulgs.testyourluck.lead.LeadDtos.LeadResponse;
import com.doulgs.testyourluck.lead.LeadDtos.PageResponse;
import com.doulgs.testyourluck.lead.LeadDtos.WebhookStatus;
import com.doulgs.testyourluck.participant.Participant;
import com.doulgs.testyourluck.shared.util.Formats;
import com.doulgs.testyourluck.webhook.WebhookDelivery;
import com.doulgs.testyourluck.webhook.WebhookDeliveryRepository;
import com.doulgs.testyourluck.webhook.WebhookEnums.Trigger;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class LeadService {

    static final int DEFAULT_SIZE = 50;
    static final int MAX_SIZE = 200;
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "drawnAt");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(Formats.ZONE);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm").withZone(Formats.ZONE);

    private final DrawRepository drawRepository;
    private final CustomFieldRepository customFieldRepository;
    private final WebhookDeliveryRepository deliveryRepository;

    @Transactional(readOnly = true)
    public PageResponse<LeadResponse> search(String q, UUID eventId, Integer page, Integer size) {
        int p = page == null || page < 0 ? 0 : page;
        int s = size == null || size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        Page<Draw> result = drawRepository.findAll(filter(q, eventId), PageRequest.of(p, s, NEWEST_FIRST));
        Map<UUID, WebhookStatus> status = webhookStatus(result.getContent());
        List<LeadResponse> content = result.getContent().stream()
                .map(d -> LeadResponse.of(d, status.getOrDefault(d.getId(), WebhookStatus.NONE))).toList();
        return new PageResponse<>(content, p, s, result.getTotalElements(), result.getTotalPages());
    }

    /** CSV para Excel pt-BR: separador ';', valores entre aspas, BOM UTF-8 e uma coluna por campo personalizado. */
    @Transactional(readOnly = true)
    public String exportCsv(String q, UUID eventId) {
        List<Draw> leads = drawRepository.findAll(filter(q, eventId), NEWEST_FIRST);
        List<CustomField> fields = customFieldRepository.findAllByOrderByCreatedAtAsc();
        List<String> header = new ArrayList<>(List.of("Nome", "E-mail", "Telefone", "CPF"));
        fields.forEach(f -> header.add(f.getLabel()));
        header.addAll(List.of("Evento", "Brinde", "Data", "Hora"));
        Stream<List<String>> rows = leads.stream().map(d -> {
            Participant p = d.getParticipant();
            List<String> row = new ArrayList<>(List.of(p.getName(), nvl(p.getEmail()), Formats.phone(p.getPhone()),
                    Formats.cpf(p.getCpf())));
            fields.forEach(f -> row.add(nvl(p.getAnswers().get(f.getFieldKey()))));
            row.addAll(List.of(d.getEventName(), d.getProductName(), DATE.format(d.getDrawnAt()),
                    TIME.format(d.getDrawnAt())));
            return row;
        });
        return "﻿" + Stream.concat(Stream.of(header), rows)
                .map(r -> r.stream().map(v -> "\"" + v.replace("\"", "\"\"") + "\"").collect(Collectors.joining(";")))
                .collect(Collectors.joining("\n")) + "\n";
    }

    /** Status por lead: disparos da jogada + disparos de cadastro do participante. */
    private Map<UUID, WebhookStatus> webhookStatus(List<Draw> draws) {
        if (draws.isEmpty()) {
            return Map.of();
        }
        List<UUID> drawIds = draws.stream().map(Draw::getId).toList();
        List<UUID> participantIds = draws.stream().map(Draw::getParticipantId).distinct().toList();
        List<WebhookDelivery> deliveries = deliveryRepository.findForLeads(drawIds, participantIds);
        return draws.stream().collect(Collectors.toMap(Draw::getId, d -> {
            List<WebhookDelivery> mine = deliveries.stream()
                    .filter(x -> d.getId().equals(x.getDrawId())
                            || (x.getTrigger() == Trigger.LEAD && d.getParticipantId().equals(x.getParticipantId())))
                    .toList();
            if (mine.isEmpty()) {
                return WebhookStatus.NONE;
            }
            return mine.stream().allMatch(WebhookDelivery::isOk) ? WebhookStatus.OK : WebhookStatus.ERROR;
        }));
    }

    static Specification<Draw> filter(String q, UUID eventId) {
        return (root, query, cb) -> {
            Join<Draw, Participant> p;
            if (query != null && Long.class != query.getResultType() && long.class != query.getResultType()) {
                Fetch<Draw, Participant> fetch = root.fetch("participant", JoinType.INNER);
                @SuppressWarnings("unchecked")
                Join<Draw, Participant> joined = (Join<Draw, Participant>) fetch;
                p = joined;
            } else {
                p = root.join("participant", JoinType.INNER);
            }
            List<Predicate> and = new ArrayList<>();
            if (eventId != null) {
                and.add(cb.equal(root.get("eventId"), eventId));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                List<Predicate> or = new ArrayList<>();
                or.add(cb.like(cb.lower(p.get("name")), like));
                or.add(cb.like(cb.lower(cb.coalesce(p.get("email"), "")), like));
                String digits = q.replaceAll("\\D", "");
                if (!digits.isEmpty()) {
                    or.add(cb.like(cb.coalesce(p.get("cpf"), ""), "%" + digits + "%"));
                }
                and.add(cb.or(or.toArray(Predicate[]::new)));
            }
            return cb.and(and.toArray(Predicate[]::new));
        };
    }

    private static String nvl(String v) {
        return v == null ? "" : v;
    }
}
