package com.doulgs.testyourluck.event;

import com.doulgs.testyourluck.shared.web.BusinessRuleException;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

/** Regras que bloqueiam cadastros e jogadas: status, limite de sorteios e janela no timezone do evento. */
public final class EventRules {

    private EventRules() {
    }

    public static Optional<EventBlock> block(Event e, long draws, Instant now) {
        if (e.getStatus() != EventStatus.ATIVO) {
            return Optional.of(EventBlock.INACTIVE);
        }
        if (e.getMaxDraws() > 0 && draws >= e.getMaxDraws()) {
            return Optional.of(EventBlock.LIMIT);
        }
        if (!e.isAllowOutside() && e.getStartDate() != null) {
            ZoneId zone = zone(e.getTimezone());
            boolean timed = !e.isAllDay();
            LocalDateTime start = e.getStartDate().atTime(timed && e.getStartTime() != null ? e.getStartTime() : LocalTime.MIN);
            LocalDateTime end = (e.getEndDate() != null ? e.getEndDate() : e.getStartDate())
                    .atTime(timed && e.getEndTime() != null ? e.getEndTime() : LocalTime.of(23, 59, 59));
            LocalDateTime local = LocalDateTime.ofInstant(now, zone);
            if (local.isBefore(start) || local.isAfter(end)) {
                return Optional.of(EventBlock.OUTSIDE_PERIOD);
            }
        }
        return Optional.empty();
    }

    public static void check(Event e, long draws, Instant now) {
        block(e, draws, now).ifPresent(b -> {
            throw new BusinessRuleException(b.message());
        });
    }

    static ZoneId zone(String tz) {
        try {
            return ZoneId.of(tz == null ? Event.DEFAULT_TIMEZONE : tz);
        } catch (DateTimeException ex) {
            return ZoneId.of(Event.DEFAULT_TIMEZONE);
        }
    }
}
