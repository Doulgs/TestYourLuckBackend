package com.doulgs.testyourluck.webhook;

import com.doulgs.testyourluck.webhook.WebhookDtos.DeliveryDetail;
import com.doulgs.testyourluck.webhook.WebhookDtos.DeliverySummary;
import com.doulgs.testyourluck.webhook.WebhookDtos.TestRequest;
import com.doulgs.testyourluck.webhook.WebhookDtos.WebhookRequest;
import com.doulgs.testyourluck.webhook.WebhookDtos.WebhookResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService service;

    @GetMapping("/webhooks")
    public List<WebhookResponse> list() {
        return service.list();
    }

    @PostMapping("/webhooks")
    @ResponseStatus(HttpStatus.CREATED)
    public WebhookResponse create(@Valid @RequestBody WebhookRequest req) {
        return service.create(req);
    }

    @PutMapping("/webhooks/{id}")
    public WebhookResponse update(@PathVariable UUID id, @Valid @RequestBody WebhookRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/webhooks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @PostMapping("/webhooks/{id}/test")
    public DeliverySummary test(@PathVariable UUID id, @RequestBody(required = false) TestRequest req) {
        return service.test(id, req == null ? null : req.eventId());
    }

    @GetMapping("/webhook-deliveries")
    public List<DeliverySummary> recent(@RequestParam(defaultValue = "8") int limit) {
        return service.recent(limit);
    }

    @GetMapping("/leads/{drawId}/deliveries")
    public List<DeliveryDetail> forLead(@PathVariable UUID drawId) {
        return service.forLead(drawId);
    }
}
