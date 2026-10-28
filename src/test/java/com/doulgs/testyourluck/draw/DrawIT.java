package com.doulgs.testyourluck.draw;

import com.doulgs.testyourluck.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DrawIT extends IntegrationTest {

    private static final String SETTINGS = """
            {"style":"SLOT","size":"M","credits":%d,"primaryColor":"#F36ACB","background":"PRETO","logoMode":"DEFAULT",
             "title":"Teste sua sorte","winMessage":"Retire seu brinde no balcão","askEmail":%s,"askPhone":%s,"askCpf":%s,
             "lights":true,"sound":true,"showPrizes":true,"customFieldIds":[]}
            """;

    @Test
    void jogadaConsomeCreditoBaixaEstoqueERegistraLead() throws Exception {
        String t = signup("a@empresa.com");
        String e = createEvent(t, "Expo", "ATIVO");
        String caneca = createProduct(t, "Caneca", "BR-003", 20, 15, 44);

        String p = id(register(t, e, "Marina Costa", "123.456.789-01").andExpect(status().isCreated())
                .andExpect(jsonPath("$.credits").value(1)));
        play(t, p).andExpect(status().isCreated())
                .andExpect(jsonPath("$.product.id").value(caneca))
                .andExpect(jsonPath("$.product.short").value("CA"))
                .andExpect(jsonPath("$.participantName").value("Marina Costa"))
                .andExpect(jsonPath("$.creditsRemaining").value(0))
                .andExpect(jsonPath("$.eventDraws").value(1));
        play(t, p).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("Sem créditos disponíveis"));

        getJson(t, "/api/products").andExpect(jsonPath("$[0].quantity").value(43));
        assertThat(jdbc.queryForObject("select cpf from participants", String.class)).isEqualTo("12345678901");
        assertThat(jdbc.queryForObject("select phone from participants", String.class)).isEqualTo("11987654321");
        assertThat(jdbc.queryForObject("select count(*) from draws", Long.class)).isEqualTo(1);
    }

    @Test
    void creditosConfiguradosPermitemVariasJogadas() throws Exception {
        String t = signup("a@empresa.com");
        putJson(t, "/api/draw-settings", SETTINGS.formatted(3, true, true, true)).andExpect(status().isOk());
        String e = createEvent(t, "Expo", "ATIVO");
        createProduct(t, "Caneca", "BR-003", 20, 15, 10);
        String p = id(register(t, e, "Ana", "12345678901").andExpect(jsonPath("$.creditsRemaining").value(3)));
        play(t, p).andExpect(jsonPath("$.creditsRemaining").value(2));
        play(t, p).andExpect(jsonPath("$.creditsRemaining").value(1));
        play(t, p).andExpect(jsonPath("$.creditsRemaining").value(0));
        getJson(t, "/api/leads").andExpect(jsonPath("$.content", hasSize(3)));
    }

    @Test
    void cadastroRespeitaCamposHabilitados() throws Exception {
        String t = signup("a@empresa.com");
        String e = createEvent(t, "Expo", "ATIVO");
        postJson(t, "/api/participants", "{\"eventId\":\"%s\",\"name\":\"\"}".formatted(e))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Informe seu nome."));
        postJson(t, "/api/participants", "{\"eventId\":\"%s\",\"name\":\"Ana\",\"email\":\"x\"}".formatted(e))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Informe um e-mail válido."));
        postJson(t, "/api/participants", "{\"eventId\":\"%s\",\"name\":\"Ana\",\"email\":\"a@b.com\",\"phone\":\"119\"}".formatted(e))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Informe um telefone válido."));
        postJson(t, "/api/participants", "{\"eventId\":\"%s\",\"name\":\"Ana\",\"email\":\"a@b.com\",\"phone\":\"11987654321\",\"cpf\":\"123\"}".formatted(e))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Informe um CPF válido."));

        putJson(t, "/api/draw-settings", SETTINGS.formatted(1, false, false, false)).andExpect(status().isOk());
        postJson(t, "/api/participants", "{\"eventId\":\"%s\",\"name\":\"Ana\",\"cpf\":\"123\"}".formatted(e))
                .andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("select count(*) from participants where cpf is null and email is null", Long.class)).isEqualTo(1);
    }

    @Test
    void produtoSemEstoqueNaoVinculadoOuPesoZeroFicaDeFora() throws Exception {
        String t = signup("a@empresa.com");
        String e = createEvent(t, "Expo", "ATIVO");
        String outro = createEvent(t, "Outro", "ATIVO");
        createProduct(t, "Camiseta", "BR-002", 30, 20, 0);
        createProduct(t, "Peso zero", "BR-010", 0, 1, 10);
        postJson(t, "/api/products", """
                {"name":"Mochila","sku":"BR-006","weight":50,"minStock":1,"quantity":10,"eventIds":["%s"]}
                """.formatted(outro)).andExpect(status().isCreated());
        createProduct(t, "Caneca", "BR-003", 20, 15, 50);

        getJson(t, "/api/events/" + e + "/draw-pool")
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].chance").value(100.0))
                .andExpect(jsonPath("$.block").doesNotExist());
        for (int i = 0; i < 8; i++) {
            draw(t, e, "Participante " + i, "1234567890" + i).andExpect(jsonPath("$.product.name").value("Caneca"));
        }
    }

    @Test
    void semEstoqueEventoInativoLimiteEForaDoPeriodo() throws Exception {
        String t = signup("a@empresa.com");
        String ativo = createEvent(t, "Expo", "ATIVO");
        String rascunho = createEvent(t, "Feira", "RASCUNHO");
        String limitado = id(postJson(t, "/api/events", "{\"name\":\"Lim\",\"status\":\"ATIVO\",\"allowOutside\":true,\"maxDraws\":1}"));
        String passado = id(postJson(t, "/api/events", """
                {"name":"Passado","status":"ATIVO","startDate":"2020-01-01","endDate":"2020-01-02","allowOutside":false}
                """));
        String passadoPermitido = id(postJson(t, "/api/events", """
                {"name":"Passado ok","status":"ATIVO","startDate":"2020-01-01","endDate":"2020-01-02","allowOutside":true}
                """));

        draw(t, ativo, "Marina Costa", "12345678901")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("Sem estoque disponível neste evento."));
        createProduct(t, "Caneca", "BR-003", 20, 15, 10);

        register(t, rascunho, "Ana", "12345678901").andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("Evento não está ativo"));
        draw(t, limitado, "Ana", "12345678901").andExpect(status().isCreated());
        register(t, limitado, "Bia", "12345678902").andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("Limite de sorteios atingido"));
        getJson(t, "/api/events/" + limitado + "/draw-pool").andExpect(jsonPath("$.block").value("LIMIT"));
        register(t, passado, "Ana", "12345678901").andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("Fora do período do evento"));
        getJson(t, "/api/events/" + passado + "/draw-pool").andExpect(jsonPath("$.block").value("OUTSIDE_PERIOD"))
                .andExpect(jsonPath("$.blockMessage").value("Fora do período do evento"));
        draw(t, passadoPermitido, "Ana", "12345678901").andExpect(status().isCreated());
    }

    @Test
    void jogadasSimultaneasNaoDeixamEstoqueNegativo() throws Exception {
        String t = signup("a@empresa.com");
        putJson(t, "/api/draw-settings", SETTINGS.formatted(1, false, false, false)).andExpect(status().isOk());
        String e = createEvent(t, "Expo", "ATIVO");
        createProduct(t, "Smartwatch", "BR-007", 2, 2, 1);

        int threads = 8;
        List<String> participants = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            participants.add(id(postJson(t, "/api/participants", "{\"eventId\":\"%s\",\"name\":\"P%d\"}".formatted(e, i))));
        }
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (String p : participants) {
            Callable<Integer> task = () -> {
                start.await();
                return play(t, p).andReturn().getResponse().getStatus();
            };
            results.add(pool.submit(task));
        }
        start.countDown();
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> f : results) {
            statuses.add(f.get());
        }
        pool.shutdown();

        assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
        assertThat(statuses).filteredOn(s -> s == 422).hasSize(threads - 1);
        assertThat(jdbc.queryForObject("select quantity from products", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from draws", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select sum(credits_used) from participants", Long.class)).isEqualTo(1);
    }
}
