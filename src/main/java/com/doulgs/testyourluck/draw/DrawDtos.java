package com.doulgs.testyourluck.draw;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.doulgs.testyourluck.event.EventBlock;
import com.doulgs.testyourluck.event.EventStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DrawDtos {

    private DrawDtos() {
    }

    public record DrawRequest(@NotNull(message = "Informe o participante") UUID participantId) {
    }

    public record PoolItem(UUID productId, String name, @JsonProperty("short") String shortCode, int hue,
                           String imageUrl, int weight, int quantity, double chance) {
    }

    public record DrawPool(UUID eventId, String eventName, EventStatus eventStatus, long draws, EventBlock block,
                           String blockMessage, List<PoolItem> items) {
    }

    public record DrawnProduct(UUID id, String name, @JsonProperty("short") String shortCode, int hue, String imageUrl) {
    }

    public record DrawResult(UUID id, UUID eventId, String eventName, UUID participantId, String participantName,
                             Instant drawnAt, DrawnProduct product, long eventDraws, int poolCount,
                             int creditsRemaining) {
    }
}
