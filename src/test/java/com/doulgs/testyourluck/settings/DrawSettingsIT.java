package com.doulgs.testyourluck.settings;

import com.doulgs.testyourluck.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DrawSettingsIT extends IntegrationTest {

    private static final String BODY = """
            {"style":"WHEEL","size":"G","credits":%d,"primaryColor":"%s","background":"GRAFITE","logoMode":"DEFAULT",
             "title":"Gire e ganhe","winMessage":"Retire no estande","askEmail":true,"askPhone":false,"askCpf":true,
             "lights":false,"sound":true,"showPrizes":false,"customFieldIds":%s}
            """;

    @Test
    void padroesPersistenciaEIsolamento() throws Exception {
        String a = signup("a@empresa.com");
        String b = signup("b@empresa.com");
        getJson(a, "/api/draw-settings")
                .andExpect(jsonPath("$.style").value("SLOT"))
                .andExpect(jsonPath("$.credits").value(1))
                .andExpect(jsonPath("$.primaryColor").value("#F36ACB"))
                .andExpect(jsonPath("$.title").value("Teste sua sorte"))
                .andExpect(jsonPath("$.winMessage").value("Retire seu brinde no balcão"));
        putJson(a, "/api/draw-settings", BODY.formatted(3, "#a3e635", "[]")).andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryColor").value("#A3E635"));
        getJson(a, "/api/draw-settings")
                .andExpect(jsonPath("$.style").value("WHEEL"))
                .andExpect(jsonPath("$.title").value("Gire e ganhe"))
                .andExpect(jsonPath("$.askPhone").value(false));
        getJson(b, "/api/draw-settings").andExpect(jsonPath("$.style").value("SLOT"));
    }

    @Test
    void validacoes() throws Exception {
        String a = signup("a@empresa.com");
        putJson(a, "/api/draw-settings", BODY.formatted(6, "#F36ACB", "[]")).andExpect(status().isBadRequest());
        putJson(a, "/api/draw-settings", BODY.formatted(1, "rosa", "[]")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.primaryColor").value("Cor inválida"));
        String b = signup("b@empresa.com");
        String campoAlheio = id(postJson(b, "/api/custom-fields", "{\"label\":\"Empresa\",\"type\":\"TEXT\"}"));
        putJson(a, "/api/draw-settings", BODY.formatted(1, "#F36ACB", "[\"" + campoAlheio + "\"]"))
                .andExpect(status().isNotFound());
    }
}
