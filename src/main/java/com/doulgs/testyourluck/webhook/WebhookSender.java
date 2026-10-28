package com.doulgs.testyourluck.webhook;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Resolve o template de um webhook e executa a requisição HTTP, devolvendo o registro do disparo. */
@Component
@RequiredArgsConstructor
public class WebhookSender {

    static final Duration TIMEOUT = Duration.ofSeconds(10);
    static final int MAX_RESPONSE = 2000;
    private static final Set<String> RESTRICTED = Set.of("host", "content-length", "connection", "expect", "upgrade");

    public record Outcome(String method, String url, Map<String, Object> request, Map<String, Object> response,
                          Integer statusCode, boolean ok, String error) {
    }

    private final WebhookTargetGuard guard;
    private final JsonMapper json;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER).build();

    public Outcome send(Webhook w, Map<String, String> vars) {
        String base = TemplateRenderer.render(w.getUrl(), vars).trim();
        StringBuilder qs = new StringBuilder();
        for (KeyValue p : w.getParams()) {
            if (p.k() == null || p.k().isBlank()) {
                continue;
            }
            qs.append(qs.isEmpty() ? "" : "&").append(enc(p.k())).append('=')
                    .append(enc(TemplateRenderer.render(p.v(), vars)));
        }
        String url = qs.isEmpty() ? base : base + (base.contains("?") ? "&" : "?") + qs;
        Map<String, String> headers = new LinkedHashMap<>();
        for (KeyValue h : w.getHeaders()) {
            if (h.k() != null && !h.k().isBlank()) {
                headers.put(h.k().trim(), TemplateRenderer.render(h.v(), vars));
            }
        }
        String method = w.getMethod().name();
        String body = w.getMethod() == WebhookEnums.Method.GET ? null : TemplateRenderer.render(w.getBody(), vars);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("method", method);
        request.put("url", url);
        request.put("headers", headers);
        if (body != null) {
            request.put("body", parse(body));
        }

        try {
            URI uri = guard.check(url);
            HttpRequest.Builder b = HttpRequest.newBuilder(uri).timeout(TIMEOUT);
            headers.forEach((k, v) -> {
                if (!RESTRICTED.contains(k.toLowerCase())) {
                    b.header(k, v);
                }
            });
            b.method(method, body == null || body.isEmpty()
                    ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
            HttpResponse<String> r = client.send(b.build(), HttpResponse.BodyHandlers.ofString());
            boolean ok = r.statusCode() >= 200 && r.statusCode() < 300;
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", r.statusCode());
            String resBody = r.body() == null ? "" : r.body();
            response.put("body", parse(resBody.length() > MAX_RESPONSE ? resBody.substring(0, MAX_RESPONSE) : resBody));
            return new Outcome(method, url, request, response, r.statusCode(), ok, ok ? null : "HTTP " + r.statusCode());
        } catch (IllegalArgumentException ex) {
            return failure(method, url, request, ex.getMessage());
        } catch (HttpTimeoutException ex) {
            return failure(method, url, request, "Tempo esgotado ao conectar com o servidor de destino");
        } catch (IOException ex) {
            return failure(method, url, request, "Falha ao conectar com o servidor de destino");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return failure(method, url, request, "Envio interrompido");
        }
    }

    private static Outcome failure(String method, String url, Map<String, Object> request, String error) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("error", error);
        return new Outcome(method, url, request, response, null, false, error);
    }

    /** JSON vira Map/List/valores simples (gravável em jsonb); texto não-JSON é mantido como string. */
    private Object parse(String text) {
        String t = text == null ? "" : text.trim();
        if (!(t.startsWith("{") || t.startsWith("["))) {
            return text;
        }
        try {
            return json.readValue(t, Object.class);
        } catch (RuntimeException ex) {
            return text;
        }
    }

    private static String enc(String v) {
        return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
    }
}
