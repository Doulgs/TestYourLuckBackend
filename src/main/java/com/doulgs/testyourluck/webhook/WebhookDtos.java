package com.doulgs.testyourluck.webhook;

import com.doulgs.testyourluck.webhook.WebhookEnums.Method;
import com.doulgs.testyourluck.webhook.WebhookEnums.Trigger;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WebhookDtos {

    private WebhookDtos() {
    }

    public record WebhookRequest(
            @NotBlank(message = "Informe nome e URL") @Size(max = 160) String name,
            @NotNull Trigger trigger,
            @NotNull Method method,
            @NotBlank(message = "Informe nome e URL") @Size(max = 2000) String url,
            List<KeyValue> headers,
            List<KeyValue> params,
            String body,
            List<UUID> eventIds,
            Boolean active) {
    }

    public record WebhookResponse(UUID id, String name, Trigger trigger, Method method, String url,
                                  List<KeyValue> headers, List<KeyValue> params, String body, List<UUID> eventIds,
                                  boolean active, int dispatchCount, Instant lastDispatchAt) {
    }

    public record TestRequest(UUID eventId) {
    }

    public record DeliverySummary(UUID id, String webhookName, boolean ok, String status, String method, String url,
                                  String eventName, boolean test, Instant sentAt) {
    }

    public record DeliveryDetail(UUID id, String webhookName, Trigger trigger, boolean ok, String status,
                                 String method, String url, Instant sentAt, Map<String, Object> request,
                                 Map<String, Object> response) {
    }
}
