package com.doulgs.testyourluck.participant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

public final class ParticipantDtos {

    private ParticipantDtos() {
    }

    public record ParticipantRequest(
            @NotNull(message = "Selecione o evento") UUID eventId,
            @Size(max = 160) String name,
            @Size(max = 255) String email,
            String phone,
            String cpf,
            Map<String, Object> answers) {
    }

    public record ParticipantResponse(UUID id, String name, int credits, int creditsRemaining) {
    }
}
