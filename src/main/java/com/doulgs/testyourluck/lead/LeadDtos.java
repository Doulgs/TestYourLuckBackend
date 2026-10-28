package com.doulgs.testyourluck.lead;

import com.doulgs.testyourluck.draw.Draw;
import com.doulgs.testyourluck.participant.Participant;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class LeadDtos {

    private LeadDtos() {
    }

    public enum WebhookStatus { NONE, OK, ERROR }

    public record LeadResponse(UUID id, UUID participantId, String name, String email, String phone, String cpf,
                               Map<String, String> answers, UUID eventId, String eventName, UUID productId,
                               String productName, String productShort, int productHue, Instant drawnAt,
                               WebhookStatus webhookStatus) {

        static LeadResponse of(Draw d, WebhookStatus status) {
            Participant p = d.getParticipant();
            return new LeadResponse(d.getId(), p.getId(), p.getName(), p.getEmail(), p.getPhone(), p.getCpf(),
                    p.getAnswers(), d.getEventId(), d.getEventName(), d.getProductId(), d.getProductName(),
                    d.getProductShort(), d.getProductHue(), d.getDrawnAt(), status);
        }
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    }
}
