package com.doulgs.testyourluck.shared;

import com.doulgs.testyourluck.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MultitenancyIT extends IntegrationTest {

    private static final List<String> REQUIRED = List.of("id", "tenant", "tenant_owner", "created_at", "updated_at");

    @Test
    void todasAsTabelasPossuemColunasObrigatoriasNaoNulas() {
        List<String> tables = jdbc.queryForList("""
                select table_name from information_schema.tables
                where table_schema = 'public' and table_type = 'BASE TABLE' and table_name <> 'flyway_schema_history'
                """, String.class);
        assertThat(tables).contains("tenants", "users", "events", "categories", "products", "product_events", "draws",
                "participants", "custom_fields", "draw_settings", "webhooks", "webhook_deliveries", "media_files")
                .doesNotContain("event_stocks");
        for (String table : tables) {
            List<Map<String, Object>> cols = jdbc.queryForList("""
                    select column_name, is_nullable from information_schema.columns
                    where table_schema = 'public' and table_name = ?
                    """, table);
            for (String required : REQUIRED) {
                assertThat(cols).as("%s.%s", table, required)
                        .anySatisfy(c -> {
                            assertThat(c.get("column_name")).isEqualTo(required);
                            assertThat(c.get("is_nullable")).isEqualTo("NO");
                        });
            }
        }
    }

    @Test
    void insertRecebeTenantDoTokenEIgnoraValorEnviado() throws Exception {
        String token = signup("a@empresa.com");
        String forged = UUID.randomUUID().toString();
        String id = id(postJson(token, "/api/events", """
                {"name":"Expo","status":"ATIVO","tenant":"%s","tenantOwner":"%s"}
                """.formatted(forged, forged)).andExpect(status().isCreated()));

        Map<String, Object> row = jdbc.queryForMap("select tenant, tenant_owner, created_at, updated_at from events where id = ?", uuid(id));
        Map<String, Object> user = jdbc.queryForMap("select tenant, tenant_owner from users where email = 'a@empresa.com'");
        assertThat(row.get("tenant")).isEqualTo(user.get("tenant")).isNotEqualTo(UUID.fromString(forged));
        assertThat(row.get("tenant_owner")).isEqualTo(user.get("tenant_owner"));
        assertThat(row.get("created_at")).isNotNull();
        assertThat(row.get("updated_at")).isNotNull();
    }

    @Test
    void updateAtualizaUpdatedAtEPreservaCreatedAt() throws Exception {
        String token = signup("a@empresa.com");
        String id = createEvent(token, "Expo", "RASCUNHO");
        Map<String, Object> before = jdbc.queryForMap("select created_at, updated_at from events where id = ?", uuid(id));
        Thread.sleep(20);
        putJson(token, "/api/events/" + id, "{\"name\":\"Expo 2\",\"status\":\"ATIVO\"}").andExpect(status().isOk());
        Map<String, Object> after = jdbc.queryForMap("select created_at, updated_at from events where id = ?", uuid(id));
        assertThat(after.get("created_at")).isEqualTo(before.get("created_at"));
        assertThat(((java.sql.Timestamp) after.get("updated_at")).after((java.sql.Timestamp) before.get("updated_at"))).isTrue();
    }

    @Test
    void tenantNaoEnxergaNemAlteraDadosDeOutroTenant() throws Exception {
        String a = signup("a@empresa.com");
        String b = signup("b@empresa.com");
        String eventA = createEvent(a, "Evento A", "ATIVO");
        createEvent(b, "Evento B", "ATIVO");

        getJson(a, "/api/events").andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].name").value("Evento A"));
        putJson(b, "/api/events/" + eventA, "{\"name\":\"Hack\",\"status\":\"ATIVO\"}").andExpect(status().isNotFound());
        deleteReq(b, "/api/events/" + eventA).andExpect(status().isNotFound());
        getJson(b, "/api/events/" + eventA + "/draw-pool").andExpect(status().isNotFound());
        getJson(b, "/api/draw-settings").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select name from events where id = ?", String.class, uuid(eventA))).isEqualTo("Evento A");
    }

    @Test
    void mesmoSkuEmTenantsDiferentesEhPermitido() throws Exception {
        String a = signup("a@empresa.com");
        String b = signup("b@empresa.com");
        createProduct(a, "Caneca", "BR-003", 20, 15);
        createProduct(b, "Caneca", "BR-003", 20, 15);
    }
}
