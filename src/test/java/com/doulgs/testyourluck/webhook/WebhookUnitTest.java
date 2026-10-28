package com.doulgs.testyourluck.webhook;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebhookUnitTest {

    @Test
    void renderSubstituiEscapaEAusenteViraVazio() {
        String out = TemplateRenderer.render("{\"n\":\"{{ lead.nome }}\",\"x\":\"{{nada}}\",\"e\":\"{{campo.empresa}}\"}",
                Map.of("lead.nome", "Ana \"A\"", "campo.empresa", "C:\\dir"));
        assertThat(out).isEqualTo("{\"n\":\"Ana \\\"A\\\"\",\"x\":\"\",\"e\":\"C:\\\\dir\"}");
    }

    @Test
    void guardaRecusaHttpELoopback() throws Exception {
        WebhookTargetGuard guard = new WebhookTargetGuard(false);
        assertThatThrownBy(() -> guard.check("http://api.exemplo.com")).hasMessage("URL inválida ou não permitida");
        assertThatThrownBy(() -> guard.check("https://127.0.0.1/hook")).hasMessage("URL inválida ou não permitida");
        assertThatThrownBy(() -> guard.check("https://10.0.0.5/hook")).hasMessage("URL inválida ou não permitida");
        assertThat(WebhookTargetGuard.forbidden(InetAddress.getByName("192.168.1.10"))).isTrue();
        assertThat(WebhookTargetGuard.forbidden(InetAddress.getByName("169.254.169.254"))).isTrue();
        assertThat(WebhookTargetGuard.forbidden(InetAddress.getByName("fd00::1"))).isTrue();
        assertThat(WebhookTargetGuard.forbidden(InetAddress.getByName("8.8.8.8"))).isFalse();
    }
}
