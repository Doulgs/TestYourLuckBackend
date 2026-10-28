package com.doulgs.testyourluck.support;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Base dos testes de integração: Postgres/Redis do docker-compose, banco {@code testyourluck_test}. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTest {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    private StringRedisTemplate redis;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE webhook_deliveries, webhooks, draws, participants, product_events, products, categories, "
                + "custom_fields, draw_settings, events, media_files, users, tenants CASCADE");
        var keys = redis.keys("*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    /** Cria uma conta (novo tenant) e devolve o token. */
    protected String signup(String email) throws Exception {
        String body = """
                {"name":"Ana Ribeiro","company":"Empresa %s","email":"%s","password":"12345678"}
                """.formatted(email, email);
        String res = mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(res, "$.token");
    }

    protected ResultActions getJson(String token, String url) throws Exception {
        return mvc.perform(get(url).header("Authorization", "Bearer " + token));
    }

    protected ResultActions postJson(String token, String url, String json) throws Exception {
        return mvc.perform(post(url).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions putJson(String token, String url, String json) throws Exception {
        return mvc.perform(put(url).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions deleteReq(String token, String url) throws Exception {
        return mvc.perform(delete(url).header("Authorization", "Bearer " + token));
    }

    protected String id(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }

    protected String createCategory(String token, String name) throws Exception {
        return id(postJson(token, "/api/categories", "{\"name\":\"%s\"}".formatted(name)).andExpect(status().isCreated()));
    }

    protected String createProduct(String token, String name, String sku, int weight, int min) throws Exception {
        return createProduct(token, name, sku, weight, min, 0);
    }

    protected String createProduct(String token, String name, String sku, int weight, int min, int quantity) throws Exception {
        return id(postJson(token, "/api/products", """
                {"name":"%s","sku":"%s","weight":%d,"minStock":%d,"quantity":%d}
                """.formatted(name, sku, weight, min, quantity)).andExpect(status().isCreated()));
    }

    /** Evento ativo por padrão aberto fora do período, para não depender da data corrente. */
    protected String createEvent(String token, String name, String status) throws Exception {
        return id(postJson(token, "/api/events", """
                {"name":"%s","startDate":"2026-11-12","place":"São Paulo, SP","status":"%s","allowOutside":true}
                """.formatted(name, status)).andExpect(status().isCreated()));
    }

    /** Atualiza só a quantidade de um produto (PUT com os dados atuais). */
    protected void setQuantity(String token, String productId, int qty) throws Exception {
        String list = getJson(token, "/api/products").andReturn().getResponse().getContentAsString();
        java.util.List<java.util.Map<String, Object>> products = JsonPath.read(list, "$[?(@.id == '" + productId + "')]");
        var p = products.getFirst();
        putJson(token, "/api/products/" + productId, """
                {"name":"%s","sku":"%s","weight":%s,"minStock":%s,"quantity":%d}
                """.formatted(p.get("name"), p.get("sku"), p.get("weight"), p.get("minStock"), qty))
                .andExpect(status().isOk());
    }

    protected ResultActions register(String token, String eventId, String name, String cpf) throws Exception {
        return postJson(token, "/api/participants", """
                {"eventId":"%s","name":"%s","email":"teste@gmail.com","phone":"(11) 98765-4321","cpf":"%s"}
                """.formatted(eventId, name, cpf));
    }

    protected ResultActions play(String token, String participantId) throws Exception {
        return postJson(token, "/api/draws", "{\"participantId\":\"" + participantId + "\"}");
    }

    /** Cadastra um participante (1 crédito por padrão) e faz uma jogada. */
    protected ResultActions draw(String token, String eventId, String name, String cpf) throws Exception {
        String participant = id(register(token, eventId, name, cpf).andExpect(status().isCreated()));
        return play(token, participant);
    }

    protected UUID uuid(String s) {
        return UUID.fromString(s);
    }
}
