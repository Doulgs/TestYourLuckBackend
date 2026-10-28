package com.doulgs.testyourluck.media;

import com.jayway.jsonpath.JsonPath;
import com.doulgs.testyourluck.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MediaIT extends IntegrationTest {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};

    private String upload(String token, String name, byte[] bytes) throws Exception {
        String res = mvc.perform(multipart("/api/media").file(new MockMultipartFile("file", name, "image/png", bytes))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(res, "$.id");
    }

    @Test
    void envioELeituraPublica() throws Exception {
        String t = signup("a@empresa.com");
        String id = upload(t, "logo.png", PNG);
        mvc.perform(get("/api/media/" + id))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(PNG));
        mvc.perform(get("/api/media/00000000-0000-0000-0000-000000000001")).andExpect(status().isNotFound());
    }

    @Test
    void svgServidoComCsp() throws Exception {
        String t = signup("a@empresa.com");
        String id = upload(t, "logo.svg", "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>".getBytes());
        mvc.perform(get("/api/media/" + id))
                .andExpect(content().contentType("image/svg+xml"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'"));
    }

    @Test
    void recusaArquivoGrandeEFormatoInvalido() throws Exception {
        String t = signup("a@empresa.com");
        byte[] big = Arrays.copyOf(PNG, 5 * 1024 * 1024 + 10);
        mvc.perform(multipart("/api/media").file(new MockMultipartFile("file", "x.png", "image/png", big))
                        .header("Authorization", "Bearer " + t))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.message").value("Imagem maior que 5 MB"));
        mvc.perform(multipart("/api/media").file(new MockMultipartFile("file", "x.pdf", "application/pdf", "%PDF-1.4".getBytes()))
                        .header("Authorization", "Bearer " + t))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value("Formato de imagem não suportado"));
    }

    @Test
    void imagemDeOutroTenantNaoPodeSerReferenciada() throws Exception {
        String a = signup("a@empresa.com");
        String b = signup("b@empresa.com");
        String alheia = upload(b, "x.png", PNG);
        postJson(a, "/api/products", "{\"name\":\"X\",\"sku\":\"S1\",\"weight\":1,\"minStock\":1,\"quantity\":1,\"imageMediaId\":\"%s\"}"
                .formatted(alheia))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Imagem não encontrada"));
        String propria = upload(a, "y.png", PNG);
        postJson(a, "/api/products", "{\"name\":\"X\",\"sku\":\"S1\",\"weight\":1,\"minStock\":1,\"quantity\":1,\"imageMediaId\":\"%s\"}"
                .formatted(propria))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl").value("http://localhost:9090/api/media/" + propria));
    }
}
