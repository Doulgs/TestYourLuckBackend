package com.doulgs.testyourluck.event;

import com.doulgs.testyourluck.dashboard.DashboardCache;
import com.doulgs.testyourluck.draw.DrawRepository;
import com.doulgs.testyourluck.event.EventDtos.EventRequest;
import com.doulgs.testyourluck.event.EventDtos.EventResponse;
import com.doulgs.testyourluck.media.MediaService;
import com.doulgs.testyourluck.shared.web.BusinessRuleException;
import com.doulgs.testyourluck.shared.web.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final DrawRepository drawRepository;
    private final DashboardCache dashboardCache;
    private final MediaService mediaService;

    @Transactional(readOnly = true)
    public List<EventResponse> list() {
        Map<UUID, Long> draws = drawRepository.countByEvent();
        return eventRepository.findAll().stream()
                .sorted(Comparator.comparing(Event::getStartDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Event::getName))
                .map(e -> toResponse(e, draws.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    @Transactional
    public EventResponse create(EventRequest req) {
        Event event = new Event();
        apply(event, req);
        eventRepository.save(event);
        dashboardCache.evictCurrentTenant();
        return toResponse(event, 0);
    }

    @Transactional
    public EventResponse update(UUID id, EventRequest req) {
        Event event = find(id);
        apply(event, req);
        dashboardCache.evictCurrentTenant();
        return toResponse(event, drawRepository.countByEventId(id));
    }

    /** Exclusão lógica: preserva leads e histórico de sorteios do evento. */
    @Transactional
    public void delete(UUID id) {
        find(id).setDeletedAt(Instant.now());
        dashboardCache.evictCurrentTenant();
    }

    public Event find(UUID id) {
        return eventRepository.findById(id).orElseThrow(() -> new NotFoundException("Evento não encontrado"));
    }

    public EventResponse toResponse(Event e, long draws) {
        return new EventResponse(e.getId(), e.getName(), e.getStatus(), e.getStartDate(), e.getEndDate(), e.isAllDay(),
                e.getStartTime(), e.getEndTime(), e.isAllowOutside(), e.getPlace(), e.getCity(), e.getUf(),
                e.getRegion(), e.getCep(), e.getTimezone(), e.getMaxDraws(), e.getWebsite(), e.getBannerMediaId(),
                mediaService.url(e.getBannerMediaId()), draws);
    }

    private void apply(Event event, EventRequest req) {
        if (req.startDate() != null && req.endDate() != null && req.endDate().isBefore(req.startDate())) {
            throw new BusinessRuleException("A data de fim deve ser igual ou posterior ao início");
        }
        boolean allDay = req.allDay() == null || req.allDay();
        if (!allDay && req.startTime() != null && req.endTime() != null
                && (req.endDate() == null || req.endDate().equals(req.startDate()))
                && req.endTime().isBefore(req.startTime())) {
            throw new BusinessRuleException("A hora de fim deve ser posterior à hora de início");
        }
        event.setName(req.name().trim());
        event.setStatus(req.status());
        event.setStartDate(req.startDate());
        event.setEndDate(req.endDate());
        event.setAllDay(allDay);
        event.setStartTime(allDay ? null : req.startTime());
        event.setEndTime(allDay ? null : req.endTime());
        event.setAllowOutside(Boolean.TRUE.equals(req.allowOutside()));
        event.setPlace(blank(req.place()));
        event.setCity(blank(req.city()));
        event.setUf(blank(req.uf()));
        event.setRegion(blank(req.region()));
        event.setCep(formatCep(req.cep()));
        event.setTimezone(validZone(req.timezone()));
        event.setMaxDraws(req.maxDraws() == null ? 0 : req.maxDraws());
        event.setWebsite(blank(req.website()));
        event.setBannerMediaId(mediaService.requireOwn(req.bannerMediaId()));
    }

    private static String validZone(String tz) {
        if (tz == null || tz.isBlank()) {
            return Event.DEFAULT_TIMEZONE;
        }
        try {
            return ZoneId.of(tz).getId();
        } catch (DateTimeException ex) {
            throw new BusinessRuleException("Timezone inválido");
        }
    }

    private static String formatCep(String cep) {
        if (cep == null || cep.isBlank()) {
            return null;
        }
        String d = cep.replaceAll("\\D", "");
        return d.substring(0, 5) + "-" + d.substring(5);
    }

    private static String blank(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
