package com.doulgs.testyourluck.lead;

import com.doulgs.testyourluck.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LeadIT extends IntegrationTest {

    @Test
    void buscaFiltroPaginacaoEExportacaoComCamposPersonalizados() throws Exception {
        String t = signup("a@empresa.com");
        postJson(t, "/api/custom-fields", "{\"label\":\"Empresa\",\"type\":\"TEXT\"}").andExpect(status().isCreated());
        postJson(t, "/api/custom-fields", "{\"label\":\"Cargo\",\"type\":\"SELECT\",\"options\":[\"Gerente\",\"Analista\"]}")
                .andExpect(status().isCreated());
        String expo = createEvent(t, "Expo Tech 2026", "ATIVO");
        String summit = createEvent(t, "Summit Irrah", "ATIVO");
        createProduct(t, "Caneca", "BR-003", 20, 15, 10);

        String p = id(postJson(t, "/api/participants", """
                {"eventId":"%s","name":"Marina Costa","email":"marina@gmail.com","phone":"11987654321","cpf":"12345678901",
                 "answers":{"empresa":"Irrah","cargo":"Gerente"}}
                """.formatted(expo)).andExpect(status().isCreated()));
        play(t, p).andExpect(status().isCreated());
        draw(t, expo, "Lucas Prado", "987.654.321-00").andExpect(status().isCreated());
        draw(t, summit, "Renata Alves", "111.222.333-44").andExpect(status().isCreated());

        getJson(t, "/api/leads").andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].name").value("Renata Alves"))
                .andExpect(jsonPath("$.content[2].answers.cargo").value("Gerente"))
                .andExpect(jsonPath("$.content[2].webhookStatus").value("NONE"));
        getJson(t, "/api/leads?q=45678").andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Marina Costa"));
        getJson(t, "/api/leads?q=LUCAS").andExpect(jsonPath("$.content", hasSize(1)));
        getJson(t, "/api/leads?eventId=" + summit).andExpect(jsonPath("$.content", hasSize(1)));
        getJson(t, "/api/leads?page=1&size=2").andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalPages").value(2));
        getJson(t, "/api/leads?size=1000").andExpect(jsonPath("$.size").value(200));

        byte[] csv = getJson(t, "/api/leads/export?eventId=" + expo)
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"leads-testyourluck.csv\""))
                .andReturn().getResponse().getContentAsByteArray();
        String text = new String(csv, StandardCharsets.UTF_8);
        String[] lines = text.substring(1).split("\n");
        assertThat(text).startsWith("\uFEFF");
        assertThat(lines[0]).isEqualTo("\"Nome\";\"E-mail\";\"Telefone\";\"CPF\";\"Empresa\";\"Cargo\";\"Evento\";\"Brinde\";\"Data\";\"Hora\"");
        assertThat(lines).hasSize(3);
        assertThat(text).contains("\"123.456.789-01\";\"Irrah\";\"Gerente\"").contains("\"(11) 98765-4321\"")
                .doesNotContain("Renata");
    }
}
