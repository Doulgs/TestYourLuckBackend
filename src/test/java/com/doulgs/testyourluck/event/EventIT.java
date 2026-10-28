package com.doulgs.testyourluck.event;

import com.doulgs.testyourluck.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventIT extends IntegrationTest {

    @Test
    void crudComNovosCampos() throws Exception {
        String t = signup("a@empresa.com");
        postJson(t, "/api/events", "{\"name\":\"  \",\"status\":\"ATIVO\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
        String id = id(postJson(t, "/api/events", """
                {"name":"Summit Irrah","status":"RASCUNHO","startDate":"2026-12-03","endDate":"2026-12-04",
                 "allDay":false,"startTime":"09:00","endTime":"19:00","place":"Expominas","city":"Belo Horizonte",
                 "uf":"MG","region":"Sudeste","cep":"30411000","timezone":"America/Sao_Paulo","maxDraws":300,
                 "website":"https://summit.grupoirrah.com"}
                """).andExpect(status().isCreated())
                .andExpect(jsonPath("$.cep").value("30411-000"))
                .andExpect(jsonPath("$.startTime").value("09:00:00"))
                .andExpect(jsonPath("$.allowOutside").value(false))
                .andExpect(jsonPath("$.draws").value(0)));
        getJson(t, "/api/events")
                .andExpect(jsonPath("$[0].maxDraws").value(300))
                .andExpect(jsonPath("$[0].endDate").value("2026-12-04"));
        putJson(t, "/api/events/" + id, "{\"name\":\"Summit\",\"status\":\"ENCERRADO\",\"allDay\":true,\"startTime\":\"09:00\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENCERRADO"))
                .andExpect(jsonPath("$.startTime").doesNotExist())
                .andExpect(jsonPath("$.timezone").value("America/Sao_Paulo"))
                .andExpect(jsonPath("$.maxDraws").value(0));
        deleteReq(t, "/api/events/" + id).andExpect(status().isNoContent());
        getJson(t, "/api/events").andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void validacaoDeDatasECampos() throws Exception {
        String t = signup("a@empresa.com");
        postJson(t, "/api/events", "{\"name\":\"X\",\"status\":\"ATIVO\",\"startDate\":\"2026-12-04\",\"endDate\":\"2026-12-03\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("A data de fim deve ser igual ou posterior ao início"));
        postJson(t, "/api/events", "{\"name\":\"X\",\"status\":\"ATIVO\",\"maxDraws\":-1,\"uf\":\"Minas\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.maxDraws").exists())
                .andExpect(jsonPath("$.errors.uf").exists());
    }

    @Test
    void eventoComSorteiosExcluidoPreservaLeads() throws Exception {
        String t = signup("a@empresa.com");
        String e = createEvent(t, "Expo Tech 2026", "ATIVO");
        createProduct(t, "Caneca", "BR-003", 20, 15, 3);
        draw(t, e, "Marina Costa", "12345678901").andExpect(status().isCreated());
        deleteReq(t, "/api/events/" + e).andExpect(status().isNoContent());
        getJson(t, "/api/leads").andExpect(jsonPath("$.content[0].eventName").value("Expo Tech 2026"));
    }
}
