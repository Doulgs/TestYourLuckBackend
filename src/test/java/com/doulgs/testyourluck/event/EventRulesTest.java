package com.doulgs.testyourluck.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class EventRulesTest {

    private Event event() {
        Event e = new Event();
        e.setStatus(EventStatus.ATIVO);
        e.setStartDate(LocalDate.of(2026, 10, 27));
        e.setEndDate(LocalDate.of(2026, 10, 30));
        return e;
    }

    @Test
    void statusELimite() {
        Event e = event();
        e.setAllowOutside(true);
        e.setStatus(EventStatus.RASCUNHO);
        assertThat(EventRules.block(e, 0, Instant.now())).contains(EventBlock.INACTIVE);
        e.setStatus(EventStatus.ATIVO);
        e.setMaxDraws(50);
        assertThat(EventRules.block(e, 50, Instant.now())).contains(EventBlock.LIMIT);
        assertThat(EventRules.block(e, 49, Instant.now())).isEmpty();
        e.setMaxDraws(0);
        assertThat(EventRules.block(e, 10_000, Instant.now())).isEmpty();
    }

    @Test
    void janelaDiaTodoNoTimezoneDoEvento() {
        Event e = event();
        // 30/10 23:30 em São Paulo = 31/10 02:30 UTC: ainda dentro
        assertThat(EventRules.block(e, 0, Instant.parse("2026-10-31T02:30:00Z"))).isEmpty();
        // 31/10 00:30 em São Paulo: fora
        assertThat(EventRules.block(e, 0, Instant.parse("2026-10-31T03:30:00Z"))).contains(EventBlock.OUTSIDE_PERIOD);
        assertThat(EventRules.block(e, 0, Instant.parse("2026-10-27T02:59:00Z"))).contains(EventBlock.OUTSIDE_PERIOD);
        e.setAllowOutside(true);
        assertThat(EventRules.block(e, 0, Instant.parse("2026-12-01T12:00:00Z"))).isEmpty();
    }

    @Test
    void janelaComHorario() {
        Event e = event();
        e.setAllDay(false);
        e.setStartTime(LocalTime.of(9, 0));
        e.setEndTime(LocalTime.of(18, 0));
        assertThat(EventRules.block(e, 0, Instant.parse("2026-10-27T11:59:00Z"))).contains(EventBlock.OUTSIDE_PERIOD);
        assertThat(EventRules.block(e, 0, Instant.parse("2026-10-27T12:00:00Z"))).isEmpty();
        assertThat(EventRules.block(e, 0, Instant.parse("2026-10-30T21:01:00Z"))).contains(EventBlock.OUTSIDE_PERIOD);
    }
}
