package com.doulgs.testyourluck.field;

import com.doulgs.testyourluck.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomFieldIT extends IntegrationTest {

    @Test
    void chaveGeradaNormalizadaUnicaEReservada() throws Exception {
        String t = signup("a@empresa.com");
        postJson(t, "/api/custom-fields", "{\"label\":\"Aceito receber novidades\",\"type\":\"BOOL\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("aceito_receber_novidades"))
                .andExpect(jsonPath("$.token").value("{{aceito_receber_novidades}}"))
                .andExpect(jsonPath("$.active").value(true));
        postJson(t, "/api/custom-fields", "{\"label\":\"Área\",\"key\":\"Área de Atuação\",\"type\":\"TEXT\"}")
                .andExpect(jsonPath("$.key").value("area_de_atuacao"));
        postJson(t, "/api/custom-fields", "{\"label\":\"Outra\",\"key\":\"area_de_atuacao\",\"type\":\"TEXT\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Chave já utilizada"));
        postJson(t, "/api/custom-fields", "{\"label\":\"Lead\",\"key\":\"lead_x\",\"type\":\"TEXT\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Chave reservada"));
        postJson(t, "/api/custom-fields", "{\"label\":\"Cargo\",\"type\":\"SELECT\",\"options\":[\"\",\" \"]}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Informe ao menos uma opção"));
        getJson(t, "/api/draw-settings").andExpect(jsonPath("$.customFieldIds", hasSize(2)));
    }

    @Test
    void respostasValidadasEPreservadasAposExclusao() throws Exception {
        String t = signup("a@empresa.com");
        String cargo = id(postJson(t, "/api/custom-fields", """
                {"label":"Cargo","type":"SELECT","options":["Gerente","Analista",""],"required":true}
                """).andExpect(jsonPath("$.options", hasSize(2))));
        postJson(t, "/api/custom-fields", "{\"label\":\"Novidades\",\"key\":\"novidades\",\"type\":\"BOOL\"}");
        String e = createEvent(t, "Expo", "ATIVO");
        String base = "{\"eventId\":\"%s\",\"name\":\"Ana\",\"email\":\"a@b.com\",\"phone\":\"11987654321\",\"cpf\":\"12345678901\",\"answers\":%s}";
        postJson(t, "/api/participants", base.formatted(e, "{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Preencha: Cargo"));
        postJson(t, "/api/participants", base.formatted(e, "{\"cargo\":\"Diretor\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Opção inválida para Cargo"));
        postJson(t, "/api/participants", base.formatted(e, "{\"cargo\":\"Gerente\",\"novidades\":false}"))
                .andExpect(status().isCreated());
        org.assertj.core.api.Assertions.assertThat(
                jdbc.queryForObject("select answers->>'novidades' from participants", String.class)).isEqualTo("nao");

        deleteReq(t, "/api/custom-fields/" + cargo).andExpect(status().isNoContent());
        getJson(t, "/api/custom-fields").andExpect(jsonPath("$", hasSize(1)));
        org.assertj.core.api.Assertions.assertThat(
                jdbc.queryForObject("select answers->>'cargo' from participants", String.class)).isEqualTo("Gerente");
    }
}
