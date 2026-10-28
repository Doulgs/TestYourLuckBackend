package com.doulgs.testyourluck.dashboard;

import com.doulgs.testyourluck.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardIT extends IntegrationTest {

    @Test
    void agregadosComEstoqueDosProdutos() throws Exception {
        String t = signup("a@empresa.com");
        String c = createCategory(t, "Eletrônicos");
        String e = createEvent(t, "Expo", "ATIVO");
        createEvent(t, "Summit", "ATIVO");
        postJson(t, "/api/products", "{\"name\":\"Power bank\",\"sku\":\"BR-005\",\"categoryId\":\"%s\",\"weight\":8,\"minStock\":6,\"quantity\":8}"
                .formatted(c)).andExpect(status().isCreated());
        createProduct(t, "Caneca", "BR-003", 20, 15, 100);
        String p = id(register(t, e, "Marina Costa", "12345678901"));
        play(t, p).andExpect(status().isCreated());
        draw(t, e, "Marina Costa", "12345678901").andExpect(status().isCreated());

        getJson(t, "/api/dashboard?range=30")
                .andExpect(jsonPath("$.kpis.draws.value").value(2))
                .andExpect(jsonPath("$.kpis.leads.value").value(1))
                .andExpect(jsonPath("$.kpis.stock.value").value(106))
                .andExpect(jsonPath("$.drawsChart.labels", hasSize(10)))
                .andExpect(jsonPath("$.recent[0].name").value("Marina Costa"))
                .andExpect(jsonPath("$.activeEvents", hasSize(2)));
        getJson(t, "/api/dashboard?range=7").andExpect(jsonPath("$.leadsChart.values", hasSize(7)));
    }

    @Test
    void estoqueBaixoComCategoriaEEventos() throws Exception {
        String t = signup("a@empresa.com");
        String c = createCategory(t, "Eletrônicos");
        createEvent(t, "Expo", "ATIVO");
        String summit = createEvent(t, "Summit", "ATIVO");
        postJson(t, "/api/products", "{\"name\":\"Power bank\",\"sku\":\"BR-005\",\"categoryId\":\"%s\",\"weight\":8,\"minStock\":6,\"quantity\":6}"
                .formatted(c)).andExpect(status().isCreated());
        postJson(t, "/api/products", "{\"name\":\"Mochila\",\"sku\":\"BR-006\",\"weight\":5,\"minStock\":4,\"quantity\":1,\"eventIds\":[\"%s\"]}"
                .formatted(summit)).andExpect(status().isCreated());
        getJson(t, "/api/dashboard")
                .andExpect(jsonPath("$.lowStock", hasSize(2)))
                .andExpect(jsonPath("$.lowStock[0].detail").value("Eletrônicos · todos os eventos"))
                .andExpect(jsonPath("$.lowStock[0].quantity").value(6))
                .andExpect(jsonPath("$.lowStock[1].detail").value("Sem categoria · Summit"));
    }

    @Test
    void cacheEhInvalidadoAposJogada() throws Exception {
        String t = signup("a@empresa.com");
        String e = createEvent(t, "Expo", "ATIVO");
        createProduct(t, "Caneca", "BR-003", 20, 15, 5);
        getJson(t, "/api/dashboard").andExpect(jsonPath("$.kpis.draws.value").value(0));
        draw(t, e, "Marina Costa", "12345678901").andExpect(status().isCreated());
        getJson(t, "/api/dashboard").andExpect(jsonPath("$.kpis.draws.value").value(1));
    }
}
