package com.doulgs.testyourluck.event;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public final class EventDtos {

    private EventDtos() {
    }

    public record EventRequest(
            @NotBlank(message = "Informe o nome do evento") @Size(max = 160) String name,
            @NotNull(message = "Informe o status") EventStatus status,
            LocalDate startDate,
            LocalDate endDate,
            Boolean allDay,
            LocalTime startTime,
            LocalTime endTime,
            Boolean allowOutside,
            @Size(max = 200) String place,
            @Size(max = 120) String city,
            @Pattern(regexp = "^([A-Z]{2})?$", message = "UF inválida") String uf,
            @Size(max = 20) String region,
            @Pattern(regexp = "^(\\d{5}-?\\d{3})?$", message = "CEP inválido") String cep,
            @Size(max = 40) String timezone,
            @Min(value = 0, message = "O limite deve ser maior ou igual a 0") Integer maxDraws,
            @Size(max = 255) String website,
            UUID bannerMediaId) {
    }

    public record EventResponse(UUID id, String name, EventStatus status, LocalDate startDate, LocalDate endDate,
                                boolean allDay, LocalTime startTime, LocalTime endTime, boolean allowOutside,
                                String place, String city, String uf, String region, String cep, String timezone,
                                int maxDraws, String website, UUID bannerMediaId, String bannerUrl, long draws) {
    }
}
