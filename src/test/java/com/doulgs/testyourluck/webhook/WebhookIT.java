package com.doulgs.testyourluck.webhook;

import com.doulgs.testyourluck.support.IntegrationTest;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WebhookIT extends IntegrationTest {

    record Received(String method, String path, String query, String auth, String body) {
    }

    private HttpServer server;
    private final List<Received> received = new CopyOnWriteArrayList<>();
    private String base;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            received.add(new Received(ex.getRequestMethod(), ex.getRequestURI().getPath(), ex.getRequestURI().getQuery(),
                    ex.getRequestHeaders().getFirst("Authorization"), body));
            int code = ex.getRequestURI().getPath().contains("fail") ? 500 : 200;
            byte[] res = "{\"received\":true}".getBytes();
            ex.sendResponseHeaders(code, res.length);
            ex.getResponseBody().write(res);
            ex.close();
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private String webhook(String token, String name, String trigger, String path, String eventId) throws Exception {
        return id(postJson(token, "/api/webhooks", """
                {"name":"%s","trigger":"%s","method":"POST","url":"%s%s",
                 "headers":[{"k":"Authorization","v":"Bearer {{lead.cpf}}"},{"k":"Content-Type","v":"application/json"}],
                 "params":[{"k":"origem","v":"totem"}],
                 "body":"{\\"nome\\": \\"{{lead.nome}}\\", \\"premio\\": \\"{{premio.nome}}\\", \\"empresa\\": \\"{{campo.empresa}}\\", \\"evento\\": \\"{{evento.nome}}\\"}",
                 "eventIds":["%s"],"active":true}
                """.formatted(name, trigger, base, path, eventId)).andExpect(status().isCreated()));
    }

    @Test
    void disparosPorGatilhoComVariaveis() throws Exception {
        String t = signup("a@empresa.com");
        postJson(t, "/api/custom-fields", "{\"label\":\"Empresa\",\"type\":\"TEXT\"}");
        String e = createEvent(t, "Expo", "ATIVO");
        String outro = createEvent(t, "Outro", "ATIVO");
        createProduct(t, "Caneca", "BR-003", 20, 1, 10);
        webhook(t, "Lead", "LEAD", "/lead", e);
        String play = webhook(t, "Jogada", "PLAY", "/play", e);
        webhook(t, "Outro evento", "PLAY", "/outro", outro);
        String inativo = webhook(t, "Inativo", "PLAY", "/inativo", e);
        putJson(t, "/api/webhooks/" + inativo, """
                {"name":"Inativo","trigger":"PLAY","method":"POST","url":"%s/inativo","eventIds":["%s"],"active":false}
                """.formatted(base, e)).andExpect(status().isOk());

        String p = id(postJson(t, "/api/participants", """
                {"eventId":"%s","name":"Ana \\"Teste\\"","email":"ana@gmail.com","phone":"11987654321","cpf":"12345678901",
                 "answers":{"empresa":"Irrah"}}
                """.formatted(e)));
        String drawId = id(play(t, p).andExpect(status().isCreated()));

        await().until(() -> received.size() >= 2);
        Thread.sleep(300);
        assertThat(received).extracting(Received::path).containsExactlyInAnyOrder("/lead", "/play");
        Received r = received.stream().filter(x -> x.path().equals("/play")).findFirst().orElseThrow();
        assertThat(r.query()).isEqualTo("origem=totem");
        assertThat(r.auth()).isEqualTo("Bearer 123.456.789-01");
        assertThat(r.body()).isEqualTo("{\"nome\": \"Ana \\\"Teste\\\"\", \"premio\": \"Caneca\", \"empresa\": \"Irrah\", \"evento\": \"Expo\"}");
        Received lead = received.stream().filter(x -> x.path().equals("/lead")).findFirst().orElseThrow();
        assertThat(lead.body()).contains("\"premio\": \"\"");

        await().untilAsserted(() -> getJson(t, "/api/webhook-deliveries").andExpect(jsonPath("$", hasSize(2))));
        getJson(t, "/api/leads").andExpect(jsonPath("$.content[0].webhookStatus").value("OK"));
        getJson(t, "/api/leads/" + drawId + "/deliveries")
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.trigger == 'PLAY')].request.body.premio").value(hasItem("Caneca")))
                .andExpect(jsonPath("$[?(@.trigger == 'PLAY')].response.status").value(hasItem(200)));
        getJson(t, "/api/webhooks").andExpect(jsonPath("$[?(@.id == '" + play + "')].dispatchCount").value(1));
    }

    @Test
    void erroNoDestinoNaoAfetaJogada() throws Exception {
        String t = signup("a@empresa.com");
        String e = createEvent(t, "Expo", "ATIVO");
        createProduct(t, "Caneca", "BR-003", 20, 1, 10);
        webhook(t, "Falha", "PLAY", "/fail", e);
        draw(t, e, "Ana", "12345678901").andExpect(status().isCreated());
        await().untilAsserted(() -> getJson(t, "/api/leads").andExpect(jsonPath("$.content[0].webhookStatus").value("ERROR")));
        getJson(t, "/api/webhook-deliveries").andExpect(jsonPath("$[0].status").value("500 ERRO"))
                .andExpect(jsonPath("$[0].ok").value(false));
    }

    @Test
    void testarDisparoComDadosDeExemplo() throws Exception {
        String t = signup("a@empresa.com");
        postJson(t, "/api/custom-fields", "{\"label\":\"Empresa\",\"type\":\"TEXT\"}");
        String e = createEvent(t, "Expo", "ATIVO");
        createProduct(t, "Caneca", "BR-003", 20, 1, 10);
        String id = webhook(t, "CRM", "PLAY", "/test", e);
        postJson(t, "/api/webhooks/" + id + "/test", "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.status").value("200 OK"))
                .andExpect(jsonPath("$.eventName").value("teste"));
        assertThat(received.getFirst().body()).contains("Maria Teste").contains("Caneca").contains("(Empresa)");
    }
}
