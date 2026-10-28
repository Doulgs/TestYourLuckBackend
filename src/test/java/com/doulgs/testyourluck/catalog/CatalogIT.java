package com.doulgs.testyourluck.catalog;

import com.doulgs.testyourluck.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogIT extends IntegrationTest {

    @Test
    void categoriaComNomeDuplicadoRetorna409() throws Exception {
        String t = signup("a@empresa.com");
        createCategory(t, "Eletrônicos");
        postJson(t, "/api/categories", "{\"name\":\"eletrônicos\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Categoria já existe"));
    }

    @Test
    void indicadoresDaCategoriaUsamEstoqueDosProdutos() throws Exception {
        String t = signup("a@empresa.com");
        String c = createCategory(t, "Eletrônicos");
        int[][] items = {{10, 14}, {8, 6}, {2, 2}};
        for (int i = 0; i < items.length; i++) {
            postJson(t, "/api/products", """
                    {"name":"P%d","sku":"S%d","categoryId":"%s","weight":%d,"minStock":1,"quantity":%d}
                    """.formatted(i, i, c, items[i][0], items[i][1])).andExpect(status().isCreated());
        }
        createProduct(t, "Camiseta", "BR-002", 80, 1, 50);
        getJson(t, "/api/categories")
                .andExpect(jsonPath("$[0].productCount").value(3))
                .andExpect(jsonPath("$[0].stock").value(22))
                .andExpect(jsonPath("$[0].chance").value(20.0));
    }

    @Test
    void naoExcluiCategoriaComProdutos() throws Exception {
        String t = signup("a@empresa.com");
        String c = createCategory(t, "Utilidades");
        postJson(t, "/api/products", "{\"name\":\"Caneca\",\"sku\":\"BR-003\",\"categoryId\":\"%s\",\"weight\":20,\"minStock\":15,\"quantity\":1}"
                .formatted(c)).andExpect(status().isCreated());
        deleteReq(t, "/api/categories/" + c)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("Categoria possui produtos vinculados"));
    }

    @Test
    void produtoRecebeSiglaMatizChanceEStatusDeEstoque() throws Exception {
        String t = signup("a@empresa.com");
        createProduct(t, "Garrafa térmica", "BR-001", 25, 15, 0);
        postJson(t, "/api/products", "{\"name\":\"Fone Bluetooth\",\"sku\":\"BR-004\",\"weight\":75,\"minStock\":8,\"quantity\":8}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.short").value("FB"))
                .andExpect(jsonPath("$.hue").value(150))
                .andExpect(jsonPath("$.chance").value(75.0))
                .andExpect(jsonPath("$.quantity").value(8))
                .andExpect(jsonPath("$.stockStatus").value("BAIXO"));
        getJson(t, "/api/products")
                .andExpect(jsonPath("$[0].stockStatus").value("ESGOTADO"))
                .andExpect(jsonPath("$[0].chance").value(25.0));
    }

    @Test
    void validacaoPesoQuantidadeESkuDuplicado() throws Exception {
        String t = signup("a@empresa.com");
        createProduct(t, "Caneca", "BR-003", 20, 15);
        postJson(t, "/api/products", "{\"name\":\"X\",\"sku\":\"br-003\",\"weight\":1,\"minStock\":1,\"quantity\":0}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("SKU já cadastrado"));
        postJson(t, "/api/products", "{\"name\":\"X\",\"sku\":\"BR-009\",\"weight\":101,\"minStock\":1,\"quantity\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.weight").exists());
        postJson(t, "/api/products", "{\"name\":\"X\",\"sku\":\"BR-010\",\"weight\":10,\"minStock\":1,\"quantity\":-1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.quantity").exists());
    }

    @Test
    void vinculosComEventos() throws Exception {
        String t = signup("a@empresa.com");
        String expo = createEvent(t, "Expo", "ATIVO");
        String summit = createEvent(t, "Summit", "ATIVO");
        String todos = createProduct(t, "Caneca", "BR-003", 20, 1, 5);
        getJson(t, "/api/products").andExpect(jsonPath("$[0].eventIds", containsInAnyOrder(expo, summit)));

        String so = id(postJson(t, "/api/products", """
                {"name":"Mochila","sku":"BR-006","weight":5,"minStock":1,"quantity":3,"eventIds":["%s"]}
                """.formatted(summit)).andExpect(status().isCreated()));
        getJson(t, "/api/products").andExpect(jsonPath("$[1].eventIds", hasSize(1)));
        getJson(t, "/api/events/" + expo + "/draw-pool").andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productId").value(todos));
        getJson(t, "/api/events/" + summit + "/draw-pool").andExpect(jsonPath("$.items", hasSize(2)));

        String outro = signup("b@empresa.com");
        String eventoAlheio = createEvent(outro, "Alheio", "ATIVO");
        putJson(t, "/api/products/" + so, """
                {"name":"Mochila","sku":"BR-006","weight":5,"minStock":1,"quantity":3,"eventIds":["%s"]}
                """.formatted(eventoAlheio))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Evento não encontrado"));
    }

    @Test
    void produtoExcluidoSaiDoCatalogoMasPermaneceNosLeads() throws Exception {
        String t = signup("a@empresa.com");
        String e = createEvent(t, "Expo", "ATIVO");
        String p = createProduct(t, "Smartwatch", "BR-007", 2, 2, 1);
        draw(t, e, "Marina Costa", "12345678901").andExpect(status().isCreated());

        deleteReq(t, "/api/products/" + p).andExpect(status().isNoContent());
        getJson(t, "/api/products").andExpect(jsonPath("$", hasSize(0)));
        getJson(t, "/api/events/" + e + "/draw-pool").andExpect(jsonPath("$.items", hasSize(0)));
        getJson(t, "/api/leads").andExpect(jsonPath("$.content[0].productName").value("Smartwatch"));
    }
}
