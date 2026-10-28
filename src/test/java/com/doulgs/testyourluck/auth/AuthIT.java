package com.doulgs.testyourluck.auth;

import com.doulgs.testyourluck.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIT extends IntegrationTest {

    @Test
    void cadastroCriaTenantEUsuarioProprietario() throws Exception {
        String token = signup("ana@grupoirrah.com");
        getJson(token, "/api/me")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@grupoirrah.com"))
                .andExpect(jsonPath("$.company").value("Empresa ana@grupoirrah.com"))
                .andExpect(jsonPath("$.initials").value("AR"));
        var user = jdbc.queryForMap("select id, tenant, tenant_owner from users");
        var tenant = jdbc.queryForMap("select id, tenant, tenant_owner from tenants");
        assertThat(user.get("tenant")).isEqualTo(tenant.get("id")).isEqualTo(tenant.get("tenant"));
        assertThat(user.get("tenant_owner")).isEqualTo(user.get("id")).isEqualTo(tenant.get("tenant_owner"));
    }

    @Test
    void emailDuplicadoRetorna409() throws Exception {
        signup("ana@grupoirrah.com");
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Outra","company":"X","email":"ANA@grupoirrah.com","password":"12345678"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("E-mail já cadastrado"));
        assertThat(jdbc.queryForObject("select count(*) from tenants", Long.class)).isEqualTo(1);
    }

    @Test
    void cadastroInvalidoRetornaErrosPorCampo() throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"","company":"X","email":"invalido","password":"123"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").value("E-mail inválido"))
                .andExpect(jsonPath("$.errors.password").value("A senha deve ter no mínimo 8 caracteres"));
    }

    @Test
    void loginValidoEInvalido() throws Exception {
        signup("ana@grupoirrah.com");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@grupoirrah.com\",\"password\":\"12345678\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.name").value("Ana Ribeiro"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@grupoirrah.com\",\"password\":\"errada123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nao@existe.com\",\"password\":\"12345678\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos"));
    }

    @Test
    void endpointsProtegidosExigemToken() throws Exception {
        mvc.perform(get("/api/events")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").header("Authorization", "Bearer token.invalido")).andExpect(status().isUnauthorized());
    }

    @Test
    void healthEDocumentacaoSaoPublicos() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}
